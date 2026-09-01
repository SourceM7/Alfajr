package io.github.sourcem7.alfajralarm.app

import android.content.Context
import io.github.sourcem7.alfajralarm.alarm.AlarmSchedulingCoordinator
import io.github.sourcem7.alfajralarm.alarm.AndroidCapabilityProbe
import io.github.sourcem7.alfajralarm.alarm.AndroidExactAlarmGateway
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

    val scheduler = AlarmSchedulingCoordinator(
        preferences = PreferencesProvider { preferencesRepository.preferences.first() },
        stateStore = alarmStateRepository,
        gateway = AndroidExactAlarmGateway(appContext),
        capabilities = capabilityProbe,
        selector = NextOccurrenceSelector(calculator),
    )

    companion object {
        fun from(context: Context): AppGraph = (context.applicationContext as AlfajrApplication).graph
    }
}
