package io.github.sourcem7.alfajralarm.alarm

import io.github.sourcem7.alfajralarm.domain.AlarmCapabilities
import io.github.sourcem7.alfajralarm.domain.AlarmKind
import io.github.sourcem7.alfajralarm.domain.AlarmPreferences
import io.github.sourcem7.alfajralarm.domain.AlarmRequest
import io.github.sourcem7.alfajralarm.domain.AlarmState
import io.github.sourcem7.alfajralarm.domain.AlarmStateStore
import io.github.sourcem7.alfajralarm.domain.ExactAlarmGateway
import io.github.sourcem7.alfajralarm.domain.FajrCalculator
import io.github.sourcem7.alfajralarm.domain.FajrMethod
import io.github.sourcem7.alfajralarm.domain.FajrOccurrence
import io.github.sourcem7.alfajralarm.domain.FixedLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.minutes

internal val DAMASCUS = FixedLocation(
    id = "damascus",
    displayName = "Damascus",
    countryCode = "SY",
    latitude = 33.5138,
    longitude = 36.2765,
    zoneId = "Asia/Damascus",
)

internal fun testPreferences(
    location: FixedLocation? = DAMASCUS,
    method: FajrMethod? = FajrMethod.EGYPTIAN,
    correction: Int = 0,
    wake: Int = 0,
) = AlarmPreferences(
    location = location,
    method = method,
    correctionMinutes = correction,
    wakeOffsetMinutes = wake,
)

internal fun healthyCapabilities() = AlarmCapabilities(
    canScheduleExactAlarms = true,
    notificationsEnabled = true,
    alarmChannelEnabled = true,
    canUseFullScreenIntent = true,
    alarmVolume = 6,
    maxAlarmVolume = 7,
)

internal class FakeAlarmStateStore(initial: AlarmState = AlarmState()) : AlarmStateStore {
    private val stored = MutableStateFlow(initial)
    override val state: Flow<AlarmState> = stored
    override suspend fun current(): AlarmState = stored.value
    override suspend fun update(transform: (AlarmState) -> AlarmState): AlarmState {
        stored.value = transform(stored.value)
        return stored.value
    }
}

/** Records every AlarmManager interaction so replacement can be asserted. */
internal class RecordingExactAlarmGateway : ExactAlarmGateway {
    val interactions = mutableListOf<String>()
    val registered = linkedMapOf<AlarmKind, Long>()
    var exactAlarmsAllowed = true
    var scheduleSucceeds = true

    override fun canScheduleExactAlarms(): Boolean = exactAlarmsAllowed

    override fun scheduleAlarmClock(request: AlarmRequest): Boolean = register("alarmClock", request)

    override fun scheduleExactWhileIdle(request: AlarmRequest): Boolean = register("whileIdle", request)

    override fun cancel(kind: AlarmKind) {
        interactions += "cancel:$kind"
        registered.remove(kind)
    }

    private fun register(api: String, request: AlarmRequest): Boolean {
        interactions += "$api:${request.kind}:${request.triggerAtMillis}"
        if (!scheduleSucceeds) return false
        registered[request.kind] = request.triggerAtMillis
        return true
    }
}

/**
 * A calculator with a fixed local Fajr time. It makes offset and date-window
 * behavior explicit without depending on astronomical values.
 */
internal class StubFajrCalculator(
    private val fajrLocalTime: LocalTime,
    private val zoneId: String = DAMASCUS.zoneId,
) : FajrCalculator {
    override fun calculate(date: LocalDate, preferences: AlarmPreferences): FajrOccurrence {
        val zone = TimeZone.of(zoneId)
        val prayer = date.atTime(fajrLocalTime).toInstant(zone)
        val corrected = prayer + preferences.correctionMinutes.minutes
        return FajrOccurrence(
            prayerInstant = prayer,
            correctedPrayerInstant = corrected,
            alarmInstant = corrected + preferences.wakeOffsetMinutes.minutes,
            prayerLocalDate = date,
            zoneId = zoneId,
            highLatitudeRuleActive = false,
        )
    }
}
