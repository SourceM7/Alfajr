package io.github.sourcem7.alfajralarm.data.location

import org.junit.Assert.assertEquals
import org.junit.Test

class CityNormalizerTest {
    @Test fun `normalization is Latin case and Arabic diacritic insensitive`() {
        assertEquals("damascus", CityNormalizer.normalize("  DÁMASCUS  "))
        assertEquals(CityNormalizer.normalize("دِمَشْق"), CityNormalizer.normalize("دمشق"))
    }

    @Test fun `prefix search ranks populous matching cities without scanning unrelated names`() {
        val index = CitySearchIndex(
            listOf(
                city(id = "damascus", name = "Damascus", population = 1_800_000),
                city(id = "damietta", name = "Damietta", population = 300_000),
                city(id = "damanhur", name = "Damanhur", population = 250_000),
            ),
        )

        assertEquals(
            listOf("damascus", "damietta", "damanhur"),
            index.search("dam", limit = 30).map { it.id },
        )
    }

    @Test fun `search retains a middle fragment fallback when no name starts with it`() {
        val index = CitySearchIndex(listOf(city(id = "damascus", name = "Damascus")))

        assertEquals(listOf("damascus"), index.search("masc", limit = 30).map { it.id })
    }

    private fun city(id: String, name: String, population: Long = 1) = CityRecord(
        id = id,
        name = name,
        asciiName = name,
        countryCode = "SY",
        latitude = 33.5,
        longitude = 36.3,
        population = population,
        zoneId = "Asia/Damascus",
    )
}
