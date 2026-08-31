package io.github.sourcem7.alfajralarm.calculation

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.HighLatitudeRule
import com.batoulapps.adhan2.PrayerAdjustments
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import io.github.sourcem7.alfajralarm.domain.AlarmPreferences
import io.github.sourcem7.alfajralarm.domain.FajrCalculator
import io.github.sourcem7.alfajralarm.domain.FajrMethod
import io.github.sourcem7.alfajralarm.domain.FajrOccurrence
import io.github.sourcem7.alfajralarm.domain.PreferenceValidation
import io.github.sourcem7.alfajralarm.domain.validateForPreview
import kotlinx.datetime.LocalDate
import kotlin.time.Duration.Companion.minutes

/** Adhan adapter. Domain callers never receive Adhan types. */
class AdhanFajrCalculator : FajrCalculator {
    override fun calculate(date: LocalDate, preferences: AlarmPreferences): FajrOccurrence {
        val validation = preferences.validateForPreview()
        require(validation is PreferenceValidation.Valid) {
            (validation as? PreferenceValidation.Invalid)?.reason?.name ?: "INVALID_FAJR_PREFERENCES"
        }
        require(preferences.correctionMinutes in -30..30) { "INVALID_CORRECTION_MINUTES" }
        require(preferences.wakeOffsetMinutes in -60..30) { "INVALID_WAKE_OFFSET_MINUTES" }

        val location = checkNotNull(preferences.location)
        val method = checkNotNull(preferences.method)
        val baseParameters = method.toAdhanCalculationMethod().parameters
        val parameters = baseParameters.copy(
            highLatitudeRule = HighLatitudeRule.TWILIGHT_ANGLE,
            prayerAdjustments = PrayerAdjustments(fajr = preferences.correctionMinutes),
        )
        val times = PrayerTimes(
            Coordinates(location.latitude, location.longitude),
            DateComponents(date.year, date.monthNumber, date.day),
            parameters,
        )
        // Adhan applies prayerAdjustments to its returned Fajr. Recover the
        // unadjusted instant for the tracer, while preserving Adhan's rounding.
        val corrected = times.fajr
        val rawFajr = corrected - preferences.correctionMinutes.minutes
        return FajrOccurrence(
            prayerInstant = rawFajr,
            correctedPrayerInstant = corrected,
            alarmInstant = corrected + preferences.wakeOffsetMinutes.minutes,
            prayerLocalDate = date,
            zoneId = location.zoneId,
            highLatitudeRuleActive = kotlin.math.abs(location.latitude) >= HIGH_LATITUDE_LABEL_DEGREES,
        )
    }

    private companion object {
        const val HIGH_LATITUDE_LABEL_DEGREES = 48.0
    }
}

internal fun FajrMethod.toAdhanCalculationMethod(): CalculationMethod = when (this) {
    FajrMethod.MUSLIM_WORLD_LEAGUE -> CalculationMethod.MUSLIM_WORLD_LEAGUE
    FajrMethod.EGYPTIAN -> CalculationMethod.EGYPTIAN
    FajrMethod.KARACHI -> CalculationMethod.KARACHI
    FajrMethod.UMM_AL_QURA -> CalculationMethod.UMM_AL_QURA
    FajrMethod.DUBAI -> CalculationMethod.DUBAI
    FajrMethod.QATAR -> CalculationMethod.QATAR
    FajrMethod.KUWAIT -> CalculationMethod.KUWAIT
    FajrMethod.MOON_SIGHTING_COMMITTEE -> CalculationMethod.MOON_SIGHTING_COMMITTEE
    FajrMethod.SINGAPORE -> CalculationMethod.SINGAPORE
    FajrMethod.TURKEY -> CalculationMethod.TURKEY
}
