package io.github.sourcem7.alfajralarm.app

import android.content.Context
import io.github.sourcem7.alfajralarm.alarm.AlarmSchedulingCoordinator
import io.github.sourcem7.alfajralarm.alarm.AndroidCapabilityProbe
import io.github.sourcem7.alfajralarm.alarm.AndroidExactAlarmGateway
import io.github.sourcem7.alfajralarm.alarm.DeviceAlarmVibration
import io.github.sourcem7.alfajralarm.alarm.MediaPlayerAlarmAudio
import io.github.sourcem7.alfajralarm.alarm.PartialRingingWakeLock
import io.github.sourcem7.alfajralarm.alarm.RingingController
import io.github.sourcem7.alfajralarm.alarm.StatusMissedAlarmNotifier
import io.github.sourcem7.alfajralarm.calculation.AdhanFajrCalculator
import io.github.sourcem7.alfajralarm.data.location.OfflineCityRepository
import io.github.sourcem7.alfajralarm.data.settings.AlarmPreferencesRepository
import io.github.sourcem7.alfajralarm.data.settings.AlarmStateRepository
import io.github.sourcem7.alfajralarm.domain.CapabilityProbe
import io.github.sourcem7.alfajralarm.domain.NextOccurrenceSelector
import io.github.sourcem7.alfajralarm.domain.PreferencesProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first

/** Manual dependency wiring. The application owns exactly one graph. */
class AppGraph(context: Context) {
    private val appContext = context.applicationContext

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val preferencesRepository = AlarmPreferencesRepository(appContext)
    val alarmStateRepository = AlarmStateRepository(appContext)
    val cityRepository = OfflineCityRepository(appContext)
    val capabilityProbe: CapabilityProbe = AndroidCapabilityProbe(appContext)
    val calculator = AdhanFajrCalculator()

    private val preferencesProvider = PreferencesProvider { preferencesRepository.preferences.first() }

    val scheduler = AlarmSchedulingCoordinator(
        preferences = preferencesProvider,
        stateStore = alarmStateRepository,
        gateway = AndroidExactAlarmGateway(appContext),
        capabilities = capabilityProbe,
        selector = NextOccurrenceSelector(calculator),
    )

    /**
     * One ringing controller per process. The service, the notification
     * actions, and the full-screen activity all act on this instance, which is
     * what keeps a recreated activity from starting a second player.
     */
    val ringing = RingingController(
        scheduler = scheduler,
        stateStore = alarmStateRepository,
        preferences = preferencesProvider,
        audio = MediaPlayerAlarmAudio(appContext),
        vibration = DeviceAlarmVibration(appContext),
        wakeLock = PartialRingingWakeLock(appContext),
        missedNotifier = StatusMissedAlarmNotifier(appContext),
    )

    companion object {
        fun from(context: Context): AppGraph = (context.applicationContext as AlfajrApplication).graph
    }
}
