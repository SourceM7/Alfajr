package io.github.sourcem7.alfajralarm.data.location

import io.github.sourcem7.alfajralarm.domain.FixedLocation
import kotlinx.serialization.Serializable

@Serializable
data class CityAsset(
    val version: Int,
    val sourceDate: String,
    val cities: List<CityRecord>,
)

@Serializable
data class CityRecord(
    val id: String,
    val name: String,
    val asciiName: String,
    val arabicName: String? = null,
    val countryCode: String,
    val administrationName: String? = null,
    val latitude: Double,
    val longitude: Double,
    val population: Long,
    val zoneId: String,
) {
    fun toLocation(): FixedLocation = FixedLocation(
        id = id,
        displayName = name,
        displayNameArabic = arabicName,
        countryCode = countryCode,
        administrationName = administrationName,
        latitude = latitude,
        longitude = longitude,
        zoneId = zoneId,
    )
}
