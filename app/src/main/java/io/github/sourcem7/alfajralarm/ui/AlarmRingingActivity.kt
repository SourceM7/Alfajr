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
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sourcem7.alfajralarm.alarm.AlarmIntents
import io.github.sourcem7.alfajralarm.alarm.AlarmRingingService
import io.github.sourcem7.alfajralarm.app.AppGraph

/**
 * The full-screen alarm. It is opened two ways: by the ringing notification's
 * full-screen intent, which Android fires when the screen is off or locked, and
 * by a direct start from the ringing runtime, which covers the case where the
 * device is unlocked and in use and the platform downgrades that intent to a
 * heads-up notification.
 *
 * Either path can arrive before the session exists, so the screen follows two
 * signals: the running session, and the session id a start command has claimed.
 * It finishes only when both are absent, which means the alarm really is over.
 *
 * It renders the running session and owns no playback, so being recreated by a
 * configuration change cannot start a second alarm.
 */
class AlarmRingingActivity : ComponentActivity() {
    private val graph: AppGraph by lazy { AppGraph.from(this) }
    private val viewModel: RingingViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = graph.createRingingViewModel() as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        setContent {
            val session by viewModel.session.collectAsStateWithLifecycle()
            val starting by viewModel.startingSessionId.collectAsStateWithLifecycle()
            val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
            // The session ends in the runtime; the screen follows it rather than
            // deciding for itself when the alarm is over. A claimed session that
            // has not begun still counts as ringing, so the window is never
            // empty and never closes on a slow start.
            LaunchedEffect(session, starting) {
                if (session == null && starting == null) finish()
            }
            AlfajrTheme(dynamicColor = dynamicColor) {
                val active = session
                if (active != null) {
                    RingingScreen(
                        session = active,
                        onSnooze = { send(AlarmRingingService.ACTION_SNOOZE, active.sessionId) },
                        onDismiss = { send(AlarmRingingService.ACTION_DISMISS, active.sessionId) },
                    )
                } else if (starting != null) {
                    RingingStartingScreen()
                }
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
