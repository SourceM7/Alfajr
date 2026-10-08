package io.github.sourcem7.alfajralarm.widget

import io.github.sourcem7.alfajralarm.domain.AlarmPreferences
import io.github.sourcem7.alfajralarm.domain.AlarmPreferencesStore
import io.github.sourcem7.alfajralarm.domain.AlarmState
import io.github.sourcem7.alfajralarm.domain.AlarmStateStore
import io.github.sourcem7.alfajralarm.domain.FajrCalculator
import io.github.sourcem7.alfajralarm.domain.NextOccurrenceSelector
import io.github.sourcem7.alfajralarm.domain.PreviewNextAlarmUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlin.time.Instant

/** A snapshot together with the instant it was taken, which the sky and ring are drawn for. */
data class WidgetFrame(val snapshot: WidgetSnapshot, val now: Instant)

/**
 * The widgets' only view of the alarm. Every write the application makes ends
 * in one of the two stores combined here, so watching them covers the
 * scheduler, the settings screens, and the ringing runtime alike.
 */
class WidgetSnapshots(
    private val preferences: AlarmPreferencesStore,
    private val alarmState: AlarmStateStore,
    private val calculator: FajrCalculator,
    private val selector: NextOccurrenceSelector,
    private val clock: () -> Instant = { Clock.System.now() },
) {
    private val redraws = MutableStateFlow(0)

    /** Emits whenever stored alarm data changes. */
    val inputs: Flow<Pair<AlarmPreferences, AlarmState>> =
        combine(preferences.preferences, alarmState.state, ::Pair).distinctUntilChanged()

    /**
     * What a running widget composition collects. Glance recomposes a live
     * session rather than reloading it, so time-driven changes (the sky phase,
     * the ring, Today becoming Tomorrow) arrive through [requestRedraw].
     */
    val frames: Flow<WidgetFrame> = combine(inputs, redraws) { (preferences, state), _ -> frame(preferences, state) }

    suspend fun current(): WidgetFrame = frame(preferences.load(), alarmState.current())

    fun requestRedraw() = redraws.update { it + 1 }

    private fun frame(preferences: AlarmPreferences, state: AlarmState): WidgetFrame {
        val now = clock()
        val snapshot = buildWidgetSnapshot(
            preferences = preferences,
            state = state,
            now = now,
            occurrenceOn = { date -> runCatching { calculator.calculate(date, preferences) }.getOrNull() },
            preview = { PreviewNextAlarmUseCase(selector) { now }(preferences) },
        )
        return WidgetFrame(snapshot, now)
    }
}
