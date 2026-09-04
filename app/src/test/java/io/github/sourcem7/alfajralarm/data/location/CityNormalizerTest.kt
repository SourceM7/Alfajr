package io.github.sourcem7.alfajralarm.data.location

import org.junit.Assert.assertEquals
import org.junit.Test

class CityNormalizerTest {
    @Test fun `normalization is Latin case and Arabic diacritic insensitive`() {
        assertEquals("damascus", CityNormalizer.normalize("  DÁMASCUS  "))
        assertEquals(CityNormalizer.normalize("دِمَشْق"), CityNormalizer.normalize("دمشق"))
    }
}
