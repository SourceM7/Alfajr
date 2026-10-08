package io.github.sourcem7.alfajralarm.widget

import io.github.sourcem7.alfajralarm.domain.AlarmPreferences
import io.github.sourcem7.alfajralarm.domain.AlarmState
import io.github.sourcem7.alfajralarm.domain.FajrOccurrence
import io.github.sourcem7.alfajralarm.domain.FixedLocation
import io.github.sourcem7.alfajralarm.domain.PreferenceValidation
import io.github.sourcem7.alfajralarm.domain.RINGING_TIMEOUT
import io.github.sourcem7.alfajralarm.domain.validateForPreview
import kotlinx.datetime.LocalDate
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Everything a widget shows, decided once per update. Widgets are display
 * only, so this is the whole contract between the alarm and the home screen.
 */
sealed interface WidgetSnapshot {
    /** No location or method yet; the alarm cannot exist. */
    data object NeedsSetup : WidgetSnapshot

    /** The daily alarm is off. [preview] is the Fajr it would ring for. */
    data class Off(val preview: FajrOccurrence?) : WidgetSnapshot

    /**
     * The daily alarm is on but nothing is registered with Android, so a
     * widget must not claim it will ring.
     */
    data class NeedsAttention(val preview: FajrOccurrence?) : WidgetSnapshot

    /** The registered daily alarm. [occurrence] carries its exact trigger. */
    data class Scheduled(val occurrence: FajrOccurrence, val location: FixedLocation) : WidgetSnapshot

    data class Snoozed(val until: Instant, val zoneId: String) : WidgetSnapshot

    /** [until] is when the ringing screen gives up, should nothing else end it. */
    data class Ringing(val until: Instant) : WidgetSnapshot
}

/**
 * The registered alarm in [state] is authoritative over anything recalculated,
 * because it is what Android will actually fire. [occurrenceOn] supplies the
 * remaining detail (prayer time, high-latitude flag) for the registered date;
 * [preview] is only consulted while no alarm is registered.
 */
fun buildWidgetSnapshot(
    preferences: AlarmPreferences,
    state: AlarmState,
    now: Instant,
    occurrenceOn: (LocalDate) -> FajrOccurrence?,
    preview: () -> FajrOccurrence?,
): WidgetSnapshot {
    if (state.ringingSessionId != null) {
        val snoozeUntil = state.snoozeAlarmEpochMillis?.let(Instant::fromEpochMilliseconds)
        val zoneId = preferences.location?.zoneId
        if (snoozeUntil != null && snoozeUntil > now && zoneId != null) {
            return WidgetSnapshot.Snoozed(snoozeUntil, zoneId)
        }
        // A process killed mid-ring can leave the session behind. Past the
        // ringing timeout it is stale, and the widget falls back to the alarm.
        val delivered = state.lastDeliveryEpochMillis?.let(Instant::fromEpochMilliseconds)
        if (snoozeUntil == null && delivered != null && now < delivered + RINGING_TIMEOUT + STALE_MARGIN) {
            return WidgetSnapshot.Ringing(delivered + RINGING_TIMEOUT + STALE_MARGIN)
        }
    }

    val location = preferences.location
    if (location == null || preferences.validateForPreview() !is PreferenceValidation.Valid) {
        return WidgetSnapshot.NeedsSetup
    }
    if (!state.dailyEnabled) return WidgetSnapshot.Off(preview())

    val registeredMillis = state.nextAlarmEpochMillis
    val registeredDate = state.nextPrayerDate
    if (registeredMillis == null || registeredDate == null) return WidgetSnapshot.NeedsAttention(preview())

    val registered = Instant.fromEpochMilliseconds(registeredMillis)
    val calculated = occurrenceOn(registeredDate) ?: return WidgetSnapshot.NeedsAttention(preview())
    return WidgetSnapshot.Scheduled(calculated.copy(alarmInstant = registered), location)
}

/** Room for the ringing service to record its outcome before the widget moves on. */
private val STALE_MARGIN = 1.minutes
