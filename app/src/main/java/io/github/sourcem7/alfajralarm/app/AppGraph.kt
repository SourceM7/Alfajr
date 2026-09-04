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
import io.github.sourcem7.alfajralarm.data.settings.DataStoreAlarmPreferencesRepository
import io.github.sourcem7.alfajralarm.data.settings.AlarmStateRepository
import io.github.sourcem7.alfajralarm.data.settings.DataStoreAppearancePreferencesRepository
import io.github.sourcem7.alfajralarm.domain.CapabilityProbe
import io.github.sourcem7.alfajralarm.domain.NextOccurrenceSelector
import io.github.sourcem7.alfajralarm.domain.CreateManualLocationUseCase
import io.github.sourcem7.alfajralarm.domain.PreviewNextAlarmUseCase
import io.github.sourcem7.alfajralarm.domain.SearchCitiesUseCase
import io.github.sourcem7.alfajralarm.domain.SuggestFajrMethodUseCase
import io.github.sourcem7.alfajralarm.ui.AlfajrViewModel
import io.github.sourcem7.alfajralarm.ui.RingingViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual dependency wiring. The application owns exactly one graph. */
class AppGraph(context: Context) {
    private val appContext = context.applicationContext

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val preferencesRepository = DataStoreAlarmPreferencesRepository(appContext)
    private val appearancePreferencesRepository = DataStoreAppearancePreferencesRepository(appContext)
    private val alarmStateRepository = AlarmStateRepository(appContext)
    private val cityRepository = OfflineCityRepository(appContext)
    private val capabilityProbe: CapabilityProbe = AndroidCapabilityProbe(appContext)
    private val calculator = AdhanFajrCalculator()
    private val nextOccurrenceSelector = NextOccurrenceSelector(calculator)

    fun createAlfajrViewModel(): AlfajrViewModel = AlfajrViewModel(
        preferences = preferencesRepository,
        appearance = appearancePreferencesRepository,
        alarmState = alarmStateRepository,
        scheduler = scheduler,
        capabilityProbe = capabilityProbe,
        previewNextAlarm = PreviewNextAlarmUseCase(nextOccurrenceSelector),
        searchCities = SearchCitiesUseCase(cityRepository),
        createManualLocation = CreateManualLocationUseCase(),
        suggestFajrMethod = SuggestFajrMethodUseCase(),
        deviceZoneId = { kotlinx.datetime.TimeZone.currentSystemDefault().id },
    )

    fun createRingingViewModel(): RingingViewModel = RingingViewModel(ringing)

    val scheduler = AlarmSchedulingCoordinator(
        preferences = preferencesRepository,
        stateStore = alarmStateRepository,
        gateway = AndroidExactAlarmGateway(appContext),
        capabilities = capabilityProbe,
        selector = nextOccurrenceSelector,
    )

    /**
     * One ringing controller per process. The service, the notification
     * actions, and the full-screen activity all act on this instance, which is
     * what keeps a recreated activity from starting a second player.
     */
    val ringing = RingingController(
        scheduler = scheduler,
        stateStore = alarmStateRepository,
        preferences = preferencesRepository,
        audio = MediaPlayerAlarmAudio(appContext),
        vibration = DeviceAlarmVibration(appContext),
        wakeLock = PartialRingingWakeLock(appContext),
        missedNotifier = StatusMissedAlarmNotifier(appContext),
    )

    companion object {
        fun from(context: Context): AppGraph = (context.applicationContext as AlfajrApplication).graph
    }
}
