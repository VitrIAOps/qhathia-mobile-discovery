## 1. QA baseline

- [x] 1.1 Build the `profile` APK from `fix/qa-wave-2` before any change (same code as `fix/qa-wave-1`) and reproduce the wave-2 cases on `Medium_Tablet` (API 26) with adb (uiautomator dump, screenshots, logcat): INI-020 (double tap "Ir al catálogo" → two Catalogs), DET-021 / DET-022, CAT-024 / CAT-025 (system Back with items skips the confirmation), BUS-060 (system Back skips the history), INI-051 (no-audio prompt still shown after 2 min; the emulator mic carries no speech, so it is easy to reach), VOZ-004 (`dumpsys audio` / `dumpsys media.audio_flinger` shows a recording client after Home goes to the background), VOZ-008 (revoke `RECORD_AUDIO` with `pm revoke`, tap the Search mic), VOZ-010, CAR-033, CAR-043 / CAR-044 (outside tap while sending), CAR-020 / CAR-031 (empty cart / empty form), CAR-021 (21 products → `422`), VIN-024 on `Tablet_API36` (unpaired), AJU-031 / AJU-050 (paired tablet with Wi-Fi off).
- [x] 1.2 Create `qa-report.md` with one row per case ID: before, after, evidence, who ran it. Share and order checks use the test phone; never tap "Realizar pedido" against production unless the user agrees.
  - Done 2026-10-30. Every case reproduced except AJU-031 / AJU-050 (Settings needs the staff exit PIN; pending with the user) and CAR-021 (reused from the wave 1 baseline). VIN-024: 3 requests for 4 taps, no false error in this run.

## 2. Navigation guard (D1): INI-020, DET-021, DET-022

- [x] 2.1 `:app`: add a small helper that runs an action only when the given `NavBackStackEntry` is `RESUMED`, and use it in every `NavHost` callback in `MainActivity` that navigates or pops (Home → Catalog / Search, Catalog → Detail / Search, Search → Detail / back / Home, Detail → Detail / Search / back / Home, Settings back).
- [x] 2.2 Cart overlay "Ver detalle": close the overlay and navigate once (a second tap after the dialog closed has nothing to act on); check it does not stack two details.
- [x] 2.3 Unit test for the helper (resumed runs, created / started do not). Instrumented tests are not set up in the project; the double-tap cases are checked on the emulator in 9.2.
  - Done 2026-10-30. `NavigationGuard.kt` (`allowsNavigation`, `NavController.fromSettledScreen`); every tap-driven callback in `MainActivity` goes through it (`goHome`, `goBack`, `openDetail`). Not guarded on purpose: the pairing result to Home, the cart's return Home after a result (idempotent pop) and the inactivity timeout.

## 3. System Back follows "Volver" (D2): CAT-024, CAT-025, BUS-052, BUS-060

- [x] 3.1 `features:catalog`: `BackHandler` in `CatalogScreen` sending `CatalogUiEvent.Back`; while the close dialog is shown, Back dismisses it (`DismissDialog`).
- [x] 3.2 `features:product-search`: `BackHandler` sending `ProductSearchUiEvent.Back` (history step, then the close confirmation); Back dismisses the open dialog. While voice capture is running, Back cancels it (same as the cancel control).
- [x] 3.3 `features:product-detail`: `BackHandler` sending `ProductDetailUiEvent.Back`; with the share dialog open, its own dismissal rules apply (group 6).
- [x] 3.4 Unit tests: the existing `Back` event tests cover the decisions; add the dialog-open cases (Back dismisses the dialog) where the ViewModel owns that state.
  - Done 2026-10-30. Back on Search while the mic is open cancels the capture (`onBack`). The close confirmations are `AlertDialog` windows, so Back reaches them first and dismisses them with no extra code.

## 4. Voice (D3, D4): INI-051, VOZ-003, VOZ-004, VOZ-008, VOZ-010

- [x] 4.1 `features:home`: start the `ERROR_IDLE_TIMEOUT_SECONDS` timer on `NoAudioRetry`; cancel it on any mic event. Unit test with the virtual clock: idle after 30 s, no idle after a retry at 20 s.
- [x] 4.2 `features:home`: `ScreenStopped` cancels any voice session in progress (listening, transcribing, slow response) and resets to `Idle`; no search effect is emitted from a cancelled session. Unit test.
- [x] 4.3 `features:product-search`: `LifecycleEventEffect(ON_STOP)` → new `ProductSearchUiEvent.ScreenStopped`, which cancels voice capture and restores `phaseBeforeVoice`. Unit test.
- [x] 4.4 `core:ui`: move the microphone-permission request out of `HomeScreen` into `rememberMicPermissionGate(onGranted, onDenied)` plus the shared "Necesitamos acceso al micrófono…" string; Home uses it with no change in behavior.
- [x] 4.5 `features:product-search`: the mic button goes through the permission gate; refusal shows the `Toast`.
- [x] 4.6 `features:product-detail`: `ActivateMic` emits a navigation effect to Search with an empty query and `SearchSource.Voice` (same hand-off as Catalog); wire it in `ProductDetailNavigation` / `MainActivity` through the guard of 2.1. Unit test for the effect.
  - Done 2026-10-30. Found while implementing: Search starts listening from `init` when opened by voice, so Catalog and Detail also go through the permission gate before the hand-off. Search stopped while opened by voice (nothing to restore) leaves on the next `ON_RESUME` (a navigation while hidden would be dropped by the guard).

## 5. Cart: share errors and actions (D5, D7): CAR-033, CAR-031, CAR-020, CAR-021, CAR-024

- [x] 5.1 `features:cart`: add `OrderSubmission.ShareError`; `share()` maps `Rejected` / `Failed` to it. Render it with the new share-error strings and a single "Entendido" (`DismissError`). `QuoteNotDelivered` loses "Cancelar orden". `OrderSubmission.Error` (order) is unchanged.
- [x] 5.2 `features:cart`: `CartUiState.canSubmit` includes `!isEmpty`; both buttons use `canSubmit` (Compartir no longer uses only `!hasUnavailableLines`).
- [x] 5.3 `core:domain`: `CartConstants.MAX_DISTINCT_LINES = 20`; `AddToCartUseCase` returns `AddToCartResult` (`Added`, `NotOrderable`, `LineLimitReached`). A product already in the basket is always `Added` (quantity capped by the existing rule). Unit tests: 20 → 21st refused, 20 + existing line added, quantity cap 99 kept (CAR-024).
- [x] 5.4 Screens that add (Catalog, Search incl. suggestions, Detail incl. related products): on `LineLimitReached` emit a one-shot effect and show the limit message as a `Toast`. Shared string in `core:ui`.
- [x] 5.5 `CartViewModel` tests: share error state and its "Entendido"; not-delivered without cancel; order error unchanged; empty basket disables both actions; empty form disables Compartir.
- [x] 5.6 Check the product share dialog (`ShareProductDialog`): its error and not-delivered states offer no action that clears the cart; adjust copy if it still uses the order text.
  - Done 2026-10-30, no change: the product share error and not-delivered states already use share copy, and their "Cancelar" only closes the dialog (the cart is untouched). `SubmissionErrorContent` (core:ui) now takes an optional secondary action.

## 6. Dialog dismissal (D6): CAR-043, CAR-044

- [x] 6.1 `features:cart`: `CartDialog` maps `onDismissRequest` from the submission state (`Submitting` → ignore, `Success` → `AcceptSuccess`, error states → `DismissError`, `Idle` → dismiss). The close "X" is disabled while `Submitting`. "Seguir buscando" is already disabled with the other inputs.
- [x] 6.2 `features:product-detail`: same mapping for `ShareProductDialog` (`Submitting` → ignore, `Success` → `AcceptShareSuccess`, errors → `DismissShareError`).
- [x] 6.3 Tests: a dismiss event while `Submitting` changes nothing; dismiss on success behaves as accept (session ended for the cart); reopening after a result shows the summary.
  - Done 2026-10-30. The decision is in the ViewModels (`CartUiEvent.DismissRequest`, `ProductDetailUiEvent.ShareDismissRequest`); an error dismissed this way returns to the cart summary / share form, as "Entendido" does. `FakeCartShareRepository.awaitBeforeAnswer` holds a request in flight for the tests.

## 7. Pairing and settings (D8): VIN-024, AJU-031, AJU-050

- [x] 7.1 `features:pairing`: track the code request in `generateJob`; ignore `GenerateCode` while it is active or the phase is `Loading`; `cancelJobs()` cancels it.
- [x] 7.2 `features:pairing`: add `PairingError.PairedDeviceLoad` with the connection copy and a `RetryPairedDeviceLoad` event that runs `loadPairedDevice()`. Ignore `GenerateCode` when the session is paired. Update the error content so each error shows its own retry.
- [x] 7.3 Unit tests: three fast `GenerateCode` → one request; paired + load failure → `PairedDeviceLoad`; its retry calls `getPairedDevice` again and never `generatePairingCode`.

## 8. Build checks

- [x] 8.1 `./gradlew testDebugUnitTest` (JDK 21 JBR) green.
- [x] 8.2 `./gradlew lint assembleDebug assembleProfile` green; previews (light and dark) for every new or changed state (share error, paired-device load error).
  - Done 2026-10-30: unit tests, `lint assembleDebug assembleProfile` green. Previews added for the cart share error and the paired-device load error (light and dark).

## 9. Emulator QA and report

- [x] 9.1 Install the `profile` APK on `Medium_Tablet` (paired) and `Tablet_API36` (unpaired) and re-run every case of 1.1 for the "after" column.
- [x] 9.2 Navigation: double taps (`input tap` twice within 100 ms) on "Ir al catálogo", a related product and a search result, and "Volver" ×3 → `dumpsys activity` / back-stack checks show a single copy; Back from each screen matches "Volver".
- [x] 9.3 Voice: no-audio prompt back to idle at about 30 s; Home and Search in the background while listening → no recording client in `dumpsys`; Search mic with the permission revoked → system request, then the message on refusal; Detail mic → Search listening.
- [x] 9.4 Cart: share error and not-delivered (Meta `FAILED`) show only "Entendido" and keep the basket; outside tap and Back while sending do nothing; empty basket and empty form disable both actions; 21st distinct product refused with the message.
- [ ] 9.5 Pairing: fast taps on "Generar nuevo código" on `Tablet_API36` → one `POST /devices/pairing-token` in logcat, no error; paired tablet with Wi-Fi off → connection error, "Reintentar" with Wi-Fi back → details, no `pairing-token` request.
  - Partial 2026-10-30: VIN-024 ✔ (1 request for 4 fast taps). The paired-tablet check (AJU-031 / AJU-050) needs the staff exit PIN to open Settings on `Medium_Tablet`; pending with the user.
  - Archived 2026-10-30 with this task open: the AJU-031 / AJU-050 emulator check is tracked in `qa-report.md` and the QA handoff as pending (needs the exit PIN). The code is covered by unit tests.
- [x] 9.6 Regression sweep: wave-1 flows still pass (customer session timeout, share outcome, flagged lines, pinning, bars over dialogs).
- [x] 9.7 Complete `qa-report.md` and update `docs/handoff/qa-2026-10-25-casos-observados-por-flujo.md` with an **Ola 2** status in each wave-2 row.
  - Done 2026-10-30. Every case has its "after" except AJU-031 / AJU-050 (9.5). The handoff has a wave-2 note and an **Ola 2** status in each wave-2 row.
