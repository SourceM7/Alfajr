package io.github.sourcem7.alfajralarm.alarm

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import io.github.sourcem7.alfajralarm.domain.AlarmVibration

/**
 * The alarm vibration waveform. It is declared as alarm usage so Do Not Disturb
 * policies that allow alarms treat it the same way they treat the audio.
 */
class DeviceAlarmVibration(context: Context) : AlarmVibration {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    override fun start() {
        val device = vibrator?.takeIf { it.hasVibrator() } ?: return
        val effect = VibrationEffect.createWaveform(PATTERN, REPEAT_FROM)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            device.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            device.vibrate(
                effect,
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
            )
        }
    }

    override fun stop() {
        vibrator?.cancel()
    }

    private companion object {
        /** Wait, buzz, pause, buzz — repeated until the alarm stops. */
        val PATTERN = longArrayOf(0L, 600L, 500L, 600L, 1_200L)
        const val REPEAT_FROM = 0
    }
}
