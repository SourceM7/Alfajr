package io.github.sourcem7.alfajralarm.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Device-local alarm state. It is excluded from Android backup, so a restored
 * installation always starts disabled and rechecks capabilities.
 */
data class AlarmState(
    val dailyEnabled: Boolean = false,
    val nextPrayerDate: LocalDate? = null,
    val nextAlarmEpochMillis: Long? = null,
    val skippedPrayerDate: LocalDate? = null,
    val ringingSessionId: String? = null,
    val snoozeCount: Int = 0,
    val testSessionId: String? = null,
    val lastOutcome: AlarmOutcome? = null,
    val lastOutcomeEpochMillis: Long? = null,
    val lastDeliveryEpochMillis: Long? = null,
    val activationConfirmed: Boolean = false,
) {
    /** True only when an alarm is both enabled and actually registered. */
    val claimsActiveAlarm: Boolean get() = dailyEnabled && nextAlarmEpochMillis != null
}

interface AlarmStateStore {
    val state: Flow<AlarmState>
    suspend fun current(): AlarmState
    suspend fun update(transform: (AlarmState) -> AlarmState): AlarmState
}

/** Fixed by the product specification rather than exposed as a setting. */
const val MAX_SNOOZE_COUNT: Int = 3
