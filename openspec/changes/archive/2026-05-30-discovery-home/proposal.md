## Why

The Qhathia Discovery app (the in-store, consumer-facing kiosk interface) currently ships only the default scaffold screen. In-store consumers need an inviting, low-friction entry point that lets them start a product search by voice, jump to the catalog, or open settings — while the operating store's identity stays visible. This is the first real feature of the Discovery app, so it also bootstraps the modular architecture and the shared design system every later screen will build on.

## What Changes

- Introduce a **Home screen** for Discovery as a proper feature module (`features:home`): branded surface showing the store name, a search bar, a microphone (voice search) button, a catalog shortcut button, and a settings button — with entrance/state animations.
- Implement the **microphone interaction state machine**: tapping the mic enters a listening state with a **30-second** maximum input window; while the consumer is interacting with the mic, the search bar and the catalog/settings buttons are hidden. When no audio is captured before the timeout, the assistant cycles through **multiple retry prompts** asking the consumer to try again. (Real speech-to-text via Deepgram is **out of scope** — the mic drives UI state only for now.)
- Introduce a **store session** capability (`core:domain` + `core:data`): a locally persisted holder for store/merchant values (starting with the store name) that the Home screen observes, so store identity is always visible and reusable by future screens.
- **Bootstrap the multi-module architecture and shared design system** required by the project standards: create `core:ui` (Material 3 theme, design tokens, shared composables), `core:common`, `core:domain`, `core:data`, and `core:testing` modules; convert `:app` into the Hilt application shell that hosts the `NavHost`.
- Wire navigation **effects** for the catalog, search-results, and settings destinations. The destination screens themselves are **out of scope**; Home only emits the navigation intents (placeholder routes).
- Expand `gradle/libs.versions.toml` to add the dependencies the standards mandate (Hilt + KSP2, Navigation Compose, Lifecycle ViewModel/runtime, DataStore, coroutines, immutable collections, JUnit5/MockK/Turbine).

## Capabilities

### New Capabilities
- `discovery-home`: The Discovery Home screen — its layout, the visibility rules tied to mic interaction, the microphone listening/timeout/retry behavior, navigation intents to catalog/results/settings, and store-name visibility.
- `store-session`: A persisted store/merchant session that stores values needed across the app (starting with the store name) and exposes them as an observable stream to the UI.

### Modified Capabilities
<!-- None — there are no existing specs in openspec/specs/. -->

## Impact

- **Build / tooling:** `gradle/libs.versions.toml` (new libraries + plugins), root `settings.gradle.kts` (register new modules), `app/build.gradle.kts` (Hilt, module deps). Version reconciliation of Kotlin ↔ KSP2 ↔ Hilt per the standards' canonical catalog.
- **New modules:** `core:common`, `core:ui`, `core:domain`, `core:data`, `core:testing`, `features:home`.
- **`:app`:** new `@HiltAndroidApp` Application class, `@AndroidEntryPoint` on `MainActivity`, app-level `NavHost`, theme entry moved to `core:ui`; manifest gains the Application `android:name` (and `INTERNET`/`ACCESS_NETWORK_STATE` reserved for the deferred store fetch — not added until network is used).
- **Persistence:** new DataStore (Preferences) instance for the store session.
- **Design:** implements the Home flow from Figma (`Qhatia-DS`, node `286-9269`) and establishes the imported design system as `core:ui`.
- **Out of scope / deferred:** Deepgram STT integration, the catalog/results/settings destination screens, device pairing/auth that will later populate the store session from the backend.
