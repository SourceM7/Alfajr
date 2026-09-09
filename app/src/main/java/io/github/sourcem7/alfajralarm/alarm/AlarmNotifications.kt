package io.github.sourcem7.alfajralarm.alarm

import android.Manifest
import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.sourcem7.alfajralarm.MainActivity
import io.github.sourcem7.alfajralarm.R
import io.github.sourcem7.alfajralarm.domain.MissedAlarmNotifier
import io.github.sourcem7.alfajralarm.domain.RingingSession
import io.github.sourcem7.alfajralarm.ui.AlarmRingingActivity
import java.util.Date

/**
 * The two notifications the ringing runtime posts: the ongoing alarm that owns
 * the foreground service and carries the full-screen intent, and the missed
 * message on the status channel.
 */
object AlarmNotifications {
    const val RINGING_ID = 1
    const val MISSED_ID = 2

    private const val FULL_SCREEN_REQUEST = 3001
    private const val SNOOZE_REQUEST = 3002
    private const val DISMISS_REQUEST = 3003

    /**
     * Android launches the full-screen intent when the screen is off or locked
     * and otherwise shows this as a heads-up notification, so the actions have
     * to be enough to snooze or dismiss on their own.
     *
     * [withFullScreen] belongs only to the post that begins a ringing session.
     * The platform evaluates a full-screen intent when a notification is first
     * added, not when it is updated, so re-posting one buys nothing — and it
     * costs something: a notification action arriving after the alarm ended
     * would add a *new* record carrying the intent, which launches the ringing
     * screen for a session that no longer exists.
     */
    fun ringing(
        context: Context,
        sessionId: String,
        isTest: Boolean,
        alarmAtMillis: Long,
        snoozeAvailable: Boolean,
        withFullScreen: Boolean,
    ): Notification {
        val open = activityPendingIntent(context, sessionId, FULL_SCREEN_REQUEST)
        val builder = NotificationCompat.Builder(context, NotificationChannels.ALARM)
            .setSmallIcon(R.drawable.ic_alarm_notification)
            .setContentTitle(context.getString(if (isTest) R.string.ringing_title_test else R.string.ringing_title_daily))
            .setContentText(context.formatTime(alarmAtMillis))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            // The service owns audio and vibration, so the notification adds none.
            .setSilent(true)
            .setContentIntent(open)
        if (withFullScreen) builder.setFullScreenIntent(open, true)
        if (snoozeAvailable) {
            builder.addAction(
                R.drawable.ic_alarm_notification,
                context.getString(R.string.action_snooze),
                command(context, AlarmRingingService.ACTION_SNOOZE, sessionId, SNOOZE_REQUEST),
            )
        }
        builder.addAction(
            R.drawable.ic_alarm_notification,
            context.getString(R.string.action_dismiss),
            command(context, AlarmRingingService.ACTION_DISMISS, sessionId, DISMISS_REQUEST),
        )
        return builder.build()
    }

    private fun command(context: Context, action: String, sessionId: String, requestCode: Int): PendingIntent =
        PendingIntent.getForegroundService(
            context,
            requestCode,
            Intent(context, AlarmRingingService::class.java)
                .setAction(action)
                .putExtra(AlarmIntents.EXTRA_SESSION_ID, sessionId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun activityPendingIntent(context: Context, sessionId: String, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            ringingActivityIntent(context, sessionId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            fullScreenActivityOptions(),
        )

    /**
     * The intent every path to the ringing screen uses: the notification's
     * content action, its full-screen intent, and the direct start in
     * [AndroidRingingSurface].
     *
     * `FLAG_ACTIVITY_CLEAR_TASK` is deliberately absent. The activity declares
     * its own `taskAffinity` and `launchMode="singleTask"`, so a repeat launch
     * already resolves to the single existing instance; clearing the task would
     * instead tear down a ringing screen one path had already put up and
     * rebuild it, which is visible as a flicker now that two paths compete.
     */
    internal fun ringingActivityIntent(context: Context, sessionId: String): Intent =
        Intent(context, AlarmRingingActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(AlarmIntents.EXTRA_SESSION_ID, sessionId)

    /**
     * Android 15+ requires the creator of a PendingIntent to delegate its
     * background-activity privilege explicitly. The system sends this intent
     * for the alarm notification's full-screen presentation.
     */
    @Suppress("DEPRECATION") // The API 35 constant is required on Android 15.
    private fun fullScreenActivityOptions() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        ActivityOptions.makeBasic().apply {
            pendingIntentCreatorBackgroundActivityStartMode =
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
        }.toBundle()
    } else {
        null
    }

    /** Respects the device's own 12/24-hour setting. */
    private fun Context.formatTime(millis: Long): String =
        DateFormat.getTimeFormat(this).format(Date(millis))
}

/** Posts the missed message once a ringing alarm times out unattended. */
class StatusMissedAlarmNotifier(private val context: Context) : MissedAlarmNotifier {
    override fun postMissed(session: RingingSession) {
        val notification = NotificationCompat.Builder(context, NotificationChannels.STATUS)
            .setSmallIcon(R.drawable.ic_alarm_notification)
            .setContentTitle(context.getString(R.string.missed_notification_title))
            .setContentText(context.getString(R.string.missed_notification_text))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    MISSED_REQUEST,
                    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()
        // A denied notification permission is reported as a degraded alarm on
        // the home screen; there is nothing useful to do about it here.
        val manager = NotificationManagerCompat.from(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (!manager.areNotificationsEnabled()) return
        manager.notify(AlarmNotifications.MISSED_ID, notification)
    }

    private companion object {
        const val MISSED_REQUEST = 3004
    }
}
