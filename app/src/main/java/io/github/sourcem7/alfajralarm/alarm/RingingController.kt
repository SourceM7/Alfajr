package io.github.sourcem7.alfajralarm.alarm

import io.github.sourcem7.alfajralarm.domain.AlarmAudio
import io.github.sourcem7.alfajralarm.domain.AlarmOutcome
import io.github.sourcem7.alfajralarm.domain.AlarmScheduler
import io.github.sourcem7.alfajralarm.domain.AlarmStateStore
import io.github.sourcem7.alfajralarm.domain.AlarmVibration
import io.github.sourcem7.alfajralarm.domain.MissedAlarmNotifier
import io.github.sourcem7.alfajralarm.domain.PreferencesProvider
import io.github.sourcem7.alfajralarm.domain.RINGING_TIMEOUT
import io.github.sourcem7.alfajralarm.domain.RingingCommand
import io.github.sourcem7.alfajralarm.domain.RingingSession
import io.github.sourcem7.alfajralarm.domain.RingingSessionObserver
import io.github.sourcem7.alfajralarm.domain.RingingSurface
import io.github.sourcem7.alfajralarm.domain.RingingWakeLock
import io.github.sourcem7.alfajralarm.domain.RingtoneSource
import io.github.sourcem7.alfajralarm.domain.ScheduleResult
import io.github.sourcem7.alfajralarm.domain.SessionKind
import io.github.sourcem7.alfajralarm.domain.VolumeRamp
import io.github.sourcem7.alfajralarm.domain.sessionKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * Owns one ringing alarm at a time. The foreground service supplies the Android
 * output devices and the timer; every decision about what rings, for how long,
 * and what it records lives here so it can be tested without a device.
 *
 * There is exactly one instance per process, which is what makes a recreated
 * activity or a redelivered start command observe the running session rather
 * than begin a second one.
 */
class RingingController(
    private val scheduler: AlarmScheduler,
    private val stateStore: AlarmStateStore,
    private val preferences: PreferencesProvider,
    private val audio: AlarmAudio,
    private val vibration: AlarmVibration,
    private val wakeLock: RingingWakeLock,
    private val missedNotifier: MissedAlarmNotifier,
    private val surface: RingingSurface,
    private val clock: () -> Instant = { Instant.fromEpochMilliseconds(System.currentTimeMillis()) },
) : RingingSessionObserver {
    private val mutex = Mutex()
    private val current = MutableStateFlow<RingingSession?>(null)
    private val claimed = MutableStateFlow<String?>(null)

    /** The running session, or null when nothing is ringing. */
    override val session: StateFlow<RingingSession?> = current.asStateFlow()

    /** The session a start command has claimed but not yet begun. */
    override val startingSessionId: StateFlow<String?> = claimed.asStateFlow()

    /**
     * Announces a session the service is about to start, before any suspending
     * work. The service calls this before it posts the foreground notification,
     * so by the time Android can act on that notification's full-screen intent
     * the ringing screen already has a session to name. Without it the screen
     * opens against a null session and shows nothing.
     */
    fun claim(sessionId: String) {
        claimed.value = sessionId
    }

    /**
     * Retires a claim, but only if it still names [sessionId]. A delivery that
     * is dropped as stale must not cancel the claim of the alarm that is
     * genuinely starting.
     */
    private fun releaseClaim(sessionId: String) {
        claimed.compareAndSet(sessionId, null)
    }

    /**
     * Begins ringing for [sessionId], or returns the already-running session
     * when it is the same one. Returns null for a session the application no
     * longer owns, which is how a stale or duplicated delivery is dropped.
     */
    suspend fun start(sessionId: String, isTest: Boolean, alarmAt: Instant): RingingSession? = mutex.withLock {
        current.value?.let { running ->
            // A recreated activity, a redelivered broadcast, or a second start
            // command must never open a second player.
            releaseClaim(sessionId)
            return@withLock running.takeIf { it.sessionId == sessionId }
        }
        val state = stateStore.current()
        val kind = state.sessionKind(sessionId)
        if (kind == null) {
            releaseClaim(sessionId)
            return@withLock null
        }
        // The claimed kind has to match the stored one, so a test intent cannot
        // borrow the daily session or the reverse.
        if ((kind == SessionKind.TEST) != isTest) {
            releaseClaim(sessionId)
            return@withLock null
        }

        // Ask for the screen before the output devices, because starting audio
        // walks a fallback chain of blocking prepare() calls and the screen must
        // not queue behind it. This is also the only place that knows a session
        // is genuinely starting: the guards above have already dropped every
        // stale or duplicated delivery, so the screen is asked for exactly once
        // per session and never for one that will not ring.
        surface.show(sessionId)

        val loaded = preferences.load()
        wakeLock.acquire()
        val source = RingtoneSource.chainFor(loaded.ringtoneUri)
            .firstOrNull { candidate -> audio.start(candidate, loaded.ringtoneUri) }
        audio.setVolume(VolumeRamp.scalarAt(Duration.ZERO))
        if (loaded.vibrationEnabled) vibration.start()

        val started = RingingSession(
            sessionId = sessionId,
            isTest = isTest,
            alarmAt = alarmAt,
            startedAt = clock(),
            snoozesUsed = if (kind == SessionKind.TEST) state.testSnoozeCount else state.snoozeCount,
            snoozeMinutes = loaded.snoozeMinutes,
            tapToDismiss = loaded.tapToDismiss,
            vibrating = loaded.vibrationEnabled,
            ringtoneSource = source,
        )
        // The session is published before the claim is retired, so the ringing
        // screen never observes both as null and never finishes itself between
        // the two writes.
        current.value = started
        releaseClaim(sessionId)
        started
    }

    /**
     * Advances the volume ramp and enforces the ten-minute timeout. The service
     * calls this on a timer; nothing here depends on how often it runs.
     */
    suspend fun tick(now: Instant = clock()) {
        val running = current.value ?: return
        if (now - running.startedAt >= RINGING_TIMEOUT) {
            handle(running.sessionId, RingingCommand.TIMEOUT)
            return
        }
        val scalar = VolumeRamp.scalarAt(now - running.startedAt)
        if (scalar == running.volumeScalar) return
        audio.setVolume(scalar)
        current.update { session ->
            if (session?.sessionId == running.sessionId) session.copy(volumeScalar = scalar) else session
        }
    }

    /** Applies a command, ignoring any that names a session that is not ringing. */
    suspend fun handle(sessionId: String, command: RingingCommand) = mutex.withLock {
        val running = current.value ?: return@withLock
        if (running.sessionId != sessionId) return@withLock
        when (command) {
            RingingCommand.SNOOZE -> snooze(running)
            RingingCommand.DISMISS -> finish(running, AlarmOutcome.DISMISSED)
            RingingCommand.TIMEOUT -> finish(running, AlarmOutcome.MISSED)
        }
    }

    /**
     * Releases the output devices without recording an outcome, for the case
     * where Android tears the service down on its own. Nothing rang to a
     * conclusion, so nothing is claimed about how the alarm ended. A session ID
     * that is not the running one is ignored, so a service instance shutting
     * down can never silence the alarm that replaced it.
     */
    suspend fun abandon(sessionId: String) = mutex.withLock {
        if (current.value?.sessionId != sessionId) return@withLock
        releaseOutputs()
        current.value = null
        releaseClaim(sessionId)
    }

    private suspend fun snooze(running: RingingSession) {
        if (!running.snoozeAvailable) return
        // The coordinator owns the count and the limit; a refusal there leaves
        // the alarm ringing rather than silently ending it.
        val result = scheduler.scheduleSnooze(running.sessionId, running.snoozeMinutes)
        if (result !is ScheduleResult.TemporaryScheduled) return
        releaseOutputs()
        current.value = null
        releaseClaim(running.sessionId)
    }

    private suspend fun finish(running: RingingSession, outcome: AlarmOutcome) {
        scheduler.recordOutcome(running.sessionId, outcome)
        releaseOutputs()
        // Clearing the session first is what makes a second timeout tick, or a
        // duplicated notification action, a no-op.
        current.value = null
        releaseClaim(running.sessionId)
        // A test alarm never claims the daily Fajr alarm was missed.
        if (outcome == AlarmOutcome.MISSED && !running.isTest) missedNotifier.postMissed(running)
    }

    private fun releaseOutputs() {
        audio.stop()
        vibration.stop()
        wakeLock.release()
    }
}
