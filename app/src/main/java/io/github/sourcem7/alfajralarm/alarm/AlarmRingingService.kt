package io.github.sourcem7.alfajralarm.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import io.github.sourcem7.alfajralarm.app.AppGraph
import io.github.sourcem7.alfajralarm.domain.AlarmDelivery
import io.github.sourcem7.alfajralarm.domain.RingingCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Instant

/**
 * The ringing runtime. Android 14 and later require a foreground service to
 * post its notification immediately, so the notification is posted before any
 * suspending work and the session is resolved afterwards.
 *
 * The service owns no alarm logic. It supplies the timer, the notification, and
 * the process lifetime; [RingingController] decides what rings and what is
 * recorded.
 */
class AlarmRingingService : Service() {
    private val graph: AppGraph by lazy { AppGraph.from(this) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ticker: Job? = null

    /** The session this service instance started, if it started one. */
    private var owned: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sessionId = intent?.getStringExtra(AlarmIntents.EXTRA_SESSION_ID)
        val isTest = intent?.getBooleanExtra(EXTRA_IS_TEST, false) == true
        val alarmAtMillis = intent?.getLongExtra(AlarmIntents.EXTRA_TRIGGER_AT_MILLIS, 0L) ?: 0L
        if (sessionId == null) {
            // Nothing can be posted for an unidentifiable command, and the
            // service must not linger without a foreground notification.
            stopSelf()
            return START_NOT_STICKY
        }
        val starting = intent.action == ACTION_START
        // Claiming the session before the notification is posted is what stops
        // the full-screen screen from opening against a null session: Android
        // cannot act on the notification until notify() has returned, and the
        // claim is already published by then.
        if (starting) graph.ringing.claim(sessionId)
        enterForeground(sessionId, isTest, alarmAtMillis, withFullScreen = starting)

        when (intent.action) {
            ACTION_START -> beginRinging(sessionId, isTest, alarmAtMillis)
            ACTION_SNOOZE -> command(sessionId, RingingCommand.SNOOZE)
            ACTION_DISMISS -> command(sessionId, RingingCommand.DISMISS)
            else -> stopUnlessRinging()
        }
        // A ringing alarm is bound to one delivery. Restarting the service
        // later would ring for an occurrence that has already passed.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        ticker?.cancel()
        scope.cancel()
        // Reached with a live session only when Android tore the service down,
        // so the outputs are released without claiming an outcome.
        owned?.let { sessionId -> graph.scope.launch { graph.ringing.abandon(sessionId) } }
        super.onDestroy()
    }

    private fun beginRinging(sessionId: String, isTest: Boolean, alarmAtMillis: Long) {
        scope.launch {
            val session = graph.ringing.start(sessionId, isTest, Instant.fromEpochMilliseconds(alarmAtMillis))
            if (session == null) {
                // A stale delivery, or one that arrived while another alarm was
                // already ringing. Either way this delivery is dropped without
                // disturbing whatever is ringing now.
                stopUnlessRinging()
                return@launch
            }
            owned = session.sessionId
            // The placeholder posted before the session was resolved cannot know
            // whether snoozes remain.
            enterForeground(session.sessionId, session.isTest, session.alarmAt.toEpochMilliseconds(), session.snoozeAvailable)
            startTicker()
        }
    }

    private fun command(sessionId: String, command: RingingCommand) {
        scope.launch {
            graph.ringing.handle(sessionId, command)
            stopUnlessRinging()
        }
    }

    /** Drives the volume ramp and the ten-minute timeout. */
    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive && graph.ringing.session.value != null) {
                delay(TICK_MILLIS)
                graph.ringing.tick()
            }
            // The loop is the ticker itself, so it finishes without cancelling
            // the job it is running on.
            finish()
        }
    }

    private fun enterForeground(
        sessionId: String,
        isTest: Boolean,
        alarmAtMillis: Long,
        snoozeAvailable: Boolean = true,
        withFullScreen: Boolean = false,
    ) {
        val notification = AlarmNotifications.ringing(
            context = this,
            sessionId = sessionId,
            isTest = isTest,
            alarmAtMillis = alarmAtMillis,
            snoozeAvailable = snoozeAvailable,
            withFullScreen = withFullScreen,
        )
        ServiceCompat.startForeground(
            this,
            AlarmNotifications.RINGING_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            },
        )
    }

    /**
     * Stops the service unless an alarm is still ringing. Every path that could
     * end this service instance goes through here, because a dropped duplicate
     * delivery must never take the foreground notification away from the alarm
     * that is actually sounding.
     */
    private fun stopUnlessRinging() {
        val running = graph.ringing.session.value
        if (running != null) {
            enterForeground(running.sessionId, running.isTest, running.alarmAt.toEpochMilliseconds(), running.snoozeAvailable)
            return
        }
        ticker?.cancel()
        finish()
    }

    private fun finish() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        NotificationManagerCompat.from(this).cancel(AlarmNotifications.RINGING_ID)
        stopSelf()
    }

    companion object {
        const val ACTION_START = "io.github.sourcem7.alfajralarm.action.START_RINGING"
        const val ACTION_SNOOZE = "io.github.sourcem7.alfajralarm.action.SNOOZE_RINGING"
        const val ACTION_DISMISS = "io.github.sourcem7.alfajralarm.action.DISMISS_RINGING"

        private const val EXTRA_IS_TEST = "io.github.sourcem7.alfajralarm.extra.IS_TEST"
        private const val TICK_MILLIS = 200L

        /** The intent a validated delivery uses to start ringing. */
        fun startIntent(context: Context, delivery: AlarmDelivery.Ring, alarmAtMillis: Long): Intent =
            Intent(context, AlarmRingingService::class.java)
                .setAction(ACTION_START)
                .putExtra(AlarmIntents.EXTRA_SESSION_ID, delivery.sessionId)
                .putExtra(EXTRA_IS_TEST, delivery.isTest)
                .putExtra(AlarmIntents.EXTRA_TRIGGER_AT_MILLIS, alarmAtMillis)
    }
}
