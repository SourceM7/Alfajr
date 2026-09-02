package io.github.sourcem7.alfajralarm.alarm

import android.content.Context
import android.os.PowerManager
import io.github.sourcem7.alfajralarm.domain.RINGING_TIMEOUT
import io.github.sourcem7.alfajralarm.domain.RingingWakeLock
import kotlin.time.Duration.Companion.minutes

/**
 * Keeps the CPU running while the alarm rings. The full-screen activity turns
 * the screen on; this only guarantees that playback, the ramp, and the timeout
 * keep running if the device tries to sleep.
 */
class PartialRingingWakeLock(context: Context) : RingingWakeLock {
    private val powerManager: PowerManager = context.getSystemService(PowerManager::class.java)
    private var held: PowerManager.WakeLock? = null

    override fun acquire() {
        if (held != null) return
        held = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG).apply {
            setReferenceCounted(false)
            // A timeout slightly past the ringing limit means a crashed or
            // killed service can never leave the CPU held awake.
            acquire((RINGING_TIMEOUT + 1.minutes).inWholeMilliseconds)
        }
    }

    override fun release() {
        held?.takeIf { it.isHeld }?.release()
        held = null
    }

    private companion object {
        const val TAG = "AlfajrAlarm:ringing"
    }
}
