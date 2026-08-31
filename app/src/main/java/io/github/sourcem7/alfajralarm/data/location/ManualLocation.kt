package io.github.sourcem7.alfajralarm.data.location

import io.github.sourcem7.alfajralarm.domain.FixedLocation
import io.github.sourcem7.alfajralarm.domain.PreferenceError
import io.github.sourcem7.alfajralarm.domain.PreferenceValidation
import io.github.sourcem7.alfajralarm.domain.validate
import java.time.ZoneId

object ManualLocation {
    fun availableTimeZones(): List<String> = ZoneId.getAvailableZoneIds().sorted()

    fun create(latitude: String, longitude: String, zoneId: String): ManualLocationResult {
        val parsedLatitude = latitude.toDoubleOrNull() ?: return ManualLocationResult.Invalid(PreferenceError.INVALID_LATITUDE)
        val parsedLongitude = longitude.toDoubleOrNull() ?: return ManualLocationResult.Invalid(PreferenceError.INVALID_LONGITUDE)
        val location = FixedLocation(
            id = "manual:${latitude.trim()},${longitude.trim()}:$zoneId",
            displayName = "",
            countryCode = null,
            latitude = parsedLatitude,
            longitude = parsedLongitude,
            zoneId = zoneId.trim(),
        )
        return when (val validation = location.validate()) {
            PreferenceValidation.Valid -> ManualLocationResult.Valid(location)
            is PreferenceValidation.Invalid -> ManualLocationResult.Invalid(validation.reason)
        }
    }
}

sealed interface ManualLocationResult {
    data class Valid(val location: FixedLocation) : ManualLocationResult
    data class Invalid(val reason: PreferenceError) : ManualLocationResult
}
