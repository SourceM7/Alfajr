package io.github.sourcem7.alfajralarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import io.github.sourcem7.alfajralarm.app.AppGraph
import io.github.sourcem7.alfajralarm.domain.AlarmDelivery
import io.github.sourcem7.alfajralarm.domain.AlarmRequest
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * Receives daily, snooze, and test alarms. The receiver only validates the
 * intent and hands it to the serialized coordinator, which rejects stale or
 * duplicated deliveries; a delivery the coordinator accepts starts ringing.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = AlarmIntents.kindOf(intent.action) ?: return
        val request = AlarmRequest(
            kind = kind,
            triggerAtMillis = intent.getLongExtra(AlarmIntents.EXTRA_TRIGGER_AT_MILLIS, 0L),
            prayerDate = intent.getStringExtra(AlarmIntents.EXTRA_PRAYER_DATE)
                ?.let { raw -> runCatching { LocalDate.parse(raw) }.getOrNull() },
            sessionId = intent.getStringExtra(AlarmIntents.EXTRA_SESSION_ID),
        )
        val graph = AppGraph.from(context)
        val pending = goAsync()
        graph.scope.launch {
            try {
                val delivery = graph.scheduler.onAlarmDelivered(request)
                if (delivery !is AlarmDelivery.Ring) return@launch
                // The coordinator has already secured the following daily alarm,
                // so ringing can start. Delivering an exact alarm is what allows
                // this foreground service to start from the background.
                ContextCompat.startForegroundService(
                    context,
                    AlarmRingingService.startIntent(context, delivery, request.triggerAtMillis),
                )
            } finally {
                pending.finish()
            }
        }
    }
}
