## ADDED Requirements

### Requirement: Semantic versioning with SNAPSHOT prerelease

The app version SHALL follow **semantic versioning** (`MAJOR.MINOR.PATCH`) and SHALL be declared in a single source of truth (the Gradle version catalog, `gradle/libs.versions.toml`). The build SHALL append the prerelease identifier **`SNAPSHOT`** to the `versionName`, producing `<MAJOR>.<MINOR>.<PATCH>-SNAPSHOT`. Initially every build SHALL use the `SNAPSHOT` prerelease identifier.

#### Scenario: versionName carries the semantic version and prerelease

- **WHEN** the app is built
- **THEN** the `versionName` is `<MAJOR>.<MINOR>.<PATCH>-SNAPSHOT` (e.g. `1.0.0-SNAPSHOT`)

#### Scenario: Version is defined in one place

- **WHEN** the app version needs to change
- **THEN** it is updated in the version catalog only, and the build derives `versionName` and `versionCode` from it

### Requirement: Deterministic versionCode

The build SHALL derive an integer **`versionCode`** deterministically from the semantic version so that a higher semantic version always yields a higher `versionCode`.

#### Scenario: versionCode increases with the semantic version

- **WHEN** the semantic version is increased (major, minor, or patch)
- **THEN** the derived `versionCode` is strictly greater than the previous one

### Requirement: Versioned build artifact naming

A compiled APK SHALL be named exactly `qhathia-discovery-v<MAJOR>.<MINOR>.<PATCH>-SNAPSHOT.apk`, with no build-type suffix, so an artifact is traceable to its version. Each build type writes to its own output directory, so the identical file name does not collide.

#### Scenario: Output APK name is the versioned name

- **WHEN** an APK is assembled
- **THEN** its file name is exactly `qhathia-discovery-v<version>-SNAPSHOT.apk` (e.g. `qhathia-discovery-v1.0.0-SNAPSHOT.apk`)

### Requirement: Build variants and their intended use

The project SHALL provide distinct build variants for distinct uses: **`debug`** for development (debuggable, logs on), **`release`** for production (non-debuggable, no API logs, R8-ready, signed for distribution), and **`profile`** for QA and Android Studio profiling.

#### Scenario: Release stays production-clean

- **WHEN** the `release` variant is built
- **THEN** it is non-debuggable and does not emit network API logs

### Requirement: QA / profiling build variant

The **`profile`** build variant SHALL be **non-debuggable** and **`profileable`** (so the Android Studio profiler attaches with accurate metrics), SHALL be **signed with the debug keystore** so QA can install it without production signing, and SHALL keep **network API request/response logging enabled** even though it is non-debuggable (via a logging flag decoupled from `BuildConfig.DEBUG`). R8/minification SHALL be left off to match `release` behavior without keep-rule risk.

#### Scenario: Profiling build is installable and profileable

- **WHEN** the `profile` variant is assembled
- **THEN** the APK is signed (debug keystore) and installable on a QA device
- **AND** its manifest marks the app `profileable` and not `debuggable`

#### Scenario: API logs survive in the non-debuggable profiling build

- **WHEN** the app runs from the `profile` build and performs an API call
- **THEN** the request/response is logged (Ktor logging), despite the build being non-debuggable

### Requirement: Built artifacts are not committed

Build output artifacts (APKs and the local `dist/` staging directory used to share builds) SHALL NOT be committed to version control.

#### Scenario: dist directory is ignored

- **WHEN** a built APK is staged under `dist/`
- **THEN** Git does not track it
