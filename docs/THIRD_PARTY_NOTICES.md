# Third-party notices

Alfajr Alarm bundles and depends on the following third-party work. The
generated dependency-license report (`./gradlew generateLicenseReport`) covers
the full transitive set; this file records the attributions that the licenses
require to be distributed with the application.

## Bundled data

### GeoNames

The offline city catalogue in `app/src/main/assets/cities.v1.json` is derived
from the GeoNames geographical database.

> This work incorporates data from the GeoNames geographical database, © GeoNames,
> licensed under the Creative Commons Attribution 4.0 License
> (<https://creativecommons.org/licenses/by/4.0/>).

- Source: <https://download.geonames.org/export/dump/> (`cities15000`, plus
  Arabic alternate names from `alternateNamesV2`)
- Extracted: 2026-08-31 (recorded as `sourceDate` inside the asset)
- Records retained: 34,127
- Asset SHA-256: `bf9d16a4b81d353e915aeb20e55c9e7e2fa6c2eb8dd54585f65bb45970682252`
- Fields retained: GeoNames ID, canonical name, ASCII name, Arabic alternate
  name where available, country code, first administration name, latitude,
  longitude, population, and IANA time-zone ID.
- Modifications: fields outside the list above were dropped; searchable name
  variants are normalized at runtime rather than stored.

Attribution is a condition of the license, so it must remain in this file, in
the asset's own `attribution` metadata, and in the application's licenses screen.

### Bundled fallback tone

`app/src/main/res/raw/fallback_alarm.wav` is not third-party work. It is a
generated tone, original to this project and covered by the project's own MIT
license, and it exists so an alarm can still sound when the saved ringtone is
unreadable and the device has no default alarm sound.

- Content: two 880 Hz sine beeps of 180 ms separated by 120 ms, with 12 ms
  fades, in 1.5 s of 16-bit mono PCM at 16 kHz, played on a loop
- SHA-256: `f9937115adda9848acc08da57a3d6fb8de5375c6ecf1115ec24b7310f2cab059`

No recorded adhan is bundled, and none will be; that is out of scope by product
decision.

## Dependencies

| Component | License |
| --- | --- |
| [Adhan (adhan2)](https://github.com/batoulapps/adhan-kotlin) | MIT |
| Kotlin standard library, `kotlinx.datetime`, `kotlinx.serialization` | Apache 2.0 |
| AndroidX (Activity, Core, DataStore, Lifecycle, Navigation) | Apache 2.0 |
| Jetpack Compose, Material 3, and the Google Material Symbol `wb_twilight` used for the launcher icon | Apache 2.0 |
| JUnit 4 | Eclipse Public License 1.0 |
| AndroidX Test and Espresso | Apache 2.0 |

Alfajr Alarm itself is MIT licensed; see [LICENSE](../LICENSE).
