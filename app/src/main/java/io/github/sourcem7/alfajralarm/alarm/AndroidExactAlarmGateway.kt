package io.github.sourcem7.alfajralarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import io.github.sourcem7.alfajralarm.BuildConfig
import io.github.sourcem7.alfajralarm.MainActivity
import io.github.sourcem7.alfajralarm.domain.AlarmKind
import io.github.sourcem7.alfajralarm.domain.AlarmRequest
import io.github.sourcem7.alfajralarm.domain.ExactAlarmGateway

/**
 * The only place that talks to AlarmManager. Failures are reported rather than
 * thrown so the coordinator can drop the claimed enabled state instead of
 * silently downgrading to an inexact alarm.
 */
class AndroidExactAlarmGateway(private val context: Context) : ExactAlarmGateway {
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    override fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    override fun scheduleAlarmClock(request: AlarmRequest): Boolean = schedule(request) { pendingIntent ->
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(request.triggerAtMillis, showIntent()),
            pendingIntent,
        )
    }

    override fun scheduleExactWhileIdle(request: AlarmRequest): Boolean = schedule(request) { pendingIntent ->
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            request.triggerAtMillis,
            pendingIntent,
        )
    }

    override fun cancel(kind: AlarmKind) {
        val existing = PendingIntent.getBroadcast(
            context,
            AlarmIntents.requestCode(kind),
            deliveryIntent(AlarmRequest(kind = kind, triggerAtMillis = 0L)),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(existing)
        existing.cancel()
    }

    private inline fun schedule(request: AlarmRequest, set: (PendingIntent) -> Unit): Boolean = try {
        set(pendingIntent(request))
        true
    } catch (error: SecurityException) {
        report(request, error)
        false
    } catch (error: IllegalStateException) {
        report(request, error)
        false
    }

    private fun report(request: AlarmRequest, error: Exception) {
        // Release builds must not log schedule data; the kind alone is enough.
        if (BuildConfig.DEBUG) Log.w(TAG, "Failed to schedule ${request.kind}", error) else Log.w(TAG, "Failed to schedule alarm")
    }

    private fun pendingIntent(request: AlarmRequest): PendingIntent = PendingIntent.getBroadcast(
        context,
        AlarmIntents.requestCode(request.kind),
        deliveryIntent(request),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun deliveryIntent(request: AlarmRequest): Intent =
        Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmIntents.action(request.kind)
            putExtra(AlarmIntents.EXTRA_KIND, request.kind.name)
            putExtra(AlarmIntents.EXTRA_TRIGGER_AT_MILLIS, request.triggerAtMillis)
            request.prayerDate?.let { putExtra(AlarmIntents.EXTRA_PRAYER_DATE, it.toString()) }
            request.sessionId?.let { putExtra(AlarmIntents.EXTRA_SESSION_ID, it) }
        }

    /** Exposes the upcoming alarm to Android's system alarm surfaces. */
    private fun showIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        AlarmIntents.SHOW_INTENT_REQUEST_CODE,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val TAG = "AlfajrAlarm"
    }
}
