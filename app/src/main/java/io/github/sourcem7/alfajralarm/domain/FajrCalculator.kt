package io.github.sourcem7.alfajralarm.domain

import kotlinx.datetime.LocalDate

interface FajrCalculator {
    fun calculate(date: LocalDate, preferences: AlarmPreferences): FajrOccurrence
}
