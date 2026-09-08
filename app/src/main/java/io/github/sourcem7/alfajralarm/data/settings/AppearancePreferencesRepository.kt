package io.github.sourcem7.alfajralarm.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import io.github.sourcem7.alfajralarm.domain.AppearancePreferencesStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Backed-up display choices. They are intentionally not part of the alarm domain. */
private val Context.appearancePreferencesDataStore by preferencesDataStore(name = "appearance_preferences")

class DataStoreAppearancePreferencesRepository(private val context: Context) : AppearancePreferencesStore {
    // Material You is the platform-native default on Android 12+. Keeping the
    // absence of a key distinct from an explicit false preserves opt-outs.
    override val dynamicColor: Flow<Boolean> = context.appearancePreferencesDataStore.data.map { it[DYNAMIC_COLOR] ?: true }

    override suspend fun updateDynamicColor(enabled: Boolean) {
        context.appearancePreferencesDataStore.edit {
            it[DYNAMIC_COLOR] = enabled
        }
    }

    private companion object {
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }
}
