package io.github.sourcem7.alfajralarm.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationUseCasesTest {
    private val createManualLocation = CreateManualLocationUseCase()

    @Test fun `manual coordinates and IANA zone are validated`() {
        assertTrue(createManualLocation("33.5138", "36.2765", "Asia/Damascus") is ManualLocationResult.Valid)
        assertTrue(createManualLocation("91", "36", "Asia/Damascus") is ManualLocationResult.Invalid)
        assertTrue(createManualLocation("33", "181", "Asia/Damascus") is ManualLocationResult.Invalid)
        assertTrue(createManualLocation("33", "36", "Mars/Olympus") is ManualLocationResult.Invalid)
    }

    @Test fun `manual location identity uses normalized input`() {
        val result = createManualLocation(" 33.5138 ", " 36.2765 ", " Asia/Damascus ")

        assertEquals(
            "manual:33.5138,36.2765:Asia/Damascus",
            (result as ManualLocationResult.Valid).location.id,
        )
    }

    @Test fun `city search applies the product result limit`() = kotlinx.coroutines.runBlocking {
        var receivedLimit = 0
        val useCase = SearchCitiesUseCase(CityRepository { _, limit ->
            receivedLimit = limit
            emptyList()
        })

        useCase("Damascus")

        assertEquals(30, receivedLimit)
    }
}
