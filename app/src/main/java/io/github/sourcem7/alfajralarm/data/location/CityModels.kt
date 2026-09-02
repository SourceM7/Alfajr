package io.github.sourcem7.alfajralarm.data.location

import io.github.sourcem7.alfajralarm.domain.FixedLocation
import kotlinx.serialization.Serializable

/**
 * The bundled city catalogue. [attribution], [license], and [source] are a
 * license condition of the GeoNames data, not decoration: they travel with the
 * asset so the app can always render the notice it is required to show.
 */
@Serializable
data class CityAsset(
    val version: Int,
    val sourceDate: String,
    val cities: List<CityRecord>,
    val attribution: String = "",
    val license: String = "",
    val source: String = "",
)

/** The attribution the bundled city data must be displayed with. */
data class CityAttribution(
    val notice: String,
    val license: String,
    val source: String,
    val sourceDate: String,
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
