## Context

The Discovery tablet authenticates to Qhathia with hard-coded dev credentials via `SessionTokenProvider` (`core:data/stt`), which documents itself as the interim seam to replace "when pairing lands." The Settings feature (`features:settings`) already renders a side menu with a Wi-Fi section and a non-functional "Vincular" item; this change implements that section.

The backend models an **inverted pairing flow** (api-contract.md §5, ClickUp 86e1tvkvq): the **tablet** (this app, no JWT) generates the code + opaque `sessionId`, opens a status WebSocket, and shows the code; the **merchant phone** (OWNER JWT) approves/rejects it in its own app; the tablet then exchanges the session for a long-lived DEVICE JWT. This app implements only the tablet side.

Constraints from the standards: Clean Architecture + MVI, Hilt (KSP2), Ktor CIO, feature modules must not depend on other feature modules, every new layer is a proper Gradle module, JUnit5 + MockK + Turbine, mandatory `@Preview`.

## Goals / Non-Goals

**Goals:**
- Tablet-side pairing lifecycle: generate code (5-char, ≤5 min TTL), countdown, WebSocket status subscription, DEVICE-token exchange + persistence, paired-device detail view, stubbed unpair with confirmation.
- Make the persisted DEVICE token the source of the app's access token, with dev-login as fallback (single change point: `SessionTokenProvider`).
- Pairing section embedded in the existing Settings side-menu navigation with an animated selection shadow.

**Non-Goals:**
- Server-side unpair (no API → stub + TODO).
- Catalog sync / order forwarding wiring (separate stories).
- Sourcing the merchant (store) name where no API supplies it (shown as pending).
- The merchant-phone approval UI (different app).

## Decisions

### Module placement: pairing lives in `features:settings`, not a new feature module
The pairing UI is a sibling section of the Wi-Fi screen, sharing one side menu and an animated selection shadow when switching between them. A separate `features:pairing` module would force cross-feature coupling (feature modules must not depend on each other) to share that side menu. So the pairing presentation layer (screens, ViewModel, MVI contract, navigation) is added inside `features:settings`. The reusable business/data/network layers still go in the `core:*` modules per the dependency rules.

*Alternative considered:* a standalone `features:pairing` module — rejected because the shared side-menu/section-switch chrome would have to be hoisted to `app` or `core:ui`, adding indirection for no reuse benefit (there is exactly one consumer).

### New `core:network` surface: a REST service + a raw WebSocket client
- `DevicePairingApiService` wraps three absolute-URL calls returning `NetworkResult<T>` like the existing `AuthApiService`: `POST .../pairing-token`, `GET .../pairing-session/{sessionId}/token`, `GET .../devices/me` (the last with `Authorization: Bearer <deviceToken>`).
- `PairingSocketClient` opens the raw WSS (`.../pairing-session/{sessionId}`) and exposes a cold `Flow<PairingStatus>` that emits the server's status messages and completes on the terminal message/close. This requires the **`ktor-client-websockets`** plugin installed on the existing CIO `HttpClient` (new version-catalog lib `ktor-client-websockets`, same `ktor` version).

*Alternative considered:* OkHttp WebSocket — rejected; the project standardizes on a single Ktor `HttpClient` and CIO already supports WebSockets.

### Status as a cold Flow, owned by the ViewModel
`ObservePairingStatusUseCase` returns `Flow<PairingStatus>`; the `PairingViewModel` collects it in `viewModelScope`, drives `PairingUiState`, and the collection is cancelled when the ViewModel/section is torn down — satisfying the "subscription cleanup on leaving the section" requirement without manual socket lifecycle code in the UI.

### Token persistence reuses the existing DataStore session
Extend the existing Preferences DataStore used by `StoreSessionLocalDataSource`: add a `device_access_token` (and minimal device identity) key. `StoreSession` gains a nullable `deviceAccessToken` / `isPaired`. `SessionTokenProvider` is changed to read the persisted device token first and fall back to dev-login only when absent — this is the spec'd `store-session` modification and keeps a single auth change point.

*Alternative considered:* `EncryptedSharedPreferences` — deferred; standards note the security-crypto lib is unmaintained and prefer DataStore + Keystore. Token-at-rest hardening is tracked as a follow-up, not blocking this story.

### Countdown derived from `expiresAt`, not a free-running timer
The code's `expiresAt` (server-authoritative) drives a `viewModelScope` ticker that recomputes remaining seconds each second; on reaching zero the ViewModel requests a fresh token+session. This avoids clock drift from a purely local 5-minute counter and naturally aligns with a server `EXPIRED` socket message.

### Unpair is wired end-to-end but stubbed at the data boundary
`UnpairDeviceUseCase` → `DevicePairingRepository.unpair()` exists, but the impl is a documented TODO (no API). Confirming clears local pairing state where safe and surfaces that server-side unpair is unavailable, so the UI never falsely implies a backend unpair occurred.

## Module / dependency map

```
features:settings (pairing screens, PairingViewModel, MVI, side-menu switch)
   → core:domain (DevicePairingRepository port, PairingCode/PairingStatus/PairedDevice models, use cases)
   → core:ui, core:common
core:data (DevicePairingRepositoryImpl, device-session persistence, SessionTokenProvider change)
   → core:domain (implements port) → core:network (pairing service + socket client)
core:network (DevicePairingApiService, PairingSocketClient, pairing DTOs; + ktor-client-websockets)
app → features:settings + core:*
```
All edges already exist except the new `core:network` types and the `ktor-client-websockets` dependency — no new module is created, so no `settings.gradle.kts` / manifest additions are needed (Settings, network, data, domain modules already exist; `INTERNET` permission already declared).

## Risks / Trade-offs

- **WebSocket reliability over kiosk Wi-Fi** → treat any socket error as a non-terminal "retry" state; the TTL countdown and explicit re-generate path mean a dropped socket never strands the user (worst case the code expires and is regenerated).
- **Plaintext token at rest in DataStore** → accepted for this story; hardening (Keystore-backed cipher) tracked as follow-up. Token is device-scoped, not user credentials.
- **Stubbed unpair could confuse users** → mitigated by explicit "not yet available" messaging and not clearing remote state; documented TODO referencing the missing API.
- **Two timers (local countdown + server EXPIRED)** could race → the ViewModel treats whichever fires first as expiry and de-dupes the regenerate request via state guards.
- **`expectSuccess = true` on the client** means non-2xx throws `ResponseException`; the pairing service must keep using `runCatching { … }.toNetworkResult()` so contract errors (e.g. `409 PAIRING_NOT_APPROVED`, `410` expired) map to `HttpError` rather than crashing.

## Open Questions

- Source of the merchant (store) name for the paired view — no field in `/devices/me`. Shown as pending until a story provides it (e.g. a store-info endpoint or a claim derived from the device token).
- Whether `lastSyncAt`/`lastSyncVersion` from `/devices/me` should drive the "sync time" label directly or be formatted relative — defer to UI guidelines/Figma during apply.
