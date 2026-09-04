package io.github.sourcem7.alfajralarm.domain

import kotlin.time.Clock
import kotlin.time.Instant

/** Calculates a safe UI preview without exposing the calculation adapter. */
class PreviewNextAlarmUseCase(
    private val selector: NextOccurrenceSelector,
    private val clock: () -> Instant = { Clock.System.now() },
) {
    operator fun invoke(preferences: AlarmPreferences): FajrOccurrence? {
        if (preferences.validateForPreview() !is PreferenceValidation.Valid) return null
        return runCatching { selector.selectNext(clock(), preferences) }.getOrNull()
    }
}
