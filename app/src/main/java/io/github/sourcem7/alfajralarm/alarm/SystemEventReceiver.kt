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
 * recalculation and exits.
 */
class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reason = reasonFor(intent.action) ?: return
        val graph = AppGraph.from(context)
        val pending = goAsync()
        graph.scope.launch {
            try {
                graph.scheduler.scheduleNext(reason)
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
