## Context

QA run 2026-10-25 (v2) on the Discovery tablet. This change covers wave 1: the critical and high cases that the app can fix without the backend. The current state of the code behind each case:

| Case(s) | Root cause in the app |
|---|---|
| DET-032, CAR-007/032 | `CartShareRepositoryImpl` maps every `201` to success and never reads `status` (`SENT` / `STUB` / `FAILED`). |
| CAR-008 | Cart "Compartir" reuses the order success screen, so its title is "¡Pedido generado con éxito!". |
| DET-034, CAR-011 | `shareWithRetry` (duplicated in `CartViewModel` and `ProductDetailViewModel`) retries any failure 3 times, including deterministic 400/422. |
| CAR-040/041 | `CartDialog` is hosted outside the `NavHost`, so `hiltViewModel()` scopes `CartViewModel` to the Activity and name/phone live as long as the process. The inactivity rule only clears the basket, never applies on Home, and does not leave the screen. |
| CAR-042 | The inactivity timer listens to pointer events on the main window only. Dialogs are separate windows and the IME sends no pointer events, so typing in the cart dialog does not reset the timer. |
| DET-013, CAR-023/025 | `ProductDetailDto.active` is not mapped to the domain. Add-to-cart allows any availability, but the server accepts only `active && AVAILABLE`. Order/share 400 errors fall into the generic error, and "Entendido" goes back to the same cart, so the error repeats. |
| VIN-031/032 | **App-side (confirmed in task 1.2):** `KtorClientFactory` sets `socketTimeoutMillis = 60_000`, and the server sends nothing after the first `WAITING`. The CIO engine ends the idle socket at 60 s and `PairingSocketClient.observe` *completes normally*, so the tablet keeps waiting silently. A dropped socket goes to the Error phase. There is no reconnection. |
| VIN-030 | The manifest sets no orientation, so the activity rotates. `PairingScreen` calls `start()`/`stop()` from a `DisposableEffect`; rotation (and leaving and coming back) disposes and recomposes the screen, and `start()` always generates a new code. |
| VIN-052, CAT-027, DET-024, CAR-048 | Same cause: the app can be shown in portrait, and the layouts are landscape-only. |
| KIO-041 | `AppRestarterImpl` calls `startActivity(makeRestartActivityTask)` from the application context and then `exitProcess(0)`. While the task is pinned, the relaunch is lost, the pinned task dies and the tablet is left on the launcher. The ack was already sent, so the command shows as ACKED. |
| EXP-002/007 | Pinning is requested only from `LifecycleResumeEffect`. A refused "Pin app?" dialog is not asked again when the customer goes back to Home. |
| EXP-001/006/010 | Bars are hidden on the activity window only. Compose `Dialog` windows, the IME and window focus changes show the bars again. |
| CAT-030 | Catalog mic navigates to Search with `query = ""` and `source = Voice`. `ProductSearchViewModel.init` always calls `performSearch(initialQuery)`, so the server returns 422 and the screen returns Home after the idle timeout. |
| AJU-011 | `SettingsViewModel.submitPassword` has no attempt counter. |
| RED-060 | `KtorClientFactory` logs `LogLevel.BODY` with no redaction. The `profile` build (the one QA installs) enables it through `ENABLE_NETWORK_LOGGING`. |

Constraints: no Device Owner, the app is not a launcher (`kiosk-lock-mode` design). Clean Architecture + MVI, feature modules cannot depend on each other, and there is no new library or Gradle module. Only API fields and endpoints that already exist are used.

## Goals / Non-Goals

**Goals:**
- The tablet never claims a delivery, order or success that did not happen.
- A new customer never sees the previous customer's basket or data.
- Pairing completes even if the socket closes or the network drops for a moment.
- `RESTART_APP` leaves the tablet on Discovery, and pinning and immersive mode are restored automatically.
- Every fix is covered by unit tests and checked by me on the emulator before the PR.

**Non-Goals:**
- Backend causes: order/PDF price (B-01, B-02), inactive products still indexed in search (B-14), nginx timeout, orphan pairing codes (VIN-022), Meta registration, cleartext HTTP.
- A QR or link to `pdfUrl` as a fallback when WhatsApp fails (product decision, see Open Questions).
- Wave 2 and 3 items: navigation and back-stack duplicates, the error copy of the share dialog (CAR-033), local cart validations (CAR-020/021/024) and accessibility. Portrait layouts are not fixed; portrait is removed instead (D12).
- Removing the "Pin app?" dialog. That requires Device Owner, which is out of scope.

## Decisions

### D1. One share outcome model, one share use case

Add `ShareOutcome` to `core/domain`:
- `Delivered(pdfUrl)` when `status == SENT`.
- `NotDelivered(pdfUrl, reason)` when `FAILED` or `STUB`.
- `Rejected(kind)` for 4xx: `Unavailable` (400), `NotFound` (404), `Invalid` (422).
- `Failed(category)` for network errors, timeouts and 5xx.

`ShareQuoteUseCase` holds the retry policy (up to 3 attempts, only for `Failed`) and replaces the two private `shareWithRetry` copies. `CartShareRepositoryImpl` maps the status and the HTTP codes.

*Alternative:* keep `Result<CartShare>` and check `status` in each ViewModel. Rejected, because it would duplicate the rule in two features again.

UI mapping:
- Cart "Compartir" and detail share: `Delivered` → success screen with share copy ("¡Cotización enviada!"). Any other outcome → error state. Its copy for `NotDelivered` says that the quote could not reach their WhatsApp and asks them to go to the seller. Refining the error copy in general is wave 2 (CAR-033).
- "Realizar pedido": the order success stays. `shareDelivered = outcome is Delivered`, so `NotDelivered` now shows the existing "go to the seller and give your name" fallback.
- `STUB` is treated as not delivered. It only happens on dev or test servers, where showing it honestly helps.

### D2. Customer session as an app-level concept

- `core/domain`: `CustomerSessionRepository` port with `sessionEnded: Flow<Unit>`. `EndCustomerSessionUseCase` clears the basket and emits `sessionEnded`. The existing "Aceptar", "Cancelar orden" and timeout paths all go through it.
- `core/data`: in-memory singleton implementation, not persisted. A process restart already starts with an empty basket.
- `CartViewModel` and the detail share form reset their form and submission when `sessionEnded` fires, so the Activity-scoped `CartViewModel` no longer carries data across customers.
- `MainViewModel` owns the inactivity timer, driven by an activity stream. When the timer fires it calls `EndCustomerSessionUseCase`. The shell then closes the cart, share and settings overlays and pops to Home.
- The timer runs off Home always, and on Home only while the basket is not empty. `INACTIVITY_TIMEOUT_SECONDS` stays at 60.

*Alternative:* scope `CartViewModel` to the dialog with its own `ViewModelStoreOwner`. Rejected because it resets the form on every close and reopen inside the same visit, and it does not fix the timeout or the Home case.

### D3. Activity reporting that sees dialogs and the keyboard

`core/ui` adds `LocalUserActivity` (a CompositionLocal holding `() -> Unit`) and `Modifier.reportUserActivity()`, which observes pointer down events in the Initial pass without consuming them. The shell provides the callback, which bumps the `MainViewModel` timer.

- Every dialog content root (cart, product share, settings PIN, close-confirm dialogs) applies the modifier.
- Text fields in dialogs also call the callback from `onValueChange`, so typing on the IME counts as activity.
- The existing main-window `pointerInput` stays.

*Alternative:* `Activity.onUserInteraction()`. Rejected, because it is not called for Dialog windows.

### D4. Orderable products

- Map `ProductDetailDto.active` → `ProductDetail.active`.
- `IsOrderableUseCase(availability, active?)` returns true only when the product is `AVAILABLE` and `active != false`. List endpoints do not send `active`; the catalog already filters it out, and search is B-14 on the server.
- Detail: when the product is not orderable, add and share are disabled, the stepper is hidden or disabled, and a "No disponible por ahora" note is shown.
- Search, catalog and related products: the quick-add control is disabled for non-orderable cards. Tapping the card still opens the detail.
- Cart: when `POST /orders` or share returns 400 or 404, `RevalidateCartUseCase` calls `GET /products/{id}` for each line, with a small concurrency limit and at most 20 lines. Lines that are inactive, not `AVAILABLE` or 404 are marked unavailable. The cart shows a specific message and a per-line "No disponible" state. Order and share stay disabled until those lines are removed. If the recheck finds no culprit, the app falls back to the generic error.
- `OrderErrorCategory` and the share `Rejected.kind` gain `Unavailable`. The 400 body `code` is parsed best effort and is not required.

### D5. Pairing status: a WebSocket that stays alive and reconnects, a single exchange

The contract says the pairing WebSocket *"al conectar emite el estado actual"*. A reconnect therefore recovers any status that was pushed while the tablet was offline, so no polling is needed.

- **Keep-alive.** The Ktor `WebSockets` plugin uses `pingIntervalMillis = 20_000`. The server answers each ping with a pong, so the client's 60 s socket read timeout never fires. **Task 1.2 result (production, 2026-10-28):**
  - A plain WebSocket client without ping stayed open for 150 s, so nginx does not close it.
  - The app's own client config closed at 60.2 s with a normal completion.
  - The same config with a 20 s ping was still open at the 100 s cap.
  - Every connect and reconnect received the current status (`WAITING`) right away.
  The ping is the fix. Raising the socket timeout only for WebSockets would also work, but it would hide a dead connection for longer.
- **Reconnect loop** in `DevicePairingRepository.observeStatus(sessionId)`. If the socket completes or throws before a final status, reconnect with backoff (1 s, 2 s, 4 s, then 8 s at most) while the flow is collected and the code is valid. When `NetworkConnectivityState` goes back online, the app reconnects immediately without waiting for the backoff (VIN-032). `WAITING` is passed through, and a final status ends the loop.
- **Handshake rejected.** The server rejects the handshake when the session does not exist or has expired. In that case the app calls the exchange **once** as a fallback:
  - 200 → paired. This covers an approved session that the server no longer accepts on the socket.
  - 409 `PAIRING_NOT_APPROVED` → keep reconnecting.
  - 409 `PAIRING_REJECTED` → `Rejected`.
  - 410 `PAIRING_TOKEN_EXPIRED` / 404 → `Expired` (new code).
  The fallback runs once per rejected handshake. It is never periodic.
- **Single exchange.** `APPROVED` (from the socket or the fallback path) triggers `GET /pairing-session/{id}/token` exactly once, guarded by a `Mutex` and a "consumed" flag. A connectivity error during the exchange is retried; a 410 `ALREADY_USED` is not retried. If our own exchange never succeeded, a 410 `ALREADY_USED` is shown as an error with "Generar nuevo código", and the backend note about orphan devices applies.
- **ViewModel.** A socket failure no longer moves to the Error phase. The screen keeps waiting with the countdown running. Error is shown only when code generation fails or the exchange fails.

*Alternative rejected:* polling `GET /pairing-session/{id}/token` every 5 s. Load is not the problem (at most about 60 calls per pairing). The problem is that the endpoint is a one-shot exchange, not a status read: a 200 lost while the network is unstable (exactly the VIN-032 case) consumes the token, the next call gets 410 `ALREADY_USED`, and the server keeps an ACTIVE device the tablet can never use. It is also a GET that changes server state, which retries and proxies assume it does not do.

**Backend hardening (optional, independent of this change):**
1. Make the exchange idempotent for a short window, for example returning the same token for 60 s. That removes the orphan-device risk with any client design.
2. ~~nginx `proxy_read_timeout`~~ Not needed: task 1.2 showed the proxy keeps idle sockets open (150 s observed).
3. Only if periodic status checks are ever wanted: a side-effect-free `GET /pairing-session/{id}/status`.

### D6. Keep the pairing code across recreation

With the landscape lock (D12) rotation no longer happens, but the pairing screen is still disposed and recomposed when the screen turns off and on, when the merchant leaves and comes back, and on other configuration changes (dark mode, font size). `PairingViewModel` keeps `sessionId` and the absolute expiry, measured with `SystemClock.elapsedRealtime`. `start()` checks for a held code first. If one is still valid, it resumes the countdown and the status subscription. A new code is generated only when there is none, it has expired, or the merchant taps "Generar nuevo código". `stop()` cancels the jobs but keeps the held code.

### D7. Restart without leaving the pinned task

Add `RestartActivity` to `:app`. It runs in a separate process (`android:process=":restart"`), uses the app's **opaque** theme (not translucent, see the task 1.1 findings) and `screenOrientation="landscape"`. It is launched from `MainActivity` with an Activity context (no `NEW_TASK`), so it joins the pinned task. Its `onCreate`:
1. Kills the main process by the pid passed in the intent.
2. Starts `MainActivity` with `CLEAR_TOP` in the same task.
3. Finishes itself and exits its own process.

The pinned task always has a live activity, so pinning holds and no "Pin app?" dialog is shown.

- `AppRestarter.restart()` now emits a one-shot restart request that the shell (`MainActivity`) carries out, because it needs an Activity context.
- **Ack after relaunch:** before restarting, the command id is saved in DataStore (`pendingRestartAckId`). On the next start, after the heartbeat loop starts, the app acks it with `success = true` and clears it. If the relaunch never happens, there is no ack and the server expires the command, which is the honest result. A pending id older than the server's 10-minute expiry is dropped without an ack.
- **Fallback (decided by the spike in task 1.1).** If the emulator shows that a separate process cannot join the pinned task, use the D6 fallback from `kiosk-lock-mode`: `stopLockTask()`, restart, and pin again on start. That fallback accepts that the pin dialog shows once.

*Alternative:* soft restart (recreate the Activity and reset singletons). Rejected because it is not a real process restart, which is what `RESTART_APP` means for the merchant.

**Task 1.1 results (`Medium_Tablet`, API 26, 2026-10-28). The trampoline is chosen and the fallback is not needed.**
- **Current restart (KIO-041 reproduced).** The system logs `startActivityUnchecked: Attempt to violate Lock Task Mode`, the process dies, the lock is lost and the launcher is shown. Evidence: `qa-evidence/KIO-041-before-logcat.txt`.
- **Translucent trampoline.** It crashes on API 26 with `IllegalStateException: Only fullscreen opaque activities can request orientation` (an Android 8.0 platform bug: translucent activities cannot fix their orientation). **The trampoline must be opaque**, which also matches the landscape rule (D12).
- **Opaque trampoline.** It runs in the same task, the main process is killed, the system logs `Not finishing task in lock task mode`, and `MainActivity` starts in a new process in the same task. **The task stays pinned.** One more problem showed up here, see the next point.
- **The app unpins itself on start.** `MainViewModel.kioskLock` is seeded with `KioskLockState.INACTIVE` in `stateIn`, and `ApplyKioskLock(false)` calls `stopLockTask()` before the real state is read. The app then pins again and the "Pin app?" dialog shows, where a customer can refuse it. **Fix (part of D8):** the shell applies the lock only after the real state has been read (a `null` / `Unknown` seed is never applied). With that change, 3 of 3 restarts out of 3: new pid each time, still `PINNED`, zero `stopLockTaskMode` calls, Home shown with no dialog. Evidence: `qa-evidence/KIO-041-spike-trampoline-fixed-logcat.txt` and `.png`.
- The same unpin also happens on any cold start while pinned, so this fix is needed even without `RESTART_APP`.

### D8. Pinning asked again on Home, bars hidden everywhere

- The lock is never applied from a seed value: `MainViewModel` exposes `StateFlow<KioskLockState?>` (or an `Unknown` state) and `ApplyKioskLock` does nothing until the real state arrives. Otherwise the INACTIVE seed calls `stopLockTask()` on every start (found in task 1.1).
- Pinning is requested on resume (as today), when the route changes to Home, and on the first touch on Home while not pinned. That last trigger is throttled to once every 15 s so a customer who refused the dialog does not get it in a loop.
- `core/ui` gets `KioskImmersiveDialogEffect`: inside a Compose `Dialog` it gets the dialog window from `LocalView.current.parent as DialogWindowProvider` and hides the system bars there with the same behavior as the activity. A `LocalKioskImmersive` boolean switches it on.
- `MainActivity.onWindowFocusChanged(true)` applies the immersive state again, which covers the keyboard closing and the system dialogs going away.

### D9. Settings PIN lockout: 5 attempts, then 24 hours

`SettingsPinAttemptsRepository` (DataStore) stores `failedCount`, `lockedAt` (wall-clock epoch) and the `pinFingerprint` the lock applies to. It has to survive restarts, so it lives in DataStore.

- **Lock.** On the 5th consecutive wrong PIN, save `lockedAt = now`. The lock lasts 24 h (`SettingsConstants.PIN_LOCK_DURATION = 24.hours`).
- **While locked,** the keypad is disabled and the dialog says "Demasiados intentos. Podrás intentarlo de nuevo en HH:mm h" (time left, refreshed every minute).
- **After 24 h,** the lock and the count are cleared and the merchant gets 5 new attempts.
- **A correct PIN** before the lock resets the count.
- **Clock guard.** The customer cannot reach the system clock while the tablet is pinned, but the clock can still jump (NTP, battery). If `now < lockedAt`, the lock restarts from `now`. A clock moved back never shortens it.
- **Recovery path.** A 24 h lock could leave the merchant out of settings (Wi-Fi, pairing, exiting kiosk). When the staff app sends a new or removed exit PIN (`settingsVersion` → a different PIN fingerprint, a hash rather than the PIN itself), the lock and the count are cleared. The merchant authenticates in the staff app, so this does not weaken the protection.
- **Counted attempts** are wrong PINs only; network or validation errors do not count.

*Alternatives:* a counter in memory only (rejected: a restart or `RESTART_APP` would reset it); escalating short locks (replaced by the business rule of a fixed 24 h).

### D10. Log redaction

`KtorClientFactory` installs `Logging` with `sanitizeHeader { it == Authorization }`, and the logger passes each body through `LogRedactor`. `LogRedactor` replaces the JSON string values of `customerName`, `customerPhone`, `phone`, `deviceAccessToken`, `accessToken`, `token`, `key` and **`exitPinHash`** with `"***"`. Baseline QA found `exitPinHash` in logcat; with 4-digit PINs the hash gives the PIN away. The level stays `BODY` in `profile`, so QA still sees the shape of each call. A unit test pins the list of fields.

### D11. Catalog microphone

`ProductSearchViewModel.init`: when `initialSource == Voice && initialQuery.isBlank()`, call `startVoiceCapture()` instead of `performSearch`. The catalog code does not change.

### D12. Landscape only (business rule)

- **Manifest.** `android:screenOrientation="landscape"` on `MainActivity` and on `RestartActivity` (D7). Fixed landscape, not `sensorLandscape`: the product rule is "no rotation". If the stand ever mounts the tablet upside down, `sensorLandscape` is a one-line change.
- **Android 16 (API 36) on large screens.** For apps that target SDK 36, the system ignores `screenOrientation`, `resizeableActivity` and aspect-ratio limits on displays with a smallest width of 600 dp or more, which includes our tablets. The opt-out for API 36 is the manifest property `android.window.PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY = true` on `<application>`. Google has said this opt-out goes away at target SDK 37, so the property is commented with that deadline and listed in the setup guide.
- **Second layer on the device.** The tablet setup guide (`tablet-kiosk-setup.md`) adds turning auto-rotate off and setting the rotation to landscape (`settings put system accelerometer_rotation 0` and `user_rotation 1` for provisioning by adb). This covers any OEM or future version that ignores the app.
- **No portrait work.** The portrait layout cases (VIN-052, CAT-027, DET-024, CAR-048) are closed by this rule and leave wave 3.

*Alternative rejected:* `setRequestedOrientation()` at runtime. The system ignores it in the same way on Android 16 large screens, and it causes one rotation flash at startup.

## Verification strategy (QA by Claude)

1. **Unit tests (JVM)** for every new or changed use case, repository and ViewModel, using fakes from `core:testing` and Ktor `MockEngine` for status, HTTP codes, polling and the socket. This is the DoD gate: `./gradlew testDebugUnitTest` (JDK 21 JBR).
2. **Static checks:** `./gradlew lint assembleDebug assembleProfile`.
3. **Production check of the pairing socket (before coding D5):**
   - Generate a pairing code on production.
   - Open the WebSocket from a small script with a ping every 20 s. Confirm it is still open after 90 s.
   - Open it again without pings. Confirm the close at about 61 s.
   - Reconnect and confirm the server sends the current status on connect.
   This needs no approval and does not create any device.
4. **Emulator QA** with the `profile` APK on two AVDs: `Medium_Tablet` (API 26, same as the earlier kiosk findings) and a new API 36 tablet AVD (Android 16, to confirm the landscape opt-out). Driven through adb:
   - `uiautomator dump`, screenshots and logcat for each case ID.
   - Rotation attempts with `settings put system user_rotation 0` (portrait) and `content insert` on the rotation setting. The app must stay in landscape (`dumpsys window | grep -i rotation`, screenshot).
   - Network cut with `svc wifi/data disable`.
   - Pinning state with `dumpsys activity activities | grep -i locktask`.
   - Process ids with `pidof` to confirm a real restart.
   - A grep of logcat for the test name and phone to confirm redaction.
5. **Cases that need the merchant side** (staff app or an OWNER token): pairing approval after 61 s or after a network cut, `RESTART_APP`, `kioskMode` toggles, and a remote PIN change to lift the 24 h lock. I prepare the tablet state and the commands, and the user does the approve or send step. If an OWNER token is provided, I can call the staff endpoints with curl myself.
6. The 24 h lock is checked with a test clock in unit tests. On the emulator I check the lock itself and that it survives a restart, without waiting 24 h. The expiry is checked by moving the emulator clock forward (`date` with root on the API 26 image, or `cmd alarm set-time` where available).
7. Each case ID gets a result (✔ / ✖ / blocked) in `openspec/changes/qa-wave-1-critical-fixes/qa-report.md`, with evidence (screenshot path, log extract).

## Risks / Trade-offs

- **[The separate-process trampoline may not keep the pinned task on some OEMs]** → Spike on the emulator first (task 1.1), with the documented fallback. The target device is a Lenovo Tab M11, so that check is on the user's checklist.
- **[The ping also affects the Deepgram live socket]** The same `HttpClient` is used for voice. A 20 s ping is harmless for Deepgram, but task 5.1 checks it (or scopes the ping to the pairing client).
- **[The server stops accepting the socket after `APPROVED`]** → The fallback single exchange on a rejected handshake covers it.
- **[An exchange response lost in transit]** → The token is consumed and the tablet cannot get it again. The app shows an error with "Generar nuevo código", and the orphan device stays on the server. Only the backend can fix this (idempotent exchange); the risk is lower than with polling because there is only one exchange.
- **[Android 16 opt-out removed at target SDK 37]** → Comment in the manifest and in the setup guide, and the device rotation lock as a second layer. Raising the target to 37 must handle this first.
- **[24 h PIN lock leaves the merchant out]** → A new or removed PIN from the staff app lifts the lock. The lock message tells the merchant the time left.
- **[Home timeout with a basket may surprise a slow customer]** → Any touch, including inside dialogs, resets the timer. 60 s is already the product rule off Home.
- **[Re-asking for pinning can annoy staff]** → Throttled to once every 15 s and only on Home.
- **[Cart recheck makes N detail calls]** → At most 20 lines (the server limit), with limited concurrency. It runs only after a 400 or 404.

## Migration Plan

App-only change, delivered in one APK. No data migration: new DataStore keys default to empty. Rollback means installing the previous APK.

## Open Questions

- When WhatsApp delivery fails, should the tablet show a QR code to `pdfUrl` so the customer can still get the quote? It needs a QR library (new dependency) and product approval, so it is not in this wave.
- Is a 60 s timeout on Home with a non-empty basket right for the stand, or should Home use a longer value?
- Does the target tablet (Lenovo Tab M11) get Android 16? If it does, the landscape opt-out must be checked on the device itself.
- Should the staff app show that a tablet's settings PIN is locked? That needs a backend field, so it is out of this wave.
