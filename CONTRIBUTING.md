# Contributing

Please keep Alfajr Alarm focused, offline, and Fajr-only. Before proposing a feature, check the fixed decisions and out-of-scope list in [`docs/plans`](docs/plans/README.md).

## Development checks

Run the following before opening a change:

```sh
./gradlew assembleDebug test lint generateLicenseReport
```

Do not add telemetry, network access, location access, or alpha dependencies. Keep user-facing strings in resources and update Arabic translations in the same change.
