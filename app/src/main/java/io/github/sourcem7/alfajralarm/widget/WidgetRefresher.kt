package io.github.sourcem7.alfajralarm.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import io.github.sourcem7.alfajralarm.app.AppGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Keeps placed widgets current. Data changes arrive through
 * [WidgetSnapshots.inputs]; changes that come from the clock alone are driven
 * by one inexact, non-wakeup alarm aimed at the next moment a widget would
 * look different. It is delivered only while the device is awake, which is
 * the only time anyone can see a widget, so it costs no battery at night.
 */
class WidgetRefresher(
    private val context: Context,
    private val snapshots: WidgetSnapshots,
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            snapshots.inputs.collectLatest { refreshAll() }
        }
    }

    suspend fun refreshAll() {
        snapshots.requestRedraw()
        val placed = placedWidgets()
        placed.forEach { it.updateAll(context) }
        scheduleTick(placed)
    }

    /** For a widget being added or removed, which changes what needs ticking. */
    suspend fun rescheduleTick() = scheduleTick(placedWidgets())

    private suspend fun placedWidgets(): List<GlanceAppWidget> {
        val manager = GlanceAppWidgetManager(context)
        return listOf(DawnSkyWidget(), FajrPillWidget(), FajrRingWidget(), FajrLockWidget())
            .filter { manager.getGlanceIds(it.javaClass).isNotEmpty() }
    }

    private suspend fun scheduleTick(placed: List<GlanceAppWidget>) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val tick = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, WidgetTickReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val frame = snapshots.current()
        val next = if (placed.isEmpty()) {
            null
        } else {
            DawnTimeline.nextRefreshAt(frame.snapshot, frame.now, ringSteps = placed.any { it is FajrRingWidget })
        }
        if (next == null) {
            alarmManager.cancel(tick)
        } else {
            alarmManager.set(AlarmManager.RTC, next.toEpochMilliseconds(), tick)
        }
    }
}

/** The clock-driven redraw. Only this application can send it. */
class WidgetTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val graph = AppGraph.from(context)
        val pending = goAsync()
        graph.scope.launch {
            try {
                graph.widgets.refreshAll()
            } finally {
                pending.finish()
            }
        }
    }
}
