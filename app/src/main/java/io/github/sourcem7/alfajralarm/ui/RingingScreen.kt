package io.github.sourcem7.alfajralarm.ui

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sourcem7.alfajralarm.R
import io.github.sourcem7.alfajralarm.domain.MAX_SNOOZE_COUNT
import io.github.sourcem7.alfajralarm.domain.RingingSession
import io.github.sourcem7.alfajralarm.domain.RingtoneSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

/** Two seconds of continuous hold, as the product specification requires. */
private const val DISMISS_HOLD_MILLIS = 2_000

/**
 * The near-black ringing screen. Snooze is a single large tap; dismiss needs a
 * two-second hold unless the accessibility tap preference is on, so a hand
 * brushing the screen cannot end the alarm.
 */
@Composable
fun RingingScreen(session: RingingSession, onSnooze: () -> Unit, onDismiss: () -> Unit) {
    MaterialTheme(colorScheme = ringingColors()) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val windowLayout = currentAppWindowLayout()
            val rootModifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp)
            if (windowLayout.showTwoPanes || windowLayout.compactHeight) {
                Row(
                    modifier = rootModifier,
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        RingingHeader(session)
                        RingtoneNotice(session.ringtoneSource)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        SnoozeControl(session, onSnooze)
                        DismissControl(session, onDismiss)
                    }
                }
            } else {
                Column(
                    modifier = rootModifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    RingingHeader(session)
                    RingtoneNotice(session.ringtoneSource)
                    SnoozeControl(session, onSnooze)
                    DismissControl(session, onDismiss)
                }
            }
        }
    }
}

/**
 * What the full-screen alarm shows between being opened and its session
 * existing. Android can launch this screen from the ringing notification before
 * the service has finished resolving the session, and a near-black window with
 * nothing in it is indistinguishable from the alarm having failed.
 */
@Composable
fun RingingStartingScreen() {
    MaterialTheme(colorScheme = ringingColors()) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.ringing_starting),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.5f))
            }
        }
    }
}

@Composable
private fun RingingHeader(session: RingingSession) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(
                    if (session.isTest) R.string.ringing_title_test else R.string.ringing_title_daily,
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = formatTime(session.alarmAt.toEpochMilliseconds()),
                fontSize = 72.sp,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SnoozeControl(session: RingingSession, onSnooze: () -> Unit) {
    if (!session.snoozeAvailable) {
        Text(
            text = pluralStringResource(R.plurals.ringing_snooze_unavailable, MAX_SNOOZE_COUNT, MAX_SNOOZE_COUNT),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        return
    }
    val label = pluralStringResource(R.plurals.action_snooze_minutes, session.snoozeMinutes, session.snoozeMinutes)
    Button(
        onClick = onSnooze,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp),
    ) {
        Text(label, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    }
    Text(
        text = stringResource(R.string.ringing_snoozes_used, session.snoozesUsed, MAX_SNOOZE_COUNT),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun DismissControl(session: RingingSession, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var progress by remember(session.sessionId) { mutableFloatStateOf(0f) }
    val hint = stringResource(
        if (session.tapToDismiss) R.string.ringing_tap_to_dismiss else R.string.ringing_hold_to_dismiss,
    )
    val label = stringResource(R.string.action_dismiss)
    // A duration scale of zero is Android's user-level request to reduce motion.
    // Keep the safety hold but do not animate a progress bar in that mode.
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }

    // Deliberately not a Button: its own click handling would consume the press
    // before the hold gesture could measure it.
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "$label. $hint"
                // A hold is impractical under TalkBack, so activating the
                // control by accessibility action dismisses directly.
                onClick(label = label) {
                    onDismiss()
                    true
                }
            }
            .pointerInput(session.tapToDismiss, session.sessionId) {
                if (session.tapToDismiss) {
                    detectTapGestures(onTap = { onDismiss() })
                } else {
                    detectTapGestures(
                        onPress = {
                            val hold: Job = scope.launch {
                                if (reduceMotion) delay(DISMISS_HOLD_MILLIS.toLong()) else {
                                    animate(
                                        initialValue = 0f,
                                        targetValue = 1f,
                                        animationSpec = tween(DISMISS_HOLD_MILLIS, easing = LinearEasing),
                                    ) { value, _ -> progress = value }
                                }
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDismiss()
                            }
                            // Releasing early cancels the hold, so a hand
                            // brushing the screen never ends the alarm.
                            tryAwaitRelease()
                            hold.cancel()
                            progress = 0f
                        },
                    )
                }
            },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            Text(label, style = MaterialTheme.typography.headlineSmall)
            Text(hint, style = MaterialTheme.typography.bodyMedium)
            if (progress > 0f && !reduceMotion) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        // The surface already announces the action and its hint.
                        .clearAndSetSemantics {},
                )
            }
        }
    }
}

@Composable
private fun RingtoneNotice(source: RingtoneSource?) {
    val (notice, containerColor, contentColor) = when (source) {
        RingtoneSource.BUNDLED -> Triple(
            stringResource(R.string.ringing_source_bundled),
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurface,
        )
        null -> Triple(
            stringResource(R.string.ringing_source_none),
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
        else -> return
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            notice,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

/** Respects the device's own 12/24-hour setting. */
@Composable
private fun formatTime(millis: Long): String =
    android.text.format.DateFormat.getTimeFormat(LocalContext.current).format(Date(millis))

/**
 * A tranquil near-black dawn palette with gentle sage-green accents.
 * The ringing screen ignores the light/dark setting because it is
 * meant to be readable in a dark room without flooding it with harsh light.
 */
private fun ringingColors() = darkColorScheme(
    background = Color(0xFF070B08),
    surface = Color(0xFF070B08),
    onBackground = Color(0xFFDFE4DD),
    onSurface = Color(0xFFDFE4DD),
    primary = Color(0xFF8BD6A3),
    onPrimary = Color(0xFF00391F),
    secondaryContainer = Color(0xFF1B251F),
    onSecondaryContainer = Color(0xFFDFE4DD),
    surfaceContainerLow = Color(0xFF111914),
    surfaceContainerHigh = Color(0xFF1B251F),
    errorContainer = Color(0xFF5C201B),
    onErrorContainer = Color(0xFFFFDAD6),
)
