## 1. Extract the reusable pairing slice into `:features:pairing`

- [x] 1.1 Create the module directory `features/pairing/src/main/java/com/qhathia/mobile/discovery/feature/pairing` (and `src/test/...`, `src/main/res`).
- [x] 1.2 Write `features/pairing/build.gradle.kts` from the Feature-module template (Compose + Hilt + KSP2 + `kotlin.metadata.jvm`), depending on `:core:domain`, `:core:ui`, `:core:common`.
- [x] 1.3 Register `include(":features:pairing")` in `settings.gradle.kts`.
- [x] 1.4 Create `features/pairing/src/main/AndroidManifest.xml` (empty `<manifest />`).
- [x] 1.5 Move `PairingScreen`, `PairingContent`, `PairingViewModel`, and `model/{PairingUiState,PairingUiEvent,PairingUiEffect,PairingPhase}` from `:features:settings` into `:features:pairing` (new package `...feature.pairing...`); keep `PairingViewModel` logic unchanged. (Also moved `UnpairConfirmationDialog`, hosted by `PairingScreen`.)
- [x] 1.6 Move the `pairing_*` string resources and the `ic_clock`, `ic_link`, `ic_arrow_forward` drawables into `features/pairing/src/main/res`; remove the moved strings/drawables from `:features:settings`. (`ic_arrow_back` stays in `:features:settings` — still used by `SettingsSideMenu`; the pairing back control reuses `core:ui`'s `ic_arrow_back`.)
- [x] 1.7 Move `PairingViewModelTest` to `:features:pairing` and add the JUnit5 + MockK + Turbine test deps to its build file.

## 2. Parameterize the pairing slice for host-agnostic reuse

- [x] 2.1 Add `PairingUiEffect.Paired` and emit it from `PairingViewModel` at the successful `exchangeDeviceToken` → `PairingPhase.Paired` transition (alongside the existing state update).
- [x] 2.2 Add `title: String`, `showBackButton: Boolean`, `onBack: () -> Unit`, and `onPaired: () -> Unit` parameters to `PairingScreen`; collect `PairingUiEffect.Paired` in its `LaunchedEffect` and invoke `onPaired`.
- [x] 2.3 Replace the hard-coded `network_toolbar_title` in `PairingContent`'s header with the `title` parameter, and render an optional back control (using `ic_arrow_back`) when `showBackButton` is true, wired to `onBack`.
- [x] 2.4 Update the pairing `@Preview`s to pass the new parameters (both a TIENDA/no-back and a QHATHIA/with-back preview, light + dark).

## 3. Rewire `:features:settings` onto `:features:pairing`

- [x] 3.1 Add `implementation(project(":features:pairing"))` to `features/settings/build.gradle.kts`.
- [x] 3.2 Update `SettingsScreen` to compose `PairingScreen(title = <TIENDA>, showBackButton = false, onBack = {}, onPaired = {})` from the new module; keep the Wi-Fi section and side-menu switching unchanged.
- [x] 3.3 Keep/relocate the TIENDA header string in `:features:settings` and pass it as `title`; fix imports left dangling by the move.
- [x] 3.4 Run `:features:settings` and `:features:pairing` unit tests to confirm parity.

## 4. Create the `:features:onboarding` module

- [x] 4.1 Create the module directory `features/onboarding/src/main/java/com/qhathia/mobile/discovery/feature/onboarding` (and `src/test/...`, `src/main/res`).
- [x] 4.2 Write `features/onboarding/build.gradle.kts` from the Feature-module template, depending on `:features:pairing`, `:core:ui`, `:core:common` (and `:core:domain` if needed).
- [x] 4.3 Register `include(":features:onboarding")` in `settings.gradle.kts`.
- [x] 4.4 Create `features/onboarding/src/main/AndroidManifest.xml` (empty `<manifest />`).
- [x] 4.5 Add onboarding string resources: bienvenida title, Qhathia app name, supporting message, "Continuar", and the QHATHIA pairing header title.

## 5. Onboarding screens and navigation

- [x] 5.1 Implement `WelcomeContent` (stateless) and `WelcomeScreen` (stateful, `onContinue` hoisted) with the bienvenida title, Qhathia name, message, and "Continuar" button; add light + dark `@Preview`.
- [x] 5.2 Implement `InitialConfigurationScreen` that composes `PairingScreen(title = QHATHIA, showBackButton = true, onBack, onPaired)`.
- [x] 5.3 Implement `navigation/OnboardingNavigation.kt`: `WELCOME_ROUTE`, `INITIAL_CONFIG_ROUTE`, and `NavGraphBuilder.onboardingGraph(onNavigateToHome, ...)` with hoisted navigation callbacks (mirror `networkSettingsScreen`).

## 6. Startup routing in `:app`

- [x] 6.1 Extend `MainViewModel` with a `StateFlow<StartDestination>` (`Loading` → `Onboarding`/`Home`) resolved from `ObserveStoreSessionUseCase().first().isPaired`; inject the use case.
- [x] 6.2 In `DiscoveryAppRoot`, collect the start destination; render a minimal splash/placeholder while `Loading` and never build the `NavHost` with `HOME_ROUTE` until resolved (no Home flash for unpaired devices).
- [x] 6.3 Wire `onboardingGraph` into the `NavHost`, set `startDestination` from the resolved state, and add `:features:onboarding` (+ transitively `:features:pairing`) to `app/build.gradle.kts`.
- [x] 6.4 Implement `onPaired`/`onNavigateToHome` to `navController.navigate(HOME_ROUTE)` with `popUpTo(WELCOME_ROUTE){ inclusive = true }` so onboarding leaves the back stack; wire onboarding back to pop to Welcome.

## 7. Remove the dev-login token path

- [x] 7.1 Change `SessionTokenProvider.accessToken()` to return the persisted device token when present, else a clean `Result.failure(...)`; drop the `AuthApiService` dependency and the in-memory dev-login cache (keep `clear()`).
- [x] 7.2 Delete `core:network` `AuthApiService`, `dto/AuthDtos.kt` (`LoginRequestDto`/`LoginResponseDto`), and their `NetworkModule` provider. (There was no `@Provides` — the service was constructor-injected; the stale KDoc references were removed.)
- [x] 7.3 Remove the `QHATHIA_DEV_EMAIL` and `QHATHIA_DEV_PASSWORD` `buildConfigField`s from `core/network/build.gradle.kts`.
- [x] 7.4 Update `SessionTokenProviderTest` to expect a clean failure when no device token is persisted (no login fallback). (The catalog/detail/order/search repository tests mock `SessionTokenProvider`, so they were unaffected.)

## 8. Verification

- [x] 8.1 `./gradlew :app:assembleDebug` builds with the two new modules and the removed auth code. (BUILD SUCCESSFUL, JBR/JDK 21.)
- [x] 8.2 `./gradlew test` passes across `:core:data`, `:core:network`, `:features:pairing`, `:features:settings`, `:features:onboarding`, and `:app`. (All green.)
- [x] 8.3 `./gradlew lint` passes; every new/parameterized screen has a working light + dark `@Preview`. (lintDebug green on `:features:pairing`, `:features:onboarding`, `:features:settings`.)
- [ ] 8.4 Manual/emulator check: fresh (unpaired) launch → Welcome → Continuar → initial config (QHATHIA, back returns to Welcome); successful pairing → Home; relaunch while paired → Home directly. (Requires a device/emulator — not run in this environment.)
