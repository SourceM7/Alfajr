package io.github.sourcem7.alfajralarm.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sourcem7.alfajralarm.R
import io.github.sourcem7.alfajralarm.domain.AlarmOutcome
import io.github.sourcem7.alfajralarm.domain.AlarmPreferences
import io.github.sourcem7.alfajralarm.domain.AlarmState
import io.github.sourcem7.alfajralarm.domain.AlarmWarning
import io.github.sourcem7.alfajralarm.domain.CapabilityProblem
import io.github.sourcem7.alfajralarm.domain.FajrMethod
import io.github.sourcem7.alfajralarm.domain.FajrOccurrence
import io.github.sourcem7.alfajralarm.domain.FixedLocation
import io.github.sourcem7.alfajralarm.domain.ManualLocationResult
import io.github.sourcem7.alfajralarm.domain.PreferenceError
import io.github.sourcem7.alfajralarm.domain.ScheduleResult
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.util.TimeZone
import java.util.Date

private enum class Screen { HOME, ONBOARDING, LOCATION, METHOD, ADJUSTMENTS, SETTINGS, PRIVACY, LICENSES, TROUBLESHOOTING }

/** The production single-activity UI. */
@Composable
fun AlfajrApp(viewModel: AlfajrViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val preferences = uiState.preferences
    val dynamicColor = uiState.dynamicColor
    val state = uiState.alarmState
    var screen by remember(state.activationConfirmed) {
        mutableStateOf(if (state.activationConfirmed) Screen.HOME else Screen.ONBOARDING)
    }
    val context = LocalContext.current
    val health = uiState.health
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refreshCapabilities()
    }
    val resolve: (CapabilityProblem) -> Unit = { problem ->
        when {
            problem == CapabilityProblem.NOTIFICATIONS_DISABLED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                notifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            else -> context.openCapabilitySettings(problem)
        }
    }
    AlfajrTheme(dynamicColor = dynamicColor) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (screen) {
                Screen.HOME -> HomeScreen(viewModel, preferences, state, uiState.preview, health.problems, health.warnings, resolve, onSettings = { screen = Screen.SETTINGS })
                Screen.ONBOARDING -> OnboardingScreen(viewModel, preferences, uiState.preview, health.problems, resolve, onLocation = { screen = Screen.LOCATION }, onMethod = { screen = Screen.METHOD }, onAdjustments = { screen = Screen.ADJUSTMENTS }, onComplete = { screen = Screen.HOME })
                Screen.LOCATION -> LocationScreen(viewModel, preferences, onBack = { screen = if (state.activationConfirmed) Screen.SETTINGS else Screen.ONBOARDING })
                Screen.METHOD -> MethodScreen(viewModel, preferences, onBack = { screen = if (state.activationConfirmed) Screen.SETTINGS else Screen.ONBOARDING })
                Screen.ADJUSTMENTS -> AdjustmentsScreen(viewModel, preferences, uiState.preview, onBack = { screen = if (state.activationConfirmed) Screen.SETTINGS else Screen.ONBOARDING })
                Screen.SETTINGS -> SettingsScreen(viewModel, preferences, dynamicColor, onBack = { screen = Screen.HOME }, onLocation = { screen = Screen.LOCATION }, onMethod = { screen = Screen.METHOD }, onAdjustments = { screen = Screen.ADJUSTMENTS }, onPrivacy = { screen = Screen.PRIVACY }, onLicenses = { screen = Screen.LICENSES }, onTroubleshooting = { screen = Screen.TROUBLESHOOTING })
                Screen.PRIVACY -> ReadScreen(R.string.privacy_title, R.string.privacy_body) { screen = Screen.SETTINGS }
                Screen.LICENSES -> ReadScreen(R.string.licenses_title, R.string.licenses_body) { screen = Screen.SETTINGS }
                Screen.TROUBLESHOOTING -> ReadScreen(R.string.troubleshooting_title, R.string.troubleshooting_body) { screen = Screen.SETTINGS }
            }
        }
    }
}

@Composable
private fun Page(title: String, onBack: (() -> Unit)? = null, action: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    Scaffold(topBar = {
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
            Text(title, modifier = Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleLarge)
            action?.invoke()
        }
    }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Box(modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp).padding(horizontal = 20.dp)) { content() }
        }
    }
}

@Composable
private fun HomeScreen(viewModel: AlfajrViewModel, preferences: AlarmPreferences, state: AlarmState, occurrence: FajrOccurrence?, problems: List<CapabilityProblem>, warnings: List<AlarmWarning>, resolve: (CapabilityProblem) -> Unit, onSettings: () -> Unit) {
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<ScheduleResult?>(null) }
    Page(stringResource(R.string.home_title), action = { TextButton(onClick = onSettings) { Text(stringResource(R.string.action_settings)) } }) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
            item { Text(if (state.dailyEnabled) stringResource(R.string.home_alarm_on) else stringResource(R.string.home_alarm_off), style = MaterialTheme.typography.labelLarge, color = if (state.dailyEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
            item { AlarmTimeCard(occurrence, preferences) }
            item { HealthCard(state, problems, warnings, resolve) }
            item {
                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                    scope.launch { result = viewModel.setDailyEnabled(!state.dailyEnabled) }
                }) { Text(stringResource(if (state.dailyEnabled) R.string.action_disable_daily else R.string.action_enable_daily)) }
            }
            result?.let { result -> item { Text(result.userMessage(LocalContext.current), style = MaterialTheme.typography.bodyMedium) } }
            if (state.dailyEnabled) item { SkipCard(state, viewModel, preferences.location?.zoneId ?: ZoneId.systemDefault().id) }
            item { OutcomeCard(state) }
        }
    }
}

@Composable
private fun AlarmTimeCard(occurrence: FajrOccurrence?, preferences: AlarmPreferences) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (occurrence == null) Text(stringResource(R.string.home_setup_needed)) else {
                val dateLabel = if (occurrence.alarmInstant.toEpochMilliseconds().isToday(occurrence.zoneId)) R.string.label_today else R.string.label_tomorrow
                Text(stringResource(dateLabel), style = MaterialTheme.typography.labelLarge)
                TimePair(R.string.label_corrected_fajr, occurrence.correctedPrayerInstant.toEpochMilliseconds(), occurrence.zoneId)
                TimePair(R.string.label_final_alarm, occurrence.alarmInstant.toEpochMilliseconds(), occurrence.zoneId, prominent = true)
                preferences.location?.let { location ->
                    Text(location.displayNameForUi(), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.label_authoritative_zone, location.zoneId), style = MaterialTheme.typography.bodySmall)
                    if (location.zoneId != ZoneId.systemDefault().id) {
                        Text(stringResource(R.string.label_device_time, occurrence.alarmInstant.toEpochMilliseconds().timeFor(ZoneId.systemDefault().id), ZoneId.systemDefault().id), style = MaterialTheme.typography.bodySmall)
                    }
                }
                preferences.method?.let { Text(stringResource(R.string.method_value, it.localizedName()), style = MaterialTheme.typography.bodySmall) }
                if (occurrence.highLatitudeRuleActive) Text(stringResource(R.string.high_latitude_notice), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun TimePair(label: Int, millis: Long, zoneId: String, prominent: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(stringResource(label), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(millis.timeFor(zoneId), style = if (prominent) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineMedium, fontWeight = if (prominent) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun HealthCard(state: AlarmState, problems: List<CapabilityProblem>, warnings: List<AlarmWarning>, resolve: (CapabilityProblem) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.label_alarm_health), style = MaterialTheme.typography.titleMedium)
            when {
                !state.dailyEnabled -> Text(stringResource(R.string.status_disabled))
                problems.isEmpty() && warnings.isEmpty() -> Text(stringResource(R.string.status_healthy))
                else -> Text(stringResource(R.string.status_degraded))
            }
            problems.forEach { problem ->
                Text(stringResource(problem.labelResource()), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { resolve(problem) }) { Text(stringResource(problem.actionResource())) }
            }
            warnings.forEach { Text(stringResource(it.labelResource()), color = MaterialTheme.colorScheme.secondary) }
        }
    }
}

@Composable
private fun SkipCard(state: AlarmState, viewModel: AlfajrViewModel, selectedZoneId: String) {
    val scope = rememberCoroutineScope()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.skip_next_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.skip_next_body))
            if (state.skippedPrayerDate == null) Button(onClick = { scope.launch { viewModel.skipNext() } }) { Text(stringResource(R.string.skip_next_alarm)) }
            else {
                Text(stringResource(R.string.skipped_date, state.skippedPrayerDate.dateFor(selectedZoneId)))
                Button(onClick = { scope.launch { viewModel.undoSkip() } }) { Text(stringResource(R.string.undo_skip)) }
            }
        }
    }
}

@Composable
private fun OutcomeCard(state: AlarmState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(stringResource(R.string.outcome_title), style = MaterialTheme.typography.titleMedium)
            Text(state.lastOutcome?.let { stringResource(it.labelResource()) } ?: stringResource(R.string.outcome_none))
        }
    }
}

@Composable
private fun OnboardingScreen(viewModel: AlfajrViewModel, preferences: AlarmPreferences, preview: FajrOccurrence?, problems: List<CapabilityProblem>, resolve: (CapabilityProblem) -> Unit, onLocation: () -> Unit, onMethod: () -> Unit, onAdjustments: () -> Unit, onComplete: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    Page(stringResource(R.string.app_name)) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
            item {
                Column(modifier = Modifier.padding(top = 42.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(stringResource(R.string.welcome_eyebrow), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.welcome_body), style = MaterialTheme.typography.bodyLarge)
                }
            }
            item { SetupCard(1, R.string.location_title, preferences.location?.displayNameForUi() ?: stringResource(R.string.no_location_selected), onLocation) }
            item { SetupCard(2, R.string.method_title, preferences.method?.localizedName() ?: stringResource(R.string.error_method_required), onMethod) }
            item { SetupCard(3, R.string.adjustments_title, stringResource(R.string.adjustments_body), onAdjustments) }
            item { PreviewCard(preview) }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.onboarding_permissions_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.onboarding_permissions_body))
                        problems.forEach { problem -> TextButton(onClick = { resolve(problem) }) { Text(stringResource(problem.actionResource())) } }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.onboarding_test_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.onboarding_test_body))
                        val testScheduled = stringResource(R.string.test_alarm_scheduled)
                        TextButton(onClick = { scope.launch { viewModel.scheduleTest(); message = testScheduled } }) { Text(stringResource(R.string.action_test_alarm)) }
                    }
                }
            }
            item {
                Text(stringResource(R.string.onboarding_enable_body))
                Button(enabled = preferences.location != null && preferences.method != null && problems.isEmpty(), modifier = Modifier.fillMaxWidth(), onClick = {
                    scope.launch {
                        val result = viewModel.setDailyEnabled(true)
                        message = result.userMessage(context)
                        if (result is ScheduleResult.Scheduled) onComplete()
                    }
                }) { Text(stringResource(R.string.action_enable_daily)) }
                message?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SetupCard(step: Int, title: Int, detail: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(step.toString(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) { Text(stringResource(title), style = MaterialTheme.typography.titleMedium); Text(detail, style = MaterialTheme.typography.bodySmall) }
            Text(stringResource(R.string.action_change), color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun LocationScreen(viewModel: AlfajrViewModel, preferences: AlarmPreferences, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val results by viewModel.cityResults.collectAsStateWithLifecycle()
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var zoneId by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<PreferenceError?>(null) }
    LaunchedEffect(query) { viewModel.search(query) }
    Page(stringResource(R.string.location_title), onBack) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            item { Text(stringResource(R.string.location_body)) }
            item { OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.city_search_label)) }, singleLine = true) }
            if (query.isNotBlank() && results.isEmpty()) item { Text(stringResource(R.string.city_results_empty)) }
            items(results, key = { it.id }) { city ->
                Card(modifier = Modifier.fillMaxWidth().clickable { viewModel.selectLocation(city); onBack() }) {
                    Column(modifier = Modifier.padding(16.dp)) { Text(city.displayNameForUi(), style = MaterialTheme.typography.titleMedium); Text(listOfNotNull(city.administrationName, city.countryCode, city.zoneId).joinToString(stringResource(R.string.separator_dot)), style = MaterialTheme.typography.bodySmall) }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.manual_location_title), style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(latitude, { latitude = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.latitude)) }, singleLine = true)
                        OutlinedTextField(longitude, { longitude = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.longitude)) }, singleLine = true)
                        OutlinedTextField(zoneId, { zoneId = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.time_zone)) }, singleLine = true)
                        error?.let { Text(stringResource(it.labelResource()), color = MaterialTheme.colorScheme.error) }
                        Button(onClick = {
                            when (val manual = viewModel.saveManualLocation(latitude, longitude, zoneId)) {
                                is ManualLocationResult.Valid -> onBack()
                                is ManualLocationResult.Invalid -> error = manual.reason
                            }
                        }) { Text(stringResource(R.string.save_manual_location)) }
                    }
                }
            }
            preferences.location?.let { location -> item { Text(stringResource(R.string.location_value, location.displayNameForUi())) } }
        }
    }
}

@Composable
private fun MethodScreen(viewModel: AlfajrViewModel, preferences: AlarmPreferences, onBack: () -> Unit) {
    var candidate by remember(preferences.method) { mutableStateOf(preferences.method) }
    Page(stringResource(R.string.method_title), onBack) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            item { Text(stringResource(R.string.method_body)) }
            preferences.location?.let { location ->
                val suggestion = viewModel.suggestedMethod(location.countryCode)
                item { Text(stringResource(R.string.suggested_method, suggestion.localizedName())) }
            }
            items(FajrMethod.entries.toList()) { method ->
                FilterChip(modifier = Modifier.fillMaxWidth(), selected = method == candidate, onClick = { candidate = method }, label = { Text(method.localizedName()) })
            }
            item {
                val selected = candidate
                Text(selected?.let { stringResource(R.string.method_selection, it.localizedName()) } ?: stringResource(R.string.method_pending_selection))
                Button(enabled = selected != null, onClick = { selected?.let { method -> viewModel.selectMethod(method); onBack() } }) { Text(stringResource(R.string.action_confirm_method)) }
            }
        }
    }
}

@Composable
private fun AdjustmentsScreen(viewModel: AlfajrViewModel, preferences: AlarmPreferences, preview: FajrOccurrence?, onBack: () -> Unit) {
    Page(stringResource(R.string.adjustments_title), onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
            Text(stringResource(R.string.adjustments_body))
            OffsetControl(pluralStringResource(R.plurals.prayer_correction, kotlin.math.abs(preferences.correctionMinutes), preferences.correctionMinutes), preferences.correctionMinutes, -30, 30, viewModel::updateCorrection)
            OffsetControl(pluralStringResource(R.plurals.wake_offset, kotlin.math.abs(preferences.wakeOffsetMinutes), preferences.wakeOffsetMinutes), preferences.wakeOffsetMinutes, -60, 30, viewModel::updateWakeOffset)
            PreviewCard(preview)
        }
    }
}

@Composable
private fun OffsetControl(label: String, value: Int, minimum: Int, maximum: Int, change: (Int) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f))
            Button(onClick = { change((value - 1).coerceAtLeast(minimum)) }, enabled = value > minimum) { Text(stringResource(R.string.action_decrease)) }
            Text(value.toString(), modifier = Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.titleMedium)
            Button(onClick = { change((value + 1).coerceAtMost(maximum)) }, enabled = value < maximum) { Text(stringResource(R.string.action_increase)) }
        }
    }
}

@Composable
private fun PreviewCard(occurrence: FajrOccurrence?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.preview_title), style = MaterialTheme.typography.titleMedium)
            if (occurrence == null) Text(stringResource(R.string.preview_requires_setup)) else {
                val day = if (occurrence.alarmInstant.toEpochMilliseconds().isToday(occurrence.zoneId)) R.string.label_today else R.string.label_tomorrow
                Text(stringResource(R.string.preview_date, stringResource(day), occurrence.prayerLocalDate.dateFor(occurrence.zoneId)))
                Text(stringResource(R.string.preview_corrected, occurrence.correctedPrayerInstant.toEpochMilliseconds().timeFor(occurrence.zoneId)), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.preview_alarm, occurrence.alarmInstant.toEpochMilliseconds().timeFor(occurrence.zoneId)), style = MaterialTheme.typography.headlineSmall)
                if (occurrence.highLatitudeRuleActive) Text(stringResource(R.string.high_latitude_notice), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SettingsScreen(viewModel: AlfajrViewModel, preferences: AlarmPreferences, dynamicColor: Boolean, onBack: () -> Unit, onLocation: () -> Unit, onMethod: () -> Unit, onAdjustments: () -> Unit, onPrivacy: () -> Unit, onLicenses: () -> Unit, onTroubleshooting: () -> Unit) {
    val context = LocalContext.current
    val appName = stringResource(R.string.app_name)
    val scope = rememberCoroutineScope()
    val ringtonePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.let { IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java) }
            viewModel.updateRingtone(uri?.toString())
        }
    }
    Page(stringResource(R.string.settings_title), onBack) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            item { SettingsRow(R.string.settings_location, preferences.location?.displayNameForUi() ?: stringResource(R.string.no_location_selected), onLocation) }
            item { SettingsRow(R.string.settings_method, preferences.method?.localizedName() ?: stringResource(R.string.error_method_required), onMethod) }
            item { SettingsRow(R.string.settings_adjustments, stringResource(R.string.adjustments_body), onAdjustments) }
            item {
                Card(modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_sound), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.ringtone_row, preferences.ringtoneUri ?: stringResource(R.string.ringtone_default)))
                    Button(onClick = { ringtonePicker.launch(ringtoneIntent(context, preferences.ringtoneUri)) }) { Text(stringResource(R.string.action_choose_ringtone)) }
                    TextButton(onClick = { viewModel.updateRingtone(null) }) { Text(stringResource(R.string.action_use_default_ringtone)) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.vibration_switch), Modifier.weight(1f)); Switch(preferences.vibrationEnabled, viewModel::updateVibration) }
                } }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_behavior), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.snooze_length_label))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(5, 10).forEach { minutes -> FilterChip(preferences.snoozeMinutes == minutes, { viewModel.updateSnoozeMinutes(minutes) }, label = { Text(pluralStringResource(R.plurals.snooze_length_option, minutes, minutes)) }) } }
                    Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(stringResource(R.string.tap_to_dismiss_switch)); Text(stringResource(R.string.tap_to_dismiss_description), style = MaterialTheme.typography.bodySmall) }; Switch(preferences.tapToDismiss, viewModel::updateTapToDismiss) }
                    TextButton(onClick = { scope.launch { viewModel.scheduleTest() } }) { Text(stringResource(R.string.action_test_alarm)) }
                } }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_appearance), style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(stringResource(R.string.dynamic_color_switch)); Text(stringResource(R.string.dynamic_color_description), style = MaterialTheme.typography.bodySmall) }; Switch(dynamicColor, viewModel::updateDynamicColor) }
                    TextButton(onClick = { context.openLanguageSettings() }) { Text(stringResource(R.string.action_open_language_settings)) }
                } }
            }
            item { SettingsRow(R.string.privacy_title, "", onPrivacy) }
            item { SettingsRow(R.string.licenses_title, "", onLicenses) }
            item { SettingsRow(R.string.troubleshooting_title, "", onTroubleshooting) }
            item { Text(stringResource(R.string.about_version, context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: appName)) }
        }
    }
}

@Composable
private fun SettingsRow(title: Int, detail: String, click: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = click)) { Column(Modifier.padding(18.dp)) { Text(stringResource(title), style = MaterialTheme.typography.titleMedium); if (detail.isNotEmpty()) Text(detail, style = MaterialTheme.typography.bodySmall) } }
}

@Composable
private fun ReadScreen(title: Int, body: Int, onBack: () -> Unit) = Page(stringResource(title), onBack) { Column(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxSize().padding(top = 20.dp)) { Text(stringResource(title), style = MaterialTheme.typography.headlineMedium); Text(stringResource(body), style = MaterialTheme.typography.bodyLarge) } }

@Composable
private fun FixedLocation.displayNameForUi(): String = when {
    id.startsWith("manual:") -> stringResource(R.string.manual_location)
    LocalConfiguration.current.locales[0]?.language == "ar" -> displayNameArabic ?: displayName
    else -> displayName
}

@Composable
private fun FajrMethod.localizedName(): String = stringResource(when (this) {
    FajrMethod.MUSLIM_WORLD_LEAGUE -> R.string.method_muslim_world_league; FajrMethod.EGYPTIAN -> R.string.method_egyptian; FajrMethod.KARACHI -> R.string.method_karachi; FajrMethod.UMM_AL_QURA -> R.string.method_umm_al_qura; FajrMethod.DUBAI -> R.string.method_dubai; FajrMethod.QATAR -> R.string.method_qatar; FajrMethod.KUWAIT -> R.string.method_kuwait; FajrMethod.MOON_SIGHTING_COMMITTEE -> R.string.method_moon_sighting_committee; FajrMethod.SINGAPORE -> R.string.method_singapore; FajrMethod.TURKEY -> R.string.method_turkey
})

@Composable private fun Long.timeFor(zoneId: String): String = android.text.format.DateFormat.getTimeFormat(LocalContext.current).apply { timeZone = TimeZone.getTimeZone(zoneId) }.format(Date(this))
@Composable private fun LocalDate.dateFor(zoneId: String): String = android.text.format.DateFormat.getDateFormat(LocalContext.current).apply { timeZone = TimeZone.getTimeZone(zoneId) }.format(Date.from(java.time.LocalDateTime.of(year, month.ordinal + 1, day, 0, 0).atZone(ZoneId.of(zoneId)).toInstant()))
private fun Long.isToday(zoneId: String): Boolean = Instant.ofEpochMilli(this).atZone(ZoneId.of(zoneId)).toLocalDate() == java.time.LocalDate.now(ZoneId.of(zoneId))
private fun CapabilityProblem.labelResource() = when (this) { CapabilityProblem.CONFIGURATION_INCOMPLETE -> R.string.problem_configuration_incomplete; CapabilityProblem.EXACT_ALARMS_UNAVAILABLE -> R.string.problem_exact_alarms_unavailable; CapabilityProblem.NOTIFICATIONS_DISABLED -> R.string.problem_notifications_disabled; CapabilityProblem.FULL_SCREEN_UNAVAILABLE -> R.string.problem_full_screen_unavailable; CapabilityProblem.SCHEDULING_FAILED -> R.string.problem_scheduling_failed }
private fun CapabilityProblem.actionResource() = when (this) { CapabilityProblem.NOTIFICATIONS_DISABLED -> R.string.action_grant_notifications; CapabilityProblem.EXACT_ALARMS_UNAVAILABLE -> R.string.action_exact_alarm_settings; CapabilityProblem.FULL_SCREEN_UNAVAILABLE -> R.string.action_full_screen_settings; else -> R.string.action_settings }
private fun AlarmWarning.labelResource() = when (this) { AlarmWarning.ALARM_VOLUME_MUTED -> R.string.warning_alarm_volume_muted; AlarmWarning.ALARM_VOLUME_LOW -> R.string.warning_alarm_volume_low; AlarmWarning.TIME_ZONE_MISMATCH -> R.string.warning_time_zone_mismatch }
private fun AlarmOutcome.labelResource() = when (this) { AlarmOutcome.DISMISSED -> R.string.outcome_dismissed; AlarmOutcome.SNOOZED -> R.string.outcome_snoozed; AlarmOutcome.MISSED -> R.string.outcome_missed; AlarmOutcome.SKIPPED -> R.string.outcome_skipped }
private fun PreferenceError.labelResource() = when (this) { PreferenceError.LOCATION_REQUIRED -> R.string.error_location_required; PreferenceError.METHOD_REQUIRED -> R.string.error_method_required; PreferenceError.INVALID_LATITUDE -> R.string.error_invalid_latitude; PreferenceError.INVALID_LONGITUDE -> R.string.error_invalid_longitude; PreferenceError.INVALID_TIME_ZONE -> R.string.error_invalid_time_zone }
private fun ScheduleResult.userMessage(context: Context): String = when (this) { is ScheduleResult.Scheduled -> context.getString(R.string.status_healthy); is ScheduleResult.TemporaryScheduled -> context.getString(R.string.test_alarm_scheduled); is ScheduleResult.Disabled -> context.getString(R.string.status_disabled); is ScheduleResult.ActionRequired -> context.getString(problem.labelResource()); is ScheduleResult.InvalidConfiguration -> context.getString(problem.labelResource()) }
private fun Context.openCapabilitySettings(problem: CapabilityProblem) {
    val appUri = "package:$packageName".toUri()
    val intent = when (problem) {
        CapabilityProblem.EXACT_ALARMS_UNAVAILABLE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, appUri)
        } else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, appUri)
        CapabilityProblem.FULL_SCREEN_UNAVAILABLE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, appUri)
        } else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, appUri)
        else -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    }
    runCatching { startActivity(intent) }
}
private fun Context.openLanguageSettings() {
    val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Settings.ACTION_APP_LOCALE_SETTINGS else Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    runCatching { startActivity(Intent(action, "package:$packageName".toUri())) }
}
private fun ringtoneIntent(context: Context, current: String?) = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM).putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, context.getString(R.string.ringtone_picker_title)).putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false).putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)).putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current?.toUri())
