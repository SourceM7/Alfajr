package io.github.sourcem7.alfajralarm.widget

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * The widget sky. Every phase is measured back from the corrected Fajr instant
 * the Adhan adapter produced, so the widgets never need a sunset or Isha time
 * of their own.
 */
enum class SkyPhase { DAY, EVENING, NIGHT, FIRST_LIGHT, RESTING }

/** Where an alarm falls relative to today, in the location's own zone. */
enum class DayLabel { TODAY, TOMORROW, LATER }

object DawnTimeline {
    val EVENING_BEFORE_FAJR: Duration = 10.hours
    val NIGHT_BEFORE_FAJR: Duration = 7.hours
    val FIRST_LIGHT_BEFORE_FAJR: Duration = 60.minutes

    /** The ring widget fills across this window and is full at the alarm. */
    val RING_WINDOW: Duration = 12.hours
    val RING_STEP: Duration = 15.minutes

    /** A phase begins exactly at its boundary. */
    fun skyPhase(now: Instant, fajrAt: Instant): SkyPhase {
        val untilFajr = fajrAt - now
        return when {
            untilFajr <= FIRST_LIGHT_BEFORE_FAJR -> SkyPhase.FIRST_LIGHT
            untilFajr <= NIGHT_BEFORE_FAJR -> SkyPhase.NIGHT
            untilFajr <= EVENING_BEFORE_FAJR -> SkyPhase.EVENING
            else -> SkyPhase.DAY
        }
    }

    fun skyPhase(snapshot: WidgetSnapshot, now: Instant): SkyPhase = when (snapshot) {
        is WidgetSnapshot.Scheduled -> skyPhase(now, snapshot.occurrence.correctedPrayerInstant)
        is WidgetSnapshot.Snoozed, is WidgetSnapshot.Ringing -> SkyPhase.FIRST_LIGHT
        is WidgetSnapshot.Off, is WidgetSnapshot.NeedsAttention, WidgetSnapshot.NeedsSetup -> SkyPhase.RESTING
    }

    /** 0 until the window opens, 1 at the alarm, linear in between. */
    fun ringProgress(now: Instant, alarmAt: Instant): Float {
        val remaining = alarmAt - now
        if (remaining <= Duration.ZERO) return 1f
        if (remaining >= RING_WINDOW) return 0f
        return (1.0 - remaining / RING_WINDOW).toFloat()
    }

    fun dayLabel(at: Instant, zoneId: String, now: Instant): DayLabel {
        val zone = TimeZone.of(zoneId)
        val today = now.toLocalDateTime(zone).date
        return when (at.toLocalDateTime(zone).date) {
            today -> DayLabel.TODAY
            today.plus(1, DateTimeUnit.DAY) -> DayLabel.TOMORROW
            else -> DayLabel.LATER
        }
    }

    /**
     * The next instant at which any widget would look different with no change
     * in stored state: a sky phase boundary, local midnight (the Today and
     * Tomorrow labels), the alarm itself, and, when [ringSteps] is set, the
     * next step of the ring widget's fill. Null when nothing is time-driven.
     */
    fun nextRefreshAt(snapshot: WidgetSnapshot, now: Instant, ringSteps: Boolean): Instant? {
        val candidates = when (snapshot) {
            WidgetSnapshot.NeedsSetup -> emptyList()
            is WidgetSnapshot.Ringing -> listOf(snapshot.until)
            is WidgetSnapshot.Snoozed -> listOf(snapshot.until, nextMidnight(now, snapshot.zoneId))
            is WidgetSnapshot.Off -> snapshot.preview?.let { listOf(it.alarmInstant, nextMidnight(now, it.zoneId)) }.orEmpty()
            is WidgetSnapshot.NeedsAttention -> snapshot.preview?.let { listOf(it.alarmInstant, nextMidnight(now, it.zoneId)) }.orEmpty()
            is WidgetSnapshot.Scheduled -> {
                val occurrence = snapshot.occurrence
                val fajr = occurrence.correctedPrayerInstant
                buildList {
                    add(fajr - EVENING_BEFORE_FAJR)
                    add(fajr - NIGHT_BEFORE_FAJR)
                    add(fajr - FIRST_LIGHT_BEFORE_FAJR)
                    add(occurrence.alarmInstant)
                    add(nextMidnight(now, occurrence.zoneId))
                    if (ringSteps) nextRingStep(now, occurrence.alarmInstant)?.let(::add)
                }
            }
        }
        return candidates.filter { it > now }.minOrNull()
    }

    private fun nextRingStep(now: Instant, alarmAt: Instant): Instant? {
        val remaining = alarmAt - now
        if (remaining <= Duration.ZERO) return null
        if (remaining > RING_WINDOW) return alarmAt - RING_WINDOW
        // Steps count back from the alarm, so the last one lands on it exactly.
        val stepsBack = (remaining.inWholeMilliseconds - 1) / RING_STEP.inWholeMilliseconds
        return alarmAt - RING_STEP * stepsBack.toInt()
    }

    private fun nextMidnight(now: Instant, zoneId: String): Instant {
        val zone = TimeZone.of(zoneId)
        return now.toLocalDateTime(zone).date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone)
    }
}
