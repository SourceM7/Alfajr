# Privacy statement

Alfajr Alarm is an offline alarm clock. It has no account, no advertising, no
analytics, and no remote crash reporting.

## What the app does not do

- It does not request the `INTERNET` permission, so it cannot send or receive
  network data. You can verify this in the installed manifest.
- It does not request location permission. The city or coordinates you choose
  are entered by you and never derived from the device's location.
- It does not request storage permission. Ringtone selection goes through
  Android's own ringtone picker.
- It collects no identifier, contains no third-party SDK, and reports nothing to
  the developer or anyone else.

## What the app stores, and where

Everything is stored locally on the device, in the app's private storage.

**User preferences** — selected city or manual coordinates and IANA time zone,
calculation method, prayer-time correction, wake-up offset, ringtone selection,
vibration, snooze duration, and the dismissal preference. Android's backup may
include these, so restoring the app to a new device can restore your settings.

**Device-local alarm state** — whether the daily alarm is enabled, the next
occurrence, a skipped date, the active ringing session, and the single most
recent outcome (dismissed, snoozed, missed, or skipped). This is excluded from
Android backup, so a restored installation always starts with the alarm disabled
and rechecks permissions before it can be switched on again.

No long-term history is kept. Only the most recent outcome is retained, and it
is overwritten by the next one.

## Permissions and why they exist

| Permission | Why |
| --- | --- |
| `USE_EXACT_ALARM` (`SCHEDULE_EXACT_ALARM` on Android 12/12L) | Ring at the exact calculated time. The app is solely an alarm clock. |
| `POST_NOTIFICATIONS` | Show the ringing alarm and missed-alarm messages. |
| `USE_FULL_SCREEN_INTENT` | Show the alarm screen over the lock screen. |
| `RECEIVE_BOOT_COMPLETED` | Re-establish the next alarm after a restart. |

## Removing your data

Uninstalling the app, or clearing its storage in Android settings, deletes
everything it holds. There is nothing stored anywhere else.
