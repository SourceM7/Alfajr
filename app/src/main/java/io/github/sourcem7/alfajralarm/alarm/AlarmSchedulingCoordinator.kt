package io.github.sourcem7.alfajralarm.alarm

import io.github.sourcem7.alfajralarm.domain.AlarmDelivery
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
import io.github.sourcem7.alfajralarm.domain.evaluateAlarmHealth
import io.github.sourcem7.alfajralarm.domain.validateForPreview
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
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
        stateStore.update {
            it.copy(
                dailyEnabled = false,
                nextPrayerDate = null,
                nextAlarmEpochMillis = null,
                skippedPrayerDate = null,
                ringingSessionId = null,
                snoozeCount = 0,
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
        val isKnownSession = sessionId == state.ringingSessionId || sessionId == state.testSessionId
        if (!isKnownSession) return@withLock ScheduleResult.Disabled(DisabledReason.STALE_SESSION)
        if (state.snoozeCount >= MAX_SNOOZE_COUNT) return@withLock ScheduleResult.Disabled(DisabledReason.SNOOZE_LIMIT_REACHED)
        val now = clock()
        val trigger = (now + minutes.minutes).toEpochMilliseconds()
        val request = AlarmRequest(kind = AlarmKind.SNOOZE, triggerAtMillis = trigger, sessionId = sessionId)
        if (!gateway.scheduleAlarmClock(request)) {
            return@withLock ScheduleResult.ActionRequired(CapabilityProblem.SCHEDULING_FAILED)
        }
        // A test session must never mutate the daily snooze count or outcome.
        if (sessionId == state.ringingSessionId) {
            stateStore.update {
                it.copy(
                    snoozeCount = it.snoozeCount + 1,
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
        stateStore.update { it.copy(testSessionId = sessionId) }
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
                val sessionId = request.sessionId
                if (sessionId == null || sessionId != state.ringingSessionId) return@withLock AlarmDelivery.Ignored
                stateStore.update { it.copy(lastDeliveryEpochMillis = now.toEpochMilliseconds()) }
                AlarmDelivery.Ring(AlarmKind.SNOOZE, sessionId, isTest = false, followingDaily = null)
            }

            AlarmKind.TEST -> {
                val sessionId = request.sessionId
                if (sessionId == null || sessionId != state.testSessionId) return@withLock AlarmDelivery.Ignored
                stateStore.update { it.copy(lastDeliveryEpochMillis = now.toEpochMilliseconds()) }
                AlarmDelivery.Ring(AlarmKind.TEST, sessionId, isTest = true, followingDaily = null)
            }
        }
    }

    override suspend fun recordOutcome(sessionId: String, outcome: AlarmOutcome) {
        mutex.withLock {
            val state = stateStore.current()
            // A test session never changes the daily outcome.
            if (sessionId != state.ringingSessionId) return@withLock
            stateStore.update {
                it.copy(
                    lastOutcome = outcome,
                    lastOutcomeEpochMillis = clock().toEpochMilliseconds(),
                    ringingSessionId = null,
                    snoozeCount = 0,
                )
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
        blockingProblem(loaded)?.let { problem ->
            gateway.cancel(AlarmKind.DAILY)
            clearRegisteredAlarm()
            return ScheduleResult.ActionRequired(problem)
        }
        val occurrence = runCatching {
            selector.selectNext(from, loaded, state.skippedPrayerDate)
        }.getOrElse {
            clearRegisteredAlarm()
            return ScheduleResult.InvalidConfiguration(PreferenceError.INVALID_TIME_ZONE)
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
        stateStore.update {
            it.copy(nextPrayerDate = occurrence.prayerLocalDate, nextAlarmEpochMillis = triggerAtMillis)
        }
        return ScheduleResult.Scheduled(occurrence)
    }

    private suspend fun clearRegisteredAlarm() {
        stateStore.update { it.copy(nextPrayerDate = null, nextAlarmEpochMillis = null) }
    }

    private fun configurationProblem(loaded: AlarmPreferences): PreferenceError? =
        (loaded.validateForPreview() as? PreferenceValidation.Invalid)?.reason

    private fun blockingProblem(loaded: AlarmPreferences): CapabilityProblem? =
        evaluateAlarmHealth(loaded, capabilities.read(), deviceZoneId()).problems
            .firstOrNull { it != CapabilityProblem.CONFIGURATION_INCOMPLETE }

    companion object {
        val TEST_ALARM_DELAY: Duration = 10.seconds
    }
}
