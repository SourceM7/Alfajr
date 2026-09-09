# Alfajr Alarm

A quiet, offline Fajr alarm for Android. Prayer times are calculated on the
device — no account, internet permission, location access, advertising, or
analytics.

## Install

Download `app-release.apk` from the [latest release](https://github.com/SourceM7/Alfajr/releases/latest).
Both files in the release are needed to check it before installing:

```sh
sha256sum -c app-release.apk.sha256
```

Requires Android 8.0 (API 26) or newer.

### Full-screen alarm permission

On Android 14 and newer, a sideloaded build is not granted the *Full screen
notifications* permission automatically. Without it the alarm still rings, but
it arrives as a notification instead of covering the lock screen. The app
reports this on its home screen and links straight to the setting.

## What it does

- One daily alarm at the calculated Fajr time for a fixed city, searched offline
- Ten calculation methods, with one suggested from the selected country
- Separate prayer-time correction and wake-up offset, applied in that order
- A full-screen alarm over the lock screen, with snooze and hold-to-dismiss
- Skip a single day without disturbing the recurring alarm
- English and Arabic, with mirrored right-to-left layouts

It is deliberately Fajr-only: no other prayer times, no qibla, no calendar, and
no long-term history.

## Build

```sh
./gradlew assembleDebug
./gradlew test lint
```

Requires JDK 17 and Android SDK Platform 37 with matching build tools.
Releases are built by CI from a signed tag; see [docs/RELEASING.md](docs/RELEASING.md).

## Privacy

The app stores only local preferences and device-local alarm state. It does not
request internet, location, or storage permissions, and the release build is
checked for those at build time. See [the privacy statement](docs/PRIVACY.md).

## License

MIT. See [LICENSE](LICENSE).

Bundled city data is derived from the GeoNames geographical database and is
distributed under CC BY 4.0 with the required attribution. Interface and
launcher icons are Material Symbols, Apache 2.0. See
[third-party notices](docs/THIRD_PARTY_NOTICES.md).
