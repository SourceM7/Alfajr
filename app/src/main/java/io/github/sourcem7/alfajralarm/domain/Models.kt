package io.github.sourcem7.alfajralarm.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

data class FixedLocation(
    val id: String,
    val displayName: String,
    val countryCode: String?,
    val latitude: Double,
    val longitude: Double,
    val zoneId: String,
    val administrationName: String? = null,
    val displayNameArabic: String? = null,
)

enum class FajrMethod {
    MUSLIM_WORLD_LEAGUE,
    EGYPTIAN,
    KARACHI,
    UMM_AL_QURA,
    DUBAI,
    QATAR,
    KUWAIT,
    MOON_SIGHTING_COMMITTEE,
    SINGAPORE,
    TURKEY,
}

data class AlarmPreferences(
    val location: FixedLocation? = null,
    val method: FajrMethod? = null,
    val correctionMinutes: Int = 0,
    val wakeOffsetMinutes: Int = 0,
    val ringtoneUri: String? = null,
    val vibrationEnabled: Boolean = true,
    val snoozeMinutes: Int = 5,
    val tapToDismiss: Boolean = false,
)

enum class AlarmOutcome { DISMISSED, SNOOZED, MISSED, SKIPPED }

data class FajrOccurrence(
    val prayerInstant: Instant,
    val correctedPrayerInstant: Instant,
    val alarmInstant: Instant,
    val prayerLocalDate: LocalDate,
    val zoneId: String,
    val highLatitudeRuleActive: Boolean,
)

sealed interface PreferenceValidation {
    data object Valid : PreferenceValidation
    data class Invalid(val reason: PreferenceError) : PreferenceValidation
}

enum class PreferenceError {
    LOCATION_REQUIRED,
    METHOD_REQUIRED,
    INVALID_LATITUDE,
    INVALID_LONGITUDE,
    INVALID_TIME_ZONE,
}

fun FixedLocation.validate(): PreferenceValidation = when {
    !latitude.isFinite() || latitude !in -90.0..90.0 -> PreferenceValidation.Invalid(PreferenceError.INVALID_LATITUDE)
    !longitude.isFinite() || longitude !in -180.0..180.0 -> PreferenceValidation.Invalid(PreferenceError.INVALID_LONGITUDE)
    runCatching { TimeZone.of(zoneId) }.isFailure -> PreferenceValidation.Invalid(PreferenceError.INVALID_TIME_ZONE)
    else -> PreferenceValidation.Valid
}

fun AlarmPreferences.validateForPreview(): PreferenceValidation = when {
    location == null -> PreferenceValidation.Invalid(PreferenceError.LOCATION_REQUIRED)
    method == null -> PreferenceValidation.Invalid(PreferenceError.METHOD_REQUIRED)
    else -> location.validate()
}
