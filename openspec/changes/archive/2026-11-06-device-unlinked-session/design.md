## Context

Since backend phase 35 (api-contract §35), an unlinked tablet gets this on **any** call made with its DEVICE token:

```
401 application/problem+json
{"type":"/errors/device-unlinked","status":401,"errorCode":"DEVICE_UNLINKED","detail":"Esta tablet fue desvinculada…"}
```

auth rejects it immediately; biz and vo cache `/devices/me` per tablet and reject it within 60 s. Public pairing endpoints keep working with the old token. Since phase 42, `401`/`403` from the security filter are also `problem+json` with an `errorCode`.

Today the app cannot see that code:

- `NetworkResult.HttpError(code, message)` drops the response body. Only `DevicePairingApiService` reads `errorCode`, through a private `readErrorCode`.
- The heartbeat turns the `HttpError` into an `IllegalStateException`, and `RunDeviceHeartbeatUseCase` swallows it.
- Catalog, detail (2 calls), search, order and share retry a `401` with the same token. `SessionTokenProvider.clear()` is a no-op, and `forceRefresh` re-reads the same stored token.

The reaction to an unpaired session already exists. It just never fires:

- `MainViewModel.startDestination` → `Onboarding` → `DiscoveryNavHost(startRoute = WELCOME_ROUTE)`;
- `ObserveKioskLockUseCase` (`active = session.isPaired && …`) → `stopLockTask()`;
- `RunDeviceHeartbeatUseCase` (`collectLatest` on `isPaired`) stops beating, which also starves `SyncDeviceSettingsUseCase` and `ProcessDeviceCommandsUseCase`, since both are driven by the heartbeat.

The store session, the remote settings and the PIN attempts all live in the single `store_session` Preferences DataStore.

Modules involved (no new module): `core:network`, `core:domain`, `core:data`, `core:testing`, `features:onboarding`, `app`. Dependency rules hold:

- `core:network` knows no domain types. It exposes a signal of its own, and `core:data` adapts it to a domain port.
- `features:onboarding` gains `core:domain`, which features may depend on.

## Goals / Non-Goals

**Goals:**
- Read the `problem+json` body of every HTTP error once, in `core:network`, for this change and the next ones (F-30, F-32, F-33, F-44).
- React to `DEVICE_UNLINKED` from any service within one call (≤ 1 heartbeat, ≤ 60 s): revoke the local device state, release the kiosk lock, and land on Welcome with a notice.
- Leave nothing of the previous store behind for a later re-pair: store name, exit PIN hash, kiosk mode, settings version, cart, consumer forms, held pairing code.
- Never revoke on any other `401`/`403`.

**Non-Goals:**
- Reacting to `PRICE_CHANGED`, `PRODUCT_NOT_SELLABLE`, `STOCK_INSUFFICIENT` (later changes; they only reuse `ProblemDetailDto`).
- The local "Desvincular" stub in Settings (`DevicePairingRepositoryImpl` unpair).
- Polling `/devices/me` as a bridge: the backend already answers `401`.
- Changing per-screen error copy for the few seconds between the `401` and the navigation to Welcome.

## Decisions

### D1 — `HttpError` carries the parsed problem detail

`HttpError(code: Int, message: String, problem: ProblemDetailDto? = null)`. `toNetworkResult()` becomes `suspend` so it can read `response.bodyAsText()` for a `ResponseException`. Every call site is already `runCatching { … }.toNetworkResult()` inside a `suspend` function.

`ProblemDetailDto` (`@Serializable`, all fields optional) models the fields api-contract §35–§39 documents: `errorCode`, `detail`, `productId`, `productName`, `reason`, `available`, `requested`, `changes: List<PriceChangeDto>`, `fieldErrors`. It also accepts the legacy `code` key, and exposes `errorCode` as `errorCode ?: code`.

Parsing uses its own `Json { ignoreUnknownKeys = true; isLenient = true }` inside `runCatching`. An empty, non-JSON or unreadable body gives `problem = null`, never an exception. The default value keeps every existing `HttpError(500, "…")` in tests compiling.

`DevicePairingApiService.readErrorCode` is replaced by the shared helper (`ResponseException.readProblemDetail()`), so there is one parser.

- *Alternative:* keep only `body: String?` and parse at each repository (as the regression doc suggests). Rejected: every repository would repeat the same `Json` parsing, and `core:data` would need its own `Json`. Parsing once next to the HTTP client is simpler and keeps the DTO in `core:network`, where DTOs belong.
- *Alternative:* `errorCode: String?` only. Rejected: F-30, F-32 and F-33 need `changes`, `reason`, `available` and `fieldErrors` next. Adding them now costs nothing, because the fields are optional.

### D2 — Detect once, in the HTTP client, with a validator hook

`KtorClientFactory.configure` gains `onDeviceUnlinked: (bearerToken: String?) -> Unit = {}` and installs `HttpResponseValidator { handleResponseExceptionWithRequest { cause, request -> … } }`. When the cause is a `ClientRequestException` with status `401` and the body's `errorCode` is `DEVICE_UNLINKED`, it calls `onDeviceUnlinked` with the request's bearer token: `Authorization` minus the `Bearer ` prefix. The handler never swallows the exception, so each repository still receives its `HttpError` as today.

`NetworkModule.provideHttpClient(signal: DeviceUnlinkedSignal)` passes `signal::notifyUnlinked`.

`DeviceUnlinkedSignal` (`@Singleton`, `core:network`) holds a `MutableSharedFlow<String?>(extraBufferCapacity = 8, DROP_OLDEST)` exposed as `events`, emitted with `tryEmit`.

- *Why the client and not each repository:* there are 12 authenticated call sites across auth, biz and vo (heartbeat, settings, ack, `/devices/me`, STT token, catalog, detail × 2, search, order, share, …). One hook covers them all, plus any call added later, so no repository can forget it.
- *Alternative:* an OkHttp-style interceptor. Not available on CIO. The validator is Ktor's supported hook.
- *Body read twice:* the validator reads the body, then `toNetworkResult()` reads it again. With `expectSuccess = true` Ktor 3.4.3 saves the response body, and `DevicePairingApiService` already reads it after the default validator. A `MockEngine` test locks this in.

### D3 — Revoke only the session that actually got the 401

The signal carries the bearer token of the failed request. `HandleDeviceUnlinkedUseCase` revokes only if that token equals the currently stored `deviceAccessToken`. A token of `null` (unreadable header) counts as a match, because every `DEVICE_UNLINKED` comes from a DEVICE-token call.

This protects a fresh re-pair: a request sent with the old token and answered late cannot wipe the new session. The token is compared in memory and never logged.

- *Alternative:* revoke on any event while paired. Rejected because of the re-pair race above.
- *Alternative:* a consecutive-failure counter (the 28/10 `fixes` proposal). Not needed: `errorCode == DEVICE_UNLINKED` is unambiguous since phase 35, and a bare `401` without that code never revokes.

### D4 — Revocation order and idempotency

`HandleDeviceUnlinkedUseCase` collects `DeviceUnlinkEvents.observe()` sequentially (`collect`, not `collectLatest`), so two `401`s arriving together are handled one after the other. The second one finds the session already empty and does nothing.

For each event, if paired and the token matches (D3):

1. `EndCustomerSessionUseCase()`: cart cleared, then cart and share forms reset.
2. `DeviceSettingsRepository.clear()`: removes every `settings_*` key (exit PIN hash, kiosk mode, idle showcase, voice, brightness, auto-sync, version), so the old store's settings do not leak into a re-pair. Settings fall back to their defaults. The PIN lockout state frees itself, because `SettingsPinAttemptsRepositoryImpl` compares a hash fingerprint and no hash means `NONE`.
3. `HeldPairingCodeRepository.set(null)` and `KioskSessionRepository.setExitedFromSettings(false)`.
4. `StoreSessionRepository.revokeDeviceSession()`: in one DataStore edit, removes the token, device name, pairing code and store name, and sets `unlinked_notice = true`.

The token goes last because removing it is what flips `isPaired`, and with it navigation, kiosk release and heartbeat stop. By then everything else is clean, so Welcome never flashes old data.

- *Alternative:* one cross-source atomic DataStore edit. Rejected: it would couple `StoreSessionLocalDataSource` to the settings keys. The order above is safe even if the process dies between steps. Without a token the app is unpaired anyway, and a partial clear before step 4 only resets data that the next sync or the next pairing would set again.

### D5 — Where things run

- `DeviceUnlinkEvents` (port, `core:domain`, `fun observe(): Flow<String?>`) is implemented in `core:data` by `DeviceUnlinkEventsImpl` over `DeviceUnlinkedSignal.events`, and bound in a new `di/DeviceUnlinkDataModule`.
- `DiscoveryApp.onCreate` launches `handleDeviceUnlinked.get()()` in the application scope, main process only, lazily injected. This is the same pattern as the heartbeat, commands and settings sync. Running it at the app level, not in a ViewModel, means a `401` from the background heartbeat on Home is handled with no screen involved.

### D6 — Welcome notice

`features:onboarding` gains Hilt (`ksp` + `hilt` plugins, `hilt.android`, `hilt.navigation.compose`, `lifecycle.viewmodel`), `core:domain`, and test dependencies, mirroring `features:home`.

New `WelcomeViewModel` (`WelcomeUiState(showUnlinkedNotice: Boolean)`, `WelcomeUiEvent.Continue`):
- observes `ObserveStoreSessionUseCase`;
- on `Continue`, calls `DismissUnlinkedNoticeUseCase` and then emits `WelcomeUiEffect.NavigateToInitialConfiguration`.

`WelcomeScreen` becomes stateful (`hiltViewModel()`). `WelcomeContent(showUnlinkedNotice, onContinue)` stays stateless and draws the notice above the title, as a tonal banner (`core:ui` tokens, warning colors) announced as a polite live region.

The notice text goes in `features/onboarding/src/main/res/values/strings.xml` (`onboarding_unlinked_notice`). The repo has no `values-en/`, so none is added. There are previews for both states.

### D7 — Remove the dead 401 retry

The five repositories map a `401` like any other `HttpError`. Each keeps its own category, so the screen shows its usual error for the few seconds before Welcome. `SessionTokenProvider` drops `forceRefresh` and `clear()`, and its KDoc explains that a `401` ends the session through the unlink handler.

- *Why now:* the retry doubles every failing call (two `401`s per tap) for no benefit, and the comments that justify it describe a token refresh that does not exist.

## Risks / Trade-offs

- [Ktor does not keep the body readable for the second read] → A `MockEngine` test of `KtorClientFactory.create(engine)` covers it: a `401` problem body is seen by the validator and still parsed into `HttpError.problem`. If it fails, the validator stops reading the body itself and the signal is raised from `toNetworkResult()` instead.
- [A `401 DEVICE_UNLINKED` from biz/vo during a backend auth outage] → api-contract §35: biz and vo *fail open* when auth is down, so they never answer `DEVICE_UNLINKED` without auth confirming it.
- [The customer is mid-order when the tablet is unlinked] → The order fails with `401`, and the cart and form are cleared by the revocation. That is intended: the tablet no longer belongs to the store.
- [`toNetworkResult()` becoming `suspend` breaks a non-suspend caller] → Every current caller is in a `suspend` function; the compiler flags any exception.
- [The notice survives a process death before "Continuar"] → Intended: it is persisted, and shown until dismissed.

## Migration Plan

App-only, no data migration. The new key `unlinked_notice` is absent by default, which means false.

Rollback = revert the change. Old builds keep ignoring the `401`, as today.

Verify against the QA store (`6b89ea64-…`) with X:XVI-040 / X:XVI-041 / XVI-001 (see tasks).

## Open Questions

- None blocking. The notice copy follows the regression doc (§2.1, F-101 step 5). If product wants different wording, only the string changes.
