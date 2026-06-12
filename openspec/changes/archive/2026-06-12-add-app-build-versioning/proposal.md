## Why

Builds today carry an ad-hoc `versionName = "1.0"` and produce generically-named APKs (`app-<buildType>.apk`), so a compiled artifact can't be traced back to a meaningful version in the build history. The team needs a **semantic version with a prerelease identifier** and a **versioned artifact name** so every compiled app is identifiable by its intended use and place in history (ClickUp 86e1vq6h5). This change also formalizes the **`profile` (QA/profiling) build variant** already added while preparing a shareable QA APK, so the change record matches the code.

## What Changes

- Adopt **semantic versioning** (`MAJOR.MINOR.PATCH`) as the single source of truth for the app version, declared in the Gradle version catalog (`gradle/libs.versions.toml`).
- Append the **`SNAPSHOT` prerelease** identifier to `versionName` → `1.0.0-SNAPSHOT` (initially `SNAPSHOT` for all builds, per the task).
- Derive a deterministic integer **`versionCode`** from the semantic version (e.g. `MAJOR*10000 + MINOR*100 + PATCH`).
- Name the build output **exactly** `qhathia-discovery-v<version>-SNAPSHOT.apk` (no build-type suffix; e.g. `qhathia-discovery-v1.0.0-SNAPSHOT.apk`).
- **Formalize the already-added `profile` build type** (QA / Android Studio profiling): non-debuggable + `profileable`, signed with the debug keystore so QA can install it, R8 left off, with network API logging kept on via `ENABLE_NETWORK_LOGGING` (decoupled from `BuildConfig.DEBUG` in `:core:network`). *(Implemented.)*
- Stop committing built artifacts: `dist/` is git-ignored. *(Implemented.)*

## Capabilities

### New Capabilities
- `app-build-config`: How the Android app is versioned and packaged — semantic version + `SNAPSHOT` prerelease, deterministic `versionCode`, versioned artifact naming, and the build variants (debug / release / `profile`) and their intended use, including the QA/profiling variant's installability and API-logging behavior.

### Modified Capabilities
<!-- None — no existing product capability's requirements change. -->

## Impact

- **`gradle/libs.versions.toml`** — new `[versions]` entries for the app version and prerelease tag (single source of truth).
- **`app/build.gradle.kts`** — `versionName`/`versionCode` derived from the catalog; artifact base name set to `qhathia-discovery-v<version>-SNAPSHOT`; the `profile` build type (already present) documented as part of this capability.
- **`core/network/build.gradle.kts`** + **`NetworkModule.kt`** — `ENABLE_NETWORK_LOGGING` build flag (already present), enabling Ktor API logs in non-debuggable QA builds.
- **`.gitignore`** — `dist/` ignored (already present).
- **No runtime/product behavior change** and no new Gradle module — this is build/release configuration only. The real `release` build type is unchanged (no logs, unsigned, R8-ready).
