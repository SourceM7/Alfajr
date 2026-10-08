package io.github.sourcem7.alfajralarm.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A ring that fills across the twelve hours before the alarm, with the time
 * and a live countdown at its centre, on a round disc of the current sky.
 */
class FajrRingWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideWidget(context) { RingContent(it) }
    }
}

@Composable
private fun RingContent(content: WidgetContent) {
    val context = LocalContext.current
    val size = LocalSize.current
    val style = skyStyle(content.phase)
    val side = min(size.width.value, size.height.value).dp

    val density = context.resources.displayMetrics.density
    val ringPx = (side.value * density).roundToInt().coerceIn(48, MAX_RING_PX)
    val face = remember(ringPx, content.ringProgress, style) {
        renderRing(context, ringPx, content.ringProgress, style)
    }

    // The bitmap is the whole face, sky included, so the frame stays bare and
    // the widget is a circle whatever radius the launcher gives its widgets.
    SkyFrame(style, content.description, sky = false) {
        Image(
            provider = ImageProvider(face),
            contentDescription = null,
            modifier = GlanceModifier.size(side),
        )
        Box(modifier = GlanceModifier.size(side * 0.62f), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Crescent(style.accent, 14.dp)
                Spacer(GlanceModifier.height(2.dp))
                val time = content.time
                if (time != null) {
                    TimeDisplay(
                        time,
                        fitTimeSize(time, side * 0.56f, 30f, showDayPeriod = false),
                        style.text.provider,
                        showDayPeriod = false,
                    )
                    val countdownTo = content.countdownTo
                    if (countdownTo != null) {
                        // Full width so the countdown layout can centre its Chronometer.
                        Countdown(countdownTo, style.accent, 12.sp, GlanceModifier.fillMaxWidth())
                    } else {
                        Label(content.message ?: content.eyebrow, style.secondary, size = 11.sp, weight = FontWeight.Normal)
                    }
                } else {
                    Label(content.message.orEmpty(), style.text, size = 12.sp, maxLines = 2)
                }
            }
        }
    }
}

/** Keeps the bitmap well inside the RemoteViews memory budget on dense screens. */
private const val MAX_RING_PX = 480
