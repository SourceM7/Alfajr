@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import io.github.sourcem7.alfajralarm.ui.theme.MdSpacing
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.util.TimeZone
import java.util.Date

private object Routes {
    const val HOME = "home"
    const val ONBOARDING = "onboarding"
    const val LOCATION = "location"
    const val METHOD = "method"
    const val ADJUSTMENTS = "adjustments"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
    const val LICENSES = "licenses"
    const val TROUBLESHOOTING = "troubleshooting"
}

/** The production single-activity UI, wired with Navigation Compose. */
@Composable
fun AlfajrApp(viewModel: AlfajrViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val preferences = uiState.preferences
    val dynamicColor = uiState.dynamicColor
    val state = uiState.alarmState
    val navController = rememberNavController()
    val startDestination = if (state.activationConfirmed) Routes.HOME else Routes.ONBOARDING
    AlfajrTheme(dynamicColor = dynamicColor) {
        Surface(modifier = Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = startDestination) {
                composable(Routes.HOME) {
                    HomeScreen(
                        viewModel = viewModel,
                        preferences = preferences,
                        state = state,
                        occurrence = uiState.preview,
                        problems = uiState.health.problems,
                        warnings = uiState.health.warnings,
                        onSettings = { navController.navigate(Routes.SETTINGS) },
                    )
                }
                composable(Routes.ONBOARDING) {
                    OnboardingWizard(
                        viewModel = viewModel,
                        preferences = preferences,
                        preview = uiState.preview,
                        problems = uiState.health.problems,
                        onFinish = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        },
                    )
                }
                composable(Routes.LOCATION) {
                    DetailScaffold(title = stringResource(R.string.location_title), onBack = { navController.popBackStack() }) {
                        LocationContent(viewModel = viewModel, preferences = preferences, onLocationChosen = { navController.popBackStack() })
                    }
                }
                composable(Routes.METHOD) {
                    DetailScaffold(title = stringResource(R.string.method_title), onBack = { navController.popBackStack() }) {
                        MethodContent(viewModel = viewModel, preferences = preferences, onConfirmed = { navController.popBackStack() })
                    }
                }
                composable(Routes.ADJUSTMENTS) {
                    DetailScaffold(title = stringResource(R.string.adjustments_title), onBack = { navController.popBackStack() }) {
                        AdjustmentsContent(viewModel = viewModel, preferences = preferences, preview = uiState.preview)
                    }
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        viewModel = viewModel,
                        preferences = preferences,
                        state = state,
                        dynamicColor = dynamicColor,
                        onBack = { navController.popBackStack() },
                        onLocation = { navController.navigate(Routes.LOCATION) },
                        onMethod = { navController.navigate(Routes.METHOD) },
                        onAdjustments = { navController.navigate(Routes.ADJUSTMENTS) },
                        onPrivacy = { navController.navigate(Routes.PRIVACY) },
                        onLicenses = { navController.navigate(Routes.LICENSES) },
                        onTroubleshooting = { navController.navigate(Routes.TROUBLESHOOTING) },
                    )
                }
                composable(Routes.PRIVACY) {
                    DetailScaffold(title = stringResource(R.string.privacy_title), onBack = { navController.popBackStack() }) {
                        ReadBody(R.string.privacy_title, R.string.privacy_body)
                    }
                }
                composable(Routes.LICENSES) {
                    DetailScaffold(title = stringResource(R.string.licenses_title), onBack = { navController.popBackStack() }) {
                        ReadBody(R.string.licenses_title, R.string.licenses_body)
                    }
                }
                composable(Routes.TROUBLESHOOTING) {
                    DetailScaffold(title = stringResource(R.string.troubleshooting_title), onBack = { navController.popBackStack() }) {
                        ReadBody(R.string.troubleshooting_title, R.string.troubleshooting_body)
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberCapabilityResolver(viewModel: AlfajrViewModel): (CapabilityProblem) -> Unit {
    val context = LocalContext.current
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refreshCapabilities()
    }
    return remember(notifications) {
        { problem ->
            when {
                problem == CapabilityProblem.NOTIFICATIONS_DISABLED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                    notifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                else -> context.openCapabilitySettings(problem)
            }
        }
    }
}

@Composable
private fun CenteredContent(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp).padding(horizontal = MdSpacing.sm)) { content() }
    }
}

@Composable
private fun DetailScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) } },
        )
    }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            CenteredContent(content)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        modifier = Modifier.padding(top = MdSpacing.sm, bottom = MdSpacing.xxs),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SettingNavRow(headline: String, supporting: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(headline) },
        supportingContent = { if (supporting.isNotEmpty()) Text(supporting) },
        trailingContent = {
            Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun SwitchRow(headline: String, supporting: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(headline) },
        supportingContent = { if (!supporting.isNullOrEmpty()) Text(supporting) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        modifier = Modifier.clickable { onCheckedChange(!checked) },
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun HomeScreen(
    viewModel: AlfajrViewModel,
    preferences: AlarmPreferences,
    state: AlarmState,
    occurrence: FajrOccurrence?,
    problems: List<CapabilityProblem>,
    warnings: List<AlarmWarning>,
    onSettings: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val resolve = rememberCapabilityResolver(viewModel)
    var statusSheetOpen by remember { mutableStateOf(false) }
    val issues = problems.size
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = { TextButton(onClick = onSettings) { Text(stringResource(R.string.action_settings)) } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            CenteredContent {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(MdSpacing.sm), modifier = Modifier.fillMaxSize()) {
                    item { AlarmHeroCard(occurrence = occurrence, preferences = preferences) }
                    item {
                        StatusRow(
                            dailyEnabled = state.dailyEnabled,
                            issueCount = issues,
                            warningCount = warnings.size,
                            onDetails = { statusSheetOpen = true },
                        )
                    }
                    item {
                        Button(modifier = Modifier.fillMaxWidth(), onClick = {
                            scope.launch {
                                val result = viewModel.setDailyEnabled(!state.dailyEnabled)
                                snackbar.showSnackbar(result.userMessage(context))
                            }
                        }) { Text(stringResource(if (state.dailyEnabled) R.string.action_disable_daily else R.string.action_enable_daily)) }
                    }
                    if (state.dailyEnabled) {
                        item {
                            CompactSkipRow(
                                state = state,
                                zoneId = preferences.location?.zoneId ?: ZoneId.systemDefault().id,
                                onSkip = { scope.launch { snackbar.showSnackbar(viewModel.skipNext().userMessage(context)) } },
                                onUndoSkip = { scope.launch { snackbar.showSnackbar(viewModel.undoSkip().userMessage(context)) } },
                            )
                        }
                    }
                    item {
                        Text(
                            stringResource(
                                R.string.home_last_outcome,
                                state.lastOutcome?.let { stringResource(it.labelResource()) }
                                    ?: stringResource(R.string.outcome_none),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
    if (statusSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { statusSheetOpen = false },
            sheetState = rememberModalBottomSheetState(),
        ) {
            Column(modifier = Modifier.padding(horizontal = MdSpacing.md, vertical = MdSpacing.sm), verticalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
                Text(stringResource(R.string.label_alarm_health), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                CapabilityIssueList(problems = problems, warnings = warnings, resolve = resolve)
                Spacer(Modifier.height(MdSpacing.md))
            }
        }
    }
}

@Composable
private fun AlarmHeroCard(occurrence: FajrOccurrence?, preferences: AlarmPreferences) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(MdSpacing.md), verticalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
            if (occurrence == null) {
                Text(stringResource(R.string.home_setup_needed), style = MaterialTheme.typography.bodyLarge)
            } else {
                val dateLabel = if (occurrence.alarmInstant.toEpochMilliseconds().isToday(occurrence.zoneId)) R.string.label_today else R.string.label_tomorrow
                Text(stringResource(dateLabel), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(
                    occurrence.alarmInstant.toEpochMilliseconds().timeFor(occurrence.zoneId),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.preview_corrected, occurrence.correctedPrayerInstant.toEpochMilliseconds().timeFor(occurrence.zoneId)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                preferences.location?.let { location ->
                    Text(
                        listOfNotNull(location.displayNameForUi(), preferences.method?.localizedName()).joinToString(stringResource(R.string.separator_dot)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (occurrence.highLatitudeRuleActive) {
                    Text(stringResource(R.string.high_latitude_notice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun StatusDot(active: Boolean, hasIssues: Boolean) {
    val color = when {
        !active -> MaterialTheme.colorScheme.outline
        hasIssues -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
}

@Composable
private fun StatusRow(dailyEnabled: Boolean, issueCount: Int, warningCount: Int, onDetails: () -> Unit) {
    val headline = if (dailyEnabled) stringResource(R.string.home_alarm_on) else stringResource(R.string.home_alarm_off)
    val supporting = when {
        !dailyEnabled -> stringResource(R.string.status_disabled)
        issueCount == 0 && warningCount == 0 -> stringResource(R.string.status_healthy)
        else -> stringResource(R.string.status_degraded)
    }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = {
                Text(
                    headline,
                    color = if (dailyEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            },
            supportingContent = { Text(supporting) },
            leadingContent = { StatusDot(active = dailyEnabled, hasIssues = issueCount > 0) },
            trailingContent = {
                if (issueCount > 0 || warningCount > 0) {
                    TextButton(onClick = onDetails) { Text(stringResource(R.string.home_view_details)) }
                }
            },
            modifier = if (issueCount > 0 || warningCount > 0) Modifier.clickable(onClick = onDetails) else Modifier,
        )
    }
}

@Composable
private fun CapabilityIssueList(problems: List<CapabilityProblem>, warnings: List<AlarmWarning>, resolve: (CapabilityProblem) -> Unit) {
    if (problems.isEmpty() && warnings.isEmpty()) {
        Text(stringResource(R.string.status_healthy), style = MaterialTheme.typography.bodyMedium)
        return
    }
    problems.forEach { problem ->
        Text(stringResource(problem.labelResource()), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = { resolve(problem) }) { Text(stringResource(problem.actionResource())) }
    }
    warnings.forEach { Text(stringResource(it.labelResource()), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyMedium) }
}

@Composable
private fun CompactSkipRow(state: AlarmState, zoneId: String, onSkip: () -> Unit, onUndoSkip: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = MdSpacing.sm, vertical = MdSpacing.xxs), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.skip_next_title), style = MaterialTheme.typography.titleSmall)
                if (state.skippedPrayerDate != null) {
                    Text(stringResource(R.string.skipped_date, state.skippedPrayerDate.dateFor(zoneId)), style = MaterialTheme.typography.bodySmall)
                }
            }
            if (state.skippedPrayerDate == null) {
                TextButton(onClick = onSkip) { Text(stringResource(R.string.skip_next_alarm)) }
            } else {
                TextButton(onClick = onUndoSkip) { Text(stringResource(R.string.undo_skip)) }
            }
        }
    }
}

@Composable
private fun OnboardingWizard(
    viewModel: AlfajrViewModel,
    preferences: AlarmPreferences,
    preview: FajrOccurrence?,
    problems: List<CapabilityProblem>,
    onFinish: () -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val totalSteps = 6
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val resolve = rememberCapabilityResolver(viewModel)
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            CenteredContent {
                Column(verticalArrangement = Arrangement.spacedBy(MdSpacing.sm), modifier = Modifier.fillMaxSize()) {
                    Column(verticalArrangement = Arrangement.spacedBy(MdSpacing.xxs)) {
                        LinearProgressIndicator(progress = { (step + 1).toFloat() / totalSteps }, modifier = Modifier.fillMaxWidth())
                        Text(stringResource(R.string.setup_step, step + 1, totalSteps), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        when (step) {
                            0 -> WelcomeStep(onStart = { step = 1 })
                            1 -> LocationContent(viewModel = viewModel, preferences = preferences, onLocationChosen = {})
                            2 -> MethodContent(viewModel = viewModel, preferences = preferences, onConfirmed = {})
                            3 -> AdjustmentsContent(viewModel = viewModel, preferences = preferences, preview = preview)
                            4 -> PermissionsStep(problems = problems, resolve = resolve)
                            else -> TestStep(
                                preview = preview,
                                onTest = { scope.launch { snackbar.showSnackbar(viewModel.scheduleTest().userMessage(context)) } },
                            )
                        }
                    }
                    if (step > 0) {
                        WizardControls(
                            showBack = true,
                            onBack = { step-- },
                            nextEnabled = when (step) {
                                1 -> preferences.location != null
                                2 -> preferences.method != null
                                4 -> problems.isEmpty()
                                5 -> preferences.location != null && preferences.method != null && problems.isEmpty()
                                else -> true
                            },
                            nextLabel = if (step == 5) stringResource(R.string.action_enable_daily) else stringResource(R.string.action_continue),
                            onNext = {
                                if (step == 5) {
                                    scope.launch {
                                        val result = viewModel.setDailyEnabled(true)
                                        snackbar.showSnackbar(result.userMessage(context))
                                        if (result is ScheduleResult.Scheduled) onFinish()
                                    }
                                } else {
                                    step++
                                }
                            },
                        )
                    }
                    Spacer(Modifier.height(MdSpacing.xs))
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MdSpacing.sm), modifier = Modifier.padding(top = MdSpacing.lg)) {
        Text(stringResource(R.string.welcome_eyebrow), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.welcome_body), style = MaterialTheme.typography.bodyLarge)
        Button(modifier = Modifier.fillMaxWidth(), onClick = onStart) { Text(stringResource(R.string.action_begin_setup)) }
    }
}

@Composable
private fun PermissionsStep(problems: List<CapabilityProblem>, resolve: (CapabilityProblem) -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(MdSpacing.md), verticalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
            Text(stringResource(R.string.onboarding_permissions_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.onboarding_permissions_body), style = MaterialTheme.typography.bodyMedium)
            if (problems.isEmpty()) {
                Text(stringResource(R.string.status_healthy), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            } else {
                problems.forEach { problem ->
                    Text(stringResource(problem.labelResource()), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { resolve(problem) }) { Text(stringResource(problem.actionResource())) }
                }
            }
        }
    }
}

@Composable
private fun TestStep(preview: FajrOccurrence?, onTest: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MdSpacing.sm)) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(MdSpacing.md), verticalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
                Text(stringResource(R.string.onboarding_test_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.onboarding_test_body), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onTest) { Text(stringResource(R.string.action_test_alarm)) }
            }
        }
        Text(stringResource(R.string.onboarding_enable_body), style = MaterialTheme.typography.bodyMedium)
        PreviewCard(preview)
    }
}

@Composable
private fun WizardControls(showBack: Boolean, onBack: () -> Unit, nextEnabled: Boolean, nextLabel: String, onNext: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
        if (showBack) {
            FilledTonalButton(modifier = Modifier.weight(1f), onClick = onBack) { Text(stringResource(R.string.action_back)) }
        }
        Button(modifier = Modifier.weight(2f), enabled = nextEnabled, onClick = onNext) { Text(nextLabel) }
    }
}

@Composable
private fun LocationContent(viewModel: AlfajrViewModel, preferences: AlarmPreferences, onLocationChosen: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val results by viewModel.cityResults.collectAsStateWithLifecycle()
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var zoneId by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<PreferenceError?>(null) }
    LaunchedEffect(query) { viewModel.search(query) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(MdSpacing.xs), modifier = Modifier.fillMaxSize()) {
        item { Text(stringResource(R.string.location_body), style = MaterialTheme.typography.bodyMedium) }
        item { OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.city_search_label)) }, singleLine = true) }
        if (query.isNotBlank() && results.isEmpty()) item { Text(stringResource(R.string.city_results_empty), style = MaterialTheme.typography.bodyMedium) }
        items(results, key = { it.id }) { city ->
            ElevatedCard(modifier = Modifier.fillMaxWidth().clickable { viewModel.selectLocation(city); onLocationChosen() }) {
                ListItem(
                    headlineContent = { Text(city.displayNameForUi()) },
                    supportingContent = { Text(listOfNotNull(city.administrationName, city.countryCode, city.zoneId).joinToString(stringResource(R.string.separator_dot))) },
                )
            }
        }
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(MdSpacing.md), verticalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
                    Text(stringResource(R.string.manual_location_title), style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(latitude, { latitude = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.latitude)) }, singleLine = true)
                    OutlinedTextField(longitude, { longitude = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.longitude)) }, singleLine = true)
                    OutlinedTextField(zoneId, { zoneId = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.time_zone)) }, singleLine = true)
                    error?.let { Text(stringResource(it.labelResource()), color = MaterialTheme.colorScheme.error) }
                    FilledTonalButton(onClick = {
                        when (val manual = viewModel.saveManualLocation(latitude, longitude, zoneId)) {
                            is ManualLocationResult.Valid -> onLocationChosen()
                            is ManualLocationResult.Invalid -> error = manual.reason
                        }
                    }) { Text(stringResource(R.string.save_manual_location)) }
                }
            }
        }
        preferences.location?.let { location -> item { Text(stringResource(R.string.location_value, location.displayNameForUi()), style = MaterialTheme.typography.bodySmall) } }
    }
}

@Composable
private fun MethodContent(viewModel: AlfajrViewModel, preferences: AlarmPreferences, onConfirmed: () -> Unit) {
    var candidate by remember(preferences.method) { mutableStateOf(preferences.method) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(MdSpacing.xs), modifier = Modifier.fillMaxSize()) {
        item { Text(stringResource(R.string.method_body), style = MaterialTheme.typography.bodyMedium) }
        preferences.location?.let { location ->
            val suggestion = viewModel.suggestedMethod(location.countryCode)
            item { Text(stringResource(R.string.suggested_method, suggestion.localizedName()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(FajrMethod.entries.toList()) { method ->
            ListItem(
                headlineContent = { Text(method.localizedName()) },
                trailingContent = { RadioButton(selected = method == candidate, onClick = { candidate = method }) },
                modifier = Modifier.clickable { candidate = method },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        item {
            val selected = candidate
            Text(
                selected?.let { stringResource(R.string.method_selection, it.localizedName()) } ?: stringResource(R.string.method_pending_selection),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(enabled = selected != null, modifier = Modifier.fillMaxWidth(), onClick = { selected?.let { method -> viewModel.selectMethod(method); onConfirmed() } }) { Text(stringResource(R.string.action_confirm_method)) }
        }
    }
}

@Composable
private fun AdjustmentsContent(viewModel: AlfajrViewModel, preferences: AlarmPreferences, preview: FajrOccurrence?) {
    Column(verticalArrangement = Arrangement.spacedBy(MdSpacing.sm), modifier = Modifier.fillMaxSize()) {
        Text(stringResource(R.string.adjustments_body), style = MaterialTheme.typography.bodyMedium)
        OffsetControl(pluralStringResource(R.plurals.prayer_correction, kotlin.math.abs(preferences.correctionMinutes), preferences.correctionMinutes), preferences.correctionMinutes, -30, 30, viewModel::updateCorrection)
        OffsetControl(pluralStringResource(R.plurals.wake_offset, kotlin.math.abs(preferences.wakeOffsetMinutes), preferences.wakeOffsetMinutes), preferences.wakeOffsetMinutes, -60, 30, viewModel::updateWakeOffset)
        PreviewCard(preview)
    }
}

@Composable
private fun OffsetControl(label: String, value: Int, minimum: Int, maximum: Int, change: (Int) -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(MdSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            FilledTonalButton(onClick = { change((value - 1).coerceAtLeast(minimum)) }, enabled = value > minimum) { Text(stringResource(R.string.action_decrease)) }
            Text(value.toString(), modifier = Modifier.padding(horizontal = MdSpacing.xs), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            FilledTonalButton(onClick = { change((value + 1).coerceAtMost(maximum)) }, enabled = value < maximum) { Text(stringResource(R.string.action_increase)) }
        }
    }
}

@Composable
private fun PreviewCard(occurrence: FajrOccurrence?) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(MdSpacing.md), verticalArrangement = Arrangement.spacedBy(MdSpacing.xxs)) {
            Text(stringResource(R.string.preview_title), style = MaterialTheme.typography.titleMedium)
            if (occurrence == null) Text(stringResource(R.string.preview_requires_setup), style = MaterialTheme.typography.bodyMedium) else {
                val day = if (occurrence.alarmInstant.toEpochMilliseconds().isToday(occurrence.zoneId)) R.string.label_today else R.string.label_tomorrow
                Text(stringResource(R.string.preview_date, stringResource(day), occurrence.prayerLocalDate.dateFor(occurrence.zoneId)), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.preview_corrected, occurrence.correctedPrayerInstant.toEpochMilliseconds().timeFor(occurrence.zoneId)), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.preview_alarm, occurrence.alarmInstant.toEpochMilliseconds().timeFor(occurrence.zoneId)), style = MaterialTheme.typography.headlineSmall)
                if (occurrence.highLatitudeRuleActive) Text(stringResource(R.string.high_latitude_notice), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    viewModel: AlfajrViewModel,
    preferences: AlarmPreferences,
    state: AlarmState,
    dynamicColor: Boolean,
    onBack: () -> Unit,
    onLocation: () -> Unit,
    onMethod: () -> Unit,
    onAdjustments: () -> Unit,
    onPrivacy: () -> Unit,
    onLicenses: () -> Unit,
    onTroubleshooting: () -> Unit,
) {
    val context = LocalContext.current
    val appName = stringResource(R.string.app_name)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val ringtonePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.let { IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java) }
            viewModel.updateRingtone(uri?.toString())
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            CenteredContent {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item { SectionHeader(stringResource(R.string.settings_section_prayer)) }
                    item { SettingNavRow(stringResource(R.string.settings_location), preferences.location?.displayNameForUi() ?: stringResource(R.string.no_location_selected), onLocation) }
                    item { SettingNavRow(stringResource(R.string.settings_method), preferences.method?.localizedName() ?: stringResource(R.string.error_method_required), onMethod) }
                    item { SettingNavRow(stringResource(R.string.settings_adjustments), stringResource(R.string.adjustments_body), onAdjustments) }
                    item { SectionHeader(stringResource(R.string.settings_section_alarm)) }
                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_sound)) },
                            supportingContent = { Text(stringResource(R.string.ringtone_row, preferences.ringtoneUri ?: stringResource(R.string.ringtone_default))) },
                            trailingContent = {
                                FilledTonalButton(onClick = { ringtonePicker.launch(ringtoneIntent(context, preferences.ringtoneUri)) }) { Text(stringResource(R.string.action_choose_ringtone)) }
                            },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = MdSpacing.sm, vertical = MdSpacing.xxs)) {
                            TextButton(onClick = { viewModel.updateRingtone(null) }) { Text(stringResource(R.string.action_use_default_ringtone)) }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    item { SwitchRow(stringResource(R.string.vibration_switch), null, preferences.vibrationEnabled, viewModel::updateVibration) }
                    item {
                        ListItem(headlineContent = { Text(stringResource(R.string.snooze_length_label)) })
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = MdSpacing.sm, vertical = MdSpacing.xxs)) {
                            listOf(5, 10).forEachIndexed { index, minutes ->
                                SegmentedButton(
                                    selected = preferences.snoozeMinutes == minutes,
                                    onClick = { viewModel.updateSnoozeMinutes(minutes) },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                                    label = { Text(pluralStringResource(R.plurals.snooze_length_option, minutes, minutes)) },
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    item { SwitchRow(stringResource(R.string.tap_to_dismiss_switch), stringResource(R.string.tap_to_dismiss_description), preferences.tapToDismiss, viewModel::updateTapToDismiss) }
                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.action_test_alarm)) },
                            supportingContent = { Text(stringResource(R.string.onboarding_test_body)) },
                            trailingContent = {
                                FilledTonalButton(onClick = { scope.launch { snackbar.showSnackbar(viewModel.scheduleTest().userMessage(context)) } }) { Text(stringResource(R.string.action_test_alarm)) }
                            },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.skip_next_title)) },
                            supportingContent = {
                                Text(
                                    state.skippedPrayerDate?.let {
                                        stringResource(R.string.skipped_date, it.dateFor(preferences.location?.zoneId ?: ZoneId.systemDefault().id))
                                    } ?: stringResource(R.string.skip_next_body),
                                )
                            },
                            trailingContent = {
                                if (state.skippedPrayerDate == null) {
                                    TextButton(onClick = { scope.launch { snackbar.showSnackbar(viewModel.skipNext().userMessage(context)) } }) { Text(stringResource(R.string.skip_next_alarm)) }
                                } else {
                                    TextButton(onClick = { scope.launch { snackbar.showSnackbar(viewModel.undoSkip().userMessage(context)) } }) { Text(stringResource(R.string.undo_skip)) }
                                }
                            },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.outcome_title)) },
                            supportingContent = {
                                Text(
                                    state.lastOutcome?.let { stringResource(it.labelResource()) } ?: stringResource(R.string.outcome_none),
                                )
                            },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    item { SectionHeader(stringResource(R.string.settings_section_appearance)) }
                    item { SwitchRow(stringResource(R.string.dynamic_color_switch), stringResource(R.string.dynamic_color_description), dynamicColor, viewModel::updateDynamicColor) }
                    item { SettingNavRow(stringResource(R.string.language_settings), stringResource(R.string.language_settings_description), { context.openLanguageSettings() }) }
                    item { SectionHeader(stringResource(R.string.settings_about)) }
                    item { SettingNavRow(stringResource(R.string.privacy_title), "", onPrivacy) }
                    item { SettingNavRow(stringResource(R.string.licenses_title), "", onLicenses) }
                    item { SettingNavRow(stringResource(R.string.troubleshooting_title), "", onTroubleshooting) }
                    item {
                        Text(
                            stringResource(R.string.about_version, context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: appName),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = MdSpacing.sm),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadBody(title: Int, body: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(MdSpacing.md), modifier = Modifier.fillMaxSize().padding(top = MdSpacing.md)) {
        Text(stringResource(title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Text(stringResource(body), style = MaterialTheme.typography.bodyLarge)
    }
}

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
