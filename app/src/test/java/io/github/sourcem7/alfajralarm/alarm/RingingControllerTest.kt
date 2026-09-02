package io.github.sourcem7.alfajralarm.alarm

import io.github.sourcem7.alfajralarm.calculation.AdhanFajrCalculator
import io.github.sourcem7.alfajralarm.domain.AlarmDelivery
import io.github.sourcem7.alfajralarm.domain.AlarmKind
import io.github.sourcem7.alfajralarm.domain.AlarmOutcome
import io.github.sourcem7.alfajralarm.domain.AlarmRequest
import io.github.sourcem7.alfajralarm.domain.MAX_SNOOZE_COUNT
import io.github.sourcem7.alfajralarm.domain.NextOccurrenceSelector
import io.github.sourcem7.alfajralarm.domain.PreferencesProvider
import io.github.sourcem7.alfajralarm.domain.RINGING_TIMEOUT
import io.github.sourcem7.alfajralarm.domain.RingingCommand
import io.github.sourcem7.alfajralarm.domain.RingingSession
import io.github.sourcem7.alfajralarm.domain.RingtoneSource
import io.github.sourcem7.alfajralarm.domain.SessionIdFactory
import io.github.sourcem7.alfajralarm.domain.VolumeRamp
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
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * The ringing runtime as the service drives it: what plays, what stops it, what
 * it records, and what it must leave alone. The Android output devices are
 * fakes, so every rule here is verified without a device.
 */
class RingingControllerTest {
    private val calculator = AdhanFajrCalculator()
    private val today = LocalDate(2026, 3, 10)
    private val tomorrow = today.plus(1, DateTimeUnit.DAY)
    private val todaysAlarm = calculator.calculate(today, testPreferences()).alarmInstant
    private val tomorrowsAlarm = calculator.calculate(tomorrow, testPreferences()).alarmInstant

    private val stateStore = FakeAlarmStateStore()
    private val gateway = RecordingExactAlarmGateway()
    private var preferences = testPreferences().copy(ringtoneUri = SAVED_RINGTONE)
    private var now: Instant = todaysAlarm - 1.minutes
    private var sessionCount = 0

    private val coordinator = AlarmSchedulingCoordinator(
        preferences = PreferencesProvider { preferences },
        stateStore = stateStore,
        gateway = gateway,
        capabilities = { healthyCapabilities() },
        selector = NextOccurrenceSelector(calculator),
        sessionIds = SessionIdFactory { "session-${++sessionCount}" },
        deviceZoneId = { DAMASCUS.zoneId },
        clock = { now },
    )

    private var audio = RecordingAlarmAudio()
    private val vibration = RecordingVibration()
    private val wakeLock = RecordingWakeLock()
    private val missed = RecordingMissedNotifier()

    private fun controller(audioOverride: RecordingAlarmAudio = audio): RingingController {
        audio = audioOverride
        return RingingController(
            scheduler = coordinator,
            stateStore = stateStore,
            preferences = PreferencesProvider { preferences },
            audio = audioOverride,
            vibration = vibration,
            wakeLock = wakeLock,
            missedNotifier = missed,
            clock = { now },
        )
    }

    /** Enables the daily alarm and rings it, exactly as the receiver does. */
    private suspend fun ringDaily(controller: RingingController): RingingSession {
        coordinator.enableDaily()
        now = todaysAlarm
        val delivery = coordinator.onAlarmDelivered(
            AlarmRequest(AlarmKind.DAILY, todaysAlarm.toEpochMilliseconds(), prayerDate = today),
        ) as AlarmDelivery.Ring
        return checkNotNull(controller.start(delivery.sessionId, delivery.isTest, todaysAlarm))
    }

    /** Delivers the snooze the session just scheduled and rings it. */
    private suspend fun ringPendingSnooze(controller: RingingController): RingingSession {
        val trigger = gateway.registered.getValue(AlarmKind.SNOOZE)
        now = Instant.fromEpochMilliseconds(trigger)
        val delivery = coordinator.onAlarmDelivered(
            AlarmRequest(AlarmKind.SNOOZE, trigger, sessionId = stateStore.current().activeSessionId()),
        ) as AlarmDelivery.Ring
        return checkNotNull(controller.start(delivery.sessionId, delivery.isTest, now))
    }

    @Test fun `the selected ringtone plays without consulting a fallback`() = runBlocking {
        val controller = controller()

        val session = ringDaily(controller)

        assertEquals(listOf(RingtoneSource.SAVED), audio.attempted)
        assertEquals(RingtoneSource.SAVED, session.ringtoneSource)
        assertEquals(RingtoneSource.SAVED, audio.playing)
        assertTrue(wakeLock.held)
        assertTrue(vibration.active)
    }

    @Test fun `an unreadable ringtone falls back to the system alarm sound`() = runBlocking {
        val controller = controller(RecordingAlarmAudio(failing = setOf(RingtoneSource.SAVED)))

        val session = ringDaily(controller)

        assertEquals(listOf(RingtoneSource.SAVED, RingtoneSource.SYSTEM_ALARM), audio.attempted)
        assertEquals(RingtoneSource.SYSTEM_ALARM, session.ringtoneSource)
    }

    @Test fun `no system alarm sound falls back to the bundled tone`() = runBlocking {
        val controller = controller(
            RecordingAlarmAudio(failing = setOf(RingtoneSource.SAVED, RingtoneSource.SYSTEM_ALARM)),
        )

        val session = ringDaily(controller)

        assertEquals(
            listOf(RingtoneSource.SAVED, RingtoneSource.SYSTEM_ALARM, RingtoneSource.BUNDLED),
            audio.attempted,
        )
        assertEquals(RingtoneSource.BUNDLED, session.ringtoneSource)
    }

    @Test fun `a completely unplayable chain still rings the screen and vibration`() = runBlocking {
        val controller = controller(RecordingAlarmAudio(failing = RingtoneSource.entries.toSet()))

        val session = ringDaily(controller)

        assertNull(session.ringtoneSource)
        assertNotNull("the alarm is not abandoned when audio fails", controller.session.value)
        assertTrue(vibration.active)
    }

    @Test fun `no saved ringtone starts the chain at the system alarm sound`() = runBlocking {
        preferences = preferences.copy(ringtoneUri = null)
        val controller = controller()

        ringDaily(controller)

        assertEquals(listOf(RingtoneSource.SYSTEM_ALARM), audio.attempted)
    }

    @Test fun `the ramp reaches full player volume at thirty seconds`() = runBlocking {
        val controller = controller()
        ringDaily(controller)
        val started = now

        // AlarmAudio exposes only the player's own volume, so a ramp can never
        // reach the system alarm stream the user configured.
        listOf(0, 10, 20, 30).forEach { second ->
            now = started + second.seconds
            controller.tick(now)
        }

        assertEquals(0f, audio.volumes.first())
        assertEquals(1f, audio.volumes.last())
        assertEquals(1f, VolumeRamp.scalarAt(VolumeRamp.DURATION))
        assertEquals(audio.volumes.sorted(), audio.volumes)
        assertEquals(1f, checkNotNull(controller.session.value).volumeScalar)
    }

    @Test fun `dismissing releases the player, vibration, and wake lock`() = runBlocking {
        val controller = controller()
        val session = ringDaily(controller)

        controller.handle(session.sessionId, RingingCommand.DISMISS)

        assertNull(controller.session.value)
        assertEquals(1, audio.stops)
        assertNull(audio.playing)
        assertFalse(vibration.active)
        assertFalse(wakeLock.held)
        assertEquals(AlarmOutcome.DISMISSED, stateStore.current().lastOutcome)
    }

    @Test fun `snoozing releases every output before the alarm returns`() = runBlocking {
        val controller = controller()
        val session = ringDaily(controller)

        controller.handle(session.sessionId, RingingCommand.SNOOZE)

        assertNull(controller.session.value)
        assertEquals(1, audio.stops)
        assertFalse(vibration.active)
        assertFalse(wakeLock.held)
        assertNotNull(gateway.registered[AlarmKind.SNOOZE])
    }

    @Test fun `a timeout releases every output and records missed exactly once`() = runBlocking {
        val controller = controller()
        val session = ringDaily(controller)
        val started = now

        now = started + RINGING_TIMEOUT
        controller.tick(now)
        // A second tick must not record a second missed occurrence.
        now = started + RINGING_TIMEOUT + 1.minutes
        controller.tick(now)

        assertNull(controller.session.value)
        assertEquals(AlarmOutcome.MISSED, stateStore.current().lastOutcome)
        assertEquals(listOf(session.sessionId), missed.posted.map { it.sessionId })
        assertFalse(vibration.active)
        assertFalse(wakeLock.held)
    }

    @Test fun `dismiss and timeout leave the following daily alarm scheduled`() = runBlocking {
        val dismissController = controller()
        val session = ringDaily(dismissController)
        assertEquals(tomorrowsAlarm.toEpochMilliseconds(), gateway.registered[AlarmKind.DAILY])

        dismissController.handle(session.sessionId, RingingCommand.DISMISS)
        assertEquals(tomorrowsAlarm.toEpochMilliseconds(), gateway.registered[AlarmKind.DAILY])

        // The same has to hold when nobody answers the alarm at all.
        now = tomorrowsAlarm
        val timeoutController = controller(RecordingAlarmAudio())
        val next = coordinator.onAlarmDelivered(
            AlarmRequest(AlarmKind.DAILY, tomorrowsAlarm.toEpochMilliseconds(), prayerDate = tomorrow),
        ) as AlarmDelivery.Ring
        timeoutController.start(next.sessionId, next.isTest, tomorrowsAlarm)
        now = tomorrowsAlarm + RINGING_TIMEOUT
        timeoutController.tick(now)

        assertNotNull("a missed alarm never unschedules the next day", gateway.registered[AlarmKind.DAILY])
    }

    @Test fun `a command for another session is ignored`() = runBlocking {
        val controller = controller()
        val session = ringDaily(controller)

        controller.handle("a-session-that-ended", RingingCommand.DISMISS)

        assertEquals(session.sessionId, controller.session.value?.sessionId)
        assertEquals(0, audio.stops)
        assertNull(stateStore.current().lastOutcome)
    }

    @Test fun `a delivery the app no longer owns never rings`() = runBlocking {
        val controller = controller()

        val session = controller.start("session-from-a-previous-install", isTest = false, alarmAt = todaysAlarm)

        assertNull(session)
        assertNull(controller.session.value)
        assertTrue(audio.attempted.isEmpty())
        assertFalse(wakeLock.held)
    }

    @Test fun `a test intent cannot borrow the daily session`() = runBlocking {
        val controller = controller()
        coordinator.enableDaily()
        now = todaysAlarm
        val delivery = coordinator.onAlarmDelivered(
            AlarmRequest(AlarmKind.DAILY, todaysAlarm.toEpochMilliseconds(), prayerDate = today),
        ) as AlarmDelivery.Ring

        assertNull(controller.start(delivery.sessionId, isTest = true, alarmAt = todaysAlarm))
        assertTrue(audio.attempted.isEmpty())
    }

    @Test fun `a repeated start returns the running session instead of a second player`() = runBlocking {
        val controller = controller()
        val session = ringDaily(controller)

        // A redelivered broadcast and a recreated activity both land here.
        val again = controller.start(session.sessionId, isTest = false, alarmAt = todaysAlarm)

        assertEquals(session, again)
        assertEquals(1, audio.attempted.size)
    }

    @Test fun `snoozes count one through three and the fourth is impossible`() = runBlocking {
        val controller = controller()
        var session = ringDaily(controller)

        repeat(MAX_SNOOZE_COUNT) { index ->
            assertTrue("snooze ${index + 1} is offered", session.snoozeAvailable)
            controller.handle(session.sessionId, RingingCommand.SNOOZE)
            assertEquals(index + 1, stateStore.current().snoozeCount)
            session = ringPendingSnooze(controller)
        }

        assertEquals(MAX_SNOOZE_COUNT, session.snoozesUsed)
        assertFalse("only dismiss remains at the third snooze", session.snoozeAvailable)
        controller.handle(session.sessionId, RingingCommand.SNOOZE)
        assertNotNull("a refused snooze leaves the alarm ringing", controller.session.value)
        assertEquals(MAX_SNOOZE_COUNT, stateStore.current().snoozeCount)
    }

    @Test fun `the test alarm rings without changing daily occurrence, skip, or outcome`() = runBlocking {
        val controller = controller()
        coordinator.enableDaily()
        coordinator.skipNext()
        val dailyBefore = stateStore.current()

        coordinator.scheduleTest()
        val testSessionId = checkNotNull(stateStore.current().testSessionId)
        val delivery = coordinator.onAlarmDelivered(
            AlarmRequest(AlarmKind.TEST, gateway.registered.getValue(AlarmKind.TEST), sessionId = testSessionId),
        ) as AlarmDelivery.Ring
        assertTrue(delivery.isTest)
        val session = checkNotNull(controller.start(delivery.sessionId, delivery.isTest, now))

        controller.handle(session.sessionId, RingingCommand.SNOOZE)
        val snoozed = ringPendingSnooze(controller)
        assertTrue("a test snooze still rings as a test", snoozed.isTest)
        controller.handle(snoozed.sessionId, RingingCommand.DISMISS)

        val after = stateStore.current()
        assertEquals(dailyBefore.nextAlarmEpochMillis, after.nextAlarmEpochMillis)
        assertEquals(dailyBefore.skippedPrayerDate, after.skippedPrayerDate)
        assertEquals(AlarmOutcome.SKIPPED, after.lastOutcome)
        assertEquals(0, after.snoozeCount)
        assertNull(after.testSessionId)
    }

    @Test fun `a test alarm that times out never claims the daily Fajr was missed`() = runBlocking {
        val controller = controller()
        coordinator.scheduleTest()
        val testSessionId = checkNotNull(stateStore.current().testSessionId)
        val delivery = coordinator.onAlarmDelivered(
            AlarmRequest(AlarmKind.TEST, gateway.registered.getValue(AlarmKind.TEST), sessionId = testSessionId),
        ) as AlarmDelivery.Ring
        controller.start(delivery.sessionId, delivery.isTest, now)

        now += RINGING_TIMEOUT
        controller.tick(now)

        assertNull(controller.session.value)
        assertTrue(missed.posted.isEmpty())
        assertNull(stateStore.current().lastOutcome)
    }

    @Test fun `a second alarm arriving mid-ring never interrupts the one already ringing`() = runBlocking {
        val controller = controller()
        val ringing = ringDaily(controller)

        // A test alarm scheduled earlier can land while Fajr is still ringing.
        coordinator.scheduleTest()
        val testSessionId = checkNotNull(stateStore.current().testSessionId)
        val delivery = coordinator.onAlarmDelivered(
            AlarmRequest(AlarmKind.TEST, gateway.registered.getValue(AlarmKind.TEST), sessionId = testSessionId),
        ) as AlarmDelivery.Ring

        assertNull(controller.start(delivery.sessionId, delivery.isTest, now))
        assertEquals(ringing.sessionId, controller.session.value?.sessionId)
        assertEquals(1, audio.attempted.size)
        assertEquals(0, audio.stops)
    }

    @Test fun `abandoning another instance's session leaves the running alarm alone`() = runBlocking {
        val controller = controller()
        val session = ringDaily(controller)

        controller.abandon("a-session-from-a-stopped-service")

        assertEquals(session.sessionId, controller.session.value?.sessionId)
        assertEquals(0, audio.stops)
        assertTrue(wakeLock.held)
    }

    @Test fun `abandoning a session releases the outputs without recording an outcome`() = runBlocking {
        val controller = controller()
        val session = ringDaily(controller)

        controller.abandon(session.sessionId)

        assertNull(controller.session.value)
        assertEquals(1, audio.stops)
        assertFalse(vibration.active)
        assertFalse(wakeLock.held)
        assertNull(stateStore.current().lastOutcome)
    }

    private companion object {
        const val SAVED_RINGTONE = "content://media/internal/audio/media/7"
    }
}

/** The session that is ringing right now, whichever kind owns it. */
private fun io.github.sourcem7.alfajralarm.domain.AlarmState.activeSessionId(): String =
    checkNotNull(testSessionId ?: ringingSessionId)
