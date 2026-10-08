package io.github.sourcem7.alfajralarm.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import io.github.sourcem7.alfajralarm.app.AppGraph
import kotlinx.coroutines.launch

/**
 * Adding the first widget, or removing the last, changes which clock-driven
 * redraws are needed at all, so each receiver re-aims the tick alarm.
 */
abstract class AlfajrWidgetReceiver : GlanceAppWidgetReceiver() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        rescheduleTick(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        rescheduleTick(context)
    }

    private fun rescheduleTick(context: Context) {
        val graph = AppGraph.from(context)
        graph.scope.launch { graph.widgets.rescheduleTick() }
    }
}

class DawnSkyWidgetReceiver : AlfajrWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DawnSkyWidget()
}

class FajrPillWidgetReceiver : AlfajrWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FajrPillWidget()
}

class FajrRingWidgetReceiver : AlfajrWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FajrRingWidget()
}

class FajrLockWidgetReceiver : AlfajrWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FajrLockWidget()
}
