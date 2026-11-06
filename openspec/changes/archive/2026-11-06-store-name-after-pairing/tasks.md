## 1. Network and data

- [x] 1.1 `DeviceTokenResponseDto.storeName`; save it in `exchangeWithRetry`; `toPairedDevice(pairingCode, storeName)`.
- [x] 1.2 Tests: exchange with a name saves it; blank or absent keeps the session name.

## 2. Settings feature

- [x] 2.1 `SettingsHostViewModel` + test.
- [x] 2.2 `SettingsScreen` → `PairingScreen(title)`, `NetworkSettingsScreen`/`Content` `title`, `SettingsSideMenu` `storeName`, fallbacks; previews with a store name (light + dark).

## 3. Build and verify

- [x] 3.1 Tests of `:core:network`, `:core:data`, `:features:settings`; `lintDebug`; `:app:assembleProfile`.
- [x] 3.2 `qa-report.md` (D:VIN-003 / X:XVI-003, AJU-030, AJU-032); hand the emulator run to the QA team.
