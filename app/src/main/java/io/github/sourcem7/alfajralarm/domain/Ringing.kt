package io.github.sourcem7.alfajralarm.domain

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Where the ringing audio came from. The order is the fallback order: a saved
 * ringtone can be deleted or become unreadable, the device can have no default
 * alarm sound, and the bundled tone is the last resort that always exists.
 */
enum class RingtoneSource {
    SAVED,
    SYSTEM_ALARM,
    BUNDLED,
    ;

    companion object {
        fun chainFor(savedRingtoneUri: String?): List<RingtoneSource> =
            listOfNotNull(SAVED.takeIf { !savedRingtoneUri.isNullOrBlank() }, SYSTEM_ALARM, BUNDLED)
    }
}

/** What the user, a notification action, or the timeout asked the session to do. */
enum class RingingCommand { SNOOZE, DISMISS, TIMEOUT }

/**
 * One ringing alarm as the service and the full-screen UI see it. It is created
 * only for a session the application still owns, so a stale delivery can never
 * produce one.
 */
data class RingingSession(
    val sessionId: String,
    val isTest: Boolean,
    /** The instant the alarm was scheduled for, which is what the UI shows. */
    val alarmAt: Instant,
    /** When output actually started, which is what the ramp and timeout measure. */
    val startedAt: Instant,
    val snoozesUsed: Int,
    val snoozeMinutes: Int,
    val tapToDismiss: Boolean,
    val vibrating: Boolean,
    /** Null when every fallback failed; the alarm still rings visually. */
    val ringtoneSource: RingtoneSource?,
    val volumeScalar: Float = 0f,
) {
    /** False at the third snooze, where only dismiss remains. */
    val snoozeAvailable: Boolean get() = snoozesUsed < MAX_SNOOZE_COUNT
}

/** Fixed by the product specification rather than exposed as a setting. */
val RINGING_TIMEOUT: Duration = 10.minutes

/**
 * The per-player volume ramp. It never touches the system alarm stream, so the
 * user's own alarm volume is the ceiling and remains unchanged.
 */
object VolumeRamp {
    val DURATION: Duration = 30.seconds

    fun scalarAt(elapsed: Duration): Float = when {
        elapsed <= Duration.ZERO -> 0f
        elapsed >= DURATION -> 1f
        else -> {
            // Perceived loudness rises faster than amplitude, so squaring the
            // fraction keeps the first seconds gentle while still reaching full
            // player volume exactly at thirty seconds.
            val fraction = (elapsed / DURATION).toFloat()
            fraction * fraction
        }
    }
}

/**
 * Alarm audio output. Implementations own one player at a time and report
 * failure instead of throwing so the fallback chain can continue.
 */
interface AlarmAudio {
    /**
     * Starts looping playback of [source], replacing anything already playing.
     * Returns false when the source could not be played, leaving nothing
     * playing so the next candidate can be tried.
     */
    fun start(source: RingtoneSource, savedRingtoneUri: String?): Boolean

    /** Sets the player's own volume in 0..1. Never the system stream volume. */
    fun setVolume(scalar: Float)

    fun stop()
}

interface AlarmVibration {
    fun start()
    fun stop()
}

/** Keeps the CPU awake for as long as the alarm is ringing. */
interface RingingWakeLock {
    fun acquire()
    fun release()
}

/** Posts the missed-alarm message on the status notification channel. */
fun interface MissedAlarmNotifier {
    fun postMissed(session: RingingSession)
}
