package io.github.sourcem7.alfajralarm.widget

import android.content.Context
import io.github.sourcem7.alfajralarm.R
import io.github.sourcem7.alfajralarm.domain.FajrOccurrence
import io.github.sourcem7.alfajralarm.ui.TimeParts
import io.github.sourcem7.alfajralarm.ui.displayNameFor
import io.github.sourcem7.alfajralarm.ui.formatDate
import io.github.sourcem7.alfajralarm.ui.formatTimeParts
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** The resolved text and drawing inputs every widget lays out in its own way. */
internal data class WidgetContent(
    val phase: SkyPhase,
    /** "Fajr · Tomorrow", or just "Fajr" when no alarm time is shown. */
    val eyebrow: String,
    val time: TimeParts?,
    /** A status that replaces or qualifies the time, such as "Alarm off". */
    val message: String?,
    /** Shown on home-screen widgets only; the lock-screen widget never shows it. */
    val location: String?,
    /** Set only when an alarm will really ring at this instant. */
    val countdownTo: Instant?,
    val ringProgress: Float,
    val highLatitude: Boolean,
    val description: String,
)

internal fun WidgetFrame.present(context: Context): WidgetContent {
    val fajr = context.getString(R.string.widget_fajr)
    val phase = DawnTimeline.skyPhase(snapshot, now)

    fun dayLabel(at: Instant, zoneId: String): String = when (DawnTimeline.dayLabel(at, zoneId, now)) {
        DayLabel.TODAY -> context.getString(R.string.label_today)
        DayLabel.TOMORROW -> context.getString(R.string.label_tomorrow)
        DayLabel.LATER -> context.formatDate(at.toLocalDateTime(TimeZone.of(zoneId)).date, zoneId)
    }

    fun eyebrow(detail: String) = context.getString(R.string.widget_eyebrow, fajr, detail)

    fun status(message: String, preview: FajrOccurrence?): WidgetContent {
        if (preview == null) {
            return WidgetContent(phase, fajr, null, message, null, null, 0f, false, message)
        }
        val time = context.formatTimeParts(preview.alarmInstant.toEpochMilliseconds(), preview.zoneId)
        val day = dayLabel(preview.alarmInstant, preview.zoneId)
        return WidgetContent(
            phase = phase,
            eyebrow = eyebrow(day),
            time = time,
            message = message,
            location = null,
            countdownTo = null,
            ringProgress = 0f,
            highLatitude = preview.highLatitudeRuleActive,
            description = context.getString(R.string.widget_a11y_preview, message, time.toString(), day),
        )
    }

    return when (val snapshot = snapshot) {
        WidgetSnapshot.NeedsSetup -> status(context.getString(R.string.widget_setup_needed), null)
        is WidgetSnapshot.Off -> status(context.getString(R.string.widget_alarm_off), snapshot.preview)
        is WidgetSnapshot.NeedsAttention -> status(context.getString(R.string.widget_needs_attention), snapshot.preview)
        is WidgetSnapshot.Ringing -> {
            val message = context.getString(R.string.widget_ringing)
            WidgetContent(phase, fajr, null, message, null, null, 1f, false, message)
        }
        is WidgetSnapshot.Snoozed -> {
            val time = context.formatTimeParts(snapshot.until.toEpochMilliseconds(), snapshot.zoneId)
            WidgetContent(
                phase = phase,
                eyebrow = eyebrow(context.getString(R.string.widget_snoozed)),
                time = time,
                message = null,
                location = null,
                countdownTo = snapshot.until,
                ringProgress = 1f,
                highLatitude = false,
                description = context.getString(R.string.widget_a11y_snoozed, time.toString()),
            )
        }
        is WidgetSnapshot.Scheduled -> {
            val occurrence = snapshot.occurrence
            val alarmAt = occurrence.alarmInstant
            val time = context.formatTimeParts(alarmAt.toEpochMilliseconds(), occurrence.zoneId)
            val day = dayLabel(alarmAt, occurrence.zoneId)
            WidgetContent(
                phase = phase,
                eyebrow = eyebrow(day),
                time = time,
                message = null,
                location = snapshot.location.displayNameFor(context),
                countdownTo = alarmAt,
                ringProgress = DawnTimeline.ringProgress(now, alarmAt),
                highLatitude = occurrence.highLatitudeRuleActive,
                description = context.getString(R.string.widget_a11y_scheduled, time.toString(), day),
            )
        }
    }
}
