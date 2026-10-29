## 1. Spike and QA baseline

- [x] 1.1 Spike D7 on the `Medium_Tablet` AVD: pin the app with `startLockTask()`, launch a throwaway `:restart`-process activity into the same task from `MainActivity`, kill the main pid, relaunch `MainActivity`. Record in `design.md` whether `dumpsys activity activities` still shows lock task mode `PINNED` and whether the pinning dialog reappears. Decide between the trampoline and the D6 fallback (`stopLockTask` → restart → pin again).
- [x] 1.2 Production check of the pairing WebSocket (D5), with a throwaway script in the scratchpad and no approval:
  - `POST /devices/pairing-token`, then open the socket with no ping → note when it closes (expected about 61 s).
  - Open it again with a ping every 20 s → still open after 90 s?
  - Reconnect → is the current status (`WAITING`) sent on connect?
  - Record the results in `design.md`. If the ping does not keep it open, D5 relies on reconnecting only, and the design does not change.
- [x] 1.3 Create an API 36 tablet AVD (Android 16, 2560×1600, smallest width ≥ 600 dp) (config written by hand: no `avdmanager` installed), next to `Medium_Tablet` (API 26).
- [x] 1.4 Reproduce the wave-1 cases on the current `main` APK (`profile`) in the emulator and save evidence in `qa-report.md` (baseline "before"): CAT-030, CAR-008, CAR-041 (form carries over), CAR-042, DET-034 retries (logcat request count), RED-060 (logcat shows name/phone), VIN-030 / portrait (the app rotates and regenerates the code; on API 26 and API 36), AJU-011, EXP-001/006 (bars over dialogs).
  - Done 2026-10-28, every listed case reproduced. DET-034 was exercised through CAR-011 (a 422 sent 3 times), because no `LOW_STOCK` product exists in production search results. Not run here (user-assisted, task 11.4): VIN-031/032 approval, `RESTART_APP` sent from staff.
- [x] 1.5 Create `qa-report.md` with one row per case ID: status before, status after, evidence path, and who ran it (Claude, or user-assisted).

## 2. Share outcome (D1): DET-032, CAR-007/032, CAR-008, DET-034, CAR-011

- [x] 2.1 `core:domain`: add `ShareOutcome` (`Delivered`, `NotDelivered`, `Rejected(kind)`, `Failed(category)`), and change `CartShareRepository.shareCart` to return it.
- [x] 2.2 `core:data`: `CartShareRepositoryImpl` maps `status` (`SENT` → Delivered, `FAILED` / `STUB` / unknown → NotDelivered) and HTTP codes (400 → Rejected.Unavailable, 404 → Rejected.NotFound, 422 → Rejected.Invalid, 5xx → Failed).
- [x] 2.3 `core:domain`: `ShareQuoteUseCase` with the retry policy (up to 3 attempts, only for `Failed`). Remove both private `shareWithRetry` copies.
- [x] 2.4 `features:cart`: "Compartir" success only on Delivered, with new share-success strings ("¡Cotización enviada!"). NotDelivered → error state with the not-delivered copy. "Realizar pedido" sets `shareDelivered = outcome is Delivered`.
- [x] 2.5 `features:product-detail`: share dialog uses `ShareQuoteUseCase` with the same mapping.
- [x] 2.6 Unit tests: repository (SENT / FAILED / STUB / 400 / 404 / 422 / 500 / timeout with `MockEngine`); use case (request count per outcome: 400 → 1, timeout ×2 + SENT → 3, FAILED → 1); `CartViewModel` and `ProductDetailViewModel` (share FAILED → error, order + FAILED → success with fallback copy, share success title is the share copy).
  - Done 2026-10-28. The wire side (JSON body, HTTP codes, timeout → `NetworkResult`) is tested with `MockEngine` in `core:network` (`CartShareApiServiceTest`), because `KtorClientFactory` is internal there; the classification into `ShareOutcome` is tested in `core:data` over a mocked service. Other 4xx map to `Rejected.Other`. Share "not delivered" is its own phase (`OrderSubmission.QuoteNotDelivered`, `ShareSubmission.NotDelivered`).

## 3. Customer session (D2, D3): CAR-040, CAR-041, CAR-042

- [x] 3.1 `core:domain`: `CustomerSessionRepository` port (`sessionEnded: Flow<Unit>`, `end()`), `EndCustomerSessionUseCase` (clear basket + emit), `ObserveCustomerSessionEndUseCase`.
- [x] 3.2 `core:data`: in-memory `@Singleton` implementation and Hilt binding.
- [x] 3.3 `CartViewModel`: "Aceptar" / "Cancelar orden" go through `EndCustomerSessionUseCase`; reset the form and submission on `sessionEnded`.
- [x] 3.4 `ProductDetailViewModel`: close and reset the share dialog on `sessionEnded`.
- [x] 3.5 `core:ui`: `LocalUserActivity` and `Modifier.reportUserActivity()`. Apply them to the cart dialog, the product share dialog, the settings PIN dialog and the close-confirm dialogs; text fields report from `onValueChange`.
- [x] 3.6 `app`: move the inactivity timer into `MainViewModel` (activity stream + current route + basket count). Off Home it always runs; on Home only with a non-empty basket. On timeout: `EndCustomerSessionUseCase`, close the cart and settings overlays, pop to Home.
- [x] 3.7 Update `CartConstants` docs and `shopping-cart` references to the new rule.
- [x] 3.8 Unit tests: `EndCustomerSessionUseCase`; `CartViewModel` form reset on session end; `MainViewModel` timer (virtual time): off Home → ends at 60 s, activity at 59 s → restarts, Home + empty basket → never ends, Home + items → ends.
  - Done 2026-10-28. The timer also skips the onboarding routes (`welcome`, `initial_configuration`): there is no customer before pairing. `ClearCartUseCase` was removed; every path goes through `EndCustomerSessionUseCase`. Typing is reported from the two shared text fields (`DiscoveryTextField`, `DiscoverySearchBar`), so it covers the main window too (search, Wi-Fi). Dialog roots covered: cart, product share, settings PIN, Wi-Fi password, and `ConfirmationDialog` (close-confirm and unpair). The `app` module moved to JUnit 5 like the other modules. The main `shopping-cart` spec is updated by the delta at archive time.

## 4. Orderable products (D4): DET-013, CAR-023/025, BUS-042 (app side)

- [x] 4.1 Map `ProductDetailDto.active` → `ProductDetail.active`; add `IsOrderableUseCase`.
- [x] 4.2 Product detail: disable add, stepper and share and show a "No disponible por ahora" note when not orderable.
- [x] 4.3 Disable quick-add for non-orderable cards in search results, catalog and related products.
- [x] 4.4 `OrderErrorCategory.Unavailable` / `NotFound` from 400 / 404 in `OrderRepository`; parse the error `code` best effort (`ApiErrorDto`).
- [x] 4.5 `RevalidateCartUseCase`: `GET /products/{id}` for each line (limited concurrency), returns the ids that are not orderable or not found.
- [x] 4.6 Cart: on Unavailable / NotFound (order or share), run the recheck, mark the lines "No disponible", show the message, and keep the actions disabled until those lines are removed. Fall back to the generic error when no line is flagged.
- [x] 4.7 Unit tests: `IsOrderableUseCase` table; detail ViewModel with active = false and with `LOW_STOCK`; `RevalidateCartUseCase` (inactive, `LOW_STOCK`, 404, all ok); `CartViewModel` (400 → flagged lines → remove → enabled; 400 with nothing flagged → generic error).
  - Done 2026-10-28. The error body `code` is not parsed: the contract's error field is `errorCode`, and `NetworkResult.HttpError` does not carry the body; the recheck finds the culprits itself, and the only other 400 (duplicated `productId`) cannot happen because the basket merges lines. `AddToCartUseCase` also refuses non-`AVAILABLE` items, so no add path can bypass the rule. The cart "Compartir" button is disabled only while a flagged line remains (the empty-form case, CAR-031, stays in wave 2).

## 5. Pairing (D5, D6): VIN-031, VIN-032, VIN-030

- [x] 5.1 `core:network`: set `pingIntervalMillis = 20_000` on the `WebSockets` plugin (check it does not break the Deepgram live socket, or apply it only to the pairing client). `PairingSocketClient` tells apart "handshake rejected" (an HTTP status on upgrade) from "closed / failed".
- [x] 5.2 `core:network` / `core:data`: an exchange result that tells apart 200 / 409 not approved / 409 rejected / 410 expired / 410 already used / 404 / network error (parse the error `code`).
- [x] 5.3 `core:data`: `observeStatus` reconnect loop: backoff 1 / 2 / 4 / 8 s while the code is valid; immediate reconnect when `NetworkConnectivityState` comes back online; a rejected handshake triggers one fallback exchange. No periodic calls to the exchange endpoint.
- [x] 5.4 `core:data`: single-exchange guard (`Mutex` + consumed flag); retry the exchange only on connectivity errors; never retry 410 `ALREADY_USED`.
- [x] 5.5 `PairingViewModel`: a socket failure keeps the waiting state; `start()` resumes a held valid code (sessionId + `elapsedRealtime` expiry) instead of generating a new one; `stop()` keeps the code; a 410 `ALREADY_USED` without our own success → error with "Generar nuevo código".
- [x] 5.6 Unit tests (fake socket + `MockEngine`):
  - The socket completes without a final status → reconnects with backoff → APPROVED → one exchange.
  - Offline, then online → immediate reconnect.
  - APPROVED twice (after a reconnect) → one exchange.
  - Handshake rejected + fallback answers 200 / 409 not approved / 409 rejected / 410 expired.
  - Exchange network error → retried; 410 already used → not retried.
  - Zero exchange calls while waiting.
  - `start` / `stop` / `start` within the TTL → same code and no second generate call; after the TTL → new code.
  - Done 2026-10-29. The 20 s ping is sent by `PairingSocketClient` itself, not set on the shared `WebSockets` plugin, so the Deepgram voice socket is unchanged. A refused upgrade (`WebSocketException` / `ResponseException` before the socket opened) becomes `PairingHandshakeRejectedException`. The exchange returns `PairingExchangeResponse` with the body `errorCode`; without one, 409 is read as not approved and 410 as expired. "Back online" is a new `ConnectivitySnapshot.hasInternet` (offline → online transition). The exchange retries connectivity and timeout errors up to 4 times (1 / 2 / 4 / 8 s). The single-exchange guard caches the device per session, so the fallback 200 and a later `APPROVED` share one request. The held code expiry uses a new `ElapsedRealtimeClock` (`core:common`), so screen-off time counts. Exchange failures show a new "link" error with "Generar nuevo código"; code generation failures keep "Reintentar".

## 6. Kiosk (D7, D8): KIO-041, EXP-002/007, EXP-001/006/010

- [x] 6.0 Never apply the kiosk lock from the seed value: `MainViewModel.kioskLock` starts as `null` / `Unknown` and `ApplyKioskLock` waits for the real state (task 1.1 found that the INACTIVE seed unpins on every start). Unit test: the first emission of `kioskLock` before settings load is not `INACTIVE`.
- [x] 6.1 Implement the trampoline restart chosen in 1.1: `RestartActivity` (`:restart` process, **opaque app theme**, `screenOrientation="landscape"`), launched from `MainActivity` with an Activity context, carrying the main pid; kill main → start `MainActivity` with `CLEAR_TOP` → finish → exit. `AppRestarter.restart()` emits a request that the shell carries out.
- [x] 6.2 Pending restart ack: store the command id in DataStore before restarting; ack it after the heartbeat starts on the next launch; drop it after 10 min. Update `ProcessDeviceCommandsUseCase` and `FakeAppRestarter`.
- [x] 6.3 Ask for pinning again on the route change to Home and on a touch on Home while not pinned (throttled to 15 s).
- [x] 6.4 `core:ui` `KioskImmersiveDialogEffect` + `LocalKioskImmersive`; apply it in every app dialog; reapply immersive mode in `MainActivity.onWindowFocusChanged(true)`.
- [x] 6.5 Unit tests: `ProcessDeviceCommandsUseCase` (restart stores the id and sends no ack before restarting; pending id acked on start; stale id dropped); pinning throttle logic (pure function).
  - Done 2026-10-29. `kioskLock` is `StateFlow<KioskLockState?>` seeded with `null`; `ApplyKioskLock` skips `null`. `AppRestarter.restart()` hands the restart to the shell through `observeRestartRequests()` when the shell is started (collected with `flowWithLifecycle(STARTED)`); with no visible shell it relaunches directly as before. `DiscoveryApp` starts the heartbeat, commands and settings sync only in the main process, and injects them as `Lazy` so the `:restart` process builds none of them. The pending id lives in the `store_session` DataStore with its save time (`java.time.Clock`); it is acked after the first heartbeat of the new process, and dropped without ack when older than 10 min or in the future. The same restart id delivered again to the relaunched process is not restarted again (no loop). Pinning on resume is always asked; on the route change to Home and on touches on Home it goes through `PinningRequestThrottle` (15 s, `DeviceConstants.PINNING_REQUEST_MIN_INTERVAL`), and the resume request also starts the window. `KioskImmersiveDialogEffect` hides the bars on the dialog window and again on focus regained and after a layout in which they became visible (keyboard closing). Applied in `ConfirmationDialog` (close-confirm and unpair), cart, product share, settings PIN and Wi-Fi password. Emulator checks are in 11.3 / 11.4.

## 7. Catalog microphone (D11): CAT-030, VOZ-009

- [x] 7.1 `ProductSearchViewModel.init`: Voice + blank query → `startVoiceCapture()` instead of `performSearch`.
- [x] 7.2 Unit tests: Voice + "" → voice session started and no search call; Voice + "taladro" → search as before.
  - Done 2026-10-29. With no previous screen to restore, a voice entry that hears nothing, fails or is cancelled emits a new `ProductSearchUiEffect.NavigateBack` (wired to `popBackStack()` in the shell), so it goes back to the Catalog instead of leaving the initial `Searching` spinner with no request in flight.

## 8. Settings PIN lockout (D9): AJU-011

- [x] 8.1 `core:domain`: `SettingsConstants.PIN_MAX_ATTEMPTS = 5` and `PIN_LOCK_DURATION = 24.hours`; `SettingsPinAttemptsRepository` port; `RegisterPinFailureUseCase`, `ResetPinAttemptsUseCase`, `ObservePinLockUseCase` (state: attempts left / locked with time left), with a `Clock` injected for tests.
- [x] 8.2 `core:data`: DataStore implementation (`failedCount`, `lockedAt`, `pinFingerprint`); clear the lock when the remote settings bring a different PIN fingerprint (hash, never the PIN itself) or remove the PIN.
- [x] 8.3 `SettingsViewModel` / PIN dialog: keypad disabled while locked; "Demasiados intentos. Podrás intentarlo de nuevo en HH:mm h" refreshed every minute; only wrong PINs count; new strings and previews (locked / not locked).
- [x] 8.4 Unit tests (test clock):
  - 4 failures → not locked; the 5th → locked for 24 h.
  - 23 h 59 m → still locked; 24 h → unlocked with 5 attempts.
  - A success after 3 failures resets the count.
  - The lock read back from storage by a new instance.
  - Clock moved back → still locked from the new time.
  - Remote PIN changed or removed → lock cleared.
  - Network error → not counted.
  - Done 2026-10-29. The rules live in `PinLockPolicy` (internal to `core:domain`), shared by the three use cases. `ObservePinLockUseCase` re-emits every minute while locked (`SettingsConstants.PIN_LOCK_REFRESH_INTERVAL`) and clears the stored lock once 24 h have passed. The fingerprint is a SHA-256 of the stored bcrypt hash, written with every attempt; the lock is lifted lazily: when the current PIN hash no longer matches (new PIN or removed), the attempts read as none, so no hook in the settings sync is needed. Failures while already locked are not counted again and do not extend the lock. The time left is rounded up to whole minutes (never "00:00" while locked). A check that could not run still shows "PIN incorrecto" but is not counted. `java.time.Clock` is provided by `ClockDataModule`; tests use the new `MutableClock` in `core:testing`. The "new instance" data test reads a copy of the DataStore file, because DataStore forbids two live stores on one file.

## 9. Log redaction (D10): RED-060 (app side)

- [x] 9.1 `LogRedactor` in `core:network` plus `sanitizeHeader(Authorization)` in `KtorClientFactory`.
- [x] 9.2 Unit tests (including `exitPinHash`): masks each listed field in requests and responses, keeps the other fields, and leaves non-JSON bodies unchanged.
  - Done 2026-10-29. `LogRedactor` (internal, `core:network`) masks string, number, boolean and null values of the listed members, including escaped quotes and pretty-printed JSON, and leaves fields that only contain a listed name (`tokenExpiresAt`, `phoneVerified`) alone. Every logged line goes through it; `sanitizeHeader` masks `Authorization`. The field list is pinned by a test. Checked in logcat in 11.3.

## 10. Landscape only (D12): business rule, VIN-030, VIN-052, CAT-027, DET-024, CAR-048

- [x] 10.1 `app/AndroidManifest.xml`: `android:screenOrientation="landscape"` on `MainActivity` and `RestartActivity`.
- [x] 10.2 `<application>`: `<property android:name="android.window.PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY" android:value="true" />` with a comment saying the opt-out ends at target SDK 37.
- [x] 10.3 `docs/android/tablet-kiosk-setup.md`: turn auto-rotate off and set landscape on the device (UI steps and `adb shell settings put system accelerometer_rotation 0` / `user_rotation 1`), plus the target-SDK-37 note.
- [x] 10.4 Update `docs/handoff/qa-2026-10-25-casos-observados-por-flujo.md`: the portrait cases move out of wave 3 (closed by the rule).
  - Done 2026-10-29. `RestartActivity` was already landscape (group 6). Both activities carry `tools:ignore="DiscouragedApi,LockedOrientationActivity"` for lint. Checked in the merged `profile` manifest (property and both orientations present). The handoff table has no wave sections, so the four portrait rows are marked closed in place, with a note above the table; the rotation part of VIN-030 also goes away, and the rest of it is D6. Emulator rotation checks (API 26 and API 36) are in 11.3.

## 11. Verification (QA by Claude)

- [x] 11.1 `./gradlew testDebugUnitTest` (JDK 21 JBR) green; fix any regression in existing tests.
- [x] 11.2 `./gradlew lint assembleDebug assembleProfile` green.
- [ ] 11.3 Emulator QA (`profile` APK on `Medium_Tablet` API 26 and the API 36 tablet AVD), automated with adb (uiautomator dump, screenshots, logcat). Per case:
  - Landscape: on both AVDs, on every screen (onboarding, pairing, Home, catalog, search, detail, cart, settings), set `user_rotation 0` / `2` with auto-rotate off and on → still landscape (`dumpsys window | grep -i rotation` + screenshot), and the activity is not recreated (no new `onCreate` in logcat).
  - Pairing (no approval needed): a code on screen for more than 90 s → the socket is still open or reconnected (logcat), zero calls to `/token` while waiting; `svc wifi disable` / `enable` → immediate reconnect, same code.
  - CAT-030: Catalog mic shows the listening state; no `422` in logcat.
  - CAR-008 / CAR-032 / DET-032: share against production (Meta currently answers `FAILED`) shows the not-delivered message, never a success.
  - DET-034 / CAR-011: one request per 4xx (count in logcat).
  - CAR-040 / CAR-041: fill the form, wait 60 s → Home, reopen the cart → empty basket and fields. Same on Home with items.
  - CAR-042: keep typing for 90 s with `adb shell input text` in pieces → basket kept.
  - DET-013 / CAR-023 / CAR-025: product `LOW_STOCK` or inactive (from data in production or a test product the user deactivates) → actions disabled; injected 400 → lines flagged.
  - VIN-030: screen off / on with `input keyevent 26`, and leaving and coming back to pairing → same code (uiautomator).
  - AJU-011: 4 wrong PINs → still open; the 5th → locked with about 24 h shown; `am force-stop` + relaunch → still locked; emulator reboot → still locked; move the emulator clock forward 24 h → unlocked with 5 attempts.
  - EXP-001 / 006 / 010: with kiosk active, open the cart, the share dialog and the keyboard → `dumpsys window` shows the bars hidden; screenshot.
  - EXP-002 / 007: refuse the pinning dialog, go back to Home → asked again; not again within 15 s.
  - RED-060: `logcat | grep` for the test name and phone → no matches; `***` present.
- [ ] 11.4 User-assisted QA (needs the staff app or an OWNER token):
  - VIN-031: approve after 70 s → pairs.
  - VIN-032: `svc wifi disable` for 20 s while approving → pairs when the network comes back, with a single `/token` call.
  - AJU-011: with the gate locked, the staff app changes the exit PIN → lock lifted.
  - KIO-041: `RESTART_APP` while pinned → `pidof` changes, still pinned, command ACKED only after the relaunch.
  - Live `kioskMode` toggle regression.
- [ ] 11.5 Regression sweep of the wave-1 flows: order happy path, catalog → detail → cart, voice from Home, settings ↔ Home 10 times while pinned.
- [ ] 11.6 Complete `qa-report.md` (before / after / evidence for each case ID), note what stays blocked by backend, and update `docs/handoff/qa-2026-10-25-casos-observados-por-flujo.md` with the status of each wave-1 case.
