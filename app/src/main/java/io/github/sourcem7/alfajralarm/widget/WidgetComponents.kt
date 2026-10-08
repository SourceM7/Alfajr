package io.github.sourcem7.alfajralarm.widget

import android.content.Context
import android.os.SystemClock
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.sourcem7.alfajralarm.MainActivity
import io.github.sourcem7.alfajralarm.R
import io.github.sourcem7.alfajralarm.app.AppGraph
import io.github.sourcem7.alfajralarm.ui.TimeParts
import kotlin.math.min
import kotlin.time.Instant

/**
 * Starts a widget session that redraws whenever the alarm data changes or the
 * refresher asks for a redraw. The first frame is loaded before composition so
 * the widget never flashes an empty state.
 */
internal suspend fun GlanceAppWidget.provideWidget(
    context: Context,
    content: @Composable (WidgetContent) -> Unit,
): Nothing {
    val snapshots = AppGraph.from(context).widgetSnapshots
    val initial = snapshots.current()
    provideContent {
        val frame by snapshots.frames.collectAsState(initial)
        val presented = remember(frame) { frame.present(context) }
        content(presented)
    }
}

/**
 * The sky behind every widget. The whole widget opens the app: widgets are
 * display only, and TalkBack reads the alarm as one sentence. With [sky] off
 * the widget draws its own backdrop (the round ring) and the frame is bare.
 */
@Composable
internal fun SkyFrame(
    style: SkyStyle,
    description: String,
    sky: Boolean = true,
    content: @Composable () -> Unit,
) {
    // Launchers clip the marked widget background to the system radius, which
    // is what keeps these skies in step with every other widget beside them.
    val backdrop = if (sky) {
        GlanceModifier
            .appWidgetBackground()
            .cornerRadius(R.dimen.widget_corner_radius)
            .background(ImageProvider(style.background))
    } else {
        GlanceModifier
    }
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .then(backdrop)
            .clickable(actionStartActivity<MainActivity>())
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
internal fun Stars() {
    Image(
        provider = ImageProvider(R.drawable.widget_stars),
        contentDescription = null,
        modifier = GlanceModifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )
}

@Composable
internal fun Crescent(tone: Tone, size: Dp) {
    Image(
        provider = ImageProvider(R.drawable.ic_crescent),
        contentDescription = null,
        modifier = GlanceModifier.size(size),
        colorFilter = ColorFilter.tint(tone.provider),
    )
}

/** Large light digits with a small day-period marker beside them. */
@Composable
internal fun TimeDisplay(time: TimeParts, size: TextUnit, color: ColorProvider, showDayPeriod: Boolean = true) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = time.digits,
            maxLines = 1,
            style = TextStyle(
                color = color,
                fontSize = size,
                fontWeight = FontWeight.Normal,
                fontFamily = DigitsFont,
            ),
        )
        val dayPeriod = time.dayPeriod
        if (showDayPeriod && dayPeriod != null) {
            Spacer(GlanceModifier.width(4.dp))
            Text(
                text = dayPeriod,
                maxLines = 1,
                style = TextStyle(color = color, fontSize = (size.value * 0.32f).sp, fontWeight = FontWeight.Medium),
            )
        }
    }
}

@Composable
internal fun Label(text: String, tone: Tone, size: TextUnit = 13.sp, weight: FontWeight = FontWeight.Medium, maxLines: Int = 1) {
    Text(
        text = text,
        maxLines = maxLines,
        style = TextStyle(color = tone.provider, fontSize = size, fontWeight = weight),
    )
}

/**
 * A native Chronometer counting down to [to]. It ticks on the launcher's side,
 * so the seconds stay live with no widget updates at all.
 */
@Composable
internal fun Countdown(to: Instant, tone: Tone, size: TextUnit, modifier: GlanceModifier = GlanceModifier) {
    val context = LocalContext.current
    val color = tone.resolve(context).toArgb()
    val views = RemoteViews(context.packageName, R.layout.widget_countdown).apply {
        val base = SystemClock.elapsedRealtime() + (to.toEpochMilliseconds() - System.currentTimeMillis())
        setChronometer(R.id.widget_countdown, base, null, true)
        setChronometerCountDown(R.id.widget_countdown, true)
        setTextColor(R.id.widget_countdown, color)
        setTextViewTextSize(R.id.widget_countdown, TypedValue.COMPLEX_UNIT_SP, size.value)
    }
    AndroidRemoteViews(views, modifier)
}

/**
 * Sizes the time to the space it has. Light digits run a little over half an
 * em wide; the day-period marker is set at a third of the size.
 */
internal fun fitTimeSize(time: TimeParts, width: Dp, max: Float, showDayPeriod: Boolean = true): TextUnit {
    val marker = time.dayPeriod?.takeIf { showDayPeriod }?.let { it.length * 0.2f + 0.25f } ?: 0f
    val ems = time.digits.length * 0.56f + marker
    return min(max, width.value / ems).coerceAtLeast(MIN_TIME_SP).sp
}

private const val MIN_TIME_SP = 14f

/** A thin system face for the large digits; widgets cannot load bundled fonts. */
private val DigitsFont = FontFamily("sans-serif-light")
