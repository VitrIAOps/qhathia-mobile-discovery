## 1. Network

- [x] 1.1 `GeneratePairingTokenRequestDto`; `generatePairingToken(replacesSessionId)` (body only with an id); `cancelPairingSession(id)`; `MockEngine` tests.

## 2. Domain and data

- [x] 2.1 `DevicePairingRepository.cancelAbandonedSession()`, `CancelAbandonedPairingUseCase`, fake in `core:testing`.
- [x] 2.2 `PendingPairingSessionStore` + DataStore impl + binding; `DevicePairingRepositoryImpl` generate/exchange/cancel; `CANCELLED` → `Expired`.
- [x] 2.3 Tests: second `generateCode` sends the first session id; exchange forgets it; cancel on 204/404/error/paired; late cancel keeps a newer id; `CANCELLED` mapping.

## 3. App

- [x] 3.1 `DiscoveryApp.onCreate` launches `CancelAbandonedPairingUseCase`.

## 4. Build and verify

- [x] 4.1 Tests of `:core:network`, `:core:domain`, `:core:data`, `:features:pairing`, `:app`; `lintDebug`; `:app:assembleProfile`.
- [x] 4.2 `qa-report.md` (D:VIN-022 / X:XVI-011, VIN-030, Volver + Continuar); hand the emulator run to the QA team.
