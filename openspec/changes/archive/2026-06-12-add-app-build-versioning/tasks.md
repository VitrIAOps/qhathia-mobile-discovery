## 1. Version source of truth (`gradle/libs.versions.toml`)

- [x] 1.1 Added `[versions]` entries `appVersion = "1.0.0"` and `appPrerelease = "SNAPSHOT"` as the single source of truth.

## 2. Version wiring (`app/build.gradle.kts`)

- [x] 2.1 `versionName = appVersionName` = `"${appVersion}-${appPrerelease}"` → resolves to `1.0.0-SNAPSHOT` (verified via aapt2 badging).
- [x] 2.2 `versionCode = appVersionCode` derived as `major*10000 + minor*100 + patch` → `10000` for `1.0.0` (verified via aapt2 badging).

## 3. Versioned artifact naming (`app/build.gradle.kts`)

- [x] 3.1 Set the per-variant APK output file name to exactly `qhathia-discovery-v<versionName>.apk` via `androidComponents { onVariants { … VariantOutputImpl.outputFileName.set(…) } }`.
- [x] 3.2 Built profile + debug (+ release); each `outputs/apk/<buildType>/` folder contains exactly `qhathia-discovery-v1.0.0-SNAPSHOT.apk`.

## 4. QA / profiling build variant — already implemented

- [x] 4.1 `profile` build type in `app/build.gradle.kts`: `initWith(release)` + `isProfileable = true` + `signingConfig = signingConfigs.getByName("debug")` + `matchingFallbacks += "release"` (non-debuggable, profileable, debug-signed, R8 off).
- [x] 4.2 Verified the assembled `profile` APK is debug-signed, manifest is `profileable` and not `debuggable` (apksigner + aapt2 badging/xmltree).

## 5. Network API logging in non-debuggable builds — already implemented

- [x] 5.1 `core/network/build.gradle.kts`: `ENABLE_NETWORK_LOGGING` build flag (`false` by default, `true` in the network module's `profile` build type).
- [x] 5.2 `NetworkModule.provideHttpClient` uses `BuildConfig.DEBUG || BuildConfig.ENABLE_NETWORK_LOGGING`, so Ktor API logs stay on in the non-debuggable QA build (confirmed `ENABLE_NETWORK_LOGGING = true` in the profile BuildConfig).

## 6. Artifact hygiene — already implemented

- [x] 6.1 `.gitignore` ignores `dist/` (in addition to the existing `*.apk`), so staged/shared APKs are never committed.

## 7. Definition of Done

- [x] 7.1 `:app:assembleProfile` succeeds; APK named exactly `qhathia-discovery-v1.0.0-SNAPSHOT.apk`; `versionName=1.0.0-SNAPSHOT`, `versionCode=10000`.
- [x] 7.2 `release` stays production-clean: manifest has no `debuggable`/`profileable`, and `ENABLE_NETWORK_LOGGING=false` in the release variant (logs off).
