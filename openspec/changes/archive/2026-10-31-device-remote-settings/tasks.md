## 1. Domain layer (`:core:domain`)

- [x] 1.1 Add `DeviceSettings` (+ `DEFAULT`) and `BrightnessMode { AUTO, LOW, MEDIUM, HIGH }`.
- [x] 1.2 Add `DeviceSettingsRepository { fun observe(): Flow<DeviceSettings>; suspend fun storedVersion(): Long?; suspend fun refresh(): Result<DeviceSettings> }`; add `suspend fun isPinRequired(): Boolean` to `SettingsAccessRepository`.
- [x] 1.3 `SettingsConstants`: remove `MOCK_SETTINGS_PASSWORD`/`SETTINGS_PASSWORD_LENGTH`, add `MIN_PIN_LENGTH = 4`, `MAX_PIN_LENGTH = 6`.
- [x] 1.4 Add `SyncDeviceSettingsUseCase` (design D1), `ObserveDeviceSettingsUseCase`, `IsSettingsPinRequiredUseCase`.
- [x] 1.5 Tests: sync reads once per new version, skips equal version, retries after a failed read on the next heartbeat; update `ValidateSettingsPasswordUseCaseTest` (no mock constant).

## 2. Network layer (`:core:network`)

- [x] 2.1 Add `DeviceSettingsDto` (server defaults for missing fields) and `DeviceSettingsApiService.getMySettings(token)` → `GET /api/v1/devices/me/settings`, 15 s timeout.
- [x] 2.2 `MockEngine` tests: path + bearer, full parse incl. `exitPinHash`, minimal body → defaults, 401 → `HttpError`.

## 3. Data layer (`:core:data`)

- [x] 3.1 Add `at.favre.lib:bcrypt` to the version catalog and `core:data`.
- [x] 3.2 Add `settings/DeviceSettingsLocalDataSource` (keys in the `store_session` DataStore: each setting, version, exit PIN hash; `observe()`, `storedVersion()`, `exitPinHash()`, `save()`).
- [x] 3.3 Add `settings/DeviceSettingsRepositoryImpl` + mappers (unknown brightness → `AUTO`; `hasExitPin` from the hash).
- [x] 3.4 Add `settings/ExitPinVerifier` (BCrypt verify on `Dispatchers.Default`, malformed hash → `false`).
- [x] 3.5 Rewrite `SettingsAccessRepositoryImpl` on the stored hash + verifier; delete `MockSettingsPasswordDataSource`; bind `DeviceSettingsRepository`.
- [x] 3.6 Tests: mappers; repository refresh stores settings/version/hash and fails without token; verifier accepts a `$2a$` hash of the right PIN, rejects a wrong PIN and a malformed hash; access repository (no hash → not required; hash → validates); update `SettingsAccessRepositoryImplTest`.

## 4. Testing support (`:core:testing`)

- [x] 4.1 Add `FakeDeviceSettingsRepository`; extend `FakeSettingsAccessRepository` with `pinRequired`.

## 5. Shared UI (`:core:ui`)

- [x] 5.1 Add `LocalVoiceAssistantEnabled` (`staticCompositionLocalOf { true }`); `MicButton` renders nothing when `false`.

## 6. Settings gate (`:features:settings`)

- [x] 6.1 `SettingsUiState`: `isResolvingAccess`, `isValidating`, `isSubmitEnabled` (≥ 4 digits); `SettingsViewModel`: on `Reset` resolve `IsSettingsPinRequiredUseCase` (not required → `Unlocked`), cap at 6, auto-validate at 6, `SubmitPassword` from 4.
- [x] 6.2 `SettingsPasswordGate` renders the dialog only once access is resolved and a PIN is required.
- [x] 6.3 Keypad: confirm key in the empty bottom-left cell (enabled ≥ 4, disabled while validating), dots = max(4, entered); new strings; light + dark previews updated (incl. a 6-digit state).
- [x] 6.4 Update `SettingsViewModelTest`: no PIN → unlocked without dialog; 4 digits not auto-submitted; confirm validates; 6 digits auto-validate; 7th digit ignored; wrong PIN clears with error.

## 7. App wiring (`:app`)

- [x] 7.1 `DiscoveryApp` launches `SyncDeviceSettingsUseCase`.
- [x] 7.2 `MainViewModel.deviceSettings`; `DiscoveryAppRoot` provides `LocalVoiceAssistantEnabled` and applies window brightness per design D6. (Uses `LocalActivity`, as lint requires.)

## 8. Verification

- [x] 8.1 `./gradlew :app:assembleDebug` builds.
- [x] 8.2 `./gradlew testDebugUnitTest :core:domain:test` passes. (328 tests, 0 failures.)
- [x] 8.3 `./gradlew lintDebug` passes with no new warnings.
- [x] 8.4 Manual check on a paired tablet: set a 6-digit PIN from staff → gate asks for it and accepts it offline; remove it → settings open directly; voice off → mic buttons disappear; brightness HIGH/LOW visibly changes.
  - Done 2026-10-31 on `Medium_Tablet` (API 26, paired, production). Settings were sent with `PUT /api/v1/devices/{deviceId}/settings` as the store owner (the same call the staff app makes) and each one was checked on the tablet after the next heartbeat (≤ 75 s):
    - 6-digit PIN `246810` → the gear asks for it; typing it opens Settings (validated on the 6th digit).
    - `removeExitPin: true` → the gear opens Settings directly.
    - `voiceAssistantEnabled: false` → no microphone on Home, Catalog or Search; back to `true` → it returns.
    - `brightnessMode`: LOW → window brightness override 76/255 (0.3); HIGH → 255/255 (1.0) (`dumpsys power`, `mScreenBrightnessOverrideFromWindowManager`).
    - Restored afterwards: the merchant's 5-digit PIN (accepted with "Confirmar PIN"), brightness LOW, voice on (`settingsVersion` 22 → 27).
  - Offline PIN check: not reproducible on this emulator (Wi-Fi cannot be cut from adb). The gate compares against the cached `exitPinHash` with no request; covered by `SettingsAccessRepositoryImplTest` and `ValidateSettingsPasswordUseCaseTest`.
  - Found (not in this change): with voice off, the Home subtitle still says "Presiona aquí y describe lo que buscas…", which points at the microphone that is no longer shown.
