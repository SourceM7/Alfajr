package io.github.sourcem7.alfajralarm.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import io.github.sourcem7.alfajralarm.domain.AlarmPreferences
import io.github.sourcem7.alfajralarm.domain.AlarmPreferencesStore
import io.github.sourcem7.alfajralarm.domain.FajrMethod
import io.github.sourcem7.alfajralarm.domain.FixedLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_preferences")

class DataStoreAlarmPreferencesRepository(private val context: Context) : AlarmPreferencesStore {
    override val preferences: Flow<AlarmPreferences> = context.userPreferencesDataStore.data.map { stored ->
        AlarmPreferences(
            location = stored[LOCATION_ID]?.let { id ->
                FixedLocation(
                    id = id,
                    displayName = stored[LOCATION_NAME] ?: "",
                    displayNameArabic = stored[LOCATION_ARABIC_NAME],
                    countryCode = stored[LOCATION_COUNTRY],
                    administrationName = stored[LOCATION_ADMINISTRATION],
                    latitude = stored[LOCATION_LATITUDE]?.toDoubleOrNull() ?: Double.NaN,
                    longitude = stored[LOCATION_LONGITUDE]?.toDoubleOrNull() ?: Double.NaN,
                    zoneId = stored[LOCATION_ZONE] ?: "",
                )
            },
            method = stored[METHOD]?.let { runCatching { FajrMethod.valueOf(it) }.getOrNull() },
            correctionMinutes = stored[CORRECTION] ?: 0,
            wakeOffsetMinutes = stored[WAKE_OFFSET] ?: 0,
            ringtoneUri = stored[RINGTONE],
            vibrationEnabled = stored[VIBRATION] ?: true,
            snoozeMinutes = stored[SNOOZE] ?: 5,
            tapToDismiss = stored[TAP_TO_DISMISS] ?: false,
        )
    }

    override suspend fun load(): AlarmPreferences = preferences.first()

    override suspend fun updateLocation(location: FixedLocation) {
        context.userPreferencesDataStore.edit {
            it[LOCATION_ID] = location.id
            it[LOCATION_NAME] = location.displayName
            location.displayNameArabic?.let { arabic -> it[LOCATION_ARABIC_NAME] = arabic } ?: it.remove(LOCATION_ARABIC_NAME)
            location.countryCode?.let { country -> it[LOCATION_COUNTRY] = country } ?: it.remove(LOCATION_COUNTRY)
            location.administrationName?.let { administration -> it[LOCATION_ADMINISTRATION] = administration } ?: it.remove(LOCATION_ADMINISTRATION)
            it[LOCATION_LATITUDE] = location.latitude.toString()
            it[LOCATION_LONGITUDE] = location.longitude.toString()
            it[LOCATION_ZONE] = location.zoneId
        }
    }

    override suspend fun updateMethod(method: FajrMethod) {
        context.userPreferencesDataStore.edit { it[METHOD] = method.name }
    }

    override suspend fun updateCorrection(minutes: Int) {
        context.userPreferencesDataStore.edit { it[CORRECTION] = minutes.coerceIn(-30, 30) }
    }

    override suspend fun updateWakeOffset(minutes: Int) {
        context.userPreferencesDataStore.edit { it[WAKE_OFFSET] = minutes.coerceIn(-60, 30) }
    }

    /** Null restores the device's own alarm sound as the first candidate. */
    override suspend fun updateRingtone(uri: String?) {
        context.userPreferencesDataStore.edit {
            if (uri.isNullOrBlank()) it.remove(RINGTONE) else it[RINGTONE] = uri
        }
    }

    override suspend fun updateVibration(enabled: Boolean) {
        context.userPreferencesDataStore.edit { it[VIBRATION] = enabled }
    }

    /** The product specification offers five or ten minutes and nothing else. */
    override suspend fun updateSnoozeMinutes(minutes: Int) {
        context.userPreferencesDataStore.edit {
            it[SNOOZE] = if (minutes >= 10) 10 else 5
        }
    }

    override suspend fun updateTapToDismiss(enabled: Boolean) {
        context.userPreferencesDataStore.edit { it[TAP_TO_DISMISS] = enabled }
    }

    private companion object {
        val LOCATION_ID = stringPreferencesKey("location_id")
        val LOCATION_NAME = stringPreferencesKey("location_name")
        val LOCATION_ARABIC_NAME = stringPreferencesKey("location_arabic_name")
        val LOCATION_COUNTRY = stringPreferencesKey("location_country")
        val LOCATION_ADMINISTRATION = stringPreferencesKey("location_administration")
        val LOCATION_LATITUDE = stringPreferencesKey("location_latitude")
        val LOCATION_LONGITUDE = stringPreferencesKey("location_longitude")
        val LOCATION_ZONE = stringPreferencesKey("location_zone")
        val METHOD = stringPreferencesKey("method")
        val CORRECTION = intPreferencesKey("correction_minutes")
        val WAKE_OFFSET = intPreferencesKey("wake_offset_minutes")
        val RINGTONE = stringPreferencesKey("ringtone_uri")
        val VIBRATION = booleanPreferencesKey("vibration_enabled")
        val SNOOZE = intPreferencesKey("snooze_minutes")
        val TAP_TO_DISMISS = booleanPreferencesKey("tap_to_dismiss")
    }
}
