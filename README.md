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

The first release is under active development. The delivery plan and fixed product decisions are in [`docs/plans`](docs/plans/README.md).

## Privacy

The app stores only local preferences and device-local alarm state. It does not request internet, location, or storage permissions. See [the privacy statement](docs/PRIVACY.md).

## License

MIT. See [LICENSE](LICENSE).

City data in a later release will be derived from GeoNames and distributed with the required attribution; see [third-party notices](docs/THIRD_PARTY_NOTICES.md).
