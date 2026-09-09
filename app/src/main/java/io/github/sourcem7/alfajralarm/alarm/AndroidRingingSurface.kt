package io.github.sourcem7.alfajralarm.alarm

import android.content.Context
import android.util.Log
import io.github.sourcem7.alfajralarm.domain.RingingSurface

/**
 * Starts [io.github.sourcem7.alfajralarm.ui.AlarmRingingActivity] directly,
 * alongside the ringing notification's full-screen intent.
 *
 * Android permits this because delivering an exact alarm opens a window in
 * which the application may start an activity from the background. That window
 * is not guaranteed on every device or version, so a refusal is logged and
 * swallowed rather than propagated.
 */
class AndroidRingingSurface(private val context: Context) : RingingSurface {
    override fun show(sessionId: String) {
        runCatching {
            context.startActivity(AlarmNotifications.ringingActivityIntent(context, sessionId))
        }.onFailure { error ->
            Log.w(TAG, "Android refused the direct ringing-screen start", error)
        }
    }

    private companion object {
        const val TAG = "AlfajrAlarm"
    }
}
