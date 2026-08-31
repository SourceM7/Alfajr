package io.github.sourcem7.alfajralarm.calculation

import com.batoulapps.adhan2.CalculationMethod
import io.github.sourcem7.alfajralarm.domain.AlarmPreferences
import io.github.sourcem7.alfajralarm.domain.FajrMethod
import io.github.sourcem7.alfajralarm.domain.FixedLocation
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.minutes

class AdhanFajrCalculatorTest {
    private val calculator = AdhanFajrCalculator()

    @Test fun `Cairo fixture matches Adhan shared fixture`() {
        val occurrence = calculator.calculate(
            LocalDate(2020, 1, 1),
            preferences(location = FixedLocation("cairo", "Cairo", "EG", 30.028703, 31.249528, "Africa/Cairo"), method = FajrMethod.EGYPTIAN),
        )

        assertEquals("05:18", occurrence.prayerInstant.at("Africa/Cairo"))
        assertEquals(occurrence.prayerInstant, occurrence.correctedPrayerInstant)
    }

    @Test fun `correction and wake offset preserve their separate boundaries`() {
        val base = preferences(correction = -30, wake = -60)
        val earliest = calculator.calculate(LocalDate(2024, 2, 29), base)
        assertEquals(-30.minutes, earliest.correctedPrayerInstant - earliest.prayerInstant)
        assertEquals(-60.minutes, earliest.alarmInstant - earliest.correctedPrayerInstant)

        val latest = calculator.calculate(LocalDate(2024, 2, 29), base.copy(correctionMinutes = 30, wakeOffsetMinutes = 30))
        assertEquals(30.minutes, latest.correctedPrayerInstant - latest.prayerInstant)
        assertEquals(30.minutes, latest.alarmInstant - latest.correctedPrayerInstant)
    }

    @Test fun `all supported methods map to a distinct intended Adhan method`() {
        assertEquals(CalculationMethod.MUSLIM_WORLD_LEAGUE, FajrMethod.MUSLIM_WORLD_LEAGUE.toAdhanCalculationMethod())
        assertEquals(CalculationMethod.EGYPTIAN, FajrMethod.EGYPTIAN.toAdhanCalculationMethod())
        assertEquals(CalculationMethod.KARACHI, FajrMethod.KARACHI.toAdhanCalculationMethod())
        assertEquals(CalculationMethod.UMM_AL_QURA, FajrMethod.UMM_AL_QURA.toAdhanCalculationMethod())
        assertEquals(CalculationMethod.DUBAI, FajrMethod.DUBAI.toAdhanCalculationMethod())
        assertEquals(CalculationMethod.QATAR, FajrMethod.QATAR.toAdhanCalculationMethod())
        assertEquals(CalculationMethod.KUWAIT, FajrMethod.KUWAIT.toAdhanCalculationMethod())
        assertEquals(CalculationMethod.MOON_SIGHTING_COMMITTEE, FajrMethod.MOON_SIGHTING_COMMITTEE.toAdhanCalculationMethod())
        assertEquals(CalculationMethod.SINGAPORE, FajrMethod.SINGAPORE.toAdhanCalculationMethod())
        assertEquals(CalculationMethod.TURKEY, FajrMethod.TURKEY.toAdhanCalculationMethod())
    }

    @Test fun `locations at 48 degrees are labeled for high latitude handling`() {
        val occurrence = calculator.calculate(
            LocalDate(2024, 6, 1),
            preferences(location = FixedLocation("london", "London", "GB", 51.5072, -0.1276, "Europe/London")),
        )
        assertTrue(occurrence.highLatitudeRuleActive)
    }

    private fun preferences(
        location: FixedLocation = FixedLocation("damascus", "Damascus", "SY", 33.5138, 36.2765, "Asia/Damascus"),
        method: FajrMethod = FajrMethod.EGYPTIAN,
        correction: Int = 0,
        wake: Int = 0,
    ) = AlarmPreferences(location = location, method = method, correctionMinutes = correction, wakeOffsetMinutes = wake)

    private fun kotlin.time.Instant.at(zoneId: String): String =
        Instant.ofEpochMilli(toEpochMilliseconds()).atZone(ZoneId.of(zoneId)).format(DateTimeFormatter.ofPattern("HH:mm"))
}
