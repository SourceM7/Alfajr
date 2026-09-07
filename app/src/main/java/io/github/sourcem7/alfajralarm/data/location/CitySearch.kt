package io.github.sourcem7.alfajralarm.data.location

import android.content.Context
import io.github.sourcem7.alfajralarm.domain.FixedLocation
import io.github.sourcem7.alfajralarm.domain.CityRepository
import java.text.Normalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class OfflineCityRepository(private val context: Context) : CityRepository {
    private val catalogue: Catalogue by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { loadCatalogue() }

    /**
     * The GeoNames notice carried by the asset. Attribution is a condition of
     * the data's license, so the licenses screen reads it from the same file the
     * cities came from rather than from a hard-coded copy.
     */
    suspend fun attribution(): CityAttribution = withContext(Dispatchers.Default) { catalogue.attribution }

    override suspend fun warmUp() {
        withContext(Dispatchers.Default) { catalogue.index }
    }

    override suspend fun search(query: String, limit: Int): List<FixedLocation> =
        withContext(Dispatchers.Default) { catalogue.index.search(query, limit) }

    private fun loadCatalogue(): Catalogue = context.assets.open(CITY_ASSET).bufferedReader().use { reader ->
        val asset = assetJson.decodeFromString<CityAsset>(reader.readText())
        Catalogue(
            index = CitySearchIndex(asset.cities),
            attribution = CityAttribution(
                notice = asset.attribution,
                license = asset.license,
                source = asset.source,
                sourceDate = asset.sourceDate,
            ),
        )
    }

    private class Catalogue(val index: CitySearchIndex, val attribution: CityAttribution)

    private companion object {
        const val CITY_ASSET = "cities.v1.json"
        const val MAX_LIMIT = 50
        val assetJson = Json { ignoreUnknownKeys = true }
    }
}

/**
 * Keeps city search fast after the one-time asset load. The normalized names
 * are sorted once, so usual prefix searches visit only matching cities instead
 * of sorting the entire worldwide catalogue for every character typed.
 */
internal class CitySearchIndex(records: List<CityRecord>) {
    private val cities = records.map(::IndexedCity)
    private val names = cities.flatMap { city ->
        city.names.map { name -> IndexedName(name, city) }
    }.sortedBy(IndexedName::name)

    fun search(query: String, limit: Int): List<FixedLocation> {
        val needle = CityNormalizer.normalize(query)
        if (needle.isBlank()) return emptyList()

        val boundedLimit = limit.coerceIn(1, MAX_LIMIT)
        val prefixMatches = prefixMatches(needle)
        val matches = if (prefixMatches.isNotEmpty()) {
            prefixMatches
        } else {
            // Preserve a useful fallback for a remembered middle fragment
            // without making it the cost paid on each normal city search.
            cities.mapNotNull { city -> city.matchScore(needle)?.let { score -> score to city } }
        }
        return matches
            .sortedWith(cityMatchOrder)
            .take(boundedLimit)
            .map { it.second.record.toLocation() }
    }

    private fun prefixMatches(query: String): List<Pair<Int, IndexedCity>> {
        val matches = LinkedHashSet<IndexedCity>()
        var index = names.lowerBound(query)
        while (index < names.size && names[index].name.startsWith(query)) {
            matches += names[index].city
            index++
        }
        return matches.mapNotNull { city -> city.matchScore(query)?.let { score -> score to city } }
    }

    private fun List<IndexedName>.lowerBound(query: String): Int {
        var low = 0
        var high = size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (this[middle].name < query) low = middle + 1 else high = middle
        }
        return low
    }

    private class IndexedCity(val record: CityRecord) {
        val names = listOfNotNull(record.name, record.asciiName, record.arabicName)
            .map(CityNormalizer::normalize)
            .distinct()

        fun matchScore(query: String): Int? = names.minOfOrNull { name ->
            when {
                name == query -> 0
                name.startsWith(query) -> 1
                name.contains(query) -> 2
                else -> Int.MAX_VALUE
            }
        }?.takeIf { it != Int.MAX_VALUE }
    }

    private data class IndexedName(val name: String, val city: IndexedCity)

    private companion object {
        const val MAX_LIMIT = 50
        val cityMatchOrder = compareBy<Pair<Int, IndexedCity>> { it.first }
            .thenByDescending { it.second.record.population }
            .thenBy { it.second.record.name }
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
