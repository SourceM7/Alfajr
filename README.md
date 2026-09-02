# Alfajr Alarm

Alfajr Alarm is a lightweight, offline Android alarm application focused only on Fajr. It has no account, advertising, tracking, analytics, or internet permission.

## Requirements

- Android 8.0 (API 26) or newer
- JDK 17
- Android SDK Platform 37 and corresponding build tools

## Build

```sh
./gradlew assembleDebug
./gradlew test lint
```

The first release is under active development.

## Privacy

The app stores only local preferences and device-local alarm state. It does not request internet, location, or storage permissions. See [the privacy statement](docs/PRIVACY.md).

## License

MIT. See [LICENSE](LICENSE).

Bundled city data is derived from the GeoNames geographical database and is distributed under CC BY 4.0 with the required attribution; see [third-party notices](docs/THIRD_PARTY_NOTICES.md).
