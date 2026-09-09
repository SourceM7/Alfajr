@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.sourcem7.alfajralarm.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.material3.Slider
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
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
import io.github.sourcem7.alfajralarm.domain.preventsDelivery
import io.github.sourcem7.alfajralarm.ui.theme.MdSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.TimeZone
import kotlin.math.roundToInt

private object Routes {
    const val HOME = "home"
    const val ONBOARDING = "onboarding"
    const val LOCATION = "location"
    const val METHOD = "method"
    const val ADJUSTMENTS = "adjustments"
    const val SETTINGS = "settings"
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
                    )
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
private fun CenteredContent(
    maxWidth: Dp = 720.dp,
    content: @Composable () -> Unit,
) {
    val horizontalPadding = if (currentAppWindowLayout().expandedWidth) MdSpacing.md else MdSpacing.sm

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = maxWidth)
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
            CenteredContent(content = content)
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
    val windowLayout = currentAppWindowLayout()

    fun report(action: suspend () -> ScheduleResult) {
        scope.launch { snackbar.showSnackbar(action().userMessage(context)) }
    }

    val hero: @Composable (Modifier) -> Unit = { modifier ->
        AlarmHero(
            occurrence = occurrence,
            preferences = preferences,
            dailyEnabled = state.dailyEnabled,
            problems = problems,
            warnings = warnings,
            onStatus = { statusSheetOpen = true },
            onToggle = { enabled ->
                if (enabled) report { viewModel.setDailyEnabled(true) } else showDisableConfirmation = true
            },
            modifier = modifier,
        )
    }
    val details: @Composable (Modifier) -> Unit = { modifier ->
        HomeDetails(
            state = state,
            preferences = preferences,
            occurrence = occurrence,
            timeZoneMismatch = AlarmWarning.TIME_ZONE_MISMATCH in warnings,
            onSkip = { report { viewModel.skipNext() } },
            onUndoSkip = { report { viewModel.undoSkip() } },
            onTest = { report { viewModel.scheduleTest() } },
            modifier = modifier,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.home_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
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
            CenteredContent(maxWidth = 1_040.dp) {
                if (windowLayout.showTwoPanes) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = MdSpacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(MdSpacing.md),
                        verticalAlignment = Alignment.Top,
                    ) {
                        hero(Modifier.weight(1.1f))
                        details(Modifier.weight(1f))
                    }
                } else {
                    // The hero is sized from the viewport rather than left to its
                    // content, which is what stops a short healthy state from
                    // stranding half the screen empty. Anything that does not fit
                    // scrolls instead of being squeezed.
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        // Landscape has no height to spare, so the hero is left
                        // to its content there and only claims a share of a
                        // portrait screen.
                        val heroHeight = if (windowLayout.compactHeight) {
                            null
                        } else {
                            (maxHeight * HERO_HEIGHT_FRACTION).coerceAtLeast(HERO_MIN_HEIGHT)
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = MdSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
                        ) {
                            hero(heroHeight?.let { Modifier.heightIn(min = it) } ?: Modifier)
                            details(Modifier)
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
                modifier = Modifier.padding(horizontal = MdSpacing.md, vertical = MdSpacing.sm),
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
                        report { viewModel.setDailyEnabled(false) }
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

private const val HERO_HEIGHT_FRACTION = 0.44f
private val HERO_MIN_HEIGHT = 260.dp

/**
 * The one thing the home screen exists to answer: when does the alarm go off.
 *
 * Everything in it is centred on a single axis so there is one reading order —
 * state, countdown, time, date, switch — and it is given a share of the viewport
 * rather than only as much room as its text needs.
 */
@Composable
private fun AlarmHero(
    occurrence: FajrOccurrence?,
    preferences: AlarmPreferences,
    dailyEnabled: Boolean,
    problems: List<CapabilityProblem>,
    warnings: List<AlarmWarning>,
    onStatus: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    // A pre-dawn wash: warmest at the horizon line behind the time, fading into
    // the page so the hero reads as part of the screen, not a card on top of it.
    val dawn = Brush.verticalGradient(
        colors = listOf(
            colors.primaryContainer,
            colors.primaryContainer.copy(alpha = 0.45f),
            colors.surface,
        ),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(dawn)
            .padding(horizontal = MdSpacing.md, vertical = MdSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MdSpacing.sm, Alignment.CenterVertically),
    ) {
        StatusChip(
            problems = problems,
            warnings = warnings,
            dailyEnabled = dailyEnabled,
            onClick = onStatus,
        )

        if (occurrence == null) {
            Icon(
                painter = painterResource(R.drawable.ic_sunrise),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(40.dp),
            )
            Text(
                stringResource(R.string.home_setup_needed),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        } else {
            val alarmMillis = occurrence.alarmInstant.toEpochMilliseconds()
            val dateLabel = if (alarmMillis.isToday(occurrence.zoneId)) {
                R.string.label_today
            } else {
                R.string.label_tomorrow
            }
            // Grouped for TalkBack: the countdown, time and date are one
            // fact, not three unrelated lines.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MdSpacing.xxs),
            ) {
                countdownUntil(occurrence.alarmInstant)?.takeIf { dailyEnabled }?.let { countdown ->
                    Text(
                        text = countdown,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
                Text(
                    text = alarmMillis.timeFor(occurrence.zoneId),
                    style = MaterialTheme.typography.displayLarge.copy(letterSpacing = (-1.5).sp),
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.hero_alarm_date,
                        stringResource(dateLabel),
                        occurrence.prayerLocalDate.dateFor(occurrence.zoneId),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                )
            }

            if (occurrence.highLatitudeRuleActive) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MdSpacing.xs),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_info),
                        contentDescription = null,
                        tint = colors.tertiary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        stringResource(R.string.high_latitude_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.tertiary,
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MdSpacing.sm),
            modifier = Modifier
                .clip(CircleShape)
                .toggleable(
                    value = dailyEnabled,
                    role = Role.Switch,
                    onValueChange = onToggle,
                )
                .padding(horizontal = MdSpacing.sm, vertical = MdSpacing.xs),
        ) {
            Text(
                stringResource(if (dailyEnabled) R.string.home_alarm_on else R.string.home_alarm_off),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Switch(
                checked = dailyEnabled,
                // The whole row is the control, so the switch itself is not
                // separately focusable or separately announced.
                onCheckedChange = null,
                thumbContent = if (dailyEnabled) {
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

/**
 * The alarm's health in one line. A healthy alarm says so — the old screen
 * showed nothing at all when everything was fine, which reads the same as an
 * alarm that has not been checked.
 */
@Composable
private fun StatusChip(
    problems: List<CapabilityProblem>,
    warnings: List<AlarmWarning>,
    dailyEnabled: Boolean,
    onClick: () -> Unit,
) {
    val actionable = problems.isNotEmpty() || warnings.isNotEmpty()
    val label = when {
        problems.isNotEmpty() -> R.string.status_degraded
        warnings.isNotEmpty() -> R.string.status_warning
        dailyEnabled -> R.string.status_healthy
        else -> R.string.status_disabled
    }
    val icon = when {
        problems.isNotEmpty() -> R.drawable.ic_warning
        warnings.isNotEmpty() -> R.drawable.ic_info
        dailyEnabled -> R.drawable.ic_check
        else -> R.drawable.ic_info
    }
    val container = when {
        problems.isNotEmpty() -> MaterialTheme.colorScheme.errorContainer
        warnings.isNotEmpty() -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val content = when {
        problems.isNotEmpty() -> MaterialTheme.colorScheme.onErrorContainer
        warnings.isNotEmpty() -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = CircleShape,
        color = container,
        contentColor = content,
        modifier = if (actionable) Modifier.clickable(onClick = onClick) else Modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MdSpacing.sm, vertical = MdSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MdSpacing.xs),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
            if (actionable) {
                Text(
                    stringResource(R.string.home_view_details),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/**
 * Everything the hero deliberately leaves out, as one scannable list: the
 * calculated Fajr the alarm is derived from, where it is calculated for, and
 * what happened last time. Rows reuse the settings row so the two screens read
 * as the same app.
 */
@Composable
private fun HomeDetails(
    state: AlarmState,
    preferences: AlarmPreferences,
    occurrence: FajrOccurrence?,
    timeZoneMismatch: Boolean,
    onSkip: () -> Unit,
    onUndoSkip: () -> Unit,
    onTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zoneId = preferences.location?.zoneId ?: ZoneId.systemDefault().id

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
    ) {
        SettingsGroup {
            if (occurrence != null) {
                SettingsItem(
                    icon = R.drawable.ic_sunrise,
                    headline = stringResource(R.string.label_corrected_fajr),
                    trailing = { DetailValue(occurrence.correctedPrayerInstant.toEpochMilliseconds().timeFor(zoneId)) },
                )
                HomeDivider()
            }
            SettingsItem(
                icon = R.drawable.ic_location,
                headline = stringResource(R.string.label_selected_location),
                // The selected zone is authoritative, so when it differs from
                // the device's the screen shows both rather than quietly
                // presenting a time the phone's clock contradicts.
                supporting = listOfNotNull(
                    stringResource(R.string.label_authoritative_zone, zoneId),
                    if (timeZoneMismatch) {
                        stringResource(
                            R.string.label_device_time,
                            System.currentTimeMillis().timeFor(ZoneId.systemDefault().id),
                            ZoneId.systemDefault().id,
                        )
                    } else null,
                ).joinToString("\n"),
                trailing = {
                    DetailValue(
                        preferences.location?.displayNameForUi()
                            ?: stringResource(R.string.no_location_selected),
                    )
                },
            )
            HomeDivider()
            SettingsItem(
                icon = R.drawable.ic_calculate,
                headline = stringResource(R.string.label_calculation_method),
                trailing = {
                    DetailValue(
                        preferences.method?.localizedName()
                            ?: stringResource(R.string.error_method_required),
                    )
                },
            )
            if (state.dailyEnabled) {
                HomeDivider()
                val skipped = state.skippedPrayerDate
                SettingsItem(
                    icon = R.drawable.ic_skip,
                    headline = stringResource(R.string.skip_next_title),
                    supporting = skipped?.let { stringResource(R.string.skipped_date, it.dateFor(zoneId)) }
                        ?: stringResource(R.string.skip_next_body),
                    trailing = {
                        FilledTonalButton(
                            onClick = if (skipped != null) onUndoSkip else onSkip,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text(
                                stringResource(if (skipped != null) R.string.undo_skip else R.string.skip_next_alarm),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    },
                )
            }
            HomeDivider()
            SettingsItem(
                icon = R.drawable.ic_history,
                headline = stringResource(R.string.outcome_title),
                trailing = {
                    DetailValue(
                        state.lastOutcome?.let { stringResource(it.labelResource()) }
                            ?: stringResource(R.string.outcome_none),
                    )
                },
            )
        }

        FilledTonalButton(
            onClick = onTest,
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            contentPadding = PaddingValues(vertical = 14.dp),
        ) {
            Text(stringResource(R.string.action_test_alarm), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun DetailValue(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.End,
    )
}

@Composable
private fun HomeDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

/**
 * How long until the alarm, recomputed once a minute. It lives here rather than
 * in the view model because nothing outside this screen needs a ticking clock.
 */
@Composable
private fun countdownUntil(alarmAt: kotlin.time.Instant): String? {
    val alarmMillis = alarmAt.toEpochMilliseconds()
    val remaining by produceState(alarmMillis - System.currentTimeMillis(), alarmMillis) {
        while (true) {
            val left = alarmMillis - System.currentTimeMillis()
            value = left
            if (left <= 0) break
            // Wake on the minute boundary so the number never looks stale.
            delay(left % 60_000L + 1)
        }
    }
    if (remaining <= 0) return null
    val totalMinutes = remaining / 60_000L
    return when {
        totalMinutes < 1 -> stringResource(R.string.countdown_imminent)
        totalMinutes < 60 -> pluralStringResource(
            R.plurals.countdown_minutes,
            totalMinutes.toInt(),
            totalMinutes.toInt(),
        )
        else -> stringResource(
            R.string.countdown_hours_minutes,
            (totalMinutes / 60).toInt(),
            (totalMinutes % 60).toInt(),
        )
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
private fun OnboardingWizard(
    viewModel: AlfajrViewModel,
    preferences: AlarmPreferences,
    preview: FajrOccurrence?,
    problems: List<CapabilityProblem>,
    onFinish: () -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val totalSteps = 3
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val resolve = rememberCapabilityResolver(viewModel)

    // Without this, system back on step 2 leaves the app instead of going to
    // step 1, which loses everything the user has entered so far.
    BackHandler(enabled = step > 0) { step-- }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            CenteredContent(maxWidth = 640.dp) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (step > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            IconButton(onClick = { step-- }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_back),
                                    contentDescription = stringResource(R.string.content_description_back),
                                )
                            }
                            Text(
                                stringResource(R.string.setup_step, step, totalSteps),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        // How much is left is worth showing, not just counting.
                        val progress by animateFloatAsState(
                            targetValue = step.toFloat() / totalSteps,
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                            label = "setupProgress",
                        )
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
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
                            SetupPage(
                                step = currentStep,
                                viewModel = viewModel,
                                preferences = preferences,
                                preview = preview,
                                problems = problems,
                                resolve = resolve,
                                onTest = {
                                    scope.launch {
                                        snackbar.showSnackbar(
                                            viewModel.scheduleTest().userMessage(context),
                                        )
                                    }
                                },
                                onStart = { step = 1 },
                            )
                        }
                    }

                    if (step > 0) {
                        WizardControls(
                            nextEnabled = when (step) {
                                1 -> preferences.location != null
                                2 -> preferences.method != null
                                3 -> preferences.location != null &&
                                    preferences.method != null &&
                                    problems.none { it.preventsDelivery }
                                else -> true
                            },
                            nextLabel = if (step == totalSteps) {
                                stringResource(R.string.action_enable_daily)
                            } else {
                                stringResource(R.string.action_continue)
                            },
                            onNext = {
                                if (step == totalSteps) {
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
private fun SetupPage(
    step: Int,
    viewModel: AlfajrViewModel,
    preferences: AlarmPreferences,
    preview: FajrOccurrence?,
    problems: List<CapabilityProblem>,
    resolve: (CapabilityProblem) -> Unit,
    onTest: () -> Unit,
    onStart: () -> Unit,
) {
    when (step) {
        0 -> WelcomeStep(onStart = onStart)
        1 -> LocationContent(
            viewModel = viewModel,
            preferences = preferences,
            onLocationChosen = {},
        )
        2 -> MethodContent(
            viewModel = viewModel,
            preferences = preferences,
            onConfirmed = {},
            confirmInside = false,
        )
        else -> ReviewAndEnableStep(
            preview = preview,
            problems = problems,
            resolve = resolve,
            onTest = onTest,
        )
    }
}

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = MdSpacing.lg),
    ) {
        Spacer(Modifier.weight(0.4f))
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_sunrise),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .padding(MdSpacing.md)
                    .size(56.dp),
            )
        }
        Spacer(Modifier.height(MdSpacing.md))
        Text(
            stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(MdSpacing.xs))
        Text(
            stringResource(R.string.welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 440.dp),
        )
        Spacer(Modifier.height(MdSpacing.lg))
        // The three promises the product makes, which had copy in both locales
        // but were never rendered.
        Column(
            verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
            modifier = Modifier.widthIn(max = 440.dp),
        ) {
            WelcomePromise(
                icon = R.drawable.ic_shield,
                title = stringResource(R.string.welcome_feature_offline_title),
                body = stringResource(R.string.welcome_feature_offline_desc),
            )
            WelcomePromise(
                icon = R.drawable.ic_info,
                title = stringResource(R.string.welcome_feature_privacy_title),
                body = stringResource(R.string.welcome_feature_privacy_desc),
            )
            WelcomePromise(
                icon = R.drawable.ic_calculate,
                title = stringResource(R.string.welcome_feature_exact_title),
                body = stringResource(R.string.welcome_feature_exact_desc),
            )
        }
        Spacer(Modifier.weight(1f))
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
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
private fun WelcomePromise(icon: Int, title: String, body: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MdSpacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReviewAndEnableStep(
    preview: FajrOccurrence?,
    problems: List<CapabilityProblem>,
    resolve: (CapabilityProblem) -> Unit,
    onTest: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = MdSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
    ) {
        item {
            Text(
                stringResource(R.string.setup_review_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
        }
        item {
            Text(
                stringResource(R.string.setup_review_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { PreviewCard(preview) }
        if (problems.isEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MdSpacing.xs),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(R.string.status_healthy),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
            item { PermissionsStep(problems = problems, resolve = resolve) }
        }
        item {
            TextButton(onClick = onTest) {
                Icon(
                    painter = painterResource(R.drawable.ic_alarm_notification),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(MdSpacing.xs))
                Text(stringResource(R.string.action_test_alarm))
            }
        }
    }
}

@Composable
private fun PermissionsStep(problems: List<CapabilityProblem>, resolve: (CapabilityProblem) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
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
                    val isRequired = problem.preventsDelivery
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isRequired) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.tertiaryContainer
                            },
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                stringResource(problem.labelResource()),
                                color = if (isRequired) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                },
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
private fun WizardControls(
    nextEnabled: Boolean,
    nextLabel: String,
    onNext: () -> Unit,
) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = CircleShape,
        enabled = nextEnabled,
        onClick = onNext,
    ) {
        Text(nextLabel)
    }
}

@Composable
private fun LocationContent(
    viewModel: AlfajrViewModel,
    preferences: AlarmPreferences,
    onLocationChosen: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results by viewModel.cityResults.collectAsStateWithLifecycle()
    var latitude by rememberSaveable { mutableStateOf("") }
    var longitude by rememberSaveable { mutableStateOf("") }
    var zoneId by rememberSaveable { mutableStateOf("") }
    var showManual by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<PreferenceError?>(null) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { viewModel.prepareCitySearch() }
    LaunchedEffect(query) { viewModel.search(query) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(MdSpacing.sm),
        contentPadding = PaddingValues(vertical = MdSpacing.xs),
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        item {
            Text(
                stringResource(R.string.location_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            Text(
                stringResource(R.string.location_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        preferences.location?.let { location ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("selected-city"),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    ListItem(
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        },
                        headlineContent = {
                            Text(
                                location.displayNameForUi(),
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                        supportingContent = {
                            Text(location.zoneId)
                        },
                        overlineContent = {
                            Text(stringResource(R.string.label_selected_location))
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent,
                            headlineColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            supportingColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            overlineColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("city-search"),
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { focusManager.clearFocus() },
                ),
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

        items(results.take(5), key = { it.id }) { city ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("city-result-${city.id}")
                    .clickable {
                        query = ""
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
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
            TextButton(onClick = { showManual = !showManual }) {
                Text(
                    stringResource(
                        if (showManual) R.string.action_hide_coordinates
                        else R.string.action_enter_coordinates,
                    ),
                )
            }
        }

        if (showManual) item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
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
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next,
                        ),
                    )
                    OutlinedTextField(
                        value = longitude,
                        onValueChange = { longitude = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.longitude)) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next,
                        ),
                    )
                    OutlinedTextField(
                        value = zoneId,
                        onValueChange = { zoneId = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.time_zone)) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() },
                        ),
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
                                is ManualLocationResult.Valid -> {
                                    keyboardController?.hide()
                                    focusManager.clearFocus(force = true)
                                    onLocationChosen()
                                }
                                is ManualLocationResult.Invalid -> error = manual.reason
                            }
                        },
                    ) {
                        Text(stringResource(R.string.save_manual_location))
                    }
                }
            }
        }

    }
}

@Composable
private fun MethodContent(
    viewModel: AlfajrViewModel,
    preferences: AlarmPreferences,
    onConfirmed: () -> Unit,
    confirmInside: Boolean = true,
) {
    val suggestedMethod = preferences.location?.countryCode?.let(viewModel::suggestedMethod)
    var candidate by remember(preferences.method, suggestedMethod) {
        mutableStateOf(preferences.method ?: suggestedMethod.takeIf { confirmInside })
    }
    val selectCandidate: (FajrMethod) -> Unit = { method ->
        candidate = method
        if (!confirmInside) viewModel.selectMethod(method)
    }

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

        items(FajrMethod.entries.toList().sortedByDescending { it == suggestedMethod }) { method ->
            val isSelected = method == candidate
            val isSuggested = method == suggestedMethod
            val containerColor by animateColorAsState(
                targetValue = when {
                    isSelected -> MaterialTheme.colorScheme.secondaryContainer
                    else -> Color.Transparent
                },
                label = "methodRecommendationContainer",
            )

            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = containerColor,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectCandidate(method) },
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
                            fontWeight = if (isSuggested || isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            },
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
                        onClick = { selectCandidate(method) },
                    )
                }
            }
        }

        if (confirmInside) item {
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
            label = stringResource(R.string.label_prayer_correction),
            value = preferences.correctionMinutes,
            minimum = -30,
            maximum = 30,
            change = viewModel::updateCorrection,
        )

        OffsetControl(
            label = stringResource(R.string.label_wake_offset),
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
    var sliderValue by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val decreaseDescription = stringResource(R.string.content_description_decrease)
    val increaseDescription = stringResource(R.string.content_description_increase)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(MdSpacing.md),
            verticalArrangement = Arrangement.spacedBy(MdSpacing.xs),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            // The direction is spelled out rather than left to a minus sign,
            // which Arabic plurals drop along with the number they qualify.
            Text(
                offsetDescription(value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    change(sliderValue.roundToInt())
                },
                valueRange = minimum.toFloat()..maximum.toFloat(),
                steps = maximum - minimum - 1,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
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
                        modifier = Modifier.semantics {
                            contentDescription = decreaseDescription
                        },
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.widthIn(min = 48.dp),
                ) {
                    Text(
                        text = signedMinutes(value),
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
                        modifier = Modifier.semantics {
                            contentDescription = increaseDescription
                        },
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
) {
    val context = LocalContext.current
    val appName = stringResource(R.string.app_name)
    val ringtoneTitle = rememberRingtoneTitle(preferences.ringtoneUri)
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
            CenteredContent(maxWidth = 840.dp) {
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
                                    ringtoneTitle,
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
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                )
                            }
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

                    item {
                        // The GeoNames line is a licence condition of the bundled
                        // city data (CC BY 4.0), not decoration. It lives here
                        // rather than behind a Licenses screen so the obligation
                        // is met without a settings entry nobody opens.
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = MdSpacing.md),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(MdSpacing.xxs),
                        ) {
                            Text(
                                stringResource(
                                    R.string.about_version,
                                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: appName,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                stringResource(R.string.about_attribution),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A titled group of rows. Home reuses it without a title, so its detail list and
 * the settings list are visibly the same component rather than two lookalikes.
 */
@Composable
private fun SettingsGroup(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (title == null) 0.dp else MdSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MdSpacing.xs),
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = MdSpacing.xs),
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun rememberRingtoneTitle(uri: String?): String {
    val context = LocalContext.current
    val fallback = stringResource(R.string.ringtone_default)
    return remember(context, uri, fallback) {
        if (uri == null) {
            fallback
        } else {
            runCatching {
                RingtoneManager.getRingtone(context, uri.toUri())?.getTitle(context)
            }.getOrNull() ?: fallback
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

/**
 * "12 minutes earlier" rather than "-12 minutes". Arabic drops the numeral in
 * its one/two plural forms, which silently erased the sign when the direction
 * was carried by a minus sign alone.
 */
@Composable
private fun offsetDescription(minutes: Int): String = when {
    minutes == 0 -> stringResource(R.string.offset_none)
    minutes < 0 -> pluralStringResource(R.plurals.offset_earlier, -minutes, -minutes)
    else -> pluralStringResource(R.plurals.offset_later, minutes, minutes)
}

/** The stepper readout, in the digits and sign of the active locale. */
@Composable
private fun signedMinutes(minutes: Int): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale, minutes) {
        val format = NumberFormat.getIntegerInstance(locale)
        if (format is DecimalFormat) format.positivePrefix = "+"
        format.format(minutes)
    }
}

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
    is ScheduleResult.TemporaryScheduled -> context.getString(
        if (degradedBy.isEmpty()) R.string.test_alarm_scheduled else R.string.test_alarm_scheduled_degraded,
    )
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
