package io.github.sourcem7.alfajralarm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sourcem7.alfajralarm.domain.AlarmHealth
import io.github.sourcem7.alfajralarm.domain.AlarmPreferences
import io.github.sourcem7.alfajralarm.domain.AlarmPreferencesStore
import io.github.sourcem7.alfajralarm.domain.AlarmScheduler
import io.github.sourcem7.alfajralarm.domain.AlarmState
import io.github.sourcem7.alfajralarm.domain.AlarmStateStore
import io.github.sourcem7.alfajralarm.domain.AppearancePreferencesStore
import io.github.sourcem7.alfajralarm.domain.CapabilityProbe
import io.github.sourcem7.alfajralarm.domain.CreateManualLocationUseCase
import io.github.sourcem7.alfajralarm.domain.FajrMethod
import io.github.sourcem7.alfajralarm.domain.FajrOccurrence
import io.github.sourcem7.alfajralarm.domain.FixedLocation
import io.github.sourcem7.alfajralarm.domain.ManualLocationResult
import io.github.sourcem7.alfajralarm.domain.PreviewNextAlarmUseCase
import io.github.sourcem7.alfajralarm.domain.ScheduleReason
import io.github.sourcem7.alfajralarm.domain.ScheduleResult
import io.github.sourcem7.alfajralarm.domain.SearchCitiesUseCase
import io.github.sourcem7.alfajralarm.domain.SuggestFajrMethodUseCase
import io.github.sourcem7.alfajralarm.domain.evaluateAlarmHealth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AlfajrUiState(
    val preferences: AlarmPreferences = AlarmPreferences(),
    val alarmState: AlarmState = AlarmState(),
    val dynamicColor: Boolean = false,
    val preview: FajrOccurrence? = null,
    val health: AlarmHealth = AlarmHealth(),
)

/**
 * Presentation boundary for the production UI. Compose renders state and sends
 * intents here; it never reaches into DataStore, city assets, or AppGraph.
 */
class AlfajrViewModel(
    private val preferences: AlarmPreferencesStore,
    private val appearance: AppearancePreferencesStore,
    private val alarmState: AlarmStateStore,
    private val scheduler: AlarmScheduler,
    private val capabilityProbe: CapabilityProbe,
    private val previewNextAlarm: PreviewNextAlarmUseCase,
    private val searchCities: SearchCitiesUseCase,
    private val createManualLocation: CreateManualLocationUseCase,
    private val suggestFajrMethod: SuggestFajrMethodUseCase,
    private val deviceZoneId: () -> String,
) : ViewModel() {
    private val capabilityRefresh = MutableStateFlow(0)
    private val mutableCityResults = MutableStateFlow<List<FixedLocation>>(emptyList())
    private var citySearchJob: Job? = null

    val uiState: StateFlow<AlfajrUiState> = combine(
        preferences.preferences,
        alarmState.state,
        appearance.dynamicColor,
        capabilityRefresh,
    ) { currentPreferences, currentAlarmState, dynamicColor, _ ->
        AlfajrUiState(
            preferences = currentPreferences,
            alarmState = currentAlarmState,
            dynamicColor = dynamicColor,
            preview = previewNextAlarm(currentPreferences),
            health = evaluateAlarmHealth(currentPreferences, capabilityProbe.read(), deviceZoneId()),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AlfajrUiState(),
    )

    val cityResults: StateFlow<List<FixedLocation>> = mutableCityResults

    init {
        // Preference changes are the only UI changes that require replacing a
        // registered daily alarm. Alarm state transitions are handled by the
        // scheduling coordinator itself.
        viewModelScope.launch {
            preferences.preferences.drop(1).collect {
                if (alarmState.current().dailyEnabled) scheduler.scheduleNext(ScheduleReason.SettingsChanged)
            }
        }
    }

    fun refreshCapabilities() {
        capabilityRefresh.value++
    }

    fun onAppResumed() {
        viewModelScope.launch { scheduler.scheduleNext(ScheduleReason.AppOpened) }
    }

    fun search(query: String) {
        citySearchJob?.cancel()
        if (query.isBlank()) {
            mutableCityResults.value = emptyList()
            return
        }
        citySearchJob = viewModelScope.launch {
            delay(CITY_SEARCH_DEBOUNCE_MILLIS)
            mutableCityResults.value = searchCities(query)
        }
    }

    fun selectLocation(location: FixedLocation) = launchUpdate { preferences.updateLocation(location) }

    fun saveManualLocation(latitude: String, longitude: String, zoneId: String): ManualLocationResult {
        val result = createManualLocation(latitude, longitude, zoneId)
        if (result is ManualLocationResult.Valid) selectLocation(result.location)
        return result
    }

    fun suggestedMethod(countryCode: String?): FajrMethod = suggestFajrMethod(countryCode)

    fun selectMethod(method: FajrMethod) = launchUpdate { preferences.updateMethod(method) }
    fun updateCorrection(minutes: Int) = launchUpdate { preferences.updateCorrection(minutes) }
    fun updateWakeOffset(minutes: Int) = launchUpdate { preferences.updateWakeOffset(minutes) }
    fun updateRingtone(uri: String?) = launchUpdate { preferences.updateRingtone(uri) }
    fun updateVibration(enabled: Boolean) = launchUpdate { preferences.updateVibration(enabled) }
    fun updateSnoozeMinutes(minutes: Int) = launchUpdate { preferences.updateSnoozeMinutes(minutes) }
    fun updateTapToDismiss(enabled: Boolean) = launchUpdate { preferences.updateTapToDismiss(enabled) }
    fun updateDynamicColor(enabled: Boolean) = launchUpdate { appearance.updateDynamicColor(enabled) }

    suspend fun setDailyEnabled(enabled: Boolean): ScheduleResult =
        if (enabled) scheduler.enableDaily() else scheduler.disableDaily()

    suspend fun skipNext(): ScheduleResult = scheduler.skipNext()
    suspend fun undoSkip(): ScheduleResult = scheduler.undoSkip()
    suspend fun scheduleTest(): ScheduleResult = scheduler.scheduleTest()

    private fun launchUpdate(update: suspend () -> Unit) {
        viewModelScope.launch { update() }
    }

    private companion object {
        const val CITY_SEARCH_DEBOUNCE_MILLIS = 200L
    }
}
