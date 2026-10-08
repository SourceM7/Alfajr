package io.github.sourcem7.alfajralarm.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import io.github.sourcem7.alfajralarm.R

/**
 * The signature widget: the next alarm under a sky that moves from day to
 * evening to night to first light as Fajr approaches.
 */
class DawnSkyWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE, TALL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideWidget(context) { DawnSkyContent(it) }
    }

    internal companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val WIDE = DpSize(250.dp, 110.dp)
        val TALL = DpSize(250.dp, 180.dp)
    }
}

@Composable
private fun DawnSkyContent(content: WidgetContent) {
    val size = LocalSize.current
    val style = skyStyle(content.phase)
    val wide = size.width >= DawnSkyWidget.WIDE.width
    val tall = size.height >= DawnSkyWidget.TALL.height
    SkyFrame(style, content.description) {
        if (style.stars && wide) Stars()
        if (wide) WideSky(content, style, tall) else SmallSky(content, style)
    }
}

@Composable
private fun WideSky(content: WidgetContent, style: SkyStyle, tall: Boolean) {
    val size = LocalSize.current
    val horizontal = if (tall) 22.dp else 18.dp
    Column(modifier = GlanceModifier.fillMaxSize().padding(horizontal = horizontal, vertical = 16.dp)) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Crescent(style.accent, 14.dp)
            Spacer(GlanceModifier.width(6.dp))
            Label(content.eyebrow, style.secondary)
            Spacer(GlanceModifier.defaultWeight())
            content.location?.let { Label(it, style.secondary, size = 12.sp, weight = FontWeight.Normal) }
        }
        Spacer(GlanceModifier.defaultWeight())

        val time = content.time
        if (time != null) {
            val max = if (tall) 68f else 50f
            TimeDisplay(time, fitTimeSize(time, size.width - horizontal * 2, max), style.text.provider)
        } else {
            Label(content.message.orEmpty(), style.text, size = 22.sp, weight = FontWeight.Normal, maxLines = 2)
        }
        Spacer(GlanceModifier.height(4.dp))

        val countdownTo = content.countdownTo
        if (countdownTo != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label(LocalContext.current.getString(R.string.widget_rings_in), style.secondary, weight = FontWeight.Normal)
                Spacer(GlanceModifier.width(6.dp))
                Countdown(countdownTo, style.accent, 13.sp)
            }
        } else if (time != null && content.message != null) {
            Label(content.message, style.secondary, weight = FontWeight.Normal)
        }

        if (tall && content.highLatitude) {
            Spacer(GlanceModifier.height(6.dp))
            Label(
                LocalContext.current.getString(R.string.high_latitude_notice),
                style.secondary,
                size = 11.sp,
                weight = FontWeight.Normal,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun SmallSky(content: WidgetContent, style: SkyStyle) {
    val size = LocalSize.current
    val padding = 14.dp
    Column(modifier = GlanceModifier.fillMaxSize().padding(padding)) {
        Crescent(style.accent, 16.dp)
        Spacer(GlanceModifier.defaultWeight())
        val time = content.time
        if (time != null) {
            // Too narrow for both; the marker goes, the digits stay readable.
            TimeDisplay(time, fitTimeSize(time, size.width - padding * 2, 34f, showDayPeriod = false), style.text.provider, showDayPeriod = false)
            Label(content.message ?: content.eyebrow, style.secondary, size = 12.sp, weight = FontWeight.Normal)
        } else {
            Label(content.message.orEmpty(), style.text, size = 15.sp, maxLines = 3)
        }
    }
}
