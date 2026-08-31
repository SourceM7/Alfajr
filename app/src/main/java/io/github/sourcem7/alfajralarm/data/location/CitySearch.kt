package io.github.sourcem7.alfajralarm.data.location

import android.content.Context
import io.github.sourcem7.alfajralarm.domain.FixedLocation
import java.text.Normalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class OfflineCityRepository(private val context: Context) {
    private val cities: List<IndexedCity> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { loadCities() }

    suspend fun search(query: String, limit: Int = DEFAULT_LIMIT): List<FixedLocation> =
        withContext(Dispatchers.Default) {
            val needle = CityNormalizer.normalize(query)
            if (needle.isBlank()) return@withContext emptyList()
            cities.asSequence()
                .mapNotNull { city -> city.matchScore(needle)?.let { score -> score to city } }
                .sortedWith(compareBy<Pair<Int, IndexedCity>> { it.first }.thenByDescending { it.second.record.population }.thenBy { it.second.record.name })
                .take(limit.coerceIn(1, MAX_LIMIT))
                .map { it.second.record.toLocation() }
                .toList()
        }

    private fun loadCities(): List<IndexedCity> = context.assets.open(CITY_ASSET).bufferedReader().use { reader ->
        assetJson.decodeFromString<CityAsset>(reader.readText()).cities.map(::IndexedCity)
    }

    private class IndexedCity(val record: CityRecord) {
        private val names = listOfNotNull(record.name, record.asciiName, record.arabicName)
            .map(CityNormalizer::normalize).distinct()

        fun matchScore(query: String): Int? = names.minOfOrNull { name ->
            when {
                name == query -> 0
                name.startsWith(query) -> 1
                name.contains(query) -> 2
                else -> Int.MAX_VALUE
            }
        }?.takeIf { it != Int.MAX_VALUE }
    }

    private companion object {
        const val CITY_ASSET = "cities.v1.json"
        const val DEFAULT_LIMIT = 30
        const val MAX_LIMIT = 50
        val assetJson = Json { ignoreUnknownKeys = true }
    }
}

object CityNormalizer {
    private val arabicDiacritics = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")
    private val whitespace = Regex("\\s+")

    fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(arabicDiacritics, "")
        .lowercase()
        .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ى', 'ي').replace('ة', 'ه')
        .replace(whitespace, " ")
        .trim()
}
