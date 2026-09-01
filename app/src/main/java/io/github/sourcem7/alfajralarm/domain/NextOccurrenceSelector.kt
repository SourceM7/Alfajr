package io.github.sourcem7.alfajralarm.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Chooses the Fajr occurrence the daily alarm should target. The selected
 * location's zone is authoritative; the device zone is never consulted.
 */
class NextOccurrenceSelector(private val calculator: FajrCalculator) {
    /**
     * Returns the first occurrence whose alarm instant is strictly after [after]
     * and whose prayer date is not the skipped one. A wake offset that crosses
     * midnight is handled by comparing alarm instants rather than prayer times.
     */
    fun selectNext(
        after: Instant,
        preferences: AlarmPreferences,
        skippedPrayerDate: LocalDate? = null,
    ): FajrOccurrence {
        val location = checkNotNull(preferences.location) { PreferenceError.LOCATION_REQUIRED.name }
        val zone = TimeZone.of(location.zoneId)
        // A negative wake offset can move an alarm before its prayer date, so
        // start one day early rather than assuming today's date is the floor.
        var date = after.toLocalDateTime(zone).date.plus(-1, DateTimeUnit.DAY)
        repeat(SEARCH_DAYS) {
            val occurrence = calculator.calculate(date, preferences)
            if (occurrence.alarmInstant > after && occurrence.prayerLocalDate != skippedPrayerDate) return occurrence
            date = date.plus(1, DateTimeUnit.DAY)
        }
        error("NO_FUTURE_FAJR_OCCURRENCE")
    }

    private companion object {
        /** Yesterday, today, tomorrow, one skipped day, and a leap-second margin. */
        const val SEARCH_DAYS = 5
    }
}
