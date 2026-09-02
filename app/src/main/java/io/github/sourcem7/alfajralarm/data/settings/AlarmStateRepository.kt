package io.github.sourcem7.alfajralarm.data.settings

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.sourcem7.alfajralarm.domain.AlarmOutcome
import io.github.sourcem7.alfajralarm.domain.AlarmState
import io.github.sourcem7.alfajralarm.domain.AlarmStateStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

// Device-local alarm state. `backup_rules.xml` excludes this file so a restored
// installation can never inherit an enabled alarm.
private val Context.alarmStateDataStore by preferencesDataStore(name = "alarm_state")

class AlarmStateRepository(private val context: Context) : AlarmStateStore {
    override val state: Flow<AlarmState> = context.alarmStateDataStore.data.map { it.toAlarmState() }

    override suspend fun current(): AlarmState = state.first()

    override suspend fun update(transform: (AlarmState) -> AlarmState): AlarmState =
        context.alarmStateDataStore.edit { stored -> stored.write(transform(stored.toAlarmState())) }.toAlarmState()

    private fun Preferences.toAlarmState(): AlarmState = AlarmState(
        dailyEnabled = this[DAILY_ENABLED] ?: false,
        nextPrayerDate = this[NEXT_PRAYER_DATE]?.toLocalDateOrNull(),
        nextAlarmEpochMillis = this[NEXT_ALARM_MILLIS],
        skippedPrayerDate = this[SKIPPED_PRAYER_DATE]?.toLocalDateOrNull(),
        ringingSessionId = this[RINGING_SESSION_ID],
        snoozeCount = this[SNOOZE_COUNT] ?: 0,
        testSessionId = this[TEST_SESSION_ID],
        testSnoozeCount = this[TEST_SNOOZE_COUNT] ?: 0,
        lastOutcome = this[LAST_OUTCOME]?.let { runCatching { AlarmOutcome.valueOf(it) }.getOrNull() },
        lastOutcomeEpochMillis = this[LAST_OUTCOME_MILLIS],
        lastDeliveryEpochMillis = this[LAST_DELIVERY_MILLIS],
        activationConfirmed = this[ACTIVATION_CONFIRMED] ?: false,
    )

    private fun MutablePreferences.write(state: AlarmState) {
        this[DAILY_ENABLED] = state.dailyEnabled
        this[SNOOZE_COUNT] = state.snoozeCount
        this[TEST_SNOOZE_COUNT] = state.testSnoozeCount
        this[ACTIVATION_CONFIRMED] = state.activationConfirmed
        setOrRemove(NEXT_PRAYER_DATE, state.nextPrayerDate?.toString())
        setOrRemove(NEXT_ALARM_MILLIS, state.nextAlarmEpochMillis)
        setOrRemove(SKIPPED_PRAYER_DATE, state.skippedPrayerDate?.toString())
        setOrRemove(RINGING_SESSION_ID, state.ringingSessionId)
        setOrRemove(TEST_SESSION_ID, state.testSessionId)
        setOrRemove(LAST_OUTCOME, state.lastOutcome?.name)
        setOrRemove(LAST_OUTCOME_MILLIS, state.lastOutcomeEpochMillis)
        setOrRemove(LAST_DELIVERY_MILLIS, state.lastDeliveryEpochMillis)
    }

    private fun <T : Any> MutablePreferences.setOrRemove(key: Preferences.Key<T>, value: T?) {
        if (value == null) remove(key) else this[key] = value
    }

    private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

    private companion object {
        val DAILY_ENABLED = booleanPreferencesKey("daily_enabled")
        val NEXT_PRAYER_DATE = stringPreferencesKey("next_prayer_date")
        val NEXT_ALARM_MILLIS = longPreferencesKey("next_alarm_millis")
        val SKIPPED_PRAYER_DATE = stringPreferencesKey("skipped_prayer_date")
        val RINGING_SESSION_ID = stringPreferencesKey("ringing_session_id")
        val SNOOZE_COUNT = intPreferencesKey("snooze_count")
        val TEST_SESSION_ID = stringPreferencesKey("test_session_id")
        val TEST_SNOOZE_COUNT = intPreferencesKey("test_snooze_count")
        val LAST_OUTCOME = stringPreferencesKey("last_outcome")
        val LAST_OUTCOME_MILLIS = longPreferencesKey("last_outcome_millis")
        val LAST_DELIVERY_MILLIS = longPreferencesKey("last_delivery_millis")
        val ACTIVATION_CONFIRMED = booleanPreferencesKey("activation_confirmed")
    }
}
