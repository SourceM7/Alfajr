package io.github.sourcem7.alfajralarm.ui

import android.content.Context
import io.github.sourcem7.alfajralarm.R
import io.github.sourcem7.alfajralarm.domain.FixedLocation
import kotlinx.datetime.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/*
 * Formatting shared by the app's screens and the home-screen widgets, which
 * have a Context but no Compose UI locals. Times and dates are always shown in
 * the alarm location's zone, never the device's.
 */

/** The user's 12- or 24-hour preference, in the location's zone. */
internal fun Context.formatTime(epochMillis: Long, zoneId: String): String =
    android.text.format.DateFormat.getTimeFormat(this).apply {
        timeZone = TimeZone.getTimeZone(zoneId)
    }.format(Date(epochMillis))

internal fun Context.formatDate(date: LocalDate, zoneId: String): String =
    android.text.format.DateFormat.getDateFormat(this).apply {
        timeZone = TimeZone.getTimeZone(zoneId)
    }.format(
        Date.from(
            java.time.LocalDateTime.of(date.year, date.month.ordinal + 1, date.day, 0, 0)
                .atZone(ZoneId.of(zoneId)).toInstant()
        )
    )

/** The Arabic city name when the interface is Arabic and one is bundled. */
internal fun FixedLocation.displayNameFor(
    context: Context,
    locale: Locale? = context.resources.configuration.locales[0],
): String = when {
    id.startsWith("manual:") -> context.getString(R.string.manual_location)
    locale?.language == "ar" -> displayNameArabic ?: displayName
    else -> displayName
}

/** A clock reading split so the digits can be set large and the AM/PM marker small. */
internal data class TimeParts(val digits: String, val dayPeriod: String?) {
    override fun toString(): String = listOfNotNull(digits, dayPeriod).joinToString(" ")
}

internal fun Context.formatTimeParts(epochMillis: Long, zoneId: String): TimeParts {
    if (android.text.format.DateFormat.is24HourFormat(this)) {
        return TimeParts(formatTime(epochMillis, zoneId), null)
    }
    val locale = resources.configuration.locales[0] ?: Locale.getDefault()
    val zone = TimeZone.getTimeZone(zoneId)
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "hm")
    fun format(pattern: String) = java.text.SimpleDateFormat(pattern, locale)
        .apply { timeZone = zone }
        .format(Date(epochMillis))
    return TimeParts(digits = format(withoutDayPeriod(pattern)), dayPeriod = format("a"))
}

/**
 * Drops the unquoted day-period field from a localized time pattern and the
 * spacing around it, leaving quoted literals alone. Locales disagree on which
 * side the marker sits and on which space separates it.
 */
internal fun withoutDayPeriod(pattern: String): String {
    val kept = StringBuilder()
    var quoted = false
    for (char in pattern) {
        if (char == '\'') quoted = !quoted
        if (!quoted && (char == 'a' || char == 'b' || char == 'B')) continue
        kept.append(char)
    }
    return kept.toString().trim { it.isWhitespace() || it == '\u00A0' || it == '\u202F' }
}
