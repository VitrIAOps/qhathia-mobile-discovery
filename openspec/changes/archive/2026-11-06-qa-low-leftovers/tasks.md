## 1. F-07 — Microphone permission on tap

- [x] 1.1 Remove the Home permission request from `MainActivity`; update the `AudioRecorderImpl` comment.

## 2. F-24 — Heartbeat retries

- [x] 2.1 `NetworkConnectivityState.hasInternet` from `ConnectivitySnapshot.internetValidated`.
- [x] 2.2 `HEARTBEAT_RETRY_DELAYS`; `RunDeviceHeartbeatUseCase` retry loop with the internet-back trigger.
- [x] 2.3 `RunDeviceHeartbeatUseCaseTest`: retry schedule, back to interval, internet back during a retry and during the interval, no extra beat without a change.

## 3. F-63 — Device uptime

- [x] 3.1 `DeviceTelemetryProviderImpl.deviceUptimeSeconds()`.

## 4. F-43 — Gallery without empty thumbnails

- [x] 4.1 `ProductImage(showMissingLabel)`, string `discovery_image_missing`; `ProductImageGallery` without empty slots; previews (light + dark).

## 5. Build and verify

- [x] 5.1 Tests of `:core:domain`, `:core:data`, `:core:ui`, `:features:product-detail`, `:features:home`, `:app`; `lintDebug`; `:app:assembleProfile`.
- [x] 5.2 `qa-report.md` (D:EXP-004, X:XCR-006, X:EXP-005, D:DET-062/063); hand the emulator run to the QA team.
