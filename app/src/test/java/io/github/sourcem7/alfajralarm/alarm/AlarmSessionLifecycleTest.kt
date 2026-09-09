package io.github.sourcem7.alfajralarm.alarm

import io.github.sourcem7.alfajralarm.calculation.AdhanFajrCalculator
import io.github.sourcem7.alfajralarm.domain.AlarmDelivery
import io.github.sourcem7.alfajralarm.domain.AlarmKind
import io.github.sourcem7.alfajralarm.domain.AlarmOutcome
import io.github.sourcem7.alfajralarm.domain.AlarmPreferences
import io.github.sourcem7.alfajralarm.domain.AlarmRequest
import io.github.sourcem7.alfajralarm.domain.CapabilityProblem
import io.github.sourcem7.alfajralarm.domain.DisabledReason
import io.github.sourcem7.alfajralarm.domain.MAX_SNOOZE_COUNT
import io.github.sourcem7.alfajralarm.domain.NextOccurrenceSelector
import io.github.sourcem7.alfajralarm.domain.PreferencesProvider
import io.github.sourcem7.alfajralarm.domain.ScheduleReason
import io.github.sourcem7.alfajralarm.domain.ScheduleResult
import io.github.sourcem7.alfajralarm.domain.SessionIdFactory
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Ringing-session ownership and the degraded-capability contract. Phase 3 drives
 * all of this from the ringing service, so the rules have to hold before it exists.
 */
class AlarmSessionLifecycleTest {
    private val calculator = AdhanFajrCalculator()
    private val today = LocalDate(2026, 3, 10)
    private val tomorrow = today.plus(1, DateTimeUnit.DAY)
    private val todaysAlarm = calculator.calculate(today, testPreferences()).alarmInstant
    private val tomorrowsAlarm = calculator.calculate(tomorrow, testPreferences()).alarmInstant

    private val stateStore = FakeAlarmStateStore()
    private val gateway = RecordingExactAlarmGateway()
    private var capabilities = healthyCapabilities()
    private var preferences = testPreferences()
    private var now: Instant = todaysAlarm - 1.minutes
    private var sessionCount = 0

    private val coordinator = AlarmSchedulingCoordinator(
        preferences = PreferencesProvider { preferences },
        stateStore = stateStore,
        gateway = gateway,
        capabilities = { capabilities },
        selector = NextOccurrenceSelector(calculator),
        sessionIds = SessionIdFactory { "session-${++sessionCount}" },
        deviceZoneId = { DAMASCUS.zoneId },
        clock = { now },
    )

    private suspend fun deliverDailyAlarm(): AlarmDelivery.Ring {
        coordinator.enableDaily()
        now = todaysAlarm
        return coordinator.onAlarmDelivered(
            AlarmRequest(AlarmKind.DAILY, todaysAlarm.toEpochMilliseconds(), prayerDate = today),
        ) as AlarmDelivery.Ring
    }

    @Test fun `the test alarm is refused when notifications are disabled`() = runBlocking {
        // Without a notification there is no foreground surface, no controls,
        // and no screen, so a test alarm would prove nothing.
        capabilities = healthyCapabilities().copy(notificationsEnabled = false)

        val result = coordinator.scheduleTest()

        assertEquals(ScheduleResult.ActionRequired(CapabilityProblem.NOTIFICATIONS_DISABLED), result)
        assertNull(gateway.registered[AlarmKind.TEST])
        assertNull(stateStore.current().testSessionId)
    }

    @Test fun `the test alarm is refused when exact alarms are unavailable`() = runBlocking {
        capabilities = healthyCapabilities().copy(canScheduleExactAlarms = false)

        val result = coordinator.scheduleTest()

        assertEquals(ScheduleResult.ActionRequired(CapabilityProblem.EXACT_ALARMS_UNAVAILABLE), result)
        assertNull(gateway.registered[AlarmKind.TEST])
    }

    @Test fun `the test alarm reports a missing full-screen permission without refusing`() = runBlocking {
        // The test is how a user discovers the permission is missing, so it must
        // still ring, and must not claim it will behave like the real alarm.
        capabilities = healthyCapabilities().copy(canUseFullScreenIntent = false)

        val result = coordinator.scheduleTest()

        val scheduled = result as ScheduleResult.TemporaryScheduled
        assertEquals(AlarmKind.TEST, scheduled.kind)
        assertEquals(listOf(CapabilityProblem.FULL_SCREEN_UNAVAILABLE), scheduled.degradedBy)
        assertNotNull(stateStore.current().testSessionId)
    }

    @Test fun `the test alarm is offered before a location has been chosen`() = runBlocking {
        // Onboarding offers the test before setup is complete, and it does not
        // depend on a location or a calculation method.
        preferences = AlarmPreferences()

        val result = coordinator.scheduleTest()

        val scheduled = result as ScheduleResult.TemporaryScheduled
        assertTrue(scheduled.degradedBy.isEmpty())
    }

    @Test fun `a snooze scheduled by a test session rings as a test`() = runBlocking {
        coordinator.scheduleTest()
        val testSession = checkNotNull(stateStore.current().testSessionId)

        coordinator.scheduleSnooze(testSession, 5)
        val delivery = coordinator.onAlarmDelivered(
            AlarmRequest(AlarmKind.SNOOZE, gateway.registered.getValue(AlarmKind.SNOOZE), sessionId = testSession),
        )

        assertTrue(delivery is AlarmDelivery.Ring)
        assertTrue((delivery as AlarmDelivery.Ring).isTest)
        assertEquals(testSession, delivery.sessionId)
    }

    @Test fun `a test snooze neither consumes nor is limited by the daily three`() = runBlocking {
        val ring = deliverDailyAlarm()
        repeat(MAX_SNOOZE_COUNT) { coordinator.scheduleSnooze(ring.sessionId, 5) }
        assertEquals(MAX_SNOOZE_COUNT, stateStore.current().snoozeCount)

        coordinator.scheduleTest()
        val testSession = checkNotNull(stateStore.current().testSessionId)

        // The exhausted daily count must not block the test session.
        assertTrue(coordinator.scheduleSnooze(testSession, 5) is ScheduleResult.TemporaryScheduled)
        assertEquals(1, stateStore.current().testSnoozeCount)
        assertEquals(MAX_SNOOZE_COUNT, stateStore.current().snoozeCount)

        // The test session has its own limit rather than none at all.
        repeat(MAX_SNOOZE_COUNT - 1) { coordinator.scheduleSnooze(testSession, 5) }
        assertEquals(
            ScheduleResult.Disabled(DisabledReason.SNOOZE_LIMIT_REACHED),
            coordinator.scheduleSnooze(testSession, 5),
        )
    }

    @Test fun `recording SNOOZED keeps the session so the snooze can still ring`() = runBlocking {
        val ring = deliverDailyAlarm()
        coordinator.scheduleSnooze(ring.sessionId, 5)

        coordinator.recordOutcome(ring.sessionId, AlarmOutcome.SNOOZED)

        assertEquals(ring.sessionId, stateStore.current().ringingSessionId)
        assertEquals(1, stateStore.current().snoozeCount)
        val delivery = coordinator.onAlarmDelivered(
            AlarmRequest(AlarmKind.SNOOZE, gateway.registered.getValue(AlarmKind.SNOOZE), sessionId = ring.sessionId),
        )
        assertTrue(delivery is AlarmDelivery.Ring)
    }

    @Test fun `a terminal outcome ends the session and withdraws a pending snooze`() = runBlocking {
        val ring = deliverDailyAlarm()
        coordinator.scheduleSnooze(ring.sessionId, 5)

        coordinator.recordOutcome(ring.sessionId, AlarmOutcome.DISMISSED)

        assertEquals(AlarmOutcome.DISMISSED, stateStore.current().lastOutcome)
        assertNull(stateStore.current().ringingSessionId)
        assertEquals(0, stateStore.current().snoozeCount)
        assertNull("the pending snooze is cancelled", gateway.registered[AlarmKind.SNOOZE])
        assertEquals(tomorrowsAlarm.toEpochMilliseconds(), gateway.registered[AlarmKind.DAILY])
    }

    @Test fun `a terminal test outcome closes the test session without touching daily state`() = runBlocking {
        coordinator.enableDaily()
        coordinator.scheduleTest()
        val testSession = checkNotNull(stateStore.current().testSessionId)
        coordinator.scheduleSnooze(testSession, 5)

        coordinator.recordOutcome(testSession, AlarmOutcome.DISMISSED)

        assertNull(stateStore.current().testSessionId)
        assertEquals(0, stateStore.current().testSnoozeCount)
        assertNull(stateStore.current().lastOutcome)
        assertEquals(todaysAlarm.toEpochMilliseconds(), gateway.registered[AlarmKind.DAILY])
    }

    @Test fun `a revoked full-screen permission keeps the alarm scheduled but degraded`() = runBlocking {
        coordinator.enableDaily()
        capabilities = healthyCapabilities().copy(canUseFullScreenIntent = false)

        val result = coordinator.scheduleNext(ScheduleReason.AppOpened)

        assertTrue(result is ScheduleResult.Scheduled)
        result as ScheduleResult.Scheduled
        assertTrue(result.isDegraded)
        assertEquals(listOf(CapabilityProblem.FULL_SCREEN_UNAVAILABLE), result.degradedBy)
        assertEquals(todaysAlarm.toEpochMilliseconds(), gateway.registered[AlarmKind.DAILY])
        assertTrue(stateStore.current().claimsActiveAlarm)
    }

    @Test fun `revoked notifications keep the alarm scheduled but degraded`() = runBlocking {
        coordinator.enableDaily()
        capabilities = healthyCapabilities().copy(notificationsEnabled = false)

        val result = coordinator.scheduleNext(ScheduleReason.BootCompleted)

        assertTrue(result is ScheduleResult.Scheduled)
        assertEquals(
            listOf(CapabilityProblem.NOTIFICATIONS_DISABLED),
            (result as ScheduleResult.Scheduled).degradedBy,
        )
        assertEquals(todaysAlarm.toEpochMilliseconds(), gateway.registered[AlarmKind.DAILY])
    }

    @Test fun `a degrading problem still blocks activation`() = runBlocking {
        capabilities = healthyCapabilities().copy(canUseFullScreenIntent = false)

        val result = coordinator.enableDaily()

        assertEquals(ScheduleResult.ActionRequired(CapabilityProblem.FULL_SCREEN_UNAVAILABLE), result)
        assertTrue(gateway.registered.isEmpty())
        assertFalse(stateStore.current().claimsActiveAlarm)
    }

    @Test fun `a healthy reschedule reports no degradation`() = runBlocking {
        val result = coordinator.enableDaily()

        assertTrue(result is ScheduleResult.Scheduled)
        assertFalse((result as ScheduleResult.Scheduled).isDegraded)
    }

    @Test fun `a skipped date is dropped once it is behind the selected zone's date`() = runBlocking {
        coordinator.enableDaily()
        coordinator.skipNext()
        assertEquals(today, stateStore.current().skippedPrayerDate)

        // Still the skipped day: undo must remain possible.
        now = todaysAlarm + 1.minutes
        coordinator.scheduleNext(ScheduleReason.AppOpened)
        assertEquals(today, stateStore.current().skippedPrayerDate)

        // The day has passed, so the skip is spent.
        now = todaysAlarm + 2.days
        coordinator.scheduleNext(ScheduleReason.AppOpened)
        assertNull(stateStore.current().skippedPrayerDate)
    }
}
