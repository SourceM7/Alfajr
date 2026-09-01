package io.github.sourcem7.alfajralarm.alarm

import io.github.sourcem7.alfajralarm.calculation.AdhanFajrCalculator
import io.github.sourcem7.alfajralarm.domain.NextOccurrenceSelector
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class NextOccurrenceSelectorTest {
    private val selector = NextOccurrenceSelector(AdhanFajrCalculator())
    private val preferences = testPreferences()
    private val today = LocalDate(2026, 3, 10)

    @Test fun `enabling before today's alarm selects today`() {
        val todaysAlarm = AdhanFajrCalculator().calculate(today, preferences).alarmInstant

        val selected = selector.selectNext(todaysAlarm - 1.minutes, preferences)

        assertEquals(today, selected.prayerLocalDate)
        assertEquals(todaysAlarm, selected.alarmInstant)
    }

    @Test fun `enabling at or after today's alarm selects tomorrow`() {
        val todaysAlarm = AdhanFajrCalculator().calculate(today, preferences).alarmInstant
        val tomorrow = today.plus(1, DateTimeUnit.DAY)

        assertEquals(tomorrow, selector.selectNext(todaysAlarm, preferences).prayerLocalDate)
        assertEquals(tomorrow, selector.selectNext(todaysAlarm + 1.seconds, preferences).prayerLocalDate)
    }

    @Test fun `skipping a date selects the following prayer date`() {
        val todaysAlarm = AdhanFajrCalculator().calculate(today, preferences).alarmInstant
        val tomorrow = today.plus(1, DateTimeUnit.DAY)

        val selected = selector.selectNext(todaysAlarm - 1.minutes, preferences, skippedPrayerDate = today)

        assertEquals(tomorrow, selected.prayerLocalDate)
    }

    @Test fun `an alarm pulled before its prayer date is still selected`() {
        // Fajr just after midnight with a negative wake offset moves the alarm
        // to the previous local date.
        val calculator = StubFajrCalculator(LocalTime(0, 30))
        val crossing = NextOccurrenceSelector(calculator)
        val settings = testPreferences(wake = -60)
        val zone = TimeZone.of(DAMASCUS.zoneId)
        val alarm = calculator.calculate(today, settings).alarmInstant

        assertNotEquals(today, alarm.toLocalDateTime(zone).date)
        val selected = crossing.selectNext(alarm - 1.seconds, settings)
        assertEquals(today, selected.prayerLocalDate)
        assertEquals(alarm, selected.alarmInstant)
    }

    @Test fun `the selected location zone decides the candidate date`() {
        // Auckland is a day ahead of Damascus for part of every day, so the
        // device zone must not be able to influence the choice.
        val auckland = DAMASCUS.copy(id = "auckland", latitude = -36.8485, longitude = 174.7633, zoneId = "Pacific/Auckland")
        val settings = testPreferences(location = auckland)
        val zone = TimeZone.of(auckland.zoneId)

        val selected = selector.selectNext(
            AdhanFajrCalculator().calculate(today, settings).alarmInstant - 1.minutes,
            settings,
        )

        assertEquals(today, selected.prayerLocalDate)
        assertEquals(auckland.zoneId, selected.zoneId)
        assertEquals(today, selected.alarmInstant.toLocalDateTime(zone).date)
    }

    @Test fun `a spring-forward day still yields one future alarm`() {
        // Europe/London moves to summer time at 01:00 on 2026-03-29.
        val london = DAMASCUS.copy(id = "london", latitude = 51.5072, longitude = -0.1276, zoneId = "Europe/London")
        val settings = testPreferences(location = london)
        val transition = LocalDate(2026, 3, 29)
        val alarm = AdhanFajrCalculator().calculate(transition, settings).alarmInstant

        val selected = selector.selectNext(alarm - 1.minutes, settings)

        assertEquals(transition, selected.prayerLocalDate)
        assertEquals(alarm, selected.alarmInstant)
    }
}
