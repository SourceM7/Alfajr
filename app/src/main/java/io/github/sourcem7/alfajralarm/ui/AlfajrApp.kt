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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.util.Date
import java.util.TimeZone

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
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                    ) + fadeIn(animationSpec = tween(300))
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                    ) + fadeOut(animationSpec = tween(300))
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                    ) + fadeIn(animationSpec = tween(300))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                    ) + fadeOut(animationSpec = tween(300))
                },
            ) {
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
                    DetailScaffold(
                        title = stringResource(R.string.location_title),
                        onBack = { navController.popBackStack() },
                    ) {
                        LocationContent(
                            viewModel = viewModel,
                            preferences = preferences,
                            onLocationChosen = { navController.popBackStack() },
                        )
                    }
                }
                composable(Routes.METHOD) {
                    DetailScaffold(
                        title = stringResource(R.string.method_title),
                        onBack = { navController.popBackStack() },
                    ) {
                        MethodContent(
                            viewModel = viewModel,
                            preferences = preferences,
                            onConfirmed = { navController.popBackStack() },
                        )
                    }
                }
                composable(Routes.ADJUSTMENTS) {
                    DetailScaffold(
                        title = stringResource(R.string.adjustments_title),
                        onBack = { navController.popBackStack() },
                    ) {
                        AdjustmentsContent(
                            viewModel = viewModel,
                            preferences = preferences,
                            preview = uiState.preview,
                        )
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
                    DetailScaffold(
                        title = stringResource(R.string.privacy_title),
                        onBack = { navController.popBackStack() },
                    ) {
                        ReadBody(R.string.privacy_title, R.string.privacy_body)
                    }
                }
                composable(Routes.LICENSES) {
                    DetailScaffold(
                        title = stringResource(R.string.licenses_title),
                        onBack = { navController.popBackStack() },
                    ) {
                        ReadBody(R.string.licenses_title, R.string.licenses_body)
                    }
                }
                composable(Routes.TROUBLESHOOTING) {
                    DetailScaffold(
                        title = stringResource(R.string.troubleshooting_title),
                        onBack = { navController.popBackStack() },
                    ) {
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
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val horizontalPadding = if (screenWidth >= 600) MdSpacing.md else MdSpacing.sm
    val maxContentWidth = when {
        screenWidth >= 1_200 -> 960.dp
        screenWidth >= 600 -> 720.dp
        else -> 600.dp
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = maxContentWidth)
                .padding(horizontal = horizontalPadding),
        ) {
            content()
        }
    }
}

@Composable
private fun DetailScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.content_description_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            CenteredContent(content)
        }
    }
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
    var showDisableConfirmation by rememberSaveable { mutableStateOf(false) }
    val issues = problems.size

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sunrise),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            stringResource(R.string.home_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.content_description_open_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            CenteredContent {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
                    contentPadding = PaddingValues(vertical = MdSpacing.xs),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        AlarmHeroCard(
                            occurrence = occurrence,
                            preferences = preferences,
                            dailyEnabled = state.dailyEnabled,
                        )
                    }

                    item {
                        DailyAlarmToggleCard(
                            enabled = state.dailyEnabled,
                            onToggle = { enabled ->
                                if (enabled) {
                                    scope.launch {
                                        val result = viewModel.setDailyEnabled(true)
                                        snackbar.showSnackbar(result.userMessage(context))
                                    }
                                } else {
                                    showDisableConfirmation = true
                                }
                            },
                        )
                    }

                    if (issues > 0 || warnings.isNotEmpty()) {
                        item {
                            StatusRow(
                                issueCount = issues,
                                warningCount = warnings.size,
                                onDetails = { statusSheetOpen = true },
                            )
                        }
                    }

                    if (state.dailyEnabled) {
                        item {
                            CompactSkipRow(
                                state = state,
                                zoneId = preferences.location?.zoneId ?: ZoneId.systemDefault().id,
                                onSkip = {
                                    scope.launch {
                                        snackbar.showSnackbar(viewModel.skipNext().userMessage(context))
                                    }
                                },
                                onUndoSkip = {
                                    scope.launch {
                                        snackbar.showSnackbar(viewModel.undoSkip().userMessage(context))
                                    }
                                },
                            )
                        }
                    }

                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = MdSpacing.xs, vertical = MdSpacing.xxs),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_history),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
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
    }

    if (statusSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { statusSheetOpen = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = MdSpacing.md, vertical = MdSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
            ) {
                Text(
                    stringResource(R.string.label_alarm_health),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() },
                )
                CapabilityIssueList(problems = problems, warnings = warnings, resolve = resolve)
                Spacer(Modifier.height(MdSpacing.md))
            }
        }
    }

    if (showDisableConfirmation) {
        AlertDialog(
            onDismissRequest = { showDisableConfirmation = false },
            title = {
                Text(
                    stringResource(R.string.disable_daily_alarm_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    stringResource(R.string.disable_daily_alarm_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisableConfirmation = false
                        scope.launch {
                            val result = viewModel.setDailyEnabled(false)
                            snackbar.showSnackbar(result.userMessage(context))
                        }
                    },
                ) {
                    Text(stringResource(R.string.action_disable_daily))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableConfirmation = false }) {
                    Text(stringResource(R.string.action_keep_alarm))
                }
            },
        )
    }
}

@Composable
private fun AlarmHeroCard(
    occurrence: FajrOccurrence?,
    preferences: AlarmPreferences,
    dailyEnabled: Boolean,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(MdSpacing.md),
            verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
        ) {
            if (occurrence == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MdSpacing.sm),
                    modifier = Modifier.padding(vertical = MdSpacing.sm),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sunrise),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Text(
                        stringResource(R.string.home_setup_needed),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            } else {
                val isToday = occurrence.alarmInstant.toEpochMilliseconds().isToday(occurrence.zoneId)
                val dateLabel = if (isToday) R.string.label_today else R.string.label_tomorrow

                // Header badge row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = stringResource(dateLabel).uppercase(),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (dailyEnabled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline
                                ),
                        )
                        Text(
                            text = stringResource(
                                if (dailyEnabled) R.string.hero_alarm_scheduled else R.string.hero_alarm_off
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                // Main alarm time display
                Column {
                    Text(
                        text = stringResource(R.string.hero_next_fajr),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = occurrence.alarmInstant.toEpochMilliseconds().timeFor(occurrence.zoneId),
                        style = MaterialTheme.typography.displayLarge.copy(
                            letterSpacing = (-1.5).sp,
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                // Sub-details container
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_alarm_notification),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                stringResource(
                                    R.string.preview_corrected,
                                    occurrence.correctedPrayerInstant.toEpochMilliseconds().timeFor(occurrence.zoneId),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        preferences.location?.let { location ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_location),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    listOfNotNull(
                                        location.displayNameForUi(),
                                        preferences.method?.localizedName(),
                                    ).joinToString(stringResource(R.string.separator_dot)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        if (occurrence.highLatitudeRuleActive) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_info),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    stringResource(R.string.high_latitude_notice),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyAlarmToggleCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val cardShape = MaterialTheme.shapes.large
    val containerColor by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "toggleContainerColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "toggleContentColor",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .toggleable(
                value = enabled,
                role = Role.Switch,
                onValueChange = { checked ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggle(checked)
                },
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MdSpacing.md, vertical = MdSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_alarm_notification),
                        contentDescription = null,
                        tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        stringResource(if (enabled) R.string.home_alarm_on else R.string.home_alarm_off),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                    )
                    Text(
                        stringResource(if (enabled) R.string.status_healthy else R.string.status_disabled),
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.8f),
                    )
                }
            }

            Switch(
                checked = enabled,
                onCheckedChange = null,
                thumbContent = if (enabled) {
                    {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else null,
            )
        }
    }
}

@Composable
private fun StatusRow(issueCount: Int, warningCount: Int, onDetails: () -> Unit) {
    val isError = issueCount > 0
    val containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
    val contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onDetails),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MdSpacing.md, vertical = MdSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    painter = painterResource(if (isError) R.drawable.ic_warning else R.drawable.ic_info),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp),
                )
                Column {
                    Text(
                        stringResource(R.string.status_degraded),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                    )
                    Text(
                        stringResource(R.string.label_alarm_health),
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.8f),
                    )
                }
            }

            FilledTonalButton(
                onClick = onDetails,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    stringResource(R.string.home_view_details),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun CapabilityIssueList(
    problems: List<CapabilityProblem>,
    warnings: List<AlarmWarning>,
    resolve: (CapabilityProblem) -> Unit,
) {
    if (problems.isEmpty() && warnings.isEmpty()) {
        Text(stringResource(R.string.status_healthy), style = MaterialTheme.typography.bodyMedium)
        return
    }
    problems.forEach { problem ->
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(MdSpacing.sm), verticalArrangement = Arrangement.spacedBy(MdSpacing.xxs)) {
                Text(
                    stringResource(problem.labelResource()),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                FilledTonalButton(
                    onClick = { resolve(problem) },
                    shape = CircleShape,
                ) {
                    Text(stringResource(problem.actionResource()))
                }
            }
        }
    }
    warnings.forEach { warning ->
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(MdSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_warning),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    stringResource(warning.labelResource()),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun CompactSkipRow(state: AlarmState, zoneId: String, onSkip: () -> Unit, onUndoSkip: () -> Unit) {
    val isSkipped = state.skippedPrayerDate != null
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSkipped) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MdSpacing.md, vertical = MdSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSkipped) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip),
                        contentDescription = null,
                        tint = if (isSkipped) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column {
                    Text(
                        stringResource(R.string.skip_next_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (isSkipped) {
                        Text(
                            stringResource(R.string.skipped_date, state.skippedPrayerDate.dateFor(zoneId)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    } else {
                        Text(
                            stringResource(R.string.skip_next_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            FilledTonalButton(
                onClick = if (isSkipped) onUndoSkip else onSkip,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    stringResource(if (isSkipped) R.string.undo_skip else R.string.skip_next_alarm),
                    style = MaterialTheme.typography.labelMedium,
                )
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

    val animatedProgress by animateFloatAsState(
        targetValue = (step + 1).toFloat() / totalSteps,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "wizardProgress",
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            CenteredContent {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (step > 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(MdSpacing.xxs)) {
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                            )
                            Text(
                                stringResource(R.string.setup_step, step + 1, totalSteps),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        AnimatedContent(
                            targetState = step,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                        slideOutHorizontally { width -> -width } + fadeOut()
                                    )
                                } else {
                                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                        slideOutHorizontally { width -> width } + fadeOut()
                                    )
                                }
                            },
                            label = "onboardingStep",
                        ) { currentStep ->
                            when (currentStep) {
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
    Column(
        verticalArrangement = Arrangement.spacedBy(MdSpacing.md),
        modifier = Modifier
            .fillMaxSize()
            .padding(top = MdSpacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_sunrise),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(38.dp),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(MdSpacing.xxs)) {
            Text(
                stringResource(R.string.welcome_eyebrow),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )
            Text(
                stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(MdSpacing.xs)) {
            FeatureHighlight(
                icon = R.drawable.ic_shield,
                title = stringResource(R.string.welcome_feature_offline_title),
                description = stringResource(R.string.welcome_feature_offline_desc),
            )
            FeatureHighlight(
                icon = R.drawable.ic_shield,
                title = stringResource(R.string.welcome_feature_privacy_title),
                description = stringResource(R.string.welcome_feature_privacy_desc),
            )
            FeatureHighlight(
                icon = R.drawable.ic_sunrise,
                title = stringResource(R.string.welcome_feature_exact_title),
                description = stringResource(R.string.welcome_feature_exact_desc),
            )
        }

        Spacer(Modifier.weight(1f))

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = CircleShape,
            onClick = onStart,
        ) {
            Text(
                stringResource(R.string.action_begin_setup),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun FeatureHighlight(icon: Int, title: String, description: String) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PermissionsStep(problems: List<CapabilityProblem>, resolve: (CapabilityProblem) -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(MdSpacing.md),
            verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
        ) {
            Text(
                stringResource(R.string.onboarding_permissions_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.onboarding_permissions_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (problems.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            stringResource(R.string.status_healthy),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            } else {
                problems.forEach { problem ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                stringResource(problem.labelResource()),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            FilledTonalButton(
                                onClick = { resolve(problem) },
                                shape = CircleShape,
                            ) {
                                Text(stringResource(problem.actionResource()))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TestStep(preview: FajrOccurrence?, onTest: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MdSpacing.sm)) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(
                modifier = Modifier.padding(MdSpacing.md),
                verticalArrangement = Arrangement.spacedBy(MdSpacing.xs),
            ) {
                Text(
                    stringResource(R.string.onboarding_test_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.onboarding_test_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(
                    onClick = onTest,
                    shape = CircleShape,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_alarm_notification),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(stringResource(R.string.action_test_alarm))
                    }
                }
            }
        }
        Text(
            stringResource(R.string.onboarding_enable_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        PreviewCard(preview)
    }
}

@Composable
private fun WizardControls(
    showBack: Boolean,
    onBack: () -> Unit,
    nextEnabled: Boolean,
    nextLabel: String,
    onNext: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MdSpacing.sm),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (showBack) {
            FilledTonalButton(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = CircleShape,
                onClick = onBack,
            ) {
                Text(stringResource(R.string.action_back))
            }
        }
        Button(
            modifier = Modifier
                .weight(2f)
                .height(48.dp),
            shape = CircleShape,
            enabled = nextEnabled,
            onClick = onNext,
        ) {
            Text(nextLabel)
        }
    }
}

@Composable
private fun LocationContent(
    viewModel: AlfajrViewModel,
    preferences: AlarmPreferences,
    onLocationChosen: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val results by viewModel.cityResults.collectAsStateWithLifecycle()
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var zoneId by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<PreferenceError?>(null) }

    LaunchedEffect(query) { viewModel.search(query) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
        contentPadding = PaddingValues(vertical = MdSpacing.xs),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                stringResource(R.string.location_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.city_search_label)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.action_clear),
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(28.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
        }

        if (query.isNotBlank() && results.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(R.string.city_results_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(MdSpacing.md),
                    )
                }
            }
        }

        items(results, key = { it.id }) { city ->
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.selectLocation(city)
                        onLocationChosen()
                    },
            ) {
                ListItem(
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_location),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                    headlineContent = {
                        Text(
                            city.displayNameForUi(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    supportingContent = {
                        Text(
                            listOfNotNull(city.administrationName, city.countryCode, city.zoneId)
                                .joinToString(stringResource(R.string.separator_dot)),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(MdSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
                ) {
                    Text(
                        stringResource(R.string.manual_location_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = { latitude = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.latitude)) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = longitude,
                        onValueChange = { longitude = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.longitude)) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = zoneId,
                        onValueChange = { zoneId = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.time_zone)) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                    )
                    error?.let {
                        Text(
                            stringResource(it.labelResource()),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    FilledTonalButton(
                        shape = CircleShape,
                        onClick = {
                            when (val manual = viewModel.saveManualLocation(latitude, longitude, zoneId)) {
                                is ManualLocationResult.Valid -> onLocationChosen()
                                is ManualLocationResult.Invalid -> error = manual.reason
                            }
                        },
                    ) {
                        Text(stringResource(R.string.save_manual_location))
                    }
                }
            }
        }

        preferences.location?.let { location ->
            item {
                Text(
                    stringResource(R.string.location_value, location.displayNameForUi()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun MethodContent(
    viewModel: AlfajrViewModel,
    preferences: AlarmPreferences,
    onConfirmed: () -> Unit,
) {
    var candidate by remember(preferences.method) { mutableStateOf(preferences.method) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
        contentPadding = PaddingValues(vertical = MdSpacing.xs),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                stringResource(R.string.method_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        preferences.location?.let { location ->
            val suggestion = viewModel.suggestedMethod(location.countryCode)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_calculate),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            stringResource(R.string.suggested_method, suggestion.localizedName()),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        items(FajrMethod.entries.toList()) { method ->
            val isSelected = method == candidate
            val isSuggested = preferences.location?.countryCode?.let {
                viewModel.suggestedMethod(it) == method
            } ?: false

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { candidate = method },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MdSpacing.md, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            method.localizedName(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        )
                        if (isSuggested) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    stringResource(R.string.badge_suggested),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = { candidate = method },
                    )
                }
            }
        }

        item {
            val selected = candidate
            Column(
                verticalArrangement = Arrangement.spacedBy(MdSpacing.xs),
                modifier = Modifier.padding(top = MdSpacing.xs),
            ) {
                Text(
                    selected?.let { stringResource(R.string.method_selection, it.localizedName()) }
                        ?: stringResource(R.string.method_pending_selection),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    enabled = selected != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = CircleShape,
                    onClick = {
                        selected?.let { method ->
                            viewModel.selectMethod(method)
                            onConfirmed()
                        }
                    },
                ) {
                    Text(stringResource(R.string.action_confirm_method))
                }
            }
        }
    }
}

@Composable
private fun AdjustmentsContent(
    viewModel: AlfajrViewModel,
    preferences: AlarmPreferences,
    preview: FajrOccurrence?,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MdSpacing.md),
        modifier = Modifier.fillMaxSize(),
    ) {
        Text(
            stringResource(R.string.adjustments_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OffsetControl(
            label = pluralStringResource(
                R.plurals.prayer_correction,
                kotlin.math.abs(preferences.correctionMinutes),
                preferences.correctionMinutes,
            ),
            value = preferences.correctionMinutes,
            minimum = -30,
            maximum = 30,
            change = viewModel::updateCorrection,
        )

        OffsetControl(
            label = pluralStringResource(
                R.plurals.wake_offset,
                kotlin.math.abs(preferences.wakeOffsetMinutes),
                preferences.wakeOffsetMinutes,
            ),
            value = preferences.wakeOffsetMinutes,
            minimum = -60,
            maximum = 30,
            change = viewModel::updateWakeOffset,
        )

        PreviewCard(preview)
    }
}

@Composable
private fun OffsetControl(
    label: String,
    value: Int,
    minimum: Int,
    maximum: Int,
    change: (Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MdSpacing.md, vertical = MdSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        change((value - 1).coerceAtLeast(minimum))
                    },
                    enabled = value > minimum,
                    shape = CircleShape,
                ) {
                    Text(
                        stringResource(R.string.action_decrease),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.widthIn(min = 48.dp),
                ) {
                    Text(
                        text = if (value > 0) "+$value" else value.toString(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }

                FilledTonalIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        change((value + 1).coerceAtMost(maximum))
                    },
                    enabled = value < maximum,
                    shape = CircleShape,
                ) {
                    Text(
                        stringResource(R.string.action_increase),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewCard(occurrence: FajrOccurrence?) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(MdSpacing.md),
            verticalArrangement = Arrangement.spacedBy(MdSpacing.xs),
        ) {
            Text(
                stringResource(R.string.preview_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (occurrence == null) {
                Text(
                    stringResource(R.string.preview_requires_setup),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val day = if (occurrence.alarmInstant.toEpochMilliseconds().isToday(occurrence.zoneId)) R.string.label_today else R.string.label_tomorrow
                Text(
                    stringResource(
                        R.string.preview_date,
                        stringResource(day),
                        occurrence.prayerLocalDate.dateFor(occurrence.zoneId),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(
                        R.string.preview_corrected,
                        occurrence.correctedPrayerInstant.toEpochMilliseconds().timeFor(occurrence.zoneId),
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(
                        R.string.preview_alarm,
                        occurrence.alarmInstant.toEpochMilliseconds().timeFor(occurrence.zoneId),
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                if (occurrence.highLatitudeRuleActive) {
                    Text(
                        stringResource(R.string.high_latitude_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
            val uri = result.data?.let {
                IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            }
            viewModel.updateRingtone(uri?.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.content_description_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            CenteredContent {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = MdSpacing.sm),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // Prayer Section
                    item {
                        SettingsGroup(title = stringResource(R.string.settings_section_prayer)) {
                            SettingsItem(
                                icon = R.drawable.ic_location,
                                headline = stringResource(R.string.settings_location),
                                supporting = preferences.location?.displayNameForUi()
                                    ?: stringResource(R.string.no_location_selected),
                                trailing = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_chevron_right),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                onClick = onLocation,
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            SettingsItem(
                                icon = R.drawable.ic_calculate,
                                headline = stringResource(R.string.settings_method),
                                supporting = preferences.method?.localizedName()
                                    ?: stringResource(R.string.error_method_required),
                                trailing = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_chevron_right),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                onClick = onMethod,
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            SettingsItem(
                                icon = R.drawable.ic_tune,
                                headline = stringResource(R.string.settings_adjustments),
                                supporting = stringResource(R.string.adjustments_body),
                                trailing = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_chevron_right),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                onClick = onAdjustments,
                            )
                        }
                    }

                    // Alarm Section
                    item {
                        SettingsGroup(title = stringResource(R.string.settings_section_alarm)) {
                            SettingsItem(
                                icon = R.drawable.ic_music,
                                headline = stringResource(R.string.settings_sound),
                                supporting = stringResource(
                                    R.string.ringtone_row,
                                    preferences.ringtoneUri ?: stringResource(R.string.ringtone_default),
                                ),
                                trailing = {
                                    FilledTonalButton(
                                        onClick = { ringtonePicker.launch(ringtoneIntent(context, preferences.ringtoneUri)) },
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    ) {
                                        Text(stringResource(R.string.action_choose_ringtone), style = MaterialTheme.typography.labelMedium)
                                    }
                                },
                            )
                            if (preferences.ringtoneUri != null) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = MdSpacing.md, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    TextButton(onClick = { viewModel.updateRingtone(null) }) {
                                        Text(stringResource(R.string.action_use_default_ringtone))
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            SettingsItem(
                                icon = R.drawable.ic_vibration,
                                headline = stringResource(R.string.vibration_switch),
                                trailing = {
                                    Switch(
                                        checked = preferences.vibrationEnabled,
                                        onCheckedChange = viewModel::updateVibration,
                                    )
                                },
                                onClick = { viewModel.updateVibration(!preferences.vibrationEnabled) },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Column(modifier = Modifier.padding(horizontal = MdSpacing.md, vertical = 12.dp)) {
                                Text(
                                    stringResource(R.string.snooze_length_label),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Spacer(Modifier.height(8.dp))
                                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                    listOf(5, 10).forEachIndexed { index, minutes ->
                                        SegmentedButton(
                                            selected = preferences.snoozeMinutes == minutes,
                                            onClick = { viewModel.updateSnoozeMinutes(minutes) },
                                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                                            label = { Text(pluralStringResource(R.plurals.snooze_length_option, minutes, minutes)) },
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            SettingsItem(
                                icon = R.drawable.ic_tune,
                                headline = stringResource(R.string.tap_to_dismiss_switch),
                                supporting = stringResource(R.string.tap_to_dismiss_description),
                                trailing = {
                                    Switch(
                                        checked = preferences.tapToDismiss,
                                        onCheckedChange = viewModel::updateTapToDismiss,
                                    )
                                },
                                onClick = { viewModel.updateTapToDismiss(!preferences.tapToDismiss) },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            SettingsItem(
                                icon = R.drawable.ic_alarm_notification,
                                headline = stringResource(R.string.action_test_alarm),
                                supporting = stringResource(R.string.onboarding_test_body),
                                trailing = {
                                    FilledTonalButton(
                                        onClick = {
                                            scope.launch {
                                                snackbar.showSnackbar(viewModel.scheduleTest().userMessage(context))
                                            }
                                        },
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    ) {
                                        Text(stringResource(R.string.action_test_alarm), style = MaterialTheme.typography.labelMedium)
                                    }
                                },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            SettingsItem(
                                icon = R.drawable.ic_skip,
                                headline = stringResource(R.string.skip_next_title),
                                supporting = state.skippedPrayerDate?.let {
                                    stringResource(R.string.skipped_date, it.dateFor(preferences.location?.zoneId ?: ZoneId.systemDefault().id))
                                } ?: stringResource(R.string.skip_next_body),
                                trailing = {
                                    if (state.skippedPrayerDate == null) {
                                        TextButton(onClick = { scope.launch { snackbar.showSnackbar(viewModel.skipNext().userMessage(context)) } }) {
                                            Text(stringResource(R.string.skip_next_alarm))
                                        }
                                    } else {
                                        TextButton(onClick = { scope.launch { snackbar.showSnackbar(viewModel.undoSkip().userMessage(context)) } }) {
                                            Text(stringResource(R.string.undo_skip))
                                        }
                                    }
                                },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            SettingsItem(
                                icon = R.drawable.ic_history,
                                headline = stringResource(R.string.outcome_title),
                                supporting = state.lastOutcome?.let { stringResource(it.labelResource()) }
                                    ?: stringResource(R.string.outcome_none),
                            )
                        }
                    }

                    // Appearance Section
                    item {
                        SettingsGroup(title = stringResource(R.string.settings_section_appearance)) {
                            SettingsItem(
                                icon = R.drawable.ic_palette,
                                headline = stringResource(R.string.dynamic_color_switch),
                                supporting = stringResource(R.string.dynamic_color_description),
                                trailing = {
                                    Switch(
                                        checked = dynamicColor,
                                        onCheckedChange = viewModel::updateDynamicColor,
                                    )
                                },
                                onClick = { viewModel.updateDynamicColor(!dynamicColor) },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            SettingsItem(
                                icon = R.drawable.ic_language,
                                headline = stringResource(R.string.language_settings),
                                supporting = stringResource(R.string.language_settings_description),
                                trailing = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_chevron_right),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                onClick = { context.openLanguageSettings() },
                            )
                        }
                    }

                    // About Section
                    item {
                        SettingsGroup(title = stringResource(R.string.settings_about)) {
                            SettingsItem(
                                icon = R.drawable.ic_shield,
                                headline = stringResource(R.string.privacy_title),
                                trailing = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_chevron_right),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                onClick = onPrivacy,
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            SettingsItem(
                                icon = R.drawable.ic_info,
                                headline = stringResource(R.string.licenses_title),
                                trailing = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_chevron_right),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                onClick = onLicenses,
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            SettingsItem(
                                icon = R.drawable.ic_info,
                                headline = stringResource(R.string.troubleshooting_title),
                                trailing = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_chevron_right),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                onClick = onTroubleshooting,
                            )
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = MdSpacing.md),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                stringResource(
                                    R.string.about_version,
                                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: appName,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MdSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MdSpacing.xs),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = MdSpacing.xs),
        )
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsItem(
    icon: Int,
    headline: String,
    supporting: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val modifier = Modifier
        .fillMaxWidth()
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(horizontal = MdSpacing.md, vertical = 14.dp)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                headline,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!supporting.isNullOrEmpty()) {
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        trailing?.invoke()
    }
}

@Composable
private fun ReadBody(title: Int, body: Int) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MdSpacing.sm),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MdSpacing.md),
            modifier = Modifier.padding(MdSpacing.md),
        ) {
            Text(
                stringResource(title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(body),
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 26.sp,
            )
        }
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
    FajrMethod.MUSLIM_WORLD_LEAGUE -> R.string.method_muslim_world_league
    FajrMethod.EGYPTIAN -> R.string.method_egyptian
    FajrMethod.KARACHI -> R.string.method_karachi
    FajrMethod.UMM_AL_QURA -> R.string.method_umm_al_qura
    FajrMethod.DUBAI -> R.string.method_dubai
    FajrMethod.QATAR -> R.string.method_qatar
    FajrMethod.KUWAIT -> R.string.method_kuwait
    FajrMethod.MOON_SIGHTING_COMMITTEE -> R.string.method_moon_sighting_committee
    FajrMethod.SINGAPORE -> R.string.method_singapore
    FajrMethod.TURKEY -> R.string.method_turkey
})

@Composable
private fun Long.timeFor(zoneId: String): String =
    android.text.format.DateFormat.getTimeFormat(LocalContext.current).apply {
        timeZone = TimeZone.getTimeZone(zoneId)
    }.format(Date(this))

@Composable
private fun LocalDate.dateFor(zoneId: String): String =
    android.text.format.DateFormat.getDateFormat(LocalContext.current).apply {
        timeZone = TimeZone.getTimeZone(zoneId)
    }.format(
        Date.from(
            java.time.LocalDateTime.of(year, month.ordinal + 1, day, 0, 0)
                .atZone(ZoneId.of(zoneId)).toInstant()
        )
    )

private fun Long.isToday(zoneId: String): Boolean =
    Instant.ofEpochMilli(this).atZone(ZoneId.of(zoneId)).toLocalDate() == java.time.LocalDate.now(ZoneId.of(zoneId))

private fun CapabilityProblem.labelResource() = when (this) {
    CapabilityProblem.CONFIGURATION_INCOMPLETE -> R.string.problem_configuration_incomplete
    CapabilityProblem.EXACT_ALARMS_UNAVAILABLE -> R.string.problem_exact_alarms_unavailable
    CapabilityProblem.NOTIFICATIONS_DISABLED -> R.string.problem_notifications_disabled
    CapabilityProblem.FULL_SCREEN_UNAVAILABLE -> R.string.problem_full_screen_unavailable
    CapabilityProblem.SCHEDULING_FAILED -> R.string.problem_scheduling_failed
}

private fun CapabilityProblem.actionResource() = when (this) {
    CapabilityProblem.NOTIFICATIONS_DISABLED -> R.string.action_grant_notifications
    CapabilityProblem.EXACT_ALARMS_UNAVAILABLE -> R.string.action_exact_alarm_settings
    CapabilityProblem.FULL_SCREEN_UNAVAILABLE -> R.string.action_full_screen_settings
    else -> R.string.action_settings
}

private fun AlarmWarning.labelResource() = when (this) {
    AlarmWarning.ALARM_VOLUME_MUTED -> R.string.warning_alarm_volume_muted
    AlarmWarning.ALARM_VOLUME_LOW -> R.string.warning_alarm_volume_low
    AlarmWarning.TIME_ZONE_MISMATCH -> R.string.warning_time_zone_mismatch
}

private fun AlarmOutcome.labelResource() = when (this) {
    AlarmOutcome.DISMISSED -> R.string.outcome_dismissed
    AlarmOutcome.SNOOZED -> R.string.outcome_snoozed
    AlarmOutcome.MISSED -> R.string.outcome_missed
    AlarmOutcome.SKIPPED -> R.string.outcome_skipped
}

private fun PreferenceError.labelResource() = when (this) {
    PreferenceError.LOCATION_REQUIRED -> R.string.error_location_required
    PreferenceError.METHOD_REQUIRED -> R.string.error_method_required
    PreferenceError.INVALID_LATITUDE -> R.string.error_invalid_latitude
    PreferenceError.INVALID_LONGITUDE -> R.string.error_invalid_longitude
    PreferenceError.INVALID_TIME_ZONE -> R.string.error_invalid_time_zone
}

private fun ScheduleResult.userMessage(context: Context): String = when (this) {
    is ScheduleResult.Scheduled -> context.getString(R.string.status_healthy)
    is ScheduleResult.TemporaryScheduled -> context.getString(R.string.test_alarm_scheduled)
    is ScheduleResult.Disabled -> context.getString(R.string.status_disabled)
    is ScheduleResult.ActionRequired -> context.getString(problem.labelResource())
    is ScheduleResult.InvalidConfiguration -> context.getString(problem.labelResource())
}

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
    val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Settings.ACTION_APP_LOCALE_SETTINGS
    } else {
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    }
    runCatching { startActivity(Intent(action, "package:$packageName".toUri())) }
}

private fun ringtoneIntent(context: Context, current: String?) =
    Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, context.getString(R.string.ringtone_picker_title))
        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
        .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current?.toUri())
