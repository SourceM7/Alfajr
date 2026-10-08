package io.github.sourcem7.alfajralarm.widget

import io.github.sourcem7.alfajralarm.alarm.DAMASCUS
import io.github.sourcem7.alfajralarm.alarm.testPreferences
import io.github.sourcem7.alfajralarm.domain.AlarmState
import io.github.sourcem7.alfajralarm.domain.FajrOccurrence
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class WidgetSnapshotTest {
    private val now = Instant.parse("2026-06-14T20:00:00Z")
    private val fajrDate = LocalDate(2026, 6, 15)
    private val calculated = FajrOccurrence(
        prayerInstant = Instant.parse("2026-06-15T01:00:00Z"),
        correctedPrayerInstant = Instant.parse("2026-06-15T01:00:00Z"),
        alarmInstant = Instant.parse("2026-06-15T01:00:00Z"),
        prayerLocalDate = fajrDate,
        zoneId = DAMASCUS.zoneId,
        highLatitudeRuleActive = false,
    )

    @Test
    fun `missing location or method needs setup`() {
        assertEquals(WidgetSnapshot.NeedsSetup, build(state = AlarmState(), location = false))
        assertEquals(WidgetSnapshot.NeedsSetup, build(state = AlarmState(), method = false))
    }

    @Test
    fun `a disabled alarm shows the Fajr it would ring for`() {
        assertEquals(WidgetSnapshot.Off(calculated), build(state = AlarmState(dailyEnabled = false)))
    }

    @Test
    fun `an enabled alarm that Android does not hold needs attention`() {
        assertEquals(WidgetSnapshot.NeedsAttention(calculated), build(state = AlarmState(dailyEnabled = true)))
    }

    @Test
    fun `the registered trigger wins over a recalculated one`() {
        val registered = Instant.parse("2026-06-15T00:50:00Z")
        val snapshot = build(state = enabled(registered))
        assertEquals(WidgetSnapshot.Scheduled(calculated.copy(alarmInstant = registered), DAMASCUS), snapshot)
    }

    @Test
    fun `a live ringing session shows ringing until its timeout`() {
        val delivered = now - 2.minutes
        val state = enabled().copy(ringingSessionId = "s", lastDeliveryEpochMillis = delivered.toEpochMilliseconds())
        assertEquals(WidgetSnapshot.Ringing(delivered + 11.minutes), build(state = state))
    }

    @Test
    fun `a ringing session left behind by a killed process is ignored`() {
        val delivered = now - 30.minutes
        val state = enabled().copy(ringingSessionId = "s", lastDeliveryEpochMillis = delivered.toEpochMilliseconds())
        assertEquals(WidgetSnapshot.Scheduled(calculated, DAMASCUS), build(state = state))
    }

    @Test
    fun `a pending snooze shows when it rings again`() {
        val until = now + 5.minutes
        val state = enabled().copy(
            ringingSessionId = "s",
            snoozeAlarmEpochMillis = until.toEpochMilliseconds(),
            lastDeliveryEpochMillis = (now - 1.minutes).toEpochMilliseconds(),
        )
        assertEquals(WidgetSnapshot.Snoozed(until, DAMASCUS.zoneId), build(state = state))
    }

    @Test
    fun `a test alarm snooze does not touch the widget`() {
        val state = enabled().copy(
            testSessionId = "t",
            snoozeAlarmEpochMillis = (now + 5.minutes).toEpochMilliseconds(),
        )
        assertEquals(WidgetSnapshot.Scheduled(calculated, DAMASCUS), build(state = state))
    }

    private fun enabled(registered: Instant = calculated.alarmInstant) = AlarmState(
        dailyEnabled = true,
        nextPrayerDate = fajrDate,
        nextAlarmEpochMillis = registered.toEpochMilliseconds(),
    )

    private fun build(state: AlarmState, location: Boolean = true, method: Boolean = true): WidgetSnapshot {
        val base = testPreferences()
        val preferences = base.copy(
            location = base.location.takeIf { location },
            method = base.method.takeIf { method },
        )
        return buildWidgetSnapshot(
            preferences = preferences,
            state = state,
            now = now,
            occurrenceOn = { date -> calculated.takeIf { date == fajrDate } },
            preview = { calculated },
        )
    }
}
