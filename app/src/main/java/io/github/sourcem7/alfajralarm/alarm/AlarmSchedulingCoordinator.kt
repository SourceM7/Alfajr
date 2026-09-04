package io.github.sourcem7.alfajralarm.alarm

import io.github.sourcem7.alfajralarm.domain.AlarmDelivery
import io.github.sourcem7.alfajralarm.domain.AlarmHealth
import io.github.sourcem7.alfajralarm.domain.AlarmKind
import io.github.sourcem7.alfajralarm.domain.AlarmOutcome
import io.github.sourcem7.alfajralarm.domain.AlarmPreferences
import io.github.sourcem7.alfajralarm.domain.AlarmRequest
import io.github.sourcem7.alfajralarm.domain.AlarmScheduler
import io.github.sourcem7.alfajralarm.domain.AlarmState
import io.github.sourcem7.alfajralarm.domain.AlarmStateStore
import io.github.sourcem7.alfajralarm.domain.CapabilityProbe
import io.github.sourcem7.alfajralarm.domain.CapabilityProblem
import io.github.sourcem7.alfajralarm.domain.DisabledReason
import io.github.sourcem7.alfajralarm.domain.ExactAlarmGateway
import io.github.sourcem7.alfajralarm.domain.MAX_SNOOZE_COUNT
import io.github.sourcem7.alfajralarm.domain.NextOccurrenceSelector
import io.github.sourcem7.alfajralarm.domain.PreferenceError
import io.github.sourcem7.alfajralarm.domain.PreferenceValidation
import io.github.sourcem7.alfajralarm.domain.PreferencesProvider
import io.github.sourcem7.alfajralarm.domain.ScheduleReason
import io.github.sourcem7.alfajralarm.domain.ScheduleResult
import io.github.sourcem7.alfajralarm.domain.SessionIdFactory
import io.github.sourcem7.alfajralarm.domain.SessionKind
import io.github.sourcem7.alfajralarm.domain.evaluateAlarmHealth
import io.github.sourcem7.alfajralarm.domain.sessionKind
import io.github.sourcem7.alfajralarm.domain.validateForPreview
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * The single writer for everything that can change what AlarmManager holds.
 * Android delivers receiver intents concurrently and may redeliver them, so
 * every transition serializes through one mutex and is idempotent.
 */
class AlarmSchedulingCoordinator(
    private val preferences: PreferencesProvider,
    private val stateStore: AlarmStateStore,
    private val gateway: ExactAlarmGateway,
    private val capabilities: CapabilityProbe,
    private val selector: NextOccurrenceSelector,
    private val sessionIds: SessionIdFactory = SessionIdFactory { UUID.randomUUID().toString() },
    private val deviceZoneId: () -> String = { TimeZone.currentSystemDefault().id },
    private val clock: () -> Instant = { Instant.fromEpochMilliseconds(System.currentTimeMillis()) },
) : AlarmScheduler {
    private val mutex = Mutex()

    override suspend fun enableDaily(): ScheduleResult = mutex.withLock {
        val loaded = preferences.load()
        configurationProblem(loaded)?.let { return@withLock ScheduleResult.InvalidConfiguration(it) }
        blockingProblem(loaded)?.let { return@withLock ScheduleResult.ActionRequired(it) }
        stateStore.update { it.copy(dailyEnabled = true, activationConfirmed = true) }
        scheduleDaily(loaded, stateStore.current(), clock())
    }

    override suspend fun disableDaily(): ScheduleResult = mutex.withLock {
        gateway.cancel(AlarmKind.DAILY)
        gateway.cancel(AlarmKind.SNOOZE)
        gateway.cancel(AlarmKind.TEST)
        stateStore.update {
            it.copy(
                dailyEnabled = false,
                nextPrayerDate = null,
                nextAlarmEpochMillis = null,
                skippedPrayerDate = null,
                ringingSessionId = null,
                snoozeCount = 0,
                snoozeAlarmEpochMillis = null,
                testSessionId = null,
                testSnoozeCount = 0,
                testAlarmEpochMillis = null,
            )
        }
        ScheduleResult.Disabled(DisabledReason.USER_DISABLED)
    }

    override suspend fun scheduleNext(reason: ScheduleReason): ScheduleResult = mutex.withLock {
        scheduleNextLocked(clock())
    }

    override suspend fun skipNext(): ScheduleResult = mutex.withLock {
        val state = stateStore.current()
        if (!state.dailyEnabled) return@withLock ScheduleResult.Disabled(DisabledReason.NOT_ENABLED)
        // Only one skip is active at a time. Overwriting an existing skip with
        // the newly selected date would un-exclude the first skipped date and
        // reselect it, so a repeated skip only refreshes the schedule.
        if (state.skippedPrayerDate != null) return@withLock scheduleNextLocked(clock())
        val now = clock()
        val skipped = state.nextPrayerDate ?: return@withLock scheduleNextLocked(now)
        stateStore.update {
            it.copy(
                skippedPrayerDate = skipped,
                lastOutcome = AlarmOutcome.SKIPPED,
                lastOutcomeEpochMillis = now.toEpochMilliseconds(),
            )
        }
        scheduleNextLocked(now)
    }

    override suspend fun undoSkip(): ScheduleResult = mutex.withLock {
        stateStore.update { current ->
            val clearsOutcome = current.lastOutcome == AlarmOutcome.SKIPPED
            current.copy(
                skippedPrayerDate = null,
                lastOutcome = if (clearsOutcome) null else current.lastOutcome,
                lastOutcomeEpochMillis = if (clearsOutcome) null else current.lastOutcomeEpochMillis,
            )
        }
        scheduleNextLocked(clock())
    }

    override suspend fun scheduleSnooze(sessionId: String, minutes: Int): ScheduleResult = mutex.withLock {
        val state = stateStore.current()
        val session = state.sessionKind(sessionId) ?: return@withLock ScheduleResult.Disabled(DisabledReason.STALE_SESSION)
        if (minutes !in MIN_SNOOZE_MINUTES..MAX_SNOOZE_MINUTES) {
            return@withLock ScheduleResult.Disabled(DisabledReason.INVALID_SNOOZE_DURATION)
        }
        // Each session counts its own snoozes so a test alarm neither consumes
        // nor is limited by the daily session's three.
        val used = if (session == SessionKind.TEST) state.testSnoozeCount else state.snoozeCount
        if (used >= MAX_SNOOZE_COUNT) return@withLock ScheduleResult.Disabled(DisabledReason.SNOOZE_LIMIT_REACHED)
        val now = clock()
        val trigger = (now + minutes.minutes).toEpochMilliseconds()
        val request = AlarmRequest(kind = AlarmKind.SNOOZE, triggerAtMillis = trigger, sessionId = sessionId)
        if (!gateway.scheduleAlarmClock(request)) {
            return@withLock ScheduleResult.ActionRequired(CapabilityProblem.SCHEDULING_FAILED)
        }
        stateStore.update {
            when (session) {
                // A test session must never mutate the daily count or outcome.
                SessionKind.TEST -> it.copy(
                    testSnoozeCount = it.testSnoozeCount + 1,
                    snoozeAlarmEpochMillis = trigger,
                )
                SessionKind.RINGING -> it.copy(
                    snoozeCount = it.snoozeCount + 1,
                    snoozeAlarmEpochMillis = trigger,
                    lastOutcome = AlarmOutcome.SNOOZED,
                    lastOutcomeEpochMillis = now.toEpochMilliseconds(),
                )
            }
        }
        ScheduleResult.TemporaryScheduled(AlarmKind.SNOOZE, trigger)
    }

    override suspend fun scheduleTest(delay: Duration): ScheduleResult = mutex.withLock {
        val sessionId = sessionIds.next()
        val trigger = (clock() + delay).toEpochMilliseconds()
        val request = AlarmRequest(kind = AlarmKind.TEST, triggerAtMillis = trigger, sessionId = sessionId)
        // An exact-and-allow-while-idle alarm keeps Android's next-alarm-clock
        // indicator pointing at the real daily Fajr alarm.
        if (!gateway.scheduleExactWhileIdle(request)) {
            return@withLock ScheduleResult.ActionRequired(CapabilityProblem.SCHEDULING_FAILED)
        }
        stateStore.update {
            it.copy(
                testSessionId = sessionId,
                testSnoozeCount = 0,
                testAlarmEpochMillis = trigger,
            )
        }
        ScheduleResult.TemporaryScheduled(AlarmKind.TEST, trigger)
    }

    override suspend fun onAlarmDelivered(request: AlarmRequest): AlarmDelivery = mutex.withLock {
        val state = stateStore.current()
        val now = clock()
        when (request.kind) {
            AlarmKind.DAILY -> {
                if (!state.dailyEnabled || state.nextAlarmEpochMillis != request.triggerAtMillis) {
                    return@withLock AlarmDelivery.Ignored
                }
                val sessionId = sessionIds.next()
                stateStore.update {
                    it.copy(
                        ringingSessionId = sessionId,
                        snoozeCount = 0,
                        nextPrayerDate = null,
                        nextAlarmEpochMillis = null,
                        lastDeliveryEpochMillis = now.toEpochMilliseconds(),
                    )
                }
                // Secure the following daily alarm before ringing starts. The
                // delivered instant is the floor so an early delivery cannot
                // reselect the occurrence that is ringing now.
                val floor = maxOf(now, Instant.fromEpochMilliseconds(request.triggerAtMillis))
                AlarmDelivery.Ring(
                    kind = AlarmKind.DAILY,
                    sessionId = sessionId,
                    isTest = false,
                    followingDaily = scheduleNextLocked(floor),
                )
            }

            AlarmKind.SNOOZE -> {
                // A snooze belongs to whichever session scheduled it, including
                // a test session, so the test alarm exercises the real controls.
                // The stored trigger rejects redelivered broadcasts; a null
                // stored trigger accepts, so alarms scheduled before this guard
                // existed still ring exactly once per session validation.
                val sessionId = request.sessionId
                val session = sessionId?.let(state::sessionKind) ?: return@withLock AlarmDelivery.Ignored
                val expected = state.snoozeAlarmEpochMillis
                if (expected != null && expected != request.triggerAtMillis) return@withLock AlarmDelivery.Ignored
                stateStore.update { it.copy(lastDeliveryEpochMillis = now.toEpochMilliseconds()) }
                AlarmDelivery.Ring(
                    kind = AlarmKind.SNOOZE,
                    sessionId = sessionId,
                    isTest = session == SessionKind.TEST,
                    followingDaily = null,
                )
            }

            AlarmKind.TEST -> {
                val sessionId = request.sessionId
                if (sessionId == null || sessionId != state.testSessionId) return@withLock AlarmDelivery.Ignored
                val expected = state.testAlarmEpochMillis
                if (expected != null && expected != request.triggerAtMillis) return@withLock AlarmDelivery.Ignored
                stateStore.update { it.copy(lastDeliveryEpochMillis = now.toEpochMilliseconds()) }
                AlarmDelivery.Ring(AlarmKind.TEST, sessionId, isTest = true, followingDaily = null)
            }
        }
    }

    override suspend fun recordOutcome(sessionId: String, outcome: AlarmOutcome) {
        mutex.withLock {
            val state = stateStore.current()
            val session = state.sessionKind(sessionId) ?: return@withLock
            // SNOOZED is not terminal. The session has to survive it, or the
            // snooze that was just scheduled would be rejected on delivery and
            // the three-snooze limit would restart from zero.
            val isTerminal = outcome != AlarmOutcome.SNOOZED
            // Nothing should ring after a terminal outcome, so a snooze that is
            // still pending for this session is withdrawn.
            if (isTerminal) gateway.cancel(AlarmKind.SNOOZE)
            stateStore.update {
                when (session) {
                    // A test session never changes the daily outcome or counters.
                    SessionKind.TEST -> if (isTerminal) {
                        it.copy(
                            testSessionId = null,
                            testSnoozeCount = 0,
                            testAlarmEpochMillis = null,
                            snoozeAlarmEpochMillis = null,
                        )
                    } else {
                        it
                    }
                    SessionKind.RINGING -> it.copy(
                        lastOutcome = outcome,
                        lastOutcomeEpochMillis = clock().toEpochMilliseconds(),
                        ringingSessionId = if (isTerminal) null else it.ringingSessionId,
                        snoozeCount = if (isTerminal) 0 else it.snoozeCount,
                        snoozeAlarmEpochMillis = if (isTerminal) null else it.snoozeAlarmEpochMillis,
                    )
                }
            }
        }
    }

    override suspend fun cancelAll() {
        mutex.withLock { AlarmKind.entries.forEach(gateway::cancel) }
    }

    /** Must be called with the mutex held. */
    private suspend fun scheduleNextLocked(from: Instant): ScheduleResult {
        val loaded = preferences.load()
        val state = stateStore.current()
        if (!state.dailyEnabled) {
            gateway.cancel(AlarmKind.DAILY)
            clearRegisteredAlarm()
            return ScheduleResult.Disabled(DisabledReason.NOT_ENABLED)
        }
        return scheduleDaily(loaded, state, from)
    }

    /** Must be called with the mutex held. */
    private suspend fun scheduleDaily(
        loaded: AlarmPreferences,
        state: AlarmState,
        from: Instant,
    ): ScheduleResult {
        // Incomplete configuration is answered without touching AlarmManager.
        configurationProblem(loaded)?.let {
            clearRegisteredAlarm()
            return ScheduleResult.InvalidConfiguration(it)
        }
        val health = health(loaded)
        // Only a problem that makes delivery impossible may unregister the
        // alarm. A revoked notification or full-screen permission leaves the
        // alarm scheduled and reports it as degraded, because Android still
        // plays alarm audio and shows its own heads-up notification.
        health.fatalProblems.firstOrNull()?.let { problem ->
            gateway.cancel(AlarmKind.DAILY)
            clearRegisteredAlarm()
            return ScheduleResult.ActionRequired(problem)
        }
        val occurrence = runCatching {
            selector.selectNext(from, loaded, state.skippedPrayerDate)
        }.getOrElse { error ->
            clearRegisteredAlarm()
            // Only a zone failure means the time zone is wrong. Range and
            // search failures are scheduling problems with a different fix.
            return if (error is IllegalTimeZoneException || error is java.time.DateTimeException) {
                ScheduleResult.InvalidConfiguration(PreferenceError.INVALID_TIME_ZONE)
            } else {
                ScheduleResult.ActionRequired(CapabilityProblem.SCHEDULING_FAILED)
            }
        }
        val triggerAtMillis = occurrence.alarmInstant.toEpochMilliseconds()
        // Cancel before replacing so a settings change cannot leave two alarms.
        gateway.cancel(AlarmKind.DAILY)
        val request = AlarmRequest(
            kind = AlarmKind.DAILY,
            triggerAtMillis = triggerAtMillis,
            prayerDate = occurrence.prayerLocalDate,
        )
        if (!gateway.scheduleAlarmClock(request)) {
            // Preserve preferences, but never keep claiming an active alarm.
            stateStore.update {
                it.copy(dailyEnabled = false, nextPrayerDate = null, nextAlarmEpochMillis = null)
            }
            return ScheduleResult.ActionRequired(CapabilityProblem.SCHEDULING_FAILED)
        }
        val localToday = from.toLocalDateTime(TimeZone.of(occurrence.zoneId)).date
        stateStore.update {
            it.copy(
                nextPrayerDate = occurrence.prayerLocalDate,
                nextAlarmEpochMillis = triggerAtMillis,
                // A skipped date is spent once it is behind the selected zone's
                // current date. Keeping it through its own day leaves undo
                // available for as long as it can still restore anything.
                skippedPrayerDate = it.skippedPrayerDate?.takeIf { skipped -> skipped >= localToday },
            )
        }
        return ScheduleResult.Scheduled(occurrence, degradedBy = health.degradingProblems)
    }

    private suspend fun clearRegisteredAlarm() {
        stateStore.update { it.copy(nextPrayerDate = null, nextAlarmEpochMillis = null) }
    }

    private fun configurationProblem(loaded: AlarmPreferences): PreferenceError? =
        (loaded.validateForPreview() as? PreferenceValidation.Invalid)?.reason

    private fun health(loaded: AlarmPreferences): AlarmHealth =
        evaluateAlarmHealth(loaded, capabilities.read(), deviceZoneId())

    /** Every capability problem blocks activation, degrading ones included. */
    private fun blockingProblem(loaded: AlarmPreferences): CapabilityProblem? =
        health(loaded).problems.firstOrNull { it != CapabilityProblem.CONFIGURATION_INCOMPLETE }

    companion object {
        val TEST_ALARM_DELAY: Duration = 10.seconds
        const val MIN_SNOOZE_MINUTES = 1
        const val MAX_SNOOZE_MINUTES = 120
    }
}
