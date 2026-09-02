package io.github.sourcem7.alfajralarm.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sourcem7.alfajralarm.alarm.AlarmIntents
import io.github.sourcem7.alfajralarm.alarm.AlarmRingingService
import io.github.sourcem7.alfajralarm.app.AppGraph

/**
 * The full-screen alarm. Android starts it from the ringing notification's
 * full-screen intent, and shows a heads-up notification with the same controls
 * instead when the device is unlocked and in use.
 *
 * It renders the running session and owns no playback, so being recreated by a
 * configuration change cannot start a second alarm.
 */
class AlarmRingingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        val graph = AppGraph.from(this)
        setContent {
            val session by graph.ringing.session.collectAsStateWithLifecycle()
            // The session ends in the service; the screen follows it rather
            // than deciding for itself when the alarm is over.
            LaunchedEffect(session) { if (session == null) finish() }
            session?.let { active ->
                RingingScreen(
                    session = active,
                    onSnooze = { send(AlarmRingingService.ACTION_SNOOZE, active.sessionId) },
                    onDismiss = { send(AlarmRingingService.ACTION_DISMISS, active.sessionId) },
                )
            }
        }
    }

    /** Both controls go through the service, exactly as the notification does. */
    private fun send(action: String, sessionId: String) {
        ContextCompat.startForegroundService(
            this,
            Intent(this, AlarmRingingService::class.java)
                .setAction(action)
                .putExtra(AlarmIntents.EXTRA_SESSION_ID, sessionId),
        )
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        // Snooze and dismiss must stay reachable for the whole ringing window.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
