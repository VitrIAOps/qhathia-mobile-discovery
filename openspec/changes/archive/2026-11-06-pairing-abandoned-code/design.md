## Context

`HeldPairingCodeRepositoryImpl` keeps the code in memory on purpose (reuse within one process). `DevicePairingApiService.generatePairingToken()` sends no body; nothing calls `…/cancel`. The socket status `CANCELLED` maps to `null` and is ignored. Backend §37: `pairing-token` accepts `{"replacesSessionId"}` (tolerant: unknown or resolved ids are not an error); `POST …/pairing-session/{id}/cancel` is public and idempotent (`204`, `404` if unknown).

## Decisions

- **D1 — Persisted pending session.** `PendingPairingSessionStore` (interface in `core:data`, DataStore impl over `store_session`, key `pending_pairing_session_id`): `get()`, `set(id)`, `clearIf(id)`. `clearIf` only clears when the stored id still matches, so a cancel that finishes late cannot forget a newer code.
- **D2 — Generate.** `generateCode()` reads the pending id, sends it as `replacesSessionId`, and on success stores the new session id. The body is only sent when there is an id.
- **D3 — Exchange.** A successful exchange calls `clearIf(sessionId)`.
- **D4 — Cancel on start.** `cancelAbandonedSession()`: nothing when there is no pending id; when paired, just forget it; otherwise `cancelPairingSession(id)` and forget it on `204` or `404`. Other failures keep it, so the next `generateCode()` replaces it anyway. `CancelAbandonedPairingUseCase` runs from `DiscoveryApp.onCreate` on the application scope (main process only).
- **D5 — `CANCELLED` → `PairingStatus.Expired`.** The view model already requests a new code on `Expired`.
- **D6 — Not on Volver.** `PairingViewModel.stop()` does not cancel: QA approved the reuse.

## Risks / Trade-offs

- [Code A approved before being replaced] → `cancel` only cancels `WAITING`; the staff app warns (F-60).
- [Cancel without network at start] → The id is kept; the next code request replaces it.
