package io.github.sourcem7.alfajralarm.domain

import kotlinx.coroutines.flow.Flow

/** Domain-facing contract for backed-up alarm preferences. */
interface AlarmPreferencesStore : PreferencesProvider {
    val preferences: Flow<AlarmPreferences>

    suspend fun updateLocation(location: FixedLocation)
    suspend fun updateMethod(method: FajrMethod)
    suspend fun updateCorrection(minutes: Int)
    suspend fun updateWakeOffset(minutes: Int)
    suspend fun updateRingtone(uri: String?)
    suspend fun updateVibration(enabled: Boolean)
    suspend fun updateSnoozeMinutes(minutes: Int)
    suspend fun updateTapToDismiss(enabled: Boolean)
}

/** Presentation preference kept separate from the alarm domain state. */
interface AppearancePreferencesStore {
    val dynamicColor: Flow<Boolean>
    suspend fun updateDynamicColor(enabled: Boolean)
}

/** Search boundary for the bundled, offline city catalogue. */
fun interface CityRepository {
    suspend fun search(query: String, limit: Int): List<FixedLocation>

    /** Starts the offline catalogue load before the user begins typing. */
    suspend fun warmUp() = Unit
}
