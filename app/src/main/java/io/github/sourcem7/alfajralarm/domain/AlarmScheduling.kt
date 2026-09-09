package io.github.sourcem7.alfajralarm.domain

import kotlinx.datetime.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Every alarm the application can own. Kinds never share a pending-intent
 * identity, so a temporary alarm can never replace the daily one.
 */
enum class AlarmKind { DAILY, SNOOZE, TEST }

/** A single request handed to the platform alarm boundary. */
data class AlarmRequest(
    val kind: AlarmKind,
    val triggerAtMillis: Long,
    val prayerDate: LocalDate? = null,
    val sessionId: String? = null,
)

sealed interface ScheduleReason {
    data object UserEnabled : ScheduleReason
    data object UserDisabled : ScheduleReason
    data object SettingsChanged : ScheduleReason
    data object AlarmDelivered : ScheduleReason
    data object AlarmFinished : ScheduleReason
    data object BootCompleted : ScheduleReason
    data object ClockChanged : ScheduleReason
    data object PackageReplaced : ScheduleReason
    data object AppOpened : ScheduleReason
}

sealed interface ScheduleResult {
    /**
     * The daily Fajr alarm is registered with the platform. [degradedBy] is
     * empty for a healthy alarm; otherwise the alarm will still ring but worse
     * than promised, and the UI must say so rather than claim it is healthy.
     */
    data class Scheduled(
        val occurrence: FajrOccurrence,
        val degradedBy: List<CapabilityProblem> = emptyList(),
    ) : ScheduleResult {
        val isDegraded: Boolean get() = degradedBy.isNotEmpty()
    }

    /**
     * A snooze or test alarm is registered; it carries no prayer occurrence.
     * [degradedBy] is empty for a temporary alarm that will behave exactly like
     * the real one, and otherwise names what will be missing — which is the
     * whole point of a test alarm that cannot show its full screen.
     */
    data class TemporaryScheduled(
        val kind: AlarmKind,
        val triggerAtMillis: Long,
        val degradedBy: List<CapabilityProblem> = emptyList(),
    ) : ScheduleResult

    data class Disabled(val reason: DisabledReason) : ScheduleResult
    data class ActionRequired(val problem: CapabilityProblem) : ScheduleResult
    data class InvalidConfiguration(val problem: PreferenceError) : ScheduleResult
}

enum class DisabledReason {
    NOT_ENABLED,
    USER_DISABLED,
    STALE_SESSION,
    SNOOZE_LIMIT_REACHED,
    INVALID_SNOOZE_DURATION,
}

/** What a delivered alarm intent means once stale duplicates are rejected. */
sealed interface AlarmDelivery {
    data class Ring(
        val kind: AlarmKind,
        val sessionId: String,
        val isTest: Boolean,
        val followingDaily: ScheduleResult?,
    ) : AlarmDelivery

    data object Ignored : AlarmDelivery
}

interface AlarmScheduler {
    suspend fun enableDaily(): ScheduleResult
    suspend fun disableDaily(): ScheduleResult
    suspend fun scheduleNext(reason: ScheduleReason): ScheduleResult
    suspend fun skipNext(): ScheduleResult
    suspend fun undoSkip(): ScheduleResult
    suspend fun scheduleSnooze(sessionId: String, minutes: Int): ScheduleResult
    suspend fun scheduleTest(delay: Duration = 10.seconds): ScheduleResult
    suspend fun onAlarmDelivered(request: AlarmRequest): AlarmDelivery
    suspend fun recordOutcome(sessionId: String, outcome: AlarmOutcome)
    suspend fun cancelAll()
}

/**
 * The AlarmManager boundary. Scheduling calls report failure rather than
 * throwing so a revoked capability can clear the claimed enabled state.
 */
interface ExactAlarmGateway {
    fun canScheduleExactAlarms(): Boolean
    fun scheduleAlarmClock(request: AlarmRequest): Boolean
    fun scheduleExactWhileIdle(request: AlarmRequest): Boolean
    fun cancel(kind: AlarmKind)
}

fun interface PreferencesProvider {
    suspend fun load(): AlarmPreferences
}

fun interface SessionIdFactory {
    fun next(): String
}
