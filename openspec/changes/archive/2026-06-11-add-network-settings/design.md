## Context

The Qhathia Discovery kiosk is a multi-module Android app (Clean Architecture + MVI, Jetpack Compose, Hilt, Ktor). The Home screen already exposes a settings affordance wired through `onNavigateToSettings`, but `:app` currently routes `SETTINGS_ROUTE` to an inert `PlaceholderScreen`. This change replaces that placeholder with a real, password-gated settings area and a Network settings screen.

Existing module layout the change builds on:
- `core:domain` — pure-Kotlin models, repository ports, use cases.
- `core:data` — repository implementations + Android-backed data sources (e.g. `AudioRecorderImpl` injects `@ApplicationContext`) + Hilt `*DataModule`s binding ports to impls.
- `core:ui` — Material 3 theme, design tokens (`Spacing`, `Shapes`), shared components; the cart feature already ships a dialog (`CartDialog` / `CartDialogContent`) we mirror for the password and Wi-Fi dialogs.
- `features:*` — vertical slices (`Screen` → `Content` → `ViewModel`, `model/` for `UiState`/`UiEvent`/`UiEffect`, `navigation/` nav-graph extension). Feature modules never depend on each other; the app hosts cross-feature concerns (the cart dialog is hosted in `MainActivity`).

Hard platform constraint: from API 29 (the app's min SDK is 26, target 36) third-party apps **cannot** programmatically enable/disable Wi-Fi (`WifiManager.setWifiEnabled` is a no-op) nor silently join arbitrary networks. The supported paths are `WifiNetworkSuggestion`, the `WifiNetworkSpecifier` peer-to-peer request flow, or `Settings.Panel.ACTION_WIFI` panels; reading scan results requires location permission and Wi-Fi+location enabled. Toggling mobile data programmatically is not permitted for non-privileged apps at all. The kiosk may eventually run as a device-owner/privileged app, but that is not assumed here.

## Goals / Non-Goals

**Goals:**
- Password-gate the settings entry with a swappable validation port (mocked to `1234`).
- Provide the settings side-menu shell (Device settings button + Network settings button).
- Provide a Network settings screen: mobile-data switch above a Wi-Fi toggle, accessible-network list, secured-network password dialog, connect flow, and an airplane-mode guard.
- Keep the architecture honest: domain ports + use cases, data-layer Android implementations, a self-contained `features:settings` module, design-system-only UI.

**Non-Goals:**
- The **Device settings** button's destination/behavior (button present but inert).
- A real settings-password backend/API (mocked).
- Privileged/device-owner provisioning, captive-portal handling, enterprise (EAP) Wi-Fi, or saved-network management.
- Persisting connectivity preferences across process death.

## Decisions

**1. One new feature module `features:settings`, two capabilities inside it.**
The story is cohesive (reach settings → manage network) but splits into two spec capabilities (`settings-access`, `network-settings`). Rather than two thin modules, a single `features:settings` module owns both, matching how existing features bundle their screen/VM/model/navigation. Alternative (separate `features:settings-access` + `features:network-settings`) was rejected: feature modules can't depend on each other, so the password gate and the network screen would need app-level glue for no real isolation benefit.

**2. A PIN gate overlay + one MVI screen, plus the Wi-Fi dialog.**
- The **settings-PIN dialog** is hosted as an **app-level overlay** (the cart precedent), shown over Home **before** any navigation, so no settings content appears until the PIN is correct. A correct PIN navigates directly to the Network settings destination — there is no separate "settings options/side-menu shell" screen; the Network settings screen (Figma "Red Wifi") carries its own side menu and is the settings landing.
- `NetworkSettingsScreen` is the settings nav destination; the **Wi-Fi password dialog** is a dialog overlay, not a separate destination.
- The screen follows the `Screen` (stateful, `hiltViewModel()`, `collectAsStateWithLifecycle`) → `Content` (stateless, previewable) split with `*UiState` / `*UiEvent` / `*UiEffect` in `model/`; the PIN gate (`SettingsPasswordGate`) follows the same MVI contract.

**3. Domain ports isolate both the mock and the platform.**
- `SettingsAccessRepository.validatePassword(input): Result<Boolean>` — implemented by a `MockSettingsPasswordDataSource` returning the constant `1234` (a `SettingsConstants.MOCK_SETTINGS_PASSWORD`). Swapping to a real API later is a `core:data` change only.
- `NetworkSettingsRepository` exposes: `observeConnectivity(): Flow<NetworkConnectivityState>` (Wi-Fi enabled, mobile-data enabled, airplane-mode active, connected SSID), `observeWifiNetworks(): Flow<List<WifiNetwork>>`, and suspend actions `setWifiEnabled`, `setMobileDataEnabled`, `connect(network, password?)`.
- New domain models: `WifiNetwork(ssid, isSecured, signalLevel, isConnected)`, `NetworkConnectivityState(...)`. Use cases wrap each (e.g. `ValidateSettingsPasswordUseCase`, `ObserveNetworkStateUseCase`, `ObserveWifiNetworksUseCase`, `SetWifiEnabledUseCase`, `SetMobileDataEnabledUseCase`, `ConnectToWifiUseCase`).

**4. Data layer uses standard Android APIs and degrades honestly.**
`NetworkSettingsRepositoryImpl` injects `@ApplicationContext` (the `AudioRecorderImpl` precedent) and reads `WifiManager` / `ConnectivityManager`:
- Connectivity/airplane state via `ConnectivityManager` callbacks + `Settings.Global.AIRPLANE_MODE_ON`, exposed as a cold `Flow` (`callbackFlow`).
- Network list via `WifiManager` scan results (guarded by `ACCESS_FINE_LOCATION`).
- For actions that the OS forbids to non-privileged apps on API 29+ (enable Wi-Fi, toggle mobile data, silent connect), the impl uses the **supported** path where one exists (`WifiNetworkSuggestion` / `Settings.Panel` intents) and otherwise surfaces a clear "not permitted on this device" `Result.failure` that the ViewModel maps to a design-system message. This keeps the contract truthful instead of pretending success. The seam also lets a future device-owner build provide a privileged implementation without touching domain/UI.

**5. Airplane-mode guard lives in the ViewModel from observed state.**
`NetworkSettingsViewModel` derives an `airplaneModeActive` flag from `observeConnectivity()` and, when true, disables selection controls and emits the "airplane mode active" indication — keeping the business rule in one place and the `Content` composable a pure function of `UiState`.

**6. Permissions added to `:app` manifest; location requested at point of use.**
Add `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `ACCESS_FINE_LOCATION` (`ACCESS_NETWORK_STATE` already present). Location is dangerous → request at runtime when entering the Network settings screen, mirroring how `MainActivity` requests `RECORD_AUDIO`.

## Risks / Trade-offs

- **OS forbids programmatic Wi-Fi/data control on API 29+** → Use supported suggestion/panel APIs and return explicit failures elsewhere; document the device-owner seam. UI must handle the "not permitted" outcome gracefully rather than assume toggles always work.
- **Scan results need location permission + location enabled** → Gate the list behind a runtime location request and show an empty/locked state with guidance when denied or location is off.
- **Mocked password (`1234`) is insecure** → Acceptable for this change (no real API yet); isolated behind a port and a single constant so it is trivially replaced and never leaks into UI/domain.
- **Figma links in the ClickUp task are empty** → Layout follows the named Figma states ("Ingresar contraseña ajustes" empty/complete, "Ajustes de red") and the dialog component checklist from the task; exact spacing/visuals to be reconciled against Figma during implementation. → Open question below.
- **Settings unlock is per-entry, in-memory** → Re-entering settings re-prompts for the password; no "stay unlocked" persistence, consistent with the kiosk's inactivity-clearing model.

## Migration Plan

Purely additive. The only replacement is swapping the `:app` `SETTINGS_ROUTE` placeholder for the new `features:settings` nav graph; Home's `onNavigateToSettings` contract is unchanged. Rollback = revert the module registration and restore the placeholder destination. No data migration.

## Open Questions

- Are the Figma frames available so password-dialog and network-screen spacing/iconography can be matched exactly (links in the task table were empty)?
- Is the kiosk intended to ship as a **device-owner / privileged** app? If so, a privileged `NetworkSettingsRepository` implementation could perform real Wi-Fi/data toggles and silent connects; the ports are designed to accommodate this without UI/domain changes.
- Should an incorrect settings password be rate-limited or locked out after N attempts, or is a simple inline rejection sufficient for now?
