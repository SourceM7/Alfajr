package io.github.sourcem7.alfajralarm.domain

class SearchCitiesUseCase(private val repository: CityRepository) {
    suspend operator fun invoke(query: String): List<FixedLocation> =
        if (query.isBlank()) emptyList() else repository.search(query, DEFAULT_LIMIT)

    suspend fun warmUp() = repository.warmUp()

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
