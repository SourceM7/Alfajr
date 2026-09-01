package io.github.sourcem7.alfajralarm.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sourcem7.alfajralarm.R
import io.github.sourcem7.alfajralarm.app.AppGraph
import io.github.sourcem7.alfajralarm.domain.AlarmKind
import io.github.sourcem7.alfajralarm.domain.AlarmOutcome
import io.github.sourcem7.alfajralarm.domain.AlarmPreferences
import io.github.sourcem7.alfajralarm.domain.AlarmState
import io.github.sourcem7.alfajralarm.domain.AlarmWarning
import io.github.sourcem7.alfajralarm.domain.CapabilityProblem
import io.github.sourcem7.alfajralarm.domain.DisabledReason
import io.github.sourcem7.alfajralarm.domain.MAX_SNOOZE_COUNT
import io.github.sourcem7.alfajralarm.domain.PreferenceError
import io.github.sourcem7.alfajralarm.domain.ScheduleReason
import io.github.sourcem7.alfajralarm.domain.ScheduleResult
import io.github.sourcem7.alfajralarm.domain.evaluateAlarmHealth
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Temporary Phase 2 diagnostics. Phase 4 replaces it with the production home
 * screen; until then it is the only way to see stored and calculated alarm
 * state side by side.
 */
@Composable
fun SchedulingDiagnostics(graph: AppGraph, preferences: AlarmPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by graph.alarmStateRepository.state.collectAsStateWithLifecycle(initialValue = AlarmState())
    var refreshKey by remember { mutableIntStateOf(0) }
    var lastResult by remember { mutableStateOf<ScheduleResult?>(null) }
    val deviceZoneId = remember(refreshKey) { ZoneId.systemDefault().id }
    val health = remember(refreshKey, preferences) {
        evaluateAlarmHealth(preferences, graph.capabilityProbe.read(), deviceZoneId)
    }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refreshKey++ }

    // Stored state changes whenever a reschedule lands, including the one the
    // activity runs on resume, so capability results stay fresh with it.
    LaunchedEffect(state) { refreshKey++ }

    fun run(action: suspend () -> ScheduleResult) {
        scope.launch {
            lastResult = action()
            refreshKey++
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.phase_two_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.phase_two_description), style = MaterialTheme.typography.bodySmall)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.daily_alarm_switch), modifier = Modifier.weight(1f))
                Switch(
                    checked = state.dailyEnabled,
                    onCheckedChange = { enabled ->
                        run { if (enabled) graph.scheduler.enableDaily() else graph.scheduler.disableDaily() }
                    },
                )
            }

            NextAlarmSummary(state, preferences, deviceZoneId)

            state.skippedPrayerDate?.let { Text(stringResource(R.string.skipped_date, it.toString())) }
            Text(stringResource(R.string.snooze_count_row, state.snoozeCount, MAX_SNOOZE_COUNT))
            Text(stringResource(R.string.session_row, state.ringingSessionId ?: stringResource(R.string.no_session)))
            Text(
                state.lastOutcome?.let { stringResource(R.string.last_outcome, stringResource(it.labelResource())) }
                    ?: stringResource(R.string.last_outcome_none),
            )
            Text(
                state.lastDeliveryEpochMillis?.let { stringResource(R.string.last_delivery, it.formatDateTime(deviceZoneId)) }
                    ?: stringResource(R.string.last_delivery_none),
            )

            HealthSummary(health.problems, health.warnings)
            lastResult?.let { Text(it.describe(preferences), style = MaterialTheme.typography.bodySmall) }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { run { graph.scheduler.skipNext() } }, enabled = state.dailyEnabled) {
                    Text(stringResource(R.string.skip_next_alarm))
                }
                Button(onClick = { run { graph.scheduler.undoSkip() } }, enabled = state.skippedPrayerDate != null) {
                    Text(stringResource(R.string.undo_skip))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { run { graph.scheduler.scheduleTest() } }) {
                    Text(stringResource(R.string.run_test_alarm))
                }
                Button(onClick = { run { graph.scheduler.scheduleNext(ScheduleReason.AppOpened) } }) {
                    Text(stringResource(R.string.refresh_state))
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                TextButton(onClick = { notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS) }) {
                    Text(stringResource(R.string.action_grant_notifications))
                }
            }
            TextButton(onClick = { context.startActivity(notificationSettings(context.packageName)) }) {
                Text(stringResource(R.string.action_notification_settings))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                TextButton(onClick = { context.startActivity(exactAlarmSettings(context.packageName)) }) {
                    Text(stringResource(R.string.action_exact_alarm_settings))
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                TextButton(onClick = { context.startActivity(fullScreenSettings(context.packageName)) }) {
                    Text(stringResource(R.string.action_full_screen_settings))
                }
            }
        }
    }
}

@Composable
private fun NextAlarmSummary(state: AlarmState, preferences: AlarmPreferences, deviceZoneId: String) {
    val millis = state.nextAlarmEpochMillis
    val selectedZoneId = preferences.location?.zoneId
    if (millis == null || selectedZoneId == null) {
        Text(stringResource(R.string.next_alarm_none))
        return
    }
    Text(stringResource(R.string.next_alarm_row, millis.formatDate(selectedZoneId), millis.formatTime(selectedZoneId), selectedZoneId))
    state.nextPrayerDate?.let { Text(stringResource(R.string.prayer_date_row, it.toString())) }
    if (selectedZoneId != deviceZoneId) {
        Text(stringResource(R.string.next_alarm_device_zone, millis.formatTime(deviceZoneId), deviceZoneId))
    }
}

@Composable
private fun HealthSummary(problems: List<CapabilityProblem>, warnings: List<AlarmWarning>) {
    if (problems.isEmpty() && warnings.isEmpty()) {
        Text(stringResource(R.string.health_healthy))
        return
    }
    if (problems.isNotEmpty()) {
        Text(stringResource(R.string.health_problems), style = MaterialTheme.typography.titleSmall)
        problems.forEach { Text(stringResource(it.labelResource()), color = MaterialTheme.colorScheme.error) }
    }
    if (warnings.isNotEmpty()) {
        Text(stringResource(R.string.health_warnings), style = MaterialTheme.typography.titleSmall)
        warnings.forEach { Text(stringResource(it.labelResource())) }
    }
}

@Composable
private fun ScheduleResult.describe(preferences: AlarmPreferences): String {
    val zoneId = preferences.location?.zoneId ?: ZoneId.systemDefault().id
    return when (this) {
        is ScheduleResult.Scheduled ->
            stringResource(R.string.result_scheduled, occurrence.alarmInstant.toEpochMilliseconds().formatDateTime(zoneId))
        is ScheduleResult.TemporaryScheduled ->
            stringResource(R.string.result_temporary_scheduled, stringResource(kind.labelResource()), triggerAtMillis.formatDateTime(zoneId))
        is ScheduleResult.Disabled -> stringResource(R.string.result_disabled, stringResource(reason.labelResource()))
        is ScheduleResult.ActionRequired -> stringResource(R.string.result_action_required, stringResource(problem.labelResource()))
        is ScheduleResult.InvalidConfiguration -> stringResource(R.string.result_invalid_configuration, stringResource(problem.labelResource()))
    }
}

private fun CapabilityProblem.labelResource(): Int = when (this) {
    CapabilityProblem.CONFIGURATION_INCOMPLETE -> R.string.problem_configuration_incomplete
    CapabilityProblem.EXACT_ALARMS_UNAVAILABLE -> R.string.problem_exact_alarms_unavailable
    CapabilityProblem.NOTIFICATIONS_DISABLED -> R.string.problem_notifications_disabled
    CapabilityProblem.FULL_SCREEN_UNAVAILABLE -> R.string.problem_full_screen_unavailable
    CapabilityProblem.SCHEDULING_FAILED -> R.string.problem_scheduling_failed
}

private fun AlarmWarning.labelResource(): Int = when (this) {
    AlarmWarning.ALARM_VOLUME_MUTED -> R.string.warning_alarm_volume_muted
    AlarmWarning.ALARM_VOLUME_LOW -> R.string.warning_alarm_volume_low
    AlarmWarning.TIME_ZONE_MISMATCH -> R.string.warning_time_zone_mismatch
}

private fun AlarmOutcome.labelResource(): Int = when (this) {
    AlarmOutcome.DISMISSED -> R.string.outcome_dismissed
    AlarmOutcome.SNOOZED -> R.string.outcome_snoozed
    AlarmOutcome.MISSED -> R.string.outcome_missed
    AlarmOutcome.SKIPPED -> R.string.outcome_skipped
}

private fun AlarmKind.labelResource(): Int = when (this) {
    AlarmKind.DAILY -> R.string.kind_daily
    AlarmKind.SNOOZE -> R.string.kind_snooze
    AlarmKind.TEST -> R.string.kind_test
}

private fun DisabledReason.labelResource(): Int = when (this) {
    DisabledReason.NOT_ENABLED -> R.string.disabled_not_enabled
    DisabledReason.USER_DISABLED -> R.string.disabled_user_disabled
    DisabledReason.STALE_SESSION -> R.string.disabled_stale_session
    DisabledReason.SNOOZE_LIMIT_REACHED -> R.string.disabled_snooze_limit
}

private fun PreferenceError.labelResource(): Int = when (this) {
    PreferenceError.LOCATION_REQUIRED -> R.string.error_location_required
    PreferenceError.METHOD_REQUIRED -> R.string.error_method_required
    PreferenceError.INVALID_LATITUDE -> R.string.error_invalid_latitude
    PreferenceError.INVALID_LONGITUDE -> R.string.error_invalid_longitude
    PreferenceError.INVALID_TIME_ZONE -> R.string.error_invalid_time_zone
}

private fun Long.formatDate(zoneId: String): String =
    zoned(zoneId).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

private fun Long.formatTime(zoneId: String): String =
    zoned(zoneId).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

private fun Long.formatDateTime(zoneId: String): String =
    zoned(zoneId).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))

private fun Long.zoned(zoneId: String) = Instant.ofEpochMilli(this).atZone(ZoneId.of(zoneId))

private fun notificationSettings(packageName: String): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

@RequiresApi(Build.VERSION_CODES.S)
private fun exactAlarmSettings(packageName: String): Intent =
    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri(packageName))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private fun fullScreenSettings(packageName: String): Intent =
    Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri(packageName))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private fun packageUri(packageName: String): Uri = Uri.fromParts("package", packageName, null)
