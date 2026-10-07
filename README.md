# Qhathia Discovery — Android

Android application written in **Kotlin** with **Jetpack Compose**, organized as a multi-module
**Clean Architecture + MVI** codebase. Gradle root project: `QhathiaDiscovery` · application id:
`com.qhathia.mobile.discovery`.

This README is the technical entry point for developers. Deep-dive standards live under [`docs/`](#documentation).

---

## Technology stack

| Concern | Choice | Version |
|---|---|---|
| Language | Kotlin | 2.3.20 |
| UI toolkit | Jetpack Compose (Material 3) | BOM 2026.05.00 |
| Architecture | Clean Architecture + MVI (UDF) | — |
| DI | Hilt (Dagger) via **KSP2** | 2.57.1 |
| HTTP client | Ktor Client (CIO engine) | 3.4.3 |
| Serialization | kotlinx-serialization (JSON) | 1.10.0 |
| Async | Kotlin Coroutines / Flow | 1.11.0 |
| Image loading | Coil 3 | 3.1.0 |
| Navigation | Navigation Compose | 2.9.0 |
| Build system | Gradle (KTS) + Version Catalog | wrapper 9.4.1 |
| Android Gradle Plugin | AGP | 9.2.1 |
| Unit testing | JUnit 5 + MockK + Turbine | — |

- **Min SDK:** 26 · **Target / Compile SDK:** 36
- Annotation processing is **KSP2 only** — never `kapt`.
- All shared build versions are pinned in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

---

## Requirements

- **JDK 21+** — required by Gradle 9 / AGP 9. JDK 17 will not start the Gradle daemon.
  Use Eclipse Temurin 21 or the JBR bundled with Android Studio
  (`<AndroidStudio>/jbr`).
- **Android SDK** with platform **API 36** and recent build-tools, plus `ANDROID_HOME` set.
- **Android Studio** (latest stable) recommended for development and profiling.

---

## Getting started

```bash
git clone <repo-url>
cd qhathia-mobile-discovery
```

1. **`local.properties`** — point Gradle at your SDK. The `sdk.dir` value must use **forward slashes**, even on Windows:

   ```properties
   sdk.dir=C:/Users/<you>/AppData/Local/Android/Sdk
   ```

2. **Environment** — set `ANDROID_HOME` and a JDK 21 `JAVA_HOME`. Example (PowerShell):

   ```powershell
   $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
   $env:JAVA_HOME    = "C:\Program Files\Android\Android Studio\jbr"   # or a Temurin 21 path
   ```

3. **Sanity check:** `./gradlew --version` (PowerShell: `.\gradlew`, cmd: `gradlew.bat`).

> Always invoke Gradle through the **wrapper** (`./gradlew`), never a globally installed `gradle`.
> Full environment guide: [`docs/android/android-development-guide.md`](docs/android/android-development-guide.md).

---

## Build & run

```bash
./gradlew assembleDebug            # debug APK
./gradlew :app:installDebug        # build + install on a connected device/emulator
./gradlew test                     # all JVM unit tests
./gradlew lint                     # Android Lint
./gradlew clean                    # clean build outputs
```

Run a single module's tests, e.g.:

```bash
./gradlew :features:catalog:testDebugUnitTest :core:data:testDebugUnitTest
```

### Build types

| Build type | Debuggable | Signing | Network API logs | R8 | Purpose |
|---|---|---|---|---|---|
| `debug`   | yes | debug keystore | on (`BuildConfig.DEBUG`) | off | day-to-day development |
| `release` | no  | unsigned* | off | off (ready to enable) | production builds |
| `profile` | no  | debug keystore | **on** | off | QA distribution + Android Studio profiling |

\* `release` has no `signingConfig` yet (not wired for store distribution).

**`profile`** is a dedicated, non-debuggable but **`profileable`** variant: it gives clean profiler
metrics (no `debuggable` overhead) while staying installable (debug-signed) and **keeping the Ktor
API request/response logs on**. Network logging is decoupled from `BuildConfig.DEBUG` via the
`ENABLE_NETWORK_LOGGING` build flag in `:core:network` (true only in its `profile` variant), consumed
in `NetworkModule` as `BuildConfig.DEBUG || BuildConfig.ENABLE_NETWORK_LOGGING`.

```bash
./gradlew :app:assembleProfile
# → app/build/outputs/apk/profile/qhathia-discovery-v<version>-SNAPSHOT.apk
adb install -r app/build/outputs/apk/profile/qhathia-discovery-v1.0.0-SNAPSHOT.apk
adb logcat -s KtorClient            # observe API calls
```

Then *Run › Profile 'app'* in Android Studio.

---

## Versioning

Semantic versioning (`MAJOR.MINOR.PATCH`) with a `SNAPSHOT` prerelease. The **single source of
truth** is the version catalog:

```toml
# gradle/libs.versions.toml
appVersion    = "1.0.0"
appPrerelease = "SNAPSHOT"
```

`app/build.gradle.kts` derives everything from it:

- `versionName` = `"<appVersion>-<appPrerelease>"` → `1.0.0-SNAPSHOT`
- `versionCode` = `major*10000 + minor*100 + patch` → `10000` (monotonic; minor/patch < 100)
- APK output name (all variants) = `qhathia-discovery-v<versionName>.apk`
  (each build type writes to its own `outputs/apk/<buildType>/` folder, so names don't collide)

**To release a new version, bump `appVersion` only** — `versionName`, `versionCode`, and the APK
filename all recompute. Dynamic per-variant prerelease tagging is handled later in CI/CD.

---

## Project structure

Every layer and feature is a **separate Gradle module** (not a package inside `:app`).

```
root/
├── app/                     # Application shell: NavHost, theme entry, Hilt wiring, manifest
├── core/
│   ├── domain/              # Pure Kotlin (JVM): UseCases, domain models, repository interfaces
│   ├── data/                # Repository impls, data sources, DTO↔domain mappers
│   ├── network/             # Ktor client, API services, @Serializable DTOs, NetworkResult
│   ├── ui/                  # Shared Compose components, theme, design tokens
│   ├── common/              # Kotlin extensions, utilities, constants
│   └── testing/             # Shared fakes, fixtures, MainDispatcherExtension
└── features/
    ├── home/                # Each feature module: Screen, ViewModel, UiState/Event/Effect, NavGraph
    ├── catalog/
    ├── product-search/
    ├── product-detail/
    ├── cart/
    └── settings/
```

**Dependency rules (enforced by convention):**

```
features/* → core:domain ← core:data → core:network
features/* → core:ui, core:common
app        → features/* + core:*
core:*     → core:testing  (testImplementation only)
```

- Feature modules must **not** depend on other feature modules (cross-feature UI is hosted in `:app`,
  e.g. dialog overlays).
- `core:domain` is pure Kotlin with **zero Android dependencies**.
- `core:network` must **not** depend on `core:domain` (DTO→domain mapping happens in `core:data`).

New modules must be registered in [`settings.gradle.kts`](settings.gradle.kts). See the
"Creating a New Module" checklist in the frontend standards.

---

## Architecture

Unidirectional data flow with an MVI contract per feature:

```
UI (Composable) ──UiEvent──▶ ViewModel ──▶ UseCase ──▶ Repository ──▶ Ktor / local
        ▲                                                                  │
        └──────────────── UiState (StateFlow) / UiEffect (SharedFlow) ◀────┘
```

- **State** is exposed as `StateFlow<…UiState>`; **one-shot effects** (navigation, dialogs) as
  `SharedFlow<…UiEffect>`. **No `LiveData`.**
- ViewModels are `@HiltViewModel` with constructor injection; coroutines run in `viewModelScope`.
- Stateless `*Content` composables (pure functions of `UiState`) are split from stateful `*Screen`
  composables that bind the ViewModel.
- Compose collects state with `collectAsStateWithLifecycle()`; never read `StateFlow.value` in
  composition.

Full reference: [`docs/android/android-frontend-standards.md`](docs/android/android-frontend-standards.md)
and [`docs/android/android-ui-ux-compose-guidelines.md`](docs/android/android-ui-ux-compose-guidelines.md).

---

## Testing

- **Unit tests:** JUnit 5 + MockK + Turbine, with `core:testing` providing fakes (`Fake*Repository`),
  fixtures (`*Fixtures`), and `MainDispatcherExtension` (swaps `Dispatchers.Main` for a
  `TestDispatcher`).
- Prefer **fakes** from `core:testing` for repositories; reserve MockK for API services / providers.
- Use **Turbine** to assert `Flow`/`StateFlow` emissions in sequence.
- Compose UI tests set content inside the app theme.

```bash
./gradlew test                 # all unit tests
./gradlew connectedAndroidTest # instrumented tests (device/emulator required)
```

---

## Contributing

- **Branches:** `feat/<name>`, `fix/<issue-id>-<desc>`, `chore/<task>`.
- **Commits:** [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`,
  `refactor:`, `test:`, `chore:`). English only — code, comments, logs, and docs are in English.
- **Pull requests:** at least one review. Before requesting review, run `./gradlew lint test` (and
  `assembleDebug`) locally so the branch is green — these checks should also gate merges via CI once a
  pipeline is in place.
- **Definition of Done:** unit tests added, `./gradlew lint` passes, and every Screen composable has a
  working `@Preview` for both light and dark mode.

### Coding conventions

- Modules over packages; **KSP2** over kapt; do **not** apply `kotlin-android` (AGP 9 applies it).
- Every Hilt module needs `ksp(libs.kotlin.metadata.jvm)` alongside `ksp(libs.hilt.compiler)`.
- Material 3 design tokens only — no hardcoded colors, type sizes, or dimensions; strings via
  `stringResource`. Interactive controls expose content descriptions and ≥48dp touch targets.
- Prefer `val` and immutability; avoid `!!`; use `viewModelScope` (never `GlobalScope`).

### Spec-driven changes (OpenSpec)

Non-trivial changes are proposed as OpenSpec artifacts before implementation:

```
openspec/
├── specs/              # Living capability specifications (the source of truth)
└── changes/            # In-flight changes (proposal.md, design.md, specs/, tasks.md)
    └── archive/        # Completed, archived changes (dated)
```

Typical flow: **propose** (proposal + design + specs + tasks) → **apply** (implement tasks) →
**archive** (sync specs into `openspec/specs/`). Validate with `openspec validate <change>`.

---

## Documentation

| Topic | File |
|---|---|
| Architecture, module taxonomy, build templates | [`docs/android/android-frontend-standards.md`](docs/android/android-frontend-standards.md) |
| Compose UI/UX, theming, accessibility | [`docs/android/android-ui-ux-compose-guidelines.md`](docs/android/android-ui-ux-compose-guidelines.md) |
| Environment setup, Gradle tasks, build commands | [`docs/android/android-development-guide.md`](docs/android/android-development-guide.md) |
| API contracts, endpoints, DTOs | [`docs/android/api-contract.md`](docs/android/api-contract.md) |
| Repo-wide AI/agent context | [`CLAUDE.md`](CLAUDE.md) · [`AGENTS.md`](AGENTS.md) |
