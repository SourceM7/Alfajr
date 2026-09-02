package io.github.sourcem7.alfajralarm.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sourcem7.alfajralarm.R
import io.github.sourcem7.alfajralarm.app.AppGraph
import io.github.sourcem7.alfajralarm.domain.AlarmPreferences
import kotlinx.coroutines.launch

/**
 * Temporary Phase 3 controls. Phase 4 folds these into the production settings
 * screen; until then they are the only way to exercise the ringing path.
 */
@Composable
fun RingingDiagnostics(graph: AppGraph, preferences: AlarmPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = graph.preferencesRepository
    val session by graph.ringing.session.collectAsStateWithLifecycle()

    val ringtonePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        // Android's own picker keeps the app away from broad file access.
        val picked: Uri? = result.data?.let { data ->
            IntentCompat.getParcelableExtra(data, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        }
        scope.launch { settings.updateRingtone(picked?.toString()) }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.phase_three_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.phase_three_description), style = MaterialTheme.typography.bodySmall)

            Text(
                stringResource(
                    R.string.ringtone_row,
                    preferences.ringtoneUri?.let { ringtoneTitle(it) } ?: stringResource(R.string.ringtone_default),
                ),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { ringtonePicker.launch(ringtonePickerIntent(context, preferences.ringtoneUri)) }) {
                    Text(stringResource(R.string.action_choose_ringtone))
                }
                TextButton(onClick = { scope.launch { settings.updateRingtone(null) } }) {
                    Text(stringResource(R.string.action_use_default_ringtone))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.vibration_switch), modifier = Modifier.weight(1f))
                Switch(
                    checked = preferences.vibrationEnabled,
                    onCheckedChange = { enabled -> scope.launch { settings.updateVibration(enabled) } },
                )
            }

            Text(stringResource(R.string.snooze_length_label))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10).forEach { minutes ->
                    FilterChip(
                        selected = preferences.snoozeMinutes == minutes,
                        onClick = { scope.launch { settings.updateSnoozeMinutes(minutes) } },
                        label = { Text(pluralStringResource(R.plurals.snooze_length_option, minutes, minutes)) },
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.tap_to_dismiss_switch))
                    Text(
                        stringResource(R.string.tap_to_dismiss_description),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = preferences.tapToDismiss,
                    onCheckedChange = { enabled -> scope.launch { settings.updateTapToDismiss(enabled) } },
                )
            }

            val active = session
            if (active == null) {
                Text(stringResource(R.string.ringing_not_active))
            } else {
                Text(
                    stringResource(
                        R.string.ringing_now,
                        stringResource(if (active.isTest) R.string.ringing_title_test else R.string.ringing_title_daily),
                    ),
                )
                TextButton(onClick = { context.startActivity(Intent(context, AlarmRingingActivity::class.java)) }) {
                    Text(stringResource(R.string.action_open_ringing_screen))
                }
            }
        }
    }
}

@Composable
private fun ringtoneTitle(uri: String): String {
    val context = LocalContext.current
    // A deleted or unreadable sound has no title; the fallback chain covers it
    // at ringing time, so the raw value is enough to show here.
    return runCatching { RingtoneManager.getRingtone(context, uri.toUri())?.getTitle(context) }
        .getOrNull() ?: uri
}

private fun ringtonePickerIntent(context: android.content.Context, current: String?): Intent =
    Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, context.getString(R.string.ringtone_picker_title))
        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        .putExtra(
            RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
        )
        .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current?.toUri())
