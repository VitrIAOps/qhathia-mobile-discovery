## Context

`exchangeWithRetry` saves the token, device name and code, not the store; `DeviceTokenResponseDto` has no `storeName`. `getPairedDevice()` already saves `storeName` from `/devices/me` with `updateStoreName`, and Home reads it from the session. The Settings titles are fixed strings (`network_toolbar_title`, `network_store_title`).

## Decisions

- **D1 — Save on exchange.** `DeviceTokenResponseDto.storeName: String? = null`. After `saveDeviceSession(...)`, a non-blank name goes to `storeSessionRepository.updateStoreName`; `toPairedDevice(pairingCode, storeName)` carries it.
- **D2 — `SettingsHostViewModel`.** Hilt VM in `features:settings` with `storeName: StateFlow<String?>` from `ObserveStoreSessionUseCase` (blank → `null`). `SettingsScreen` passes `storeName?.uppercase()` to the toolbars and `storeName` to the side menu; a `null` title falls back to the existing strings inside the composables, so previews and other callers keep working.
- **D3 — Uppercase.** Kotlin `uppercase()` is locale-invariant; "ferretería" → "FERRETERÍA".
- **D4 — No `/devices/me` on start.** The exchange already has the name and it is persisted.

## Risks / Trade-offs

- [Long store names in the toolbar] → The toolbar text already takes the row's remaining width; one line with ellipsis.
