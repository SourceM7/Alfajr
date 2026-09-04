package io.github.sourcem7.alfajralarm.domain

class SearchCitiesUseCase(private val repository: CityRepository) {
    suspend operator fun invoke(query: String): List<FixedLocation> =
        if (query.isBlank()) emptyList() else repository.search(query, DEFAULT_LIMIT)

    private companion object {
        const val DEFAULT_LIMIT = 30
    }
}

/** Validates and creates the domain model used for manually entered coordinates. */
class CreateManualLocationUseCase {
    operator fun invoke(latitude: String, longitude: String, zoneId: String): ManualLocationResult {
        val parsedLatitude = latitude.toDoubleOrNull()
            ?: return ManualLocationResult.Invalid(PreferenceError.INVALID_LATITUDE)
        val parsedLongitude = longitude.toDoubleOrNull()
            ?: return ManualLocationResult.Invalid(PreferenceError.INVALID_LONGITUDE)
        val location = FixedLocation(
            id = "manual:${latitude.trim()},${longitude.trim()}:${zoneId.trim()}",
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

/** Product policy for suggesting a calculation method from a selected country. */
class SuggestFajrMethodUseCase {
    operator fun invoke(countryCode: String?): FajrMethod = BY_COUNTRY[countryCode?.uppercase()]
        ?: FajrMethod.MUSLIM_WORLD_LEAGUE

    private companion object {
        val BY_COUNTRY = mapOf(
            "SA" to FajrMethod.UMM_AL_QURA,
            "AE" to FajrMethod.DUBAI,
            "QA" to FajrMethod.QATAR,
            "KW" to FajrMethod.KUWAIT,
            "TR" to FajrMethod.TURKEY,
            "SG" to FajrMethod.SINGAPORE,
            "MY" to FajrMethod.SINGAPORE,
            "ID" to FajrMethod.SINGAPORE,
            "PK" to FajrMethod.KARACHI,
            "IN" to FajrMethod.KARACHI,
            "BD" to FajrMethod.KARACHI,
            "AF" to FajrMethod.KARACHI,
            "EG" to FajrMethod.EGYPTIAN,
            "SY" to FajrMethod.EGYPTIAN,
            "JO" to FajrMethod.EGYPTIAN,
            "LB" to FajrMethod.EGYPTIAN,
            "PS" to FajrMethod.EGYPTIAN,
            "IQ" to FajrMethod.EGYPTIAN,
            "US" to FajrMethod.MOON_SIGHTING_COMMITTEE,
            "CA" to FajrMethod.MOON_SIGHTING_COMMITTEE,
        )
    }
}
