## Why

The last four low-severity tablet findings of the QA regression of 2026-11-03 (`docs/handoff/android-regresion-2026-11-03.md`):

- **F-07 / D:EXP-004** — the microphone permission pops up by itself on arriving at Home; QA expects it on the first microphone tap.
- **F-24 / X:XCR-006** — after the network comes back, the heartbeat (and the settings and commands it carries) waits for the next 60 s tick; a failed heartbeat is never retried sooner.
- **F-63 / X:EXP-005** — the first heartbeat sends `uptimeSeconds: 0`: it measures the app process, while the backend means the device.
- **F-43 / D:DET-062** — a product without photos shows three empty gray thumbnails and no "Sin foto"; one-photo products also show empty squares.

## What Changes

- **F-07:** no permission request on arriving at Home. Each microphone button already asks on its first tap; that stays. Supersedes the wave-3 choice.
- **F-24:** a failed heartbeat is retried after 5, 10, 20 and 40 s, then every 60 s. Whatever the wait, the next heartbeat goes out as soon as Android reports validated internet again.
- **F-63:** `uptimeSeconds` is the time since the device booted.
- **F-43:** the detail gallery shows a thumbnail only for each extra photo that exists, never gray squares; with no photo the main placeholder says "Sin foto".

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `voice-search-stt`: the microphone permission is requested on the first microphone tap, not on Home.
- `device-heartbeat`: device uptime; quick retries and an immediate heartbeat when internet comes back.
- `product-detail`: no empty thumbnails; "Sin foto" for a product without photos.

## Impact

- `:app`: `MainActivity` (Home permission request removed).
- `core:domain`: `RunDeviceHeartbeatUseCase` (+ `NetworkSettingsRepository`), `DeviceConstants.HEARTBEAT_RETRY_DELAYS`, `NetworkConnectivityState.hasInternet`.
- `core:data`: `ConnectivitySnapshot.internetValidated`, `NetworkSettingsRepositoryImpl`, `DeviceTelemetryProviderImpl`, comment in `AudioRecorderImpl`.
- `core:ui`: `ProductImage(showMissingLabel)`, `ProductImageGallery`, string `discovery_image_missing`.
- No new module, library or manifest change.
