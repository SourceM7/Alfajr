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
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight

/**
 * Built for the lock-screen widget hub: dark whatever the system theme, a
 * large time that can be read at a glance, and no city, because anyone can
 * see a locked phone. It has no ticking seconds either; a lock screen should
 * be still.
 */
class FajrLockWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE, TALL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideWidget(context) { LockContent(it) }
    }

    internal companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val WIDE = DpSize(250.dp, 110.dp)
        val TALL = DpSize(250.dp, 180.dp)
    }
}

@Composable
private fun LockContent(content: WidgetContent) {
    val size = LocalSize.current
    val style = skyStyle(content.phase, lockScreen = true)
    val padding = 18.dp
    SkyFrame(style, content.description) {
        if (style.stars) Stars()
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Label(content.eyebrow, style.secondary, size = 14.sp)
            Spacer(GlanceModifier.height(2.dp))
            val time = content.time
            if (time != null) {
                val wide = size.width >= FajrLockWidget.WIDE.width
                val max = minOf(if (wide) 76f else 40f, size.height.value * 0.5f)
                TimeDisplay(time, fitTimeSize(time, size.width - padding * 2, max, showDayPeriod = wide), style.text.provider, showDayPeriod = wide)
                content.message?.let { Label(it, style.secondary, weight = FontWeight.Normal) }
            } else {
                Label(content.message.orEmpty(), style.text, size = 18.sp, weight = FontWeight.Normal, maxLines = 2)
            }
        }
    }
}
