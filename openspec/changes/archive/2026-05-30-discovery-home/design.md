## Context

The Discovery app is in the **single-module transitional state** described in `docs/android/android-frontend-standards.md`: `settings.gradle.kts` declares only `:app`, the version catalog carries just Compose + AndroidX basics (Kotlin 2.2.10, AGP 9.2.1, Compose BOM 2026.02.01, no Hilt/Ktor/Navigation/DataStore), and `:app` holds the default scaffold screen.

This change delivers the first real feature (the Discovery Home screen) and, because of the standards, must also stand up the modular architecture, DI, navigation, and the shared design system that every later screen depends on. The standards mandate Clean Architecture + MVI, Hilt (KSP2), `StateFlow`/`SharedFlow`, design tokens, and "every new feature or core layer is a proper Gradle module — never a package inside `:app`."

Constraints:
- Figma is the source of truth for the Home layout (`Qhatia-DS`, node `286-9269`); do not invent UI.
- Real STT (Deepgram) and the catalog/results/settings destination screens are out of scope.
- Device pairing/auth (which would populate the store session from the backend) is a separate future change.

## Goals / Non-Goals

**Goals:**
- Ship the Home screen as a `features:home` module following the MVI contract, with a stateless `HomeContent` + stateful `HomeScreen` and mandatory light/dark previews.
- Establish the shared design system as `core:ui` (Material 3 theme, color/typography/spacing/shape tokens, shared composables: search bar, mic button, icon action button).
- Provide a persisted, observable `store-session` capability across `core:domain` (model + port + use cases) and `core:data` (DataStore-backed implementation).
- Convert `:app` into the Hilt application shell hosting the `NavHost`.
- Encode the mic interaction state machine (idle → listening with 30s cap → no-audio retry) and the control-visibility rule.
- Expand `gradle/libs.versions.toml` with the dependencies the standards require and register every new module in `settings.gradle.kts`.

**Non-Goals:**
- Deepgram / speech-to-text transcription (mic drives UI state only).
- Building the catalog, search-results, or settings destination screens (Home emits intents to placeholder routes).
- Backend population of the store session (pairing, `switch-store`, `GET /stores/{id}`); the session is locally persisted and seeded locally for now.
- Network layer (`core:network`) — not needed by this change; deferred until a screen actually calls the API.

## Decisions

### Module decomposition

Per the Dependency Graph Rules, this change creates the following Gradle modules (each at the repo root, registered in `settings.gradle.kts`):

| Module | Type | Responsibility |
|---|---|---|
| `core:common` | Android library | `DispatcherProvider`, shared constants (e.g., `MIC_TIMEOUT` lives in domain instead — see below), small utilities. |
| `core:ui` | Android library + Compose | Material 3 theme migrated out of `:app`, design tokens (`Spacing`, shapes, color schemes, typography), shared composables (`DiscoverySearchBar`, `MicButton`, `IconActionButton`). The "imported Figma design system." |
| `core:domain` | Pure Kotlin (`kotlin("jvm")`) | `StoreSession` model, `StoreSessionRepository` port, `ObserveStoreSessionUseCase`, `UpdateStoreNameUseCase`. Zero Android deps. |
| `core:data` | Android library + Hilt | `StoreSessionRepositoryImpl`, `StoreSessionLocalDataSource` (DataStore Preferences), DTO/mapper, `StoreSessionDataModule`. |
| `core:testing` | Android library | `MainDispatcherRule`, `FakeStoreSessionRepository`, fixtures. |
| `features:home` | Android library + Compose + Hilt | `HomeScreen`/`HomeContent`, `HomeViewModel`, `HomeUiState`/`HomeUiEvent`/`HomeUiEffect`, `HomeNavGraph`, `HomeModule`. |

`:app` keeps only the application shell: `@HiltAndroidApp DiscoveryApp`, `@AndroidEntryPoint MainActivity`, the root `NavHost`, and the manifest. The existing `app/.../ui/theme/*` is moved into `core:ui` and the package-level `Greeting` scaffold is removed.

Dependency directions used: `features:home → core:domain, core:ui, core:common`; `core:data → core:domain`; `app → features:home + core:ui`; `core:* → core:testing` (testImplementation). No feature-to-feature deps. `core:domain` stays framework-free.

**Alternative considered:** keep everything in `:app` as packages for speed. Rejected — it directly violates the standards (rule #4 / "never a package inside `:app`") and would have to be unwound at the first second feature.

### Store session: local DataStore now, backend later

The session is a `Preferences` DataStore (`store_session.preferences_pb`) wrapped by `StoreSessionLocalDataSource`, surfaced as `Flow<StoreSession>` through `StoreSessionRepository`. The Home VM consumes `ObserveStoreSessionUseCase`.

**Rationale:** the task asks to "create a merchant session to store values (name, etc.)" but device pairing/auth — the real source of the store name — is not in this change. DataStore gives us persistence + an observable stream with no backend dependency, and the same `StoreSessionRepository` port will later be fed by the pairing/`switch-store` flow without touching the Home screen.

**Alternatives considered:** (a) in-memory `@Singleton` holder — rejected, loses values on restart, the spec requires persistence; (b) `EncryptedSharedPreferences` — rejected, the standards flag `security-crypto` as unmaintained and prefer DataStore; the store name is not a secret.

### Mic interaction as a ViewModel state machine

`HomeUiState` carries a `MicState` (`Idle`, `Listening(remainingSeconds)`, `NoAudioRetry(prompt)`). The 30s cap is a `viewModelScope` countdown (cancellable on user cancel / future audio-detected event). On timeout with no audio, the VM transitions to `NoAudioRetry`, selecting the next message from an ordered list of retry prompts (rotating index, persisted across consecutive retries) so prompts vary. Control visibility (`areControlsVisible`) is derived: `micState == Idle`.

**Rationale:** keeping the timeout/retry logic in the VM (not the composable) honors the "no business logic in composables" and "side-effects via Compose APIs only" rules, and makes the 30s timeout and prompt rotation unit-testable with `TestDispatcher` + Turbine. The retry prompt strings live in `core:ui`/`features:home` string resources (externalized strings rule); a stubbed "no audio" is simply the timeout elapsing, since there is no real audio source yet.

**Alternative considered:** drive the timer from the composable with `LaunchedEffect`. Rejected — pushes business logic and timing into the UI and is far harder to test.

### Navigation: app-level NavHost, Home emits effects

`HomeViewModel` emits `HomeUiEffect.NavigateToCatalog/NavigateToResults(query)/NavigateToSettings` over a `SharedFlow`; `HomeScreen` collects them in a `LaunchedEffect` and calls navigation callbacks hoisted from the app `NavHost`. Routes for catalog/results/settings are declared as placeholders (out-of-scope destinations) so the intents resolve without building those screens. Only primitives (the query string) cross routes, per the navigation rules.

### Version catalog & build reconciliation

Extend `gradle/libs.versions.toml` toward the canonical set in `docs/android/android-frontend-standards.md`: add `ksp`, `hilt`, `hilt-androidx`, `navigation`, `lifecycle` (viewmodel + runtime-compose), `datastore`, `coroutines`, `immutable-collections`, and test libs (`junit5`, `mockk`, `turbine`, `kover` optional), plus `kotlin-metadata-jvm`. Add the `ksp` and `hilt` plugins.

Because the project is on **Kotlin 2.2.10 / AGP 9.2.1** rather than the catalog's reference 2.3.20, the apply step MUST pin a KSP2 version compatible with the installed Kotlin and apply the Hilt-on-AGP-9 guidance (`gradle.properties` flags + `ksp(libs.kotlin.metadata.jvm)` on every Hilt module). Exact version pinning is resolved during apply against the actual toolchain; the design fixes the dependency set and the KSP2/metadata approach, not frozen numbers.

## Risks / Trade-offs

- **Kotlin/KSP2/Hilt version drift** → The installed Kotlin (2.2.10) differs from the standards' reference (2.3.20); a mismatched KSP2/Hilt pairing fails the build. *Mitigation:* during apply, pick the KSP2 build matching the resolved Kotlin, follow the metadata-override note, and validate with a clean `./gradlew :features:home:assembleDebug` before wiring `:app`.
- **Large bootstrap in one change** → Six new modules + DI + design system is a lot of surface for one change. *Mitigation:* modules are thin; tasks are ordered so each module compiles before the next depends on it, and DoD (lint + unit tests + previews) gates completion.
- **Figma fidelity without a confirmed token export** → The design system must match `Qhatia-DS`. *Mitigation:* derive tokens directly from the referenced Figma node during apply (colors, type, spacing, shapes) instead of inventing values; treat the Figma file as source of truth per the UI/UX guidelines.
- **Stubbed mic could be mistaken for real STT** → The mic produces no transcription. *Mitigation:* spec and code name the state machine explicitly as UI-only; the retry path is reachable purely via timeout, and a clear TODO marks the Deepgram integration seam.
- **Store session has no real data source yet** → Home may show the placeholder until pairing lands. *Mitigation:* `StoreSession` empty state is specified; the `StoreSessionRepository` port is ready for the future pairing flow to populate without UI changes.

## Migration Plan

1. Land the version catalog + module bootstrap (modules compile independently).
2. Move theme into `core:ui`; `:app` becomes the Hilt shell with the `NavHost`.
3. Add `store-session` (`core:domain` + `core:data`), then `features:home`, then wire `:app`.
4. Rollback is straightforward: the change is additive and behind no flag; reverting the module set and the `:app` shell edits restores the scaffold. No data migration (DataStore file is new and empty).

## Open Questions

- Exact pinned KSP2 version for Kotlin 2.2.10 — resolved at apply against the toolchain.
- Whether `core:network` + a real `GET /stores/{id}` fetch should be folded in now or deferred — current decision: **defer** until pairing/auth exists, since this task lists backend population out of scope.
- Final retry-prompt copy and count — to be taken from product/Figma during apply (placeholder set used until then).
