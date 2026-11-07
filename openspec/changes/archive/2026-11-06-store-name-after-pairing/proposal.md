## Why

Right after pairing, Home shows "TIENDA" instead of the store name, and the Settings titles always say "TIENDA" / "Tienda" (QA regression 2026-11-03, F-05 / D:VIN-003 / X:XVI-003). The name is only read from `/devices/me` when the merchant opens Settings → Vincular, although the token exchange already returns it (backend phase 37).

## What Changes

- **Store name from the exchange.** `GET …/pairing-session/{id}/token` sends `storeName`; the tablet saves it in the store session right after pairing, so Home shows it at once.
- **Settings titles follow the store.** The "Red Wifi" and "Vincular" toolbars show the store name in capitals and the side menu shows it as is; "TIENDA" / "Tienda" stay as the fallback when the name is unknown.
- No extra `/devices/me` call on start.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `store-session`: the store name is saved from the pairing exchange, and the Settings titles show it.

## Impact

- `core:network`: `DeviceTokenResponseDto.storeName`.
- `core:data`: `DevicePairingRepositoryImpl.exchangeWithRetry` saves it; `toPairedDevice` carries it.
- `features:settings`: new `SettingsHostViewModel` (`storeName` from `ObserveStoreSessionUseCase`); `SettingsScreen`, `NetworkSettingsScreen`/`Content` (title parameter), `SettingsSideMenu`; previews.
- No new module, library or manifest change.
