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
    /** Counted separately so a test alarm never consumes daily snoozes. */
    val testSnoozeCount: Int = 0,
    /** Last registered snooze trigger, so a redelivered broadcast cannot re-ring it. */
    val snoozeAlarmEpochMillis: Long? = null,
    /** Last registered test trigger, so a redelivered broadcast cannot re-ring it. */
    val testAlarmEpochMillis: Long? = null,
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

/** Which of the two possible ringing sessions an incoming session ID names. */
enum class SessionKind { RINGING, TEST }

/**
 * Null for a session the application no longer owns. Every component that acts
 * on a session ID resolves it here first, so a stale notification action, a
 * redelivered broadcast, or a rebuilt activity is ignored rather than obeyed.
 */
fun AlarmState.sessionKind(sessionId: String): SessionKind? = when (sessionId) {
    ringingSessionId -> SessionKind.RINGING
    testSessionId -> SessionKind.TEST
    else -> null
}
