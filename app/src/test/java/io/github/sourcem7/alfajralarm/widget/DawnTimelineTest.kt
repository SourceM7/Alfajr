package io.github.sourcem7.alfajralarm.widget

import io.github.sourcem7.alfajralarm.alarm.DAMASCUS
import io.github.sourcem7.alfajralarm.domain.FajrOccurrence
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class DawnTimelineTest {
    // 2026-06-15 04:00 in Damascus (UTC+3).
    private val fajr = Instant.parse("2026-06-15T01:00:00Z")

    @Test
    fun `each sky phase begins exactly at its boundary`() {
        assertEquals(SkyPhase.DAY, DawnTimeline.skyPhase(fajr - 10.hours - 1.seconds, fajr))
        assertEquals(SkyPhase.EVENING, DawnTimeline.skyPhase(fajr - 10.hours, fajr))
        assertEquals(SkyPhase.EVENING, DawnTimeline.skyPhase(fajr - 7.hours - 1.seconds, fajr))
        assertEquals(SkyPhase.NIGHT, DawnTimeline.skyPhase(fajr - 7.hours, fajr))
        assertEquals(SkyPhase.NIGHT, DawnTimeline.skyPhase(fajr - 60.minutes - 1.seconds, fajr))
        assertEquals(SkyPhase.FIRST_LIGHT, DawnTimeline.skyPhase(fajr - 60.minutes, fajr))
    }

    @Test
    fun `first light holds through a wake offset that rings after Fajr`() {
        assertEquals(SkyPhase.FIRST_LIGHT, DawnTimeline.skyPhase(fajr + 20.minutes, fajr))
    }

    @Test
    fun `an alarm that is not scheduled rests the sky`() {
        assertEquals(SkyPhase.RESTING, DawnTimeline.skyPhase(WidgetSnapshot.Off(occurrence()), fajr))
        assertEquals(SkyPhase.RESTING, DawnTimeline.skyPhase(WidgetSnapshot.NeedsSetup, fajr))
    }

    @Test
    fun `ring progress clamps outside its window and is full at the alarm`() {
        assertEquals(0f, DawnTimeline.ringProgress(fajr - 13.hours, fajr))
        assertEquals(0f, DawnTimeline.ringProgress(fajr - 12.hours, fajr))
        assertEquals(0.5f, DawnTimeline.ringProgress(fajr - 6.hours, fajr), 0.0001f)
        assertEquals(1f, DawnTimeline.ringProgress(fajr, fajr))
        assertEquals(1f, DawnTimeline.ringProgress(fajr + 5.minutes, fajr))
    }

    @Test
    fun `day labels follow the location zone rather than UTC`() {
        // 22:30 UTC on the 14th is already 01:30 on the 15th in Damascus.
        val now = Instant.parse("2026-06-14T22:30:00Z")
        assertEquals(DayLabel.TODAY, DawnTimeline.dayLabel(fajr, DAMASCUS.zoneId, now))
        assertEquals(DayLabel.TOMORROW, DawnTimeline.dayLabel(fajr + 24.hours, DAMASCUS.zoneId, now))
        assertEquals(DayLabel.LATER, DawnTimeline.dayLabel(fajr + 48.hours, DAMASCUS.zoneId, now))
    }

    @Test
    fun `next refresh is the nearest sky phase boundary`() {
        val snapshot = scheduled()
        val now = fajr - 8.hours
        assertEquals(fajr - 7.hours, DawnTimeline.nextRefreshAt(snapshot, now, ringSteps = false))
    }

    @Test
    fun `next refresh falls on local midnight before a far phase boundary`() {
        // 20:00 in Damascus; its midnight comes before the first sky boundary.
        val snapshot = scheduled(fajrAt = Instant.parse("2026-06-15T08:00:00Z"))
        val now = Instant.parse("2026-06-14T17:00:00Z")
        assertEquals(Instant.parse("2026-06-14T21:00:00Z"), DawnTimeline.nextRefreshAt(snapshot, now, ringSteps = false))
    }

    @Test
    fun `ring steps count back from the alarm so the last lands on it`() {
        val snapshot = scheduled()
        assertEquals(fajr - 30.minutes, DawnTimeline.nextRefreshAt(snapshot, fajr - 40.minutes, ringSteps = true))
        assertEquals(fajr - 15.minutes, DawnTimeline.nextRefreshAt(snapshot, fajr - 30.minutes, ringSteps = true))
        assertEquals(fajr, DawnTimeline.nextRefreshAt(snapshot, fajr - 1.minutes, ringSteps = true))
    }

    @Test
    fun `the ring window opening is a refresh even far from the alarm`() {
        val snapshot = scheduled()
        val now = fajr - 12.hours - 10.minutes
        assertEquals(fajr - 12.hours, DawnTimeline.nextRefreshAt(snapshot, now, ringSteps = true))
    }

    @Test
    fun `nothing is time driven before setup`() {
        assertNull(DawnTimeline.nextRefreshAt(WidgetSnapshot.NeedsSetup, fajr, ringSteps = true))
    }

    @Test
    fun `midnight survives a daylight saving transition in the location zone`() {
        // Europe/Berlin springs forward on 2026-03-29; that day is 23 hours long.
        val berlin = DAMASCUS.copy(zoneId = "Europe/Berlin")
        val snapshot = WidgetSnapshot.Off(
            occurrence(
                fajrAt = Instant.parse("2026-03-30T02:30:00Z"),
                zoneId = berlin.zoneId,
                date = LocalDate(2026, 3, 30),
            ),
        )
        val now = Instant.parse("2026-03-29T12:00:00Z")
        assertEquals(Instant.parse("2026-03-29T22:00:00Z"), DawnTimeline.nextRefreshAt(snapshot, now, ringSteps = false))
    }

    private fun scheduled(fajrAt: Instant = fajr) = WidgetSnapshot.Scheduled(occurrence(fajrAt), DAMASCUS)

    private fun occurrence(
        fajrAt: Instant = fajr,
        zoneId: String = DAMASCUS.zoneId,
        date: LocalDate = LocalDate(2026, 6, 15),
    ) = FajrOccurrence(
        prayerInstant = fajrAt,
        correctedPrayerInstant = fajrAt,
        alarmInstant = fajrAt,
        prayerLocalDate = date,
        zoneId = zoneId,
        highLatitudeRuleActive = false,
    )
}
