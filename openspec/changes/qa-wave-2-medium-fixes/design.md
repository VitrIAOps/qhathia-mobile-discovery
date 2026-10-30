## Context

Wave 2 of the 2026-10-25 QA run: the medium cases the app can fix by itself. It builds on `qa-wave-1-critical-fixes` (branch `fix/qa-wave-1`, not merged to `main` yet), so this change starts from that branch. The current state of the code behind each case:

| Case(s) | Root cause in the app |
|---|---|
| INI-020, DET-021, DET-022 | `MainActivity` calls `navController.navigate(...)` and `popBackStack()` straight from the screen callbacks. A second tap during the transition runs again: `navigate` pushes a second copy of the destination, and each extra `popBackStack()` pops one more screen. |
| CAT-024, CAT-025 | Catalog's "Volver" goes through `CatalogUiEvent.Back` (close confirmation when the cart has items). Catalog has no `BackHandler`, so the system Back pops the screen directly, with no confirmation. |
| BUS-052, BUS-060 | Search's "Volver" walks the in-memory result history and only then asks to close. The system Back pops the whole Search screen, so it skips the history and the confirmation. |
| INI-051, VOZ-003 | `HomeViewModel` starts the 30 s return-to-idle timer only for `MicState.Error`. `MicState.NoAudioRetry` ("No te escuché…") has no timer. |
| VOZ-004 | `HomeUiEvent.ScreenStopped` (`ON_STOP`) only resets `MicState.Success`. A running listening or transcribing session keeps going. Search has no lifecycle hook at all. |
| VOZ-008 | Home asks for `RECORD_AUDIO` before `ActivateMic`; Search sends `ActivateMic` straight to the ViewModel. Without the permission, capture fails with nothing on screen. |
| VOZ-010 | `ProductDetailViewModel` maps `ActivateMic` to `Unit`. |
| CAR-033 | The cart uses `OrderSubmission.Error` for both order and share failures, with the order copy ("Hubo un problema generando la orden") and "Cancelar orden", which runs `CancelOrder` → `finishAndReturnHome()` and ends the customer session (basket and form cleared). `QuoteNotDelivered` also offers "Cancelar orden". |
| CAR-043, CAR-044 | `CartDialog` passes `onDismissRequest = onDismiss` with no condition, and `ShareProductDialog` sends `DismissShare` with no condition. Outside tap or Back closes them while `Submitting`. `CartViewModel` is scoped to the Activity (the dialog is outside the `NavHost`), so the request finishes in the background and its result screen is waiting the next time the cart opens. |
| CAR-020, CAR-021, CAR-024 | `CartUiState.canSubmit` checks the form and flagged lines, not an empty basket. Nothing limits distinct lines, and the server rejects more than 20 with `422`. |
| CAR-031 | The cart's Compartir button is enabled with `!hasUnavailableLines` only; an empty or invalid form leaves it enabled and the tap is ignored by `beginSubmission()`. |
| VIN-024 | `PairingViewModel.startPairing()` cancels the countdown and status jobs but not the code request, which runs in an untracked `viewModelScope.launch`. Fast taps run several requests at once; one of them fails and shows "No se pudo generar…". |
| AJU-031, AJU-050 | On a paired tablet, `loadPairedDevice()` failure shows `PairingError.CodeGeneration`. Its "Reintentar" sends `GenerateCode` → `startPairing()`, which requests a pairing code on a paired tablet. |

Constraints: Clean Architecture + MVI; feature modules cannot depend on each other; no new library, Gradle module, endpoint or DTO.

## Goals / Non-Goals

**Goals:**
- One tap produces at most one navigation, and Back never leaves more screens than the customer asked for.
- The system Back and "Volver" behave the same on every screen that has "Volver".
- The microphone never keeps recording off screen, never stays stuck on "No te escuché…", and every microphone button does something visible.
- A dialog with a request in flight cannot be closed, and no result is left behind for the next customer.
- The cart never sends a request the server is known to reject (empty basket, more than 20 lines, invalid form).
- The pairing section never shows a false error, and never generates a code on a paired tablet.

**Non-Goals:**
- BUS-003 / EXP-004 (filter chips): the chips add words to the text the search AI reads; the filtering itself is a backend fix. EXP-005 (chips shown unselected after going back in the history) is low and goes to wave 3.
- DET-012 (B-02), VIN-022 (no endpoint to cancel a pairing session), and other backend causes.
- Low cases (wave 3): accessibility labels, image placeholders, stepper reset (DET-004), phone keyboard (CAR-014), "Pendiente" fields in settings, Wi-Fi details, "TIENDA" title.

## Decisions

### D1. Navigation runs only from the resumed entry (INI-020, DET-021, DET-022)

`MainActivity` wraps every screen callback that navigates or pops in a guard: the action runs only when the `NavBackStackEntry` that owns the callback is `RESUMED`. During a transition the leaving entry is no longer resumed, so the second tap is dropped. The guard is one small helper in `:app` (for example `NavBackStackEntry.ifResumed { … }`), used by the `NavHost` builder lambdas, which already receive the entry.

- Pops use `popBackStack(route, inclusive = false)` or `popBackStack()` only from a resumed entry, so "Volver" ×3 pops once.
- The cart overlay's "Ver detalle" runs from the entry under the dialog, which stays resumed; it closes the overlay first, so a second tap has no dialog to come from.

Alternatives: `launchSingleTop = true` stops a duplicate only when the same route is on top, and does not stop Detail → Detail of two different products or extra pops. A per-button debounce has to be added to every button and still depends on timing.

### D2. The system Back sends the screen's Back event (CAT-024, CAT-025, BUS-052, BUS-060)

Catalog, Search and Detail register a `BackHandler` in their `*Screen` composable that sends the same event as "Volver" (`CatalogUiEvent.Back`, `ProductSearchUiEvent.Back`, `ProductDetailUiEvent.Back`). The ViewModel stays the only owner of the decision (close confirmation, history step, pop). While a confirmation dialog is open, Back dismisses that dialog, as it does today.

Alternative: intercept Back in `MainActivity`. The Activity does not know each screen's rules, and the decision would be split in two places.

### D3. Voice: idle timer for "No te escuché…" and stop on `ON_STOP` (INI-051, VOZ-003, VOZ-004)

- `HomeViewModel` starts the same `ERROR_IDLE_TIMEOUT_SECONDS` (30 s) timer when it enters `NoAudioRetry`, and cancels it on any new mic event. When it fires, the state goes back to `Idle`.
- `HomeUiEvent.ScreenStopped` cancels any running voice session (listening, transcribing, slow response) and resets the state to `Idle`, keeping the current `Success` reset. `ON_STOP` covers both leaving Home for another screen and the app going to the background.
- Search gets the same `LifecycleEventEffect(ON_STOP)` → `ProductSearchUiEvent.ScreenStopped`, which cancels voice capture and restores the phase that was on screen before the mic (`phaseBeforeVoice`), as cancelling the mic already does.

### D4. Search asks for the microphone permission; Detail hands voice off to Search (VOZ-008, VOZ-010)

- The permission request that `HomeScreen` uses today moves to a small composable helper in `core:ui` (`rememberMicPermissionGate(onGranted, onDenied)`), so Home and Search share it. Search shows the same "Necesitamos acceso al micrófono para la búsqueda por voz" message when it is refused (string moved to `core:ui`).
- Detail's `ActivateMic` emits a navigation effect to Search with an empty query and `SearchSource.Voice`, the same hand-off Catalog uses since wave 1 (Search starts listening). The guard in D1 applies.

Alternative for Detail: hide its microphone. The mockup shows it, and the hand-off already exists.

### D5. Share errors have their own state and keep the cart (CAR-033)

- `OrderSubmission` gets `ShareError` for a share that was rejected or failed after its retries. The cart renders it with share copy ("No pudimos enviar la cotización" / "Hubo un problema enviando la cotización. Inténtalo de nuevo en unos momentos.") and a single "Entendido" that goes back to the cart with basket and form kept (`DismissError`).
- `QuoteNotDelivered` keeps its copy and also loses "Cancelar orden": a quote that did not arrive is not a reason to empty the basket.
- The order error (`OrderSubmission.Error`) keeps "Entendido" + "Cancelar orden" (the customer can still abandon an order that failed).
- The product share dialog already has share copy; it is checked against the same rule (no action that clears the cart).

### D6. Dialogs cannot be closed while sending; closing a result runs its main action (CAR-043, CAR-044)

- `CartDialog` and `ShareProductDialog` decide `onDismissRequest` from the submission state: `Submitting` → ignored; `Success` → same as the accept button (`AcceptSuccess`, which ends the session for the cart as today); error states → same as "Entendido" (`DismissError`); `Idle` → close as today. The close "X" is hidden or disabled while `Submitting`.
- `DialogProperties(dismissOnBackPress, dismissOnClickOutside)` stay `true`; the decision lives in the handler so it can map each state to its action.
- When the customer session ends on inactivity (wave 1), the cart is closed and reset whatever the state, as today.

### D7. Cart actions need a basket, valid data and at most 20 lines (CAR-020, CAR-021, CAR-024, CAR-031)

- `CartUiState.canSubmit` = not empty && valid form && no flagged line, and it drives both "Realizar pedido" and "Compartir" (CAR-031).
- `CartConstants.MAX_DISTINCT_LINES = 20`. `AddToCartUseCase` returns `AddToCartResult` (`Added`, `NotOrderable`, `LineLimitReached`). Adding more units to a line already in the basket is always allowed (up to its quantity cap).
- The screens that add (Catalog, Search, Detail, related products) show a short message on `LineLimitReached`: "Tu carrito ya tiene 20 productos distintos. Realiza tu pedido o quita alguno para agregar más." Each screen already has a one-shot effect channel; the message is a `Toast`, the pattern Home (microphone permission) and Pairing already use.
- The quantity per line is already capped (99, or the stock cap), so CAR-024's quantity rule needs no change; it is covered by a test.

Alternative: let the server answer `422` and show a specific error. It costs a request and the customer only learns about the limit at the end.

### D8. Pairing: one code request at a time; a paired tablet never generates (VIN-024, AJU-031, AJU-050)

- `PairingViewModel` keeps the code request in a tracked `generateJob`. `GenerateCode` is ignored while the phase is `Loading` or the job is active. `cancelJobs()` also cancels it, so leaving the section stops it.
- Loading the paired device gets its own error, `PairingError.PairedDeviceLoad`, with connection copy ("No pudimos cargar los datos del dispositivo. Revisa la conexión e inténtalo de nuevo."). Its "Reintentar" sends a new `RetryPairedDeviceLoad` event, which runs `loadPairedDevice()`. `GenerateCode` is ignored whenever the session is paired.

## Risks / Trade-offs

- [The resumed-entry guard drops a legitimate tap made during the 300 ms transition] → The tap is lost, not misrouted; the customer taps again once the screen is shown. This is the standard Navigation guidance.
- [Back on Search now walks the history, so leaving Search can take several Backs] → Same as "Volver" today, which is what QA expected. The close confirmation is reached after the first result.
- [Stopping capture on `ON_STOP` cancels a voice search when a system dialog (permission) covers the screen] → A permission dialog pauses but does not stop the activity, so `ON_STOP` does not fire for it. Checked on the emulator in QA.
- [The 20-line limit is duplicated from the server] → One constant in `CartConstants`, referenced by the test that pins it; the server still validates.
- [Ignoring the dismiss while sending could leave a dialog stuck if the request hangs] → The requests already have timeouts (Ktor) and the share has bounded retries, so `Submitting` always ends.

## Migration Plan

App-only change, shipped with the next APK. No data migration. Rollback is reverting the change.

## Open Questions

- Copy for the 20-line message and the share error: the drafts above follow the existing tone. Product can adjust the text without changing the behavior.
- EXP-005 (chips after going back in the history) could be fixed together with D2, since it is in the same code. It is low; it stays in wave 3 unless the implementation makes it free.
