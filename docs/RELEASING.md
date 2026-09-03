# Releasing Alfajr Alarm

Releases are created only from a semantic version tag whose value matches
`app/build.gradle.kts`: for example, `versionName = "0.1.0"` is released from
the signed, annotated tag `v0.1.0`.

The `Release APK` workflow requires these repository secrets:

- `ALFAJR_RELEASE_KEYSTORE_BASE64`: base64-encoded long-lived release keystore
- `ALFAJR_RELEASE_STORE_PASSWORD`
- `ALFAJR_RELEASE_KEY_ALIAS`
- `ALFAJR_RELEASE_KEY_PASSWORD`
- `ALFAJR_RELEASE_TAG_SIGNING_PUBLIC_KEY`: the armored public key authorized to
  sign release tags

Create and verify the tag before pushing it:

```sh
git tag -s v0.1.0 -m "Alfajr Alarm 0.1.0"
git verify-tag v0.1.0
git push origin v0.1.0
```

The workflow restores the key only on the GitHub runner, builds a signed
universal APK, checks the merged release manifest for prohibited internet,
location, and storage permissions, generates the dependency-license report,
and publishes the APK, SHA-256 checksum, compatibility, privacy/permission
summary, and known platform limitations. Keystores and local signing properties
are ignored by Git.

## Reproducible-build inputs

The release workflow uses Temurin JDK 17. The repository pins Gradle 9.5.0 in
the wrapper, Android Gradle Plugin 9.3.2 in the version catalog, Kotlin 2.3.21,
and compile/target SDK 37 with a minimum SDK of 26. The runner must install
Android SDK Platform 37 and a build-tools revision compatible with AGP 9.3.2;
record the exact installed build-tools revision alongside every public release.
The offline city artifact is the committed `cities.v1.json`; its GeoNames source
date and SHA-256 are recorded in `docs/THIRD_PARTY_NOTICES.md`.

## Distribution follow-up

Before submitting to F-Droid, create and validate its metadata file with the
app ID, source URL, license, categories, current version/code, build recipe,
and allowed APK signing key. Track Android's non-Play developer-verification
requirements in the [official verification FAQ](https://developer.android.com/developer-verification/guides/faq)
and complete its current registration/identity steps before the applicable
enforcement deadline.

Before tagging, run:

```sh
./gradlew --no-daemon assembleDebug assembleRelease test lint generateLicenseReport verifyReleaseManifestPrivacy
```

The automated checks do not replace the physical-device matrix in
`docs/plans/03-delivery-plan.md`. In particular, alarm delivery through Doze,
lock-screen behavior, audio routes, reboot recovery, DND, and OEM power
management must be recorded before a public release.
