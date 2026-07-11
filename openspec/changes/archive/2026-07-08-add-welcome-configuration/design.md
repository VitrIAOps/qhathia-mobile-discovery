## Context

The app is multi-module (Clean Architecture + MVI, Hilt, Ktor, Compose). Today `MainActivity`'s `NavHost` hard-codes `startDestination = HOME_ROUTE`, so every launch lands on Home regardless of pairing state. The pairing ("Vincular") experience lives entirely inside `:features:settings` (`PairingScreen`, `PairingContent`, `PairingViewModel`, `PairingUiState/Event/Effect`, plus `pairing_*` strings and `ic_clock`/`ic_link`/`ic_arrow_forward` drawables), composed by `SettingsScreen` next to the Wi-Fi section.

Authentication flows through `core:data`'s `SessionTokenProvider.accessToken()`, used by the STT and product/order repositories. It prefers the persisted DEVICE token from `StoreSessionRepository` but **falls back to a dev-credential login** via `core:network`'s `AuthApiService.login(...)`, with `QHATHIA_DEV_EMAIL` / `QHATHIA_DEV_PASSWORD` declared as `buildConfigField`s in `core/network/build.gradle.kts`. The device token itself is minted by pairing via `DevicePairingApiService.exchangeToken(sessionId)` → `/api/v1/devices/pairing-session/{sessionId}/token`.

This change adds a first-run onboarding flow (Welcome → initial configuration), reuses the pairing slice in that flow, routes startup by pairing state, and removes the dev-login token path so pairing is the only token source.

Constraints (from `docs/android/android-frontend-standards.md`):
- Every new feature is its **own Gradle module** at the repo root, registered in `settings.gradle.kts` — never a package in `:app`.
- **Feature modules must not depend on other feature modules.**
- MVI contract (`UiState`/`UiEvent`/`UiEffect`), `StateFlow`/`SharedFlow`, stateless `*Content` + stateful `*Screen`, mandatory light/dark `@Preview`, JUnit5 + MockK + Turbine, Hilt via KSP2 with the `kotlin-metadata-jvm` override.

## Goals / Non-Goals

**Goals:**
- First-run Welcome screen and an initial configuration screen that reuses the pairing experience with QHATHIA chrome, no side menu, and a back affordance.
- Startup routing: unpaired → onboarding, paired → Home, decided before content renders.
- Navigate to Home only on a successful pairing; clear the onboarding flow from the back stack.
- Make pairing the single source of the access token; delete the dev-login path and its build-config credentials.
- Keep all existing pairing validations behaving identically in both hosts.

**Non-Goals:**
- No changes to the pairing protocol (code generation, TTL, socket status, token exchange endpoints) — only its host/chrome and success wiring.
- No new backend endpoints; unpair remains the existing stub.
- No change to the settings PIN gate or the Wi-Fi section.
- No re-pairing/unpair-from-onboarding flow beyond returning to Welcome.

## Decisions

### Decision 1 — Extract the pairing slice into a shared `:features:pairing` module

Both `:features:settings` and the new onboarding flow need the pairing slice, but feature modules may not depend on each other. Rather than duplicate the ViewModel + composables (the task explicitly says *reuse* components) or dump shared code into `:core:ui` (which forbids ViewModels/business logic), extract the pairing slice into its own module `:features:pairing`.

Contents moved from `:features:settings` → `:features:pairing`:
- `PairingScreen`, `PairingContent` (and its private composables), `PairingViewModel`
- `model/PairingUiState.kt`, `PairingUiEvent.kt`, `PairingUiEffect.kt`, `PairingPhase`
- `pairing_*` string resources and `ic_clock`, `ic_link`, `ic_arrow_forward` drawables
- `PairingViewModelTest`

`:features:settings` and `:features:onboarding` both `implementation(project(":features:pairing"))`. The pairing slice depends only on `:core:domain`, `:core:ui`, `:core:common` — no cycle, no sibling-feature dependency. `:features:pairing` is a shared vertical-capability module, not a navigable sibling feature; this is the standards-compliant reuse path (the alternative — a feature→feature dependency — is explicitly forbidden; duplication violates the reuse requirement).

The header title string currently shared as `network_toolbar_title` ("TIENDA") stays in `:features:settings`; the pairing slice takes the header title as a composable parameter (see Decision 2) so it owns no host-specific title string.

### Decision 2 — Parameterize the pairing slice for host-agnostic reuse

`PairingScreen` / `PairingContent` gain parameters instead of hard-coded chrome:
- `title: String` — "TIENDA" from Settings, "QHATHIA" from onboarding (passed by each host via its own string resource).
- `showBackButton: Boolean` + `onBack: () -> Unit` — Settings passes `false`; onboarding passes `true` and pops to Welcome. The back control renders inside the pairing header (reuse `ic_arrow_back`, already in settings — moved to pairing).
- `onPaired: () -> Unit` — a paired-success callback. `PairingViewModel` already transitions to `PairingPhase.Paired` after a successful `exchangeDeviceToken`; add a `PairingUiEffect.Paired` emitted at that transition. `PairingScreen` collects it and invokes `onPaired`. Settings passes a no-op (it keeps showing the paired-device details in place, as today); onboarding passes "navigate to Home".

This keeps the ViewModel host-agnostic — it always drives pairing to completion and announces success; the *host* decides what success means.

### Decision 3 — `:features:onboarding` module: Welcome + initial configuration host

New feature module `:features:onboarding` with the standard internal layout:
- `ui/WelcomeScreen.kt` (stateful, trivial — no ViewModel needed; static content + `onContinue`) and `WelcomeContent.kt` (stateless, previewable).
- `ui/InitialConfigurationScreen.kt` — a thin host that composes `PairingScreen(title = QHATHIA, showBackButton = true, onBack = …, onPaired = …)` from `:features:pairing`.
- `navigation/OnboardingNavigation.kt` — exposes `WELCOME_ROUTE`, `INITIAL_CONFIG_ROUTE`, and `NavGraphBuilder.onboardingGraph(onNavigateToHome, ...)` with hoisted navigation callbacks (matching the existing `networkSettingsScreen` pattern).

Onboarding depends on `:features:pairing`, `:core:ui`, `:core:common` (and `:core:domain` only if needed for routing types). It does **not** depend on `:features:home` (feature→feature forbidden); the "go to Home" navigation is hoisted to `:app`.

### Decision 4 — Startup routing owned by `:app`

`:app` is the only module that composes the cross-feature `NavHost`, so it owns the start-destination decision. Extend `MainViewModel` to expose the initial pairing state synchronously enough to pick a start destination without flashing Home:
- Read the persisted device token before first composition. `StoreSessionRepository` is suspend/Flow-based, so `MainViewModel` exposes a small `StateFlow<StartDestination>` seeded as `Loading`, resolving to `Onboarding` or `Home` from `observeStoreSession().first().isPaired` (reuse `ObserveStoreSessionUseCase`).
- `DiscoveryAppRoot` shows a minimal splash/placeholder while `Loading`, then builds the `NavHost` with `startDestination = WELCOME_ROUTE` or `HOME_ROUTE`. Because the token read is a fast local DataStore lookup, the placeholder is momentary and Home never flashes for an unpaired device (spec: "SHALL NOT flash Home").
- On successful pairing, onboarding's `onPaired` calls `navController.navigate(HOME_ROUTE)` with `popUpTo(WELCOME_ROUTE) { inclusive = true }` so the onboarding destinations leave the back stack.

Alternative considered: gate inside `MainActivity.onCreate` before `setContent`. Rejected — it blocks the main thread on a suspend read and complicates edge-to-edge/theme setup; a `Loading` state in Compose is cleaner and matches UDF.

### Decision 5 — Remove the dev-login token path

- `SessionTokenProvider.accessToken()` returns the persisted device token when present, else `Result.failure(...)` (an `SttException`/generic auth failure) — no `AuthApiService` call, no in-memory dev-login cache. Keep the `clear()` seam for 401 handling.
- Delete `core:network` `AuthApiService`, `dto/AuthDtos.kt` (`LoginRequestDto`/`LoginResponseDto`), and their `NetworkModule` provider.
- Delete `QHATHIA_DEV_EMAIL` / `QHATHIA_DEV_PASSWORD` `buildConfigField`s from `core/network/build.gradle.kts`.
- Update `SessionTokenProviderTest` and the catalog/detail/order/search repository tests that assumed a login fallback: unpaired now yields a clean failure. The pairing-session token exchange path (`DevicePairingApiService.exchangeToken`) is untouched.

This is BREAKING for any flow that relied on the dev login on an unpaired device — which is now exactly the flow onboarding forces the user through first, so in practice authenticated calls only run once a device token exists.

## Risks / Trade-offs

- **Large extraction of the pairing slice out of `:features:settings`** → Move files wholesale (preserve package names where possible or do a single rename), keep `PairingViewModel` logic byte-for-byte, and rely on the existing `PairingViewModelTest` (moved) to prove parity. Settings' only change is composing `PairingScreen(...)` with the new parameters.
- **Startup flashes Home for a paired-looking-but-unpaired race** → Resolve the start destination from a `Loading` state and only build the `NavHost` once resolved; never default to `HOME_ROUTE` while `Loading`.
- **Removing dev login breaks local/dev token acquisition on an unpaired device** → Intended; onboarding provides the real pairing path. Confirm no CI/dev workflow depends on the dev creds before deleting the build-config fields.
- **Onboarding must not depend on `:features:home`** → Hoist "navigate to Home" to `:app` via a callback; onboarding only knows an abstract `onNavigateToHome`.
- **Back-stack correctness after pairing** → Use `popUpTo(WELCOME_ROUTE){inclusive=true}` so back from Home does not re-enter onboarding; covered by the app-onboarding spec scenario.

## Open Questions

- Exact Welcome copy (bienvenida title + supporting message) and whether a Figma node exists — placeholder strings will be used and refined against the design if provided.
- Whether an already-paired device should ever be able to reach the Welcome flow again (e.g., after unpair from Settings). Out of scope here; startup routing keys purely on the persisted token.
