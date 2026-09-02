package io.github.sourcem7.alfajralarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import io.github.sourcem7.alfajralarm.calculation.AdhanFajrCalculator
import io.github.sourcem7.alfajralarm.calculation.MethodSuggestions
import io.github.sourcem7.alfajralarm.app.AppGraph
import io.github.sourcem7.alfajralarm.data.location.ManualLocation
import io.github.sourcem7.alfajralarm.data.location.ManualLocationResult
import io.github.sourcem7.alfajralarm.data.settings.AlarmPreferencesRepository
import io.github.sourcem7.alfajralarm.domain.AlarmPreferences
import io.github.sourcem7.alfajralarm.domain.FajrMethod
import io.github.sourcem7.alfajralarm.domain.FajrOccurrence
import io.github.sourcem7.alfajralarm.domain.FixedLocation
import io.github.sourcem7.alfajralarm.domain.PreferenceError
import io.github.sourcem7.alfajralarm.domain.ScheduleReason
import io.github.sourcem7.alfajralarm.ui.RingingDiagnostics
import io.github.sourcem7.alfajralarm.ui.SchedulingDiagnostics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private val graph: AppGraph by lazy { AppGraph.from(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AlarmTracer(graph)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Force-stop and OEM power management can drop scheduled alarms, so the
        // next occurrence is reconciled on every launch and resume.
        graph.scope.launch { graph.scheduler.scheduleNext(ScheduleReason.AppOpened) }
    }
}

@Composable
private fun AlarmTracer(graph: AppGraph) {
    val settings: AlarmPreferencesRepository = graph.preferencesRepository
    val cities = graph.cityRepository
    val calculator: AdhanFajrCalculator = graph.calculator
    val preferences by produceState(initialValue = AlarmPreferences(), settings) {
        settings.preferences.collect { value = it }
    }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<FixedLocation>()) }
    var manualLatitude by remember { mutableStateOf("") }
    var manualLongitude by remember { mutableStateOf("") }
    var manualZoneId by remember { mutableStateOf("") }
    var manualError by remember { mutableStateOf<PreferenceError?>(null) }
    val scope = rememberCoroutineScope()
    // A settings change must replace the registered alarm rather than add one.
    LaunchedEffect(preferences) { graph.scheduler.scheduleNext(ScheduleReason.SettingsChanged) }
    LaunchedEffect(query) {
        delay(200)
        results = cities.search(query = query)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(stringResource(R.string.phase_one_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.phase_one_description), style = MaterialTheme.typography.bodyMedium)
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.city_search_label)) },
                singleLine = true,
            )
        }
        items(results, key = { it.id }) { city ->
            CityRow(city = city, onSelect = {
                scope.launch {
                    settings.updateLocation(city)
                }
            })
        }
        item {
            ManualLocationCard(
                latitude = manualLatitude,
                longitude = manualLongitude,
                zoneId = manualZoneId,
                error = manualError,
                onLatitudeChange = { manualLatitude = it },
                onLongitudeChange = { manualLongitude = it },
                onZoneChange = { manualZoneId = it },
                onSave = {
                    when (val result = ManualLocation.create(manualLatitude, manualLongitude, manualZoneId)) {
                        is ManualLocationResult.Valid -> {
                            manualError = null
                            scope.launch {
                                settings.updateLocation(result.location)
                            }
                        }
                        is ManualLocationResult.Invalid -> manualError = result.reason
                    }
                },
            )
        }
        item { SelectedLocationCard(preferences.location) }
        item {
            MethodPicker(
                selected = preferences.method,
                suggestion = preferences.location?.let { MethodSuggestions.suggest(it.countryCode) },
                onSelect = { method -> scope.launch { settings.updateMethod(method) } },
            )
        }
        item {
            OffsetPicker(
                correction = preferences.correctionMinutes,
                wakeOffset = preferences.wakeOffsetMinutes,
                onCorrection = { value -> scope.launch { settings.updateCorrection(value) } },
                onWakeOffset = { value -> scope.launch { settings.updateWakeOffset(value) } },
            )
        }
        item { PreviewCard(preferences, calculator) }
        item { SchedulingDiagnostics(graph, preferences) }
        item { RingingDiagnostics(graph, preferences) }
    }
}

@Composable
private fun ManualLocationCard(
    latitude: String,
    longitude: String,
    zoneId: String,
    error: PreferenceError?,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onZoneChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.manual_location), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(latitude, onLatitudeChange, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.latitude)) }, singleLine = true)
            OutlinedTextField(longitude, onLongitudeChange, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.longitude)) }, singleLine = true)
            OutlinedTextField(zoneId, onZoneChange, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.time_zone)) }, singleLine = true)
            error?.let { Text(it.localizedMessage(), color = MaterialTheme.colorScheme.error) }
            Button(onClick = onSave) { Text(stringResource(R.string.save_manual_location)) }
        }
    }
}

@Composable
private fun CityRow(city: FixedLocation, onSelect: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(city.displayNameForUi(), style = MaterialTheme.typography.titleMedium)
            Text(listOfNotNull(city.administrationName, city.countryCode, city.zoneId).joinToString(" · "))
        }
    }
}

@Composable
private fun SelectedLocationCard(location: FixedLocation?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.selected_location), style = MaterialTheme.typography.titleMedium)
            Text(location?.let { "${it.displayNameForUi()} · ${it.zoneId}" } ?: stringResource(R.string.no_location_selected))
        }
    }
}

@Composable
private fun MethodPicker(selected: FajrMethod?, suggestion: FajrMethod?, onSelect: (FajrMethod) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.calculation_method), style = MaterialTheme.typography.titleMedium)
        if (selected == null && suggestion != null) {
            Text(stringResource(R.string.suggested_method, suggestion.localizedName()))
        }
        FajrMethod.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { method ->
                    Button(onClick = { onSelect(method) }) {
                        Text(
                            if (method == selected) stringResource(R.string.selected_method, method.localizedName())
                            else method.localizedName(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OffsetPicker(
    correction: Int,
    wakeOffset: Int,
    onCorrection: (Int) -> Unit,
    onWakeOffset: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OffsetControl(pluralStringResource(R.plurals.prayer_correction, kotlin.math.abs(correction), correction), correction, -30, 30, onCorrection)
        OffsetControl(pluralStringResource(R.plurals.wake_offset, kotlin.math.abs(wakeOffset), wakeOffset), wakeOffset, -60, 30, onWakeOffset)
    }
}

@Composable
private fun OffsetControl(label: String, value: Int, minimum: Int, maximum: Int, onChange: (Int) -> Unit) {
    Row {
        Text(label, modifier = Modifier.weight(1f))
        Button(onClick = { onChange((value - 1).coerceAtLeast(minimum)) }, enabled = value > minimum) { Text("−") }
        Spacer(Modifier.width(8.dp))
        Button(onClick = { onChange((value + 1).coerceAtMost(maximum)) }, enabled = value < maximum) { Text("+") }
    }
}

@Composable
private fun PreviewCard(preferences: AlarmPreferences, calculator: AdhanFajrCalculator) {
    val previews = remember(preferences) {
        val location = preferences.location
        if (location == null || preferences.method == null) emptyList()
        else runCatching {
            val zone = ZoneId.of(location.zoneId)
            val today = Instant.now().atZone(zone).toLocalDate()
            listOf(today, today.plusDays(1)).map { date ->
                calculator.calculate(LocalDate(date.year, date.monthValue, date.dayOfMonth), preferences)
            }
        }.getOrElse { emptyList() }
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.fajr_preview), style = MaterialTheme.typography.titleMedium)
            if (previews.isEmpty()) Text(stringResource(R.string.preview_requires_setup))
            previews.forEach { occurrence ->
                val formattedTimes = formatOccurrence(occurrence)
                Text(stringResource(R.string.preview_row, occurrence.prayerLocalDate.toString(), formattedTimes[0], formattedTimes[1], formattedTimes[2]))
                if (occurrence.highLatitudeRuleActive) Text(stringResource(R.string.high_latitude_notice))
            }
        }
    }
}

private fun formatOccurrence(occurrence: FajrOccurrence): List<String> {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val zone = ZoneId.of(occurrence.zoneId)
    fun format(instant: kotlin.time.Instant) = Instant.ofEpochMilli(instant.toEpochMilliseconds()).atZone(zone).format(formatter)
    return listOf(format(occurrence.prayerInstant), format(occurrence.correctedPrayerInstant), format(occurrence.alarmInstant))
}

@Composable
private fun FixedLocation.displayNameForUi(): String =
    when {
        id.startsWith("manual:") -> stringResource(R.string.manual_location)
        LocalConfiguration.current.locales[0]?.language == "ar" -> displayNameArabic ?: displayName
        else -> displayName
    }

@Composable
private fun FajrMethod.localizedName(): String = stringResource(
    when (this) {
        FajrMethod.MUSLIM_WORLD_LEAGUE -> R.string.method_muslim_world_league
        FajrMethod.EGYPTIAN -> R.string.method_egyptian
        FajrMethod.KARACHI -> R.string.method_karachi
        FajrMethod.UMM_AL_QURA -> R.string.method_umm_al_qura
        FajrMethod.DUBAI -> R.string.method_dubai
        FajrMethod.QATAR -> R.string.method_qatar
        FajrMethod.KUWAIT -> R.string.method_kuwait
        FajrMethod.MOON_SIGHTING_COMMITTEE -> R.string.method_moon_sighting_committee
        FajrMethod.SINGAPORE -> R.string.method_singapore
        FajrMethod.TURKEY -> R.string.method_turkey
    },
)

@Composable
private fun PreferenceError.localizedMessage(): String = stringResource(
    when (this) {
        PreferenceError.LOCATION_REQUIRED -> R.string.error_location_required
        PreferenceError.METHOD_REQUIRED -> R.string.error_method_required
        PreferenceError.INVALID_LATITUDE -> R.string.error_invalid_latitude
        PreferenceError.INVALID_LONGITUDE -> R.string.error_invalid_longitude
        PreferenceError.INVALID_TIME_ZONE -> R.string.error_invalid_time_zone
    },
)
