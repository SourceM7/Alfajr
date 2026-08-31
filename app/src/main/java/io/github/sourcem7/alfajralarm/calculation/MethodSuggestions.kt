package io.github.sourcem7.alfajralarm.calculation

import io.github.sourcem7.alfajralarm.domain.FajrMethod

object MethodSuggestions {
    private val byCountry = mapOf(
        "SA" to FajrMethod.UMM_AL_QURA,
        "AE" to FajrMethod.DUBAI,
        "QA" to FajrMethod.QATAR,
        "KW" to FajrMethod.KUWAIT,
        "TR" to FajrMethod.TURKEY,
        "SG" to FajrMethod.SINGAPORE, "MY" to FajrMethod.SINGAPORE, "ID" to FajrMethod.SINGAPORE,
        "PK" to FajrMethod.KARACHI, "IN" to FajrMethod.KARACHI, "BD" to FajrMethod.KARACHI, "AF" to FajrMethod.KARACHI,
        "EG" to FajrMethod.EGYPTIAN, "SY" to FajrMethod.EGYPTIAN, "JO" to FajrMethod.EGYPTIAN,
        "LB" to FajrMethod.EGYPTIAN, "PS" to FajrMethod.EGYPTIAN, "IQ" to FajrMethod.EGYPTIAN,
        "US" to FajrMethod.MOON_SIGHTING_COMMITTEE, "CA" to FajrMethod.MOON_SIGHTING_COMMITTEE,
    )

    fun suggest(countryCode: String?): FajrMethod = byCountry[countryCode?.uppercase()] ?: FajrMethod.MUSLIM_WORLD_LEAGUE
}
