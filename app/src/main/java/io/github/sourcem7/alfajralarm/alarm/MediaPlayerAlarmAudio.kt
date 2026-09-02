package io.github.sourcem7.alfajralarm.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import io.github.sourcem7.alfajralarm.BuildConfig
import io.github.sourcem7.alfajralarm.R
import io.github.sourcem7.alfajralarm.domain.AlarmAudio
import io.github.sourcem7.alfajralarm.domain.RingtoneSource

/**
 * The only place that plays audio. Every failure is reported rather than
 * thrown so [io.github.sourcem7.alfajralarm.alarm.RingingController] can walk
 * the fallback chain instead of leaving the user with no alarm at all.
 */
class MediaPlayerAlarmAudio(private val context: Context) : AlarmAudio {
    private val audioManager: AudioManager = context.getSystemService(AudioManager::class.java)

    /**
     * USAGE_ALARM is what routes the sound to the alarm stream, keeps it
     * audible under Do Not Disturb policies that allow alarms, and qualifies
     * the playback as alarm audio on releases that restrict background sound.
     */
    private val attributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null

    override fun start(source: RingtoneSource, savedRingtoneUri: String?): Boolean {
        stop()
        val uri = uriFor(source, savedRingtoneUri) ?: return false
        val prepared = try {
            MediaPlayer().apply {
                setAudioAttributes(attributes)
                setDataSource(context, uri)
                isLooping = true
                // The ramp owns the volume from the first sample onward.
                setVolume(0f, 0f)
                prepare()
            }
        } catch (error: Exception) {
            report("prepare", source, error)
            return false
        }
        return try {
            requestFocus()
            prepared.start()
            player = prepared
            true
        } catch (error: Exception) {
            report("start", source, error)
            prepared.release()
            abandonFocus()
            false
        }
    }

    /**
     * Sets this player's own volume. It deliberately has no access to
     * [AudioManager.setStreamVolume]: the user's alarm volume is the ceiling
     * and the app never changes it.
     */
    override fun setVolume(scalar: Float) {
        val clamped = scalar.coerceIn(0f, 1f)
        try {
            player?.setVolume(clamped, clamped)
        } catch (error: IllegalStateException) {
            report("volume", null, error)
        }
    }

    override fun stop() {
        player?.let { active ->
            try {
                if (active.isPlaying) active.stop()
            } catch (error: IllegalStateException) {
                report("stop", null, error)
            }
            active.release()
        }
        player = null
        abandonFocus()
    }

    private fun uriFor(source: RingtoneSource, savedRingtoneUri: String?): Uri? = when (source) {
        RingtoneSource.SAVED -> savedRingtoneUri?.takeIf { it.isNotBlank() }?.toUri()
        // A device can have no alarm sound configured at all, in which case
        // this is null and the bundled tone is the next candidate.
        RingtoneSource.SYSTEM_ALARM ->
            RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        RingtoneSource.BUNDLED ->
            "android.resource://${context.packageName}/${R.raw.fallback_alarm}".toUri()
    }

    private fun requestFocus() {
        // Alarm audio plays whether or not focus is granted; the request exists
        // so other players duck or pause rather than compete with the alarm.
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .build()
        audioManager.requestAudioFocus(request)
        focusRequest = request
    }

    private fun abandonFocus() {
        focusRequest?.let(audioManager::abandonAudioFocusRequest)
        focusRequest = null
    }

    private fun report(stage: String, source: RingtoneSource?, error: Exception) {
        // A ringtone URI can identify a file the user chose, so it is never
        // logged; the stage and the fallback step are enough to diagnose.
        if (BuildConfig.DEBUG) Log.w(TAG, "Alarm audio $stage failed for $source", error)
        else Log.w(TAG, "Alarm audio $stage failed")
    }

    private companion object {
        const val TAG = "AlfajrAlarm"
    }
}
