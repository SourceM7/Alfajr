package io.github.sourcem7.alfajralarm.alarm

import io.github.sourcem7.alfajralarm.domain.AlarmKind

/**
 * Pending-intent identity for each alarm kind. Android compares intents by
 * action, data, and component but not by extras, so every kind carries its own
 * action and request code. That is what stops a test or snooze alarm from
 * replacing the daily Fajr alarm.
 */
object AlarmIntents {
    const val ACTION_DAILY_ALARM = "io.github.sourcem7.alfajralarm.action.DAILY_ALARM"
    const val ACTION_SNOOZE_ALARM = "io.github.sourcem7.alfajralarm.action.SNOOZE_ALARM"
    const val ACTION_TEST_ALARM = "io.github.sourcem7.alfajralarm.action.TEST_ALARM"

    const val EXTRA_KIND = "io.github.sourcem7.alfajralarm.extra.KIND"
    const val EXTRA_TRIGGER_AT_MILLIS = "io.github.sourcem7.alfajralarm.extra.TRIGGER_AT_MILLIS"
    const val EXTRA_PRAYER_DATE = "io.github.sourcem7.alfajralarm.extra.PRAYER_DATE"
    const val EXTRA_SESSION_ID = "io.github.sourcem7.alfajralarm.extra.SESSION_ID"

    fun action(kind: AlarmKind): String = when (kind) {
        AlarmKind.DAILY -> ACTION_DAILY_ALARM
        AlarmKind.SNOOZE -> ACTION_SNOOZE_ALARM
        AlarmKind.TEST -> ACTION_TEST_ALARM
    }

    fun requestCode(kind: AlarmKind): Int = when (kind) {
        AlarmKind.DAILY -> 1001
        AlarmKind.SNOOZE -> 1002
        AlarmKind.TEST -> 1003
    }

    fun kindOf(rawAction: String?): AlarmKind? = AlarmKind.entries.firstOrNull { rawAction == action(it) }

    /** Request code for the AlarmClockInfo show intent that opens the app. */
    const val SHOW_INTENT_REQUEST_CODE = 2001
}
