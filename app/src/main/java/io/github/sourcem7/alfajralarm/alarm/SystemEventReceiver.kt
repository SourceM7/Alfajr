package io.github.sourcem7.alfajralarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.sourcem7.alfajralarm.app.AppGraph
import io.github.sourcem7.alfajralarm.domain.ScheduleReason
import kotlinx.coroutines.launch

/**
 * Android drops scheduled alarms across reboots, clock changes, and package
 * replacement. The receiver does no work itself; it enqueues one serialized
 * recalculation and a widget redraw, and exits.
 */
class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reason = reasonFor(intent.action)
        // A language change needs no alarm work, only widgets in the new language.
        if (reason == null && intent.action != Intent.ACTION_LOCALE_CHANGED) return
        val graph = AppGraph.from(context)
        val pending = goAsync()
        graph.scope.launch {
            try {
                reason?.let { graph.scheduler.scheduleNext(it) }
                // A clock or 12/24-hour change can leave stored state untouched
                // while every displayed time is now wrong.
                graph.widgets.refreshAll()
            } finally {
                pending.finish()
            }
        }
    }

    private fun reasonFor(action: String?): ScheduleReason? = when (action) {
        Intent.ACTION_BOOT_COMPLETED -> ScheduleReason.BootCompleted
        Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> ScheduleReason.ClockChanged
        Intent.ACTION_MY_PACKAGE_REPLACED -> ScheduleReason.PackageReplaced
        else -> null
    }
}
