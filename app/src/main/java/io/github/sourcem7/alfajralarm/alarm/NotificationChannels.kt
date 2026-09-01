package io.github.sourcem7.alfajralarm.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import io.github.sourcem7.alfajralarm.R

/**
 * Two channels: the ringing channel is owned by the alarm service, which plays
 * its own audio and vibration, and the status channel carries missed-alarm and
 * action-required messages.
 */
object NotificationChannels {
    const val ALARM = "alarm"
    const val STATUS = "status"

    fun ensure(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val alarm = NotificationChannel(
            ALARM,
            context.getString(R.string.channel_alarm_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_alarm_description)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
            // The ringing service owns playback so the channel stays silent.
            setSound(null, null)
            enableVibration(false)
        }
        val status = NotificationChannel(
            STATUS,
            context.getString(R.string.channel_status_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.channel_status_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(alarm)
        manager.createNotificationChannel(status)
    }
}
