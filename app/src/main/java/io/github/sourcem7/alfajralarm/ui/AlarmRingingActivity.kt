package io.github.sourcem7.alfajralarm.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sourcem7.alfajralarm.alarm.AlarmIntents
import io.github.sourcem7.alfajralarm.alarm.AlarmRingingService
import io.github.sourcem7.alfajralarm.app.AppGraph
import kotlinx.coroutines.delay

private const val SESSION_START_GRACE_MILLIS = 2_000L

/**
 * The full-screen alarm. Android starts it from the ringing notification's
 * full-screen intent, and shows a heads-up notification with the same controls
 * instead when the device is unlocked and in use.
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
            var observedSession by remember { mutableStateOf(false) }
            // The session ends in the service; the screen follows it rather
            // than deciding for itself when the alarm is over. A foreground
            // notification can launch this activity just before its service
            // finishes creating the session, so an initial null is not yet an
            // ended alarm.
            LaunchedEffect(session) {
                if (session != null) observedSession = true
                else if (observedSession) finish()
            }
            LaunchedEffect(Unit) {
                delay(SESSION_START_GRACE_MILLIS)
                if (viewModel.session.value == null) finish()
            }
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
