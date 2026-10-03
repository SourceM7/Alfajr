package io.github.sourcem7.alfajralarm.alarm

import android.content.Context
import android.util.Log
import io.github.sourcem7.alfajralarm.domain.RingingSurface

/**
 * Starts [io.github.sourcem7.alfajralarm.ui.AlarmRingingActivity] directly,
 * alongside the ringing notification's full-screen intent.
 *
 * Android only honours a background activity start from an application that
 * may draw over other apps, and it drops a refused one without throwing, so a
 * start that returns normally proves nothing. Whether the screen can open this
 * way is reported through the overlay capability instead; the `runCatching`
 * covers the devices that do throw.
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
