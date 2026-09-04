package io.github.sourcem7.alfajralarm.alarm

import io.github.sourcem7.alfajralarm.calculation.AdhanFajrCalculator
import io.github.sourcem7.alfajralarm.domain.AlarmDelivery
import io.github.sourcem7.alfajralarm.domain.AlarmKind
import io.github.sourcem7.alfajralarm.domain.AlarmOutcome
import io.github.sourcem7.alfajralarm.domain.AlarmRequest
import io.github.sourcem7.alfajralarm.domain.AlarmState
import io.github.sourcem7.alfajralarm.domain.CapabilityProblem
import io.github.sourcem7.alfajralarm.domain.DisabledReason
import io.github.sourcem7.alfajralarm.domain.NextOccurrenceSelector
import io.github.sourcem7.alfajralarm.domain.PreferenceError
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
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class AlarmSchedulingCoordinatorTest {
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

    @Test fun `enabling before today's alarm registers today's occurrence`() = runBlocking {
        val result = coordinator.enableDaily()

        assertTrue(result is ScheduleResult.Scheduled)
        assertEquals(today, (result as ScheduleResult.Scheduled).occurrence.prayerLocalDate)
        assertEquals(todaysAlarm.toEpochMilliseconds(), gateway.registered[AlarmKind.DAILY])
        assertTrue(stateStore.current().claimsActiveAlarm)
    }

    @Test fun `enabling at or after today's alarm registers tomorrow`() = runBlocking {
        now = todaysAlarm

        coordinator.enableDaily()

        assertEquals(tomorrowsAlarm.toEpochMilliseconds(), gateway.registered[AlarmKind.DAILY])
        assertEquals(tomorrow, stateStore.current().nextPrayerDate)
    }

    @Test fun `a settings change replaces the daily alarm instead of duplicating it`() = runBlocking {
        coordinator.enableDaily()
        preferences = testPreferences(wake = 10)
        coordinator.scheduleNext(ScheduleReason.SettingsChanged)

        assertEquals(1, gateway.registered.size)
        assertEquals((todaysAlarm + 10.minutes).toEpochMilliseconds(), gateway.registered[AlarmKind.DAILY])
        gateway.interactions.forEachIndexed { index, interaction ->
            if (interaction.startsWith("alarmClock:${AlarmKind.DAILY}")) {
                assertEquals("cancel:${AlarmKind.DAILY}", gateway.interactions[index - 1])
            }
        }
    }

    @Test fun `missing configuration never reaches AlarmManager`() = runBlocking {
        stateStore.update { it.copy(dailyEnabled = true) }
        preferences = testPreferences(location = null)

        val result = coordinator.scheduleNext(ScheduleReason.AppOpened)

        assertEquals(ScheduleResult.InvalidConfiguration(PreferenceError.LOCATION_REQUIRED), result)
        assertEquals(emptyList<String>(), gateway.interactions)
        assertFalse(stateStore.current().claimsActiveAlarm)
    }

    @Test fun `a missing capability returns action required without claiming an active alarm`() = runBlocking {
        capabilities = healthyCapabilities().copy(canScheduleExactAlarms = false)

        val result = coordinator.enableDaily()

        assertEquals(ScheduleResult.ActionRequired(CapabilityProblem.EXACT_ALARMS_UNAVAILABLE), result)
        assertTrue(gateway.registered.isEmpty())
        assertFalse(stateStore.current().claimsActiveAlarm)
    }

    @Test fun `a capability that prevents delivery stops the active claim`() = runBlocking {
        // Only a capability Android cannot work around may unregister a
        // scheduled alarm. Notifications and full-screen access degrade it
        // instead; see AlarmSessionLifecycleTest.
        coordinator.enableDaily()
        capabilities = healthyCapabilities().copy(canScheduleExactAlarms = false)

        val result = coordinator.scheduleNext(ScheduleReason.AppOpened)

        assertEquals(ScheduleResult.ActionRequired(CapabilityProblem.EXACT_ALARMS_UNAVAILABLE), result)
        assertTrue(gateway.registered.isEmpty())
        assertFalse(stateStore.current().claimsActiveAlarm)
    }

    @Test fun `a schedule failure clears the enabled state while preserving preferences`() = runBlocking {
        gateway.scheduleSucceeds = false

        val result = coordinator.enableDaily()

        assertEquals(ScheduleResult.ActionRequired(CapabilityProblem.SCHEDULING_FAILED), result)
        assertFalse(stateStore.current().dailyEnabled)
        assertFalse(stateStore.current().claimsActiveAlarm)
        assertEquals(testPreferences(), preferences)
    }

    @Test fun `duplicate daily delivery is ignored and the following alarm is secured once`() = runBlocking {
        coordinator.enableDaily()
        now = todaysAlarm
        val delivered = AlarmRequest(AlarmKind.DAILY, todaysAlarm.toEpochMilliseconds(), prayerDate = today)

        val first = coordinator.onAlarmDelivered(delivered)
        val duplicate = coordinator.onAlarmDelivered(delivered)

        assertTrue(first is AlarmDelivery.Ring)
        assertFalse((first as AlarmDelivery.Ring).isTest)
        assertEquals(AlarmDelivery.Ignored, duplicate)
        assertEquals(1, gateway.registered.size)
        assertEquals(tomorrowsAlarm.toEpochMilliseconds(), gateway.registered[AlarmKind.DAILY])
    }

    @Test fun `a stale delivery is ignored`() = runBlocking {
        coordinator.enableDaily()

        val stale = coordinator.onAlarmDelivered(
            AlarmRequest(AlarmKind.DAILY, todaysAlarm.toEpochMilliseconds() - 60_000, prayerDate = today),
        )

        assertEquals(AlarmDelivery.Ignored, stale)
        assertEquals(todaysAlarm.toEpochMilliseconds(), gateway.registered[AlarmKind.DAILY])
    }

    @Test fun `system events choose a fresh occurrence from stale stored state`() = runBlocking {
        stateStore.update {
            it.copy(dailyEnabled = true, nextPrayerDate = LocalDate(2020, 1, 1), nextAlarmEpochMillis = 1L)
        }

        listOf(
            ScheduleReason.BootCompleted,
            ScheduleReason.ClockChanged,
            ScheduleReason.PackageReplaced,
        ).forEach { reason ->
            val result = coordinator.scheduleNext(reason)
            assertTrue("$reason should schedule", result is ScheduleResult.Scheduled)
        }

        assertEquals(todaysAlarm.toEpochMilliseconds(), gateway.registered[AlarmKind.DAILY])
        assertEquals(todaysAlarm.toEpochMilliseconds(), stateStore.current().nextAlarmEpochMillis)
        assertEquals(today, stateStore.current().nextPrayerDate)
    }

    @Test fun `skipping selects the following date and undo restores a still-future occurrence`() = runBlocking {
        coordinator.enableDaily()

        coordinator.skipNext()
        assertEquals(tomorrowsAlarm.toEpochMilliseconds(), gateway.registered[AlarmKind.DAILY])
        assertEquals(today, stateStore.current().skippedPrayerDate)
        assertEquals(AlarmOutcome.SKIPPED, stateStore.current().lastOutcome)

        coordinator.undoSkip()
        assertEquals(todaysAlarm.toEpochMilliseconds(), gateway.registered[AlarmKind.DAILY])
        assertNull(stateStore.current().skippedPrayerDate)
        assertNull(stateStore.current().lastOutcome)
    }

    @Test fun `undoing a skip after the occurrence passed keeps the following date`() = runBlocking {
        coordinator.enableDaily()
        coordinator.skipNext()
        now = todaysAlarm + 1.minutes

        coordinator.undoSkip()

        assertEquals(tomorrowsAlarm.toEpochMilliseconds(), gateway.registered[AlarmKind.DAILY])
    }

    @Test fun `a repeated skip keeps the first skipped date excluded`() = runBlocking {
        coordinator.enableDaily()
        coordinator.skipNext()
        assertEquals(today, stateStore.current().skippedPrayerDate)

        coordinator.skipNext()

        assertEquals(today, stateStore.current().skippedPrayerDate)
        assertEquals(tomorrowsAlarm.toEpochMilliseconds(), gateway.registered[AlarmKind.DAILY])
        assertEquals(tomorrow, stateStore.current().nextPrayerDate)
    }

    @Test fun `disabling cancels a scheduled test alarm and clears its session`() = runBlocking {
        coordinator.enableDaily()
        coordinator.scheduleTest()
        assertTrue(gateway.registered.containsKey(AlarmKind.TEST))

        val result = coordinator.disableDaily()

        assertEquals(ScheduleResult.Disabled(DisabledReason.USER_DISABLED), result)
        assertTrue(gateway.registered.isEmpty())
        assertNull(stateStore.current().testSessionId)
        assertNull(stateStore.current().testAlarmEpochMillis)
        assertNull(stateStore.current().snoozeAlarmEpochMillis)
    }

    @Test fun `a redelivered snooze with a stale trigger is ignored`() = runBlocking {
        coordinator.enableDaily()
        stateStore.update { it.copy(ringingSessionId = "ringing") }
        coordinator.scheduleSnooze("ringing", 5)
        val trigger = gateway.registered.getValue(AlarmKind.SNOOZE)

        val first = coordinator.onAlarmDelivered(AlarmRequest(AlarmKind.SNOOZE, trigger, sessionId = "ringing"))
        val redelivery = coordinator.onAlarmDelivered(
            AlarmRequest(AlarmKind.SNOOZE, trigger - 60_000, sessionId = "ringing"),
        )

        assertTrue(first is AlarmDelivery.Ring)
        assertEquals(AlarmDelivery.Ignored, redelivery)
    }

    @Test fun `a redelivered test alarm with a stale trigger is ignored`() = runBlocking {
        coordinator.enableDaily()
        coordinator.scheduleTest()
        val sessionId = checkNotNull(stateStore.current().testSessionId)
        val trigger = gateway.registered.getValue(AlarmKind.TEST)

        val first = coordinator.onAlarmDelivered(AlarmRequest(AlarmKind.TEST, trigger, sessionId = sessionId))
        val redelivery = coordinator.onAlarmDelivered(
            AlarmRequest(AlarmKind.TEST, trigger - 60_000, sessionId = sessionId),
        )

        assertTrue(first is AlarmDelivery.Ring)
        assertEquals(AlarmDelivery.Ignored, redelivery)
    }

    @Test fun `a snooze with a non-positive duration is rejected`() = runBlocking {
        coordinator.enableDaily()
        stateStore.update { it.copy(ringingSessionId = "ringing") }

        assertEquals(
            ScheduleResult.Disabled(DisabledReason.INVALID_SNOOZE_DURATION),
            coordinator.scheduleSnooze("ringing", 0),
        )
        assertEquals(
            ScheduleResult.Disabled(DisabledReason.INVALID_SNOOZE_DURATION),
            coordinator.scheduleSnooze("ringing", -5),
        )
        assertNull(gateway.registered[AlarmKind.SNOOZE])
    }

    @Test fun `an out-of-range correction reports scheduling failure instead of a bad time zone`() = runBlocking {
        stateStore.update { it.copy(dailyEnabled = true) }
        preferences = testPreferences(correction = 31)

        val result = coordinator.scheduleNext(ScheduleReason.AppOpened)

        assertEquals(ScheduleResult.ActionRequired(CapabilityProblem.SCHEDULING_FAILED), result)
        assertFalse(stateStore.current().claimsActiveAlarm)
    }

    @Test fun `disabling cancels the daily alarm and drops the active claim`() = runBlocking {
        coordinator.enableDaily()

        val result = coordinator.disableDaily()

        assertEquals(ScheduleResult.Disabled(DisabledReason.USER_DISABLED), result)
        assertTrue(gateway.registered.isEmpty())
        assertFalse(stateStore.current().dailyEnabled)
        assertFalse(stateStore.current().claimsActiveAlarm)
    }

    @Test fun `snooze counts one through three and the fourth snooze is impossible`() = runBlocking {
        coordinator.enableDaily()
        stateStore.update { it.copy(ringingSessionId = "ringing") }

        repeat(3) { index ->
            val result = coordinator.scheduleSnooze("ringing", 5)
            assertTrue(result is ScheduleResult.TemporaryScheduled)
            assertEquals(index + 1, stateStore.current().snoozeCount)
        }

        assertEquals(
            ScheduleResult.Disabled(DisabledReason.SNOOZE_LIMIT_REACHED),
            coordinator.scheduleSnooze("ringing", 5),
        )
        assertEquals(todaysAlarm.toEpochMilliseconds(), gateway.registered[AlarmKind.DAILY])
    }

    @Test fun `a stale session cannot snooze`() = runBlocking {
        coordinator.enableDaily()
        stateStore.update { it.copy(ringingSessionId = "ringing") }

        assertEquals(
            ScheduleResult.Disabled(DisabledReason.STALE_SESSION),
            coordinator.scheduleSnooze("someone-else", 5),
        )
        assertNull(gateway.registered[AlarmKind.SNOOZE])
    }

    @Test fun `a test alarm does not mutate daily occurrence skip or outcome state`() = runBlocking {
        coordinator.enableDaily()
        coordinator.skipNext()
        val before: AlarmState = stateStore.current()

        val result = coordinator.scheduleTest()

        val after = stateStore.current()
        assertTrue(result is ScheduleResult.TemporaryScheduled)
        assertNotNull(after.testSessionId)
        assertEquals(
            before.copy(testSessionId = after.testSessionId, testAlarmEpochMillis = after.testAlarmEpochMillis),
            after,
        )
        assertEquals(tomorrowsAlarm.toEpochMilliseconds(), gateway.registered[AlarmKind.DAILY])
        assertTrue(gateway.interactions.any { it.startsWith("whileIdle:${AlarmKind.TEST}") })
    }

    @Test fun `a delivered test alarm never touches the daily schedule or outcome`() = runBlocking {
        coordinator.enableDaily()
        coordinator.scheduleTest()
        val sessionId = checkNotNull(stateStore.current().testSessionId)

        val delivery = coordinator.onAlarmDelivered(
            AlarmRequest(AlarmKind.TEST, gateway.registered.getValue(AlarmKind.TEST), sessionId = sessionId),
        )

        assertTrue(delivery is AlarmDelivery.Ring)
        assertTrue((delivery as AlarmDelivery.Ring).isTest)
        assertEquals(todaysAlarm.toEpochMilliseconds(), gateway.registered[AlarmKind.DAILY])
        assertEquals(today, stateStore.current().nextPrayerDate)
        assertNull(stateStore.current().lastOutcome)
        assertEquals(0, stateStore.current().snoozeCount)
    }

    @Test fun `a test session cannot record a daily outcome`() = runBlocking {
        coordinator.enableDaily()
        coordinator.scheduleTest()
        val sessionId = checkNotNull(stateStore.current().testSessionId)

        coordinator.recordOutcome(sessionId, AlarmOutcome.DISMISSED)

        assertNull(stateStore.current().lastOutcome)
    }

    @Test fun `an unscheduled reason with the alarm off cancels rather than schedules`() = runBlocking {
        val result = coordinator.scheduleNext(ScheduleReason.AppOpened)

        assertEquals(ScheduleResult.Disabled(DisabledReason.NOT_ENABLED), result)
        assertTrue(gateway.interactions.contains("cancel:${AlarmKind.DAILY}"))
        assertTrue(gateway.registered.isEmpty())
    }
}
