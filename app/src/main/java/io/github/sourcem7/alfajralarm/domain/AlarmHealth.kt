package io.github.sourcem7.alfajralarm.domain

/** A capability the alarm needs before it may claim to be active. */
enum class CapabilityProblem {
    CONFIGURATION_INCOMPLETE,
    EXACT_ALARMS_UNAVAILABLE,
    NOTIFICATIONS_DISABLED,
    FULL_SCREEN_UNAVAILABLE,
    SCHEDULING_FAILED,
}

/** A condition the user should see but which must not block activation. */
enum class AlarmWarning { ALARM_VOLUME_MUTED, ALARM_VOLUME_LOW, TIME_ZONE_MISMATCH }

data class AlarmCapabilities(
    val canScheduleExactAlarms: Boolean,
    val notificationsEnabled: Boolean,
    val alarmChannelEnabled: Boolean,
    val canUseFullScreenIntent: Boolean,
    val alarmVolume: Int,
    val maxAlarmVolume: Int,
)

fun interface CapabilityProbe {
    fun read(): AlarmCapabilities
}

/**
 * True when Android cannot deliver the alarm at all, so it must not stay
 * registered. Notifications and the full-screen intent are deliberately absent:
 * Android still plays alarm audio and offers its own heads-up notification, so a
 * revoked one degrades an already-scheduled alarm instead of removing it.
 */
val CapabilityProblem.preventsDelivery: Boolean
    get() = when (this) {
        CapabilityProblem.CONFIGURATION_INCOMPLETE,
        CapabilityProblem.EXACT_ALARMS_UNAVAILABLE,
        CapabilityProblem.SCHEDULING_FAILED,
        -> true

        CapabilityProblem.NOTIFICATIONS_DISABLED,
        CapabilityProblem.FULL_SCREEN_UNAVAILABLE,
        -> false
    }

data class AlarmHealth(
    val problems: List<CapabilityProblem> = emptyList(),
    val warnings: List<AlarmWarning> = emptyList(),
) {
    val isHealthy: Boolean get() = problems.isEmpty()
    val blockingProblem: CapabilityProblem? get() = problems.firstOrNull()

    /** Problems that stop delivery outright; activation is impossible. */
    val fatalProblems: List<CapabilityProblem> get() = problems.filter { it.preventsDelivery }

    /**
     * Problems that leave a registered alarm audible but worse than promised.
     * They must be reported, never presented as a healthy alarm.
     */
    val degradingProblems: List<CapabilityProblem> get() = problems.filterNot { it.preventsDelivery }
}

private const val LOW_ALARM_VOLUME_FRACTION = 0.25

fun evaluateAlarmHealth(
    preferences: AlarmPreferences,
    capabilities: AlarmCapabilities,
    deviceZoneId: String,
): AlarmHealth {
    val problems = buildList {
        if (preferences.validateForPreview() !is PreferenceValidation.Valid) add(CapabilityProblem.CONFIGURATION_INCOMPLETE)
        if (!capabilities.canScheduleExactAlarms) add(CapabilityProblem.EXACT_ALARMS_UNAVAILABLE)
        if (!capabilities.notificationsEnabled || !capabilities.alarmChannelEnabled) add(CapabilityProblem.NOTIFICATIONS_DISABLED)
        if (!capabilities.canUseFullScreenIntent) add(CapabilityProblem.FULL_SCREEN_UNAVAILABLE)
    }
    val warnings = buildList {
        val maximum = capabilities.maxAlarmVolume
        when {
            capabilities.alarmVolume <= 0 -> add(AlarmWarning.ALARM_VOLUME_MUTED)
            maximum > 0 && capabilities.alarmVolume.toDouble() / maximum <= LOW_ALARM_VOLUME_FRACTION ->
                add(AlarmWarning.ALARM_VOLUME_LOW)
        }
        val selectedZoneId = preferences.location?.zoneId
        if (selectedZoneId != null && selectedZoneId != deviceZoneId) add(AlarmWarning.TIME_ZONE_MISMATCH)
    }
    return AlarmHealth(problems = problems, warnings = warnings)
}
