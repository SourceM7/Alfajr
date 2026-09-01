package io.github.sourcem7.alfajralarm.alarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import io.github.sourcem7.alfajralarm.domain.AlarmCapabilities
import io.github.sourcem7.alfajralarm.domain.CapabilityProbe

/** Reads the Android capabilities the daily alarm depends on. */
class AndroidCapabilityProbe(private val context: Context) : CapabilityProbe {
    override fun read(): AlarmCapabilities {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val audioManager = context.getSystemService(AudioManager::class.java)
        val alarmChannel = notificationManager.getNotificationChannel(NotificationChannels.ALARM)
        return AlarmCapabilities(
            canScheduleExactAlarms = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms(),
            notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            alarmChannelEnabled = alarmChannel != null && alarmChannel.importance != NotificationManager.IMPORTANCE_NONE,
            canUseFullScreenIntent = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                notificationManager.canUseFullScreenIntent(),
            alarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM),
            maxAlarmVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
        )
    }
}
