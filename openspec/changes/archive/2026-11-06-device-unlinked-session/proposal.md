## Why

When the merchant unlinks a tablet from the staff app, the backend answers every call made with that tablet's token with `401` and `errorCode: DEVICE_UNLINKED` (api-contract §35: immediate in auth, ≤ 60 s in biz and vo). The tablet ignores it: in the QA regression of 2026-11-03 (X:XVI-040, F-101, CRITICAL) it got a `401 DEVICE_UNLINKED` on every heartbeat for 144 s, stayed pinned on Home, and showed "Perdí la conexión…" on every screen. The merchant no longer controls the tablet, and the tablet is stuck on Home with no way out short of clearing app data.

The root cause is shared with the next changes (F-30, F-32, F-33, F-44): `NetworkResult.HttpError` keeps only the HTTP status and drops the `problem+json` body, so the app cannot tell one `401` or `409` from another. This change builds that shared piece first.

## What Changes

- **Readable API errors:** `NetworkResult.HttpError` keeps the parsed `problem+json` body (`errorCode`, `detail` and the optional fields the backend sends). Pairing's private `readErrorCode` moves to this shared helper.
- **One detection point:** the shared Ktor client recognizes `401` + `errorCode: DEVICE_UNLINKED` on any response, whichever service answers (auth, biz, vo), and raises an app-wide "device unlinked" signal. Each repository still gets its usual `HttpError`.
- **Local revocation:** on that signal, while the token that got the `401` is still the stored one, the app:
  - ends the customer session (cart and consumer forms);
  - clears the stored remote settings (exit PIN hash, kiosk mode, settings version);
  - clears the held pairing code and the "exited kiosk from Settings" flag;
  - deletes the device token, device name, pairing code and store name.
  
  The pieces that already react to an unpaired session then take over: the kiosk lock is released, the heartbeat stops, and the app routes to Welcome.
- **Notice on Welcome:** after a revocation, Welcome shows "Esta tablet se desvinculó de la tienda. Para volver a usarla, vincúlala de nuevo desde la app del comercio." until the merchant taps "Continuar".
- **No false positives:** any other `401` or `403` (for example a `403 FORBIDDEN` from biz, or a Deepgram `401`) does not revoke anything.
- **Remove the useless 401 retry:** catalog, detail (product and suggestions), search, order and share repositories no longer retry a `401` with the same token. `SessionTokenProvider.clear()` and `forceRefresh` are removed, because they did nothing.

## Capabilities

### New Capabilities
- `api-error-details`: HTTP error results carry the backend's `problem+json` fields (`errorCode`, `detail` and optional extras), so callers can react to a specific error code instead of a bare status.
- `device-unlink`: detecting `DEVICE_UNLINKED` on any call, revoking the local device session and its tablet state, and the conditions that must not trigger it.

### Modified Capabilities
- `store-session`: the session can be revoked by an unlink. Revoking also drops the store name, and the session records a pending "unlinked" notice until it is dismissed.
- `app-onboarding`: the Welcome screen shows the unlink notice after a revocation and dismisses it on "Continuar".

## Impact

- `core:network`:
  - `NetworkResult` (`HttpError` gains `problem`; `toNetworkResult()` becomes `suspend`);
  - new `ProblemDetailDto` and helper for reading the error body;
  - `KtorClientFactory` (response validator hook);
  - `NetworkModule`;
  - new `DeviceUnlinkedSignal`;
  - `DevicePairingApiService` uses the shared helper.
- `core:domain`:
  - new port `DeviceUnlinkEvents`;
  - new `HandleDeviceUnlinkedUseCase` and `DismissUnlinkedNoticeUseCase`;
  - `StoreSession.unlinkedNotice`;
  - `StoreSessionRepository.revokeDeviceSession()` / `dismissUnlinkedNotice()`;
  - `DeviceSettingsRepository.clear()`.
- `core:data`:
  - `StoreSessionLocalDataSource` and `DeviceSettingsLocalDataSource` (new clears);
  - implementations of the new port;
  - the 5 repositories with the 401 retry;
  - `SessionTokenProvider`.
- `core:testing`: fakes updated for the new port methods.
- `features:onboarding`: gains Hilt, `core:domain` and a `WelcomeViewModel`; new strings.
- `app`: `DiscoveryApp` starts the unlink handler in the main process.
- No new Gradle module and no new library. No API change: it only consumes the existing api-contract §35 error. `docs/android/api-contract.md` is already up to date (phases 35–43).
- Out of scope:
  - the local "Desvincular" stub in Settings;
  - polling `/devices/me`;
  - the other error codes (`PRICE_CHANGED`, `PRODUCT_NOT_SELLABLE`, …). Those are read by later changes on top of `api-error-details`.
