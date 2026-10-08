package io.github.sourcem7.alfajralarm.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight

/** The alarm time for a dock row or a crowded home screen: 1x1 or a 2x1 pill. */
class FajrPillWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(ICON, PILL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideWidget(context) { PillContent(it) }
    }

    internal companion object {
        val ICON = DpSize(48.dp, 48.dp)
        val PILL = DpSize(110.dp, 48.dp)
    }
}

@Composable
private fun PillContent(content: WidgetContent) {
    val size = LocalSize.current
    val style = skyStyle(content.phase)
    SkyFrame(style, content.description) {
        if (size.width >= FajrPillWidget.PILL.width) Pill(content, style) else Icon(content, style)
    }
}

@Composable
private fun Pill(content: WidgetContent, style: SkyStyle) {
    val size = LocalSize.current
    val padding = 14.dp
    Row(
        modifier = GlanceModifier.fillMaxSize().padding(horizontal = padding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Crescent(style.accent, 20.dp)
        Spacer(GlanceModifier.width(10.dp))
        Column {
            val time = content.time
            if (time != null) {
                val width = size.width - padding * 2 - 30.dp
                val max = minOf(26f, size.height.value * 0.42f)
                TimeDisplay(time, fitTimeSize(time, width, max), style.text.provider)
                Label(content.message ?: content.eyebrow, style.secondary, size = 11.sp, weight = FontWeight.Normal)
            } else {
                Label(content.message.orEmpty(), style.text, size = 13.sp, maxLines = 2)
            }
        }
    }
}

@Composable
private fun Icon(content: WidgetContent, style: SkyStyle) {
    val size = LocalSize.current
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Crescent(style.accent, 14.dp)
        val time = content.time
        if (time != null) {
            Spacer(GlanceModifier.height(2.dp))
            TimeDisplay(time, fitTimeSize(time, size.width - 12.dp, 18f, showDayPeriod = false), style.text.provider, showDayPeriod = false)
        }
    }
}
