package io.github.sourcem7.alfajralarm.alarm

import io.github.sourcem7.alfajralarm.domain.AlarmAudio
import io.github.sourcem7.alfajralarm.domain.AlarmVibration
import io.github.sourcem7.alfajralarm.domain.MissedAlarmNotifier
import io.github.sourcem7.alfajralarm.domain.RingingSession
import io.github.sourcem7.alfajralarm.domain.RingingWakeLock
import io.github.sourcem7.alfajralarm.domain.RingtoneSource

/**
 * Records every attempt so the fallback chain can be asserted. [failing] names
 * the sources that cannot be played, which is how a deleted saved ringtone or a
 * device with no alarm sound is reproduced.
 */
internal class RecordingAlarmAudio(private val failing: Set<RingtoneSource> = emptySet()) : AlarmAudio {
    val attempted = mutableListOf<RingtoneSource>()
    val volumes = mutableListOf<Float>()
    var playing: RingtoneSource? = null
        private set
    var stops = 0
        private set

    override fun start(source: RingtoneSource, savedRingtoneUri: String?): Boolean {
        attempted += source
        // A failed candidate must leave nothing playing behind it.
        playing = null
        if (source in failing) return false
        playing = source
        return true
    }

    override fun setVolume(scalar: Float) {
        volumes += scalar
    }

    override fun stop() {
        stops++
        playing = null
    }
}

internal class RecordingVibration : AlarmVibration {
    var active = false
        private set
    var starts = 0
        private set

    override fun start() {
        starts++
        active = true
    }

    override fun stop() {
        active = false
    }
}

internal class RecordingWakeLock : RingingWakeLock {
    var held = false
        private set

    override fun acquire() {
        held = true
    }

    override fun release() {
        held = false
    }
}

internal class RecordingMissedNotifier : MissedAlarmNotifier {
    val posted = mutableListOf<RingingSession>()

    override fun postMissed(session: RingingSession) {
        posted += session
    }
}
