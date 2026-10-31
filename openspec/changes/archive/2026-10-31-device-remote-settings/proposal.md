## Why

The staff app's "Ajustes de la tablet" screen saves per-tablet settings on the server (api-contract.md §21, Phase 21) — voice assistant, brightness, kiosk mode, exit PIN… — but the tablet never reads them, so every toggle is a control with no observable effect (handoff §1 and §5, BUG-025). Meanwhile the in-app settings are protected by a PIN hard-coded as `1234` (`SettingsConstants.MOCK_SETTINGS_PASSWORD`), identical on every tablet and impossible to change.

## What Changes

- **Sync remote settings**: when a heartbeat reports a `settingsVersion` different from the one stored, the app reads `GET /api/v1/devices/me/settings` and persists the result (including `exitPinHash`) locally, so it applies them across restarts and without network.
- **Exit PIN replaces the mocked PIN**: the settings gate validates the entered PIN against the stored BCrypt `exitPinHash`, offline. PINs are 4–6 digits, so the keypad gains a confirm key (auto-validates at 6 digits). **When no PIN is set (`exitPinHash = null` or settings never synced), the settings open without asking for a PIN.**
- **BREAKING** — remove the mocked `1234` PIN (`MOCK_SETTINGS_PASSWORD`, `MockSettingsPasswordDataSource`).
- **Apply `voiceAssistantEnabled`**: when `false`, every microphone button (Home, Catalog, Product search, Product detail) is hidden, so voice search cannot be started.
- **Apply `brightnessMode`**: `AUTO` follows the system; `LOW` / `MEDIUM` / `HIGH` set the app window brightness.
- `kioskMode`, `idleShowcaseEnabled` and `autoSyncEnabled` are stored but not applied: kiosk lock is the next change (`kiosk-lock-mode`); Discovery has no idle showcase and no catalog sync today.

## Capabilities

### New Capabilities
- `device-remote-settings`: Version-driven sync of the tablet's remote settings, their local persistence, and the application of `voiceAssistantEnabled` and `brightnessMode`.

### Modified Capabilities
- `settings-access`: The settings PIN is the remote exit PIN validated offline against its BCrypt hash (4–6 digits, confirm key); with no PIN set the settings open directly. The mocked `1234` is removed.

## Impact

- `core/network` — `DeviceSettingsApiService.getMySettings(token)` + `DeviceSettingsDto`.
- `core/domain` — `DeviceSettings` model + `BrightnessMode`, `DeviceSettingsRepository` port, `SyncDeviceSettingsUseCase`, `ObserveDeviceSettingsUseCase`, `IsSettingsPinRequiredUseCase`; `SettingsAccessRepository` gains `isPinRequired()`; `SettingsConstants` loses the mock, gains PIN length bounds.
- `core/data` — `DeviceSettingsRepositoryImpl` + `DeviceSettingsLocalDataSource` (existing Preferences DataStore), `ExitPinVerifier` (BCrypt), `SettingsAccessRepositoryImpl` rewritten; delete `MockSettingsPasswordDataSource`.
- **New dependency**: `at.favre.lib:bcrypt` (pure-Java BCrypt verifier, Apache-2.0) in `core:data`.
- `core/ui` — `LocalVoiceAssistantEnabled` CompositionLocal; `MicButton` renders nothing when it is `false`.
- `features/settings` — PIN gate: skip when no PIN is required, 4–6 digit entry, confirm key, dots per entered digit.
- `app` — `DiscoveryApp` launches `SyncDeviceSettingsUseCase`; `MainViewModel` exposes the settings; `MainActivity` provides `LocalVoiceAssistantEnabled` and applies window brightness.
- No new permissions, no new Gradle module.
