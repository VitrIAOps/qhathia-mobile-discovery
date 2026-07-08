## Why

A freshly-installed tablet currently boots straight into Home (voice search) even though it has no device session, and it silently authenticates with hard-coded development credentials. A merchant setting up a tablet for the first time has no guided way to pair it, and the app must never ship a dev-login backdoor. We need a first-run configuration flow that pairs the device before it can be used, and we must make pairing the *only* source of the app's access token.

## What Changes

- Add a **Welcome screen** as the first-run entry point: a bienvenida title, the Qhathia app name, a short supporting message, and a "Continuar" button that navigates to the initial configuration screen.
- Add an **Initial configuration screen** that reuses the existing pairing ("Vincular") experience, but without the settings side menu, with its header titled **QHATHIA** instead of TIENDA, and with a back affordance that returns to the Welcome screen. All existing pairing validations (code TTL, status subscription, token exchange) are preserved.
- **Startup routing by pairing state**: on launch, an unpaired device opens the Welcome flow; an already-paired device opens Home directly.
- **Navigate to Home only on successful pairing**: completing pairing from the initial configuration screen transitions the app into Home (voice search); no other outcome advances to Home.
- Make the reusable pairing slice **host-agnostic** (parameterized header title, optional back affordance, and a paired-success callback) so it can be composed by both Settings and the onboarding flow without a feature-to-feature dependency.
- **BREAKING** — Remove all login-based token acquisition. The access token is obtained **only** from the persisted pairing device token or the pairing-session token endpoint (`/api/v1/devices/pairing-session/{sessionId}/token`). Delete the dev-credential login path (`AuthApiService.login`, login DTOs, and the `QHATHIA_DEV_EMAIL` / `QHATHIA_DEV_PASSWORD` build-config fields). When no device token is present, authenticated calls fail cleanly instead of falling back to a login.

## Capabilities

### New Capabilities
- `app-onboarding`: First-run Welcome screen, the initial device-configuration (pairing) screen, startup routing based on pairing state, and the navigation to Home on successful pairing.

### Modified Capabilities
- `store-session`: The persisted device token becomes the **sole** source of the authenticated access token; the development-credential login fallback is removed, and an unpaired device yields no token rather than logging in.
- `device-pairing`: The pairing section becomes host-agnostic (composable by both Settings and onboarding) with a parameterized header title, an optional back affordance, and a paired-success signal the host can act on.

## Impact

- **New module** `features/onboarding` — Welcome + initial configuration host, navigation graph.
- **New/extracted module** `features/pairing` — the reusable pairing slice (ViewModel, MVI models, `PairingScreen`/`PairingContent`, pairing strings/drawables) moved out of `features/settings` so both Settings and onboarding can compose it. `features/settings` depends on it for its Vincular section.
- `app` — `MainActivity`/`MainViewModel` startup destination now depends on pairing state; new onboarding + pairing destinations wired into the `NavHost`.
- `core/data` — `SessionTokenProvider` no longer performs dev login; STT and product/order repositories that request a token surface a clean failure when unpaired. `SessionTokenProviderTest` and dependent repository tests updated.
- `core/network` — remove `AuthApiService`, the login DTOs (`AuthDtos`), and their `NetworkModule` provider.
- `core/network` build config — remove `QHATHIA_DEV_EMAIL` / `QHATHIA_DEV_PASSWORD` fields.
- `settings.gradle.kts` — register `:features:onboarding` and `:features:pairing`.
