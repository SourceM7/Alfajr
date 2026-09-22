package io.github.sourcem7.alfajralarm.alarm

import android.content.Context
import android.os.PowerManager
import io.github.sourcem7.alfajralarm.domain.RINGING_TIMEOUT
import io.github.sourcem7.alfajralarm.domain.RingingWakeLock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Keeps the CPU running while the alarm rings, so playback, the ramp, and the
 * timeout keep running if the device tries to sleep.
 *
 * It also lights the screen briefly. The full-screen activity normally turns
 * the screen on, but when a device's SystemUI declines to launch it the alarm
 * would otherwise sound behind a dark screen. Lit, the lock screen at least
 * shows the ringing notification with its snooze and dismiss actions.
 */
class PartialRingingWakeLock(context: Context) : RingingWakeLock {
    private val powerManager: PowerManager = context.getSystemService(PowerManager::class.java)
    private var held: PowerManager.WakeLock? = null
    private var screen: PowerManager.WakeLock? = null

    override fun acquire() {
        if (held != null) return
        held = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG).apply {
            setReferenceCounted(false)
            // A timeout slightly past the ringing limit means a crashed or
            // killed service can never leave the CPU held awake.
            acquire((RINGING_TIMEOUT + 1.minutes).inWholeMilliseconds)
        }
        // Deprecated in favour of the activity's turnScreenOn, which is exactly
        // the path this covers for when it never starts.
        @Suppress("DEPRECATION")
        screen = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            SCREEN_TAG,
        ).apply {
            setReferenceCounted(false)
            acquire(SCREEN_WAKE.inWholeMilliseconds)
        }
    }

    override fun release() {
        screen?.takeIf { it.isHeld }?.release()
        screen = null
        held?.takeIf { it.isHeld }?.release()
        held = null
    }

    private companion object {
        const val TAG = "AlfajrAlarm:ringing"
        const val SCREEN_TAG = "AlfajrAlarm:screen"
        val SCREEN_WAKE = 10.seconds
    }
}
