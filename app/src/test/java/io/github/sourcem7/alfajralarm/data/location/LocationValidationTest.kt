package io.github.sourcem7.alfajralarm.data.location

import io.github.sourcem7.alfajralarm.calculation.MethodSuggestions
import io.github.sourcem7.alfajralarm.domain.FajrMethod
import io.github.sourcem7.alfajralarm.domain.PreferenceValidation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationValidationTest {
    @Test fun `manual coordinates and IANA zone are validated`() {
        assertTrue(ManualLocation.create("33.5138", "36.2765", "Asia/Damascus") is ManualLocationResult.Valid)
        assertTrue(ManualLocation.create("91", "36", "Asia/Damascus") is ManualLocationResult.Invalid)
        assertTrue(ManualLocation.create("33", "181", "Asia/Damascus") is ManualLocationResult.Invalid)
        assertTrue(ManualLocation.create("33", "36", "Mars/Olympus") is ManualLocationResult.Invalid)
    }

    @Test fun `normalization is Latin case and Arabic diacritic insensitive`() {
        assertEquals("damascus", CityNormalizer.normalize("  DÁMASCUS  "))
        assertEquals(CityNormalizer.normalize("دِمَشْق"), CityNormalizer.normalize("دمشق"))
    }

    @Test fun `country suggestions are data driven with documented fallback`() {
        assertEquals(FajrMethod.EGYPTIAN, MethodSuggestions.suggest("SY"))
        assertEquals(FajrMethod.UMM_AL_QURA, MethodSuggestions.suggest("SA"))
        assertEquals(FajrMethod.MUSLIM_WORLD_LEAGUE, MethodSuggestions.suggest("GB"))
    }
}
