## 1. Search feature

- [x] 1.1 `ProductSearchUiEffect.ExitRequested`; `onBack` (network error → previous result; no previous result → `ExitRequested`).
- [x] 1.2 `ProductSearchScreen` / `productSearchScreen(previousIsHome)`; `MainActivity` wiring.
- [x] 1.3 Tests: exit requested with no previous result; back after a network error; the Home path still ends the session.

## 2. Build and verify

- [x] 2.1 Tests of `:features:product-search` and `:app`; `lintDebug`; `:app:assembleProfile`.
- [x] 2.2 `qa-report.md` (D:BUS-060, Home path, D:BUS-052, CAT-024/025); hand the emulator run to the QA team.
