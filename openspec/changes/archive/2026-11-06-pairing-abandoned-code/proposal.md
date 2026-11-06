## Why

If the app is force-stopped while showing a pairing code, the code stays approvable: after reopening, the tablet shows a new code, the merchant can still approve the old one, and the backend creates an orphan tablet (QA regression 2026-11-03, F-02 / D:VIN-022). The app keeps the code only in memory, never tells the backend it replaced it, and never cancels it. The backend already supports both (phase 37).

## What Changes

- **Remember the code on screen.** The session id of the code being shown is stored on disk, and cleared when the exchange succeeds.
- **Replace, not add.** Each new code (`POST /devices/pairing-token`) sends `{"replacesSessionId"}` with the stored id, so the backend cancels the old one.
- **Cancel on reopen.** When the app process starts and the tablet is not paired, the stored code is cancelled (`POST /devices/pairing-session/{id}/cancel`, best-effort) and forgotten. The in-memory reuse is unaffected: a new process never reuses a code.
- **`CANCELLED` on the socket** reads as expired, so the tablet asks for a new code instead of showing a dead one.
- "Volver" + "Continuar" keeps reusing the same code (unchanged).

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `device-pairing`: a replaced or abandoned code is cancelled on the backend, and a cancelled code is replaced on screen.

## Impact

- `core:network`: `DevicePairingApiService.generatePairingToken(replacesSessionId)`, `cancelPairingSession(id)`, `GeneratePairingTokenRequestDto`.
- `core:data`: `PendingPairingSessionStore` (key `pending_pairing_session_id` in `store_session`), `DevicePairingRepositoryImpl`, `CANCELLED` mapping.
- `core:domain`: `DevicePairingRepository.cancelAbandonedSession()`, `CancelAbandonedPairingUseCase`; `core:testing` fake.
- `:app`: `DiscoveryApp.onCreate` launches the cancel.
- No new module, library or manifest change. Known limit: a code approved before it was replaced is not undone (covered by the staff app, F-60).
