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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
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

/** Tall enough to hit without aiming. */
private val ACTION_MIN_HEIGHT = 88.dp

/**
 * The full-screen ringing UI. Snooze is a single large tap; dismiss needs a
 * two-second hold unless the accessibility tap preference is on, so a hand
 * brushing the screen cannot end the alarm.
 *
 * The time sits in the upper part of the screen and the two actions are pinned
 * to the bottom, where a thumb reaches them without the phone being shifted in
 * a half-asleep hand.
 */
@Composable
fun RingingScreen(session: RingingSession, onSnooze: () -> Unit, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ringing_surface"),
        color = MaterialTheme.colorScheme.background,
    ) {
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
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SnoozeControl(session, onSnooze)
                    DismissControl(session, onDismiss)
                }
            }
        } else {
            Column(
                modifier = rootModifier,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Only the header scrolls, so a very large font can never push
                // the actions off the screen.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    RingingHeader(session)
                    RingtoneNotice(session.ringtoneSource)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
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
 * the service has finished resolving the session, and an empty window with
 * nothing in it is indistinguishable from the alarm having failed.
 */
@Composable
fun RingingStartingScreen() {
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
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.5f))
        }
    }
}

@Composable
private fun RingingHeader(session: RingingSession) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_sunrise),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp),
        )
        Text(
            text = stringResource(
                if (session.isTest) R.string.ringing_title_test else R.string.ringing_title_daily,
            ),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = formatTime(session.alarmAt.toEpochMilliseconds()),
            // The digits are the one thing read from across a dark room.
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 76.sp,
                lineHeight = 88.sp,
                letterSpacing = (-1.5).sp,
            ),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SnoozeControl(session: RingingSession, onSnooze: () -> Unit) {
    if (!session.snoozeAvailable) {
        Text(
            text = pluralStringResource(R.plurals.ringing_snooze_unavailable, MAX_SNOOZE_COUNT, MAX_SNOOZE_COUNT),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        return
    }
    val label = pluralStringResource(R.plurals.action_snooze_minutes, session.snoozeMinutes, session.snoozeMinutes)
    Button(
        onClick = onSnooze,
        shape = CircleShape,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ACTION_MIN_HEIGHT),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(
                text = stringResource(R.string.ringing_snoozes_used, session.snoozesUsed, MAX_SNOOZE_COUNT),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
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
    // Keep the safety hold but do not animate the fill in that mode.
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val fill = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.18f)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    // Deliberately not a Button: its own click handling would consume the press
    // before the hold gesture could measure it.
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ACTION_MIN_HEIGHT)
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
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ACTION_MIN_HEIGHT)
                // The hold fills the button itself from its leading edge, so
                // progress is shown without anything appearing or moving.
                .drawBehind {
                    if (progress > 0f) {
                        val filled = size.width * progress
                        drawRect(
                            color = fill,
                            topLeft = Offset(if (rtl) size.width - filled else 0f, 0f),
                            size = Size(filled, size.height),
                        )
                    }
                }
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(label, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(hint, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
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
