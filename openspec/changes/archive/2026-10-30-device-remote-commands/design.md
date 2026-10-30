## Context

`device-heartbeat` delivers commands through `DeviceHeartbeatRepository.observePendingCommands()`: a bounded `Channel` exposed with `receiveAsFlow()`, so each `DeviceCommand` (`RefreshCatalog`, `RestartApp`, `Unknown(raw)`) reaches one collector exactly once. Nothing collects it yet. `DiscoveryApp` launches the heartbeat in an `@ApplicationScope` scope.

Server contract (api-contract.md §23):

```
POST /api/v1/devices/me/commands/{commandId}/ack   (DEVICE)  → 204
  { "success": true|false, "detail": "≤300 chars, optional" }
```

`success: true` → ACKED ("Listo"), `false` → FAILED ("Falló: {detail}"). A delivered command that is never acked expires after 10 minutes. Commands of another tablet → 404.

Discovery keeps **no catalog cache**: `ProductCatalogRepositoryImpl` calls the API every time `CatalogViewModel` is created, and search/detail fetch live too. The only stale data is a Catálogo screen already on screen.

## Goals / Non-Goals

**Goals:**
- Turn every delivered command into an ack, so the staff card never stays on "Enviado".
- `REFRESH_CATALOG`: refresh what the consumer could be looking at, without disrupting them.
- `RESTART_APP`: a real restart (new process) with the ack sent *before* the process dies.

**Non-Goals:**
- Refreshing product-search results or product detail in place (short-lived screens; the cart inactivity timeout already sends idle tablets back Home within a minute).
- Clearing the Coil image cache (product images get new media URLs when they change).
- Deferring a restart until the consumer is idle — the merchant explicitly asked for it (see Open Questions).
- Restart inside kiosk lock mode — revisited in `kiosk-lock-mode`.

## Decisions

### D1 — One application-scoped command processor in the domain

`ProcessDeviceCommandsUseCase` collects `observePendingCommands()` sequentially (`collect`, not `collectLatest`, so a command is never cancelled by the next one) and dispatches:

| Command | Action | Ack |
|---|---|---|
| `RefreshCatalog` | `CatalogRefreshSignal.request()` | `success = true` |
| `RestartApp` | if `AppRestarter.canRestart()`: ack first, then `AppRestarter.restart()` | `success = true` (or `false`, "No se pudo reiniciar la app", when it cannot restart) |
| `Unknown(raw)` | none | `success = false`, `detail = "Comando no soportado: $raw"` |

An exception while executing is caught and acked as `success = false` with the exception message (truncated); `CancellationException` is rethrown. `DiscoveryApp.onCreate` launches it next to the heartbeat, in the same `@ApplicationScope`.

*Alternative:* executing inside the heartbeat use case — couples a 60 s timer to command latency and makes a slow command delay the next ping.

### D2 — Ack with bounded retries, before the restart

`DeviceCommandRepository.ack(commandId, success, detail)` → `Result<Unit>`. The use case retries a failed ack up to `ACK_MAX_ATTEMPTS = 3` with `ACK_RETRY_DELAY = 2.seconds` between attempts, then gives up (the server expires the command — the only honest outcome when the tablet cannot reach it). `detail` is truncated to `ACK_DETAIL_MAX_LENGTH = 300` client-side so the server never rejects it.

For `RESTART_APP` the restart runs **after** the ack attempts finish, success or not. If the ack never lands, the merchant sees "No se pudo entregar" although the tablet restarted — acceptable and rare; the opposite order would always lose the ack, because the process dies.

### D3 — Restart = relaunch intent + process exit (`AppRestarterImpl`, `core:data`)

```kotlin
val launch = packageManager.getLaunchIntentForPackage(packageName)
context.startActivity(Intent.makeRestartActivityTask(launch.component))
Runtime.getRuntime().exit(0)
```

A new process re-runs `DiscoveryApp.onCreate`, re-resolves the start destination from the persisted pairing, and restarts the heartbeat. Starting the activity from the background is allowed because the kiosk app is the foreground app.

Because the ack goes out before the restart, a restart failure could not be reported afterwards. So the port has two calls: `canRestart(): Boolean` (the launch intent resolves) and `restart()`. The use case checks `canRestart()` first; when it is `false` the command is acked as failed ("No se pudo reiniciar la app") and the process is left alone, instead of killing a process that would not come back.

*Alternatives:* `Activity.recreate()` (not a restart — singletons, scopes and Hilt graph survive, defeating the purpose of "Reiniciar"); `AlarmManager` + `killProcess` (more moving parts, exact-alarm permission on API 31+).

### D4 — `CatalogRefreshSignal`: an in-memory broadcast, consumed by `CatalogViewModel`

Port in `core:domain`: `fun request()` and `fun observe(): Flow<Unit>`. `CatalogRefreshSignalImpl` (`core:data`, `@Singleton`) is a `MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1, DROP_OLDEST)`: no replay, so a catalog screen opened *after* the command does not refresh again (it just loaded fresh data anyway).

`CatalogViewModel` collects `ObserveCatalogRefreshUseCase()` and calls `refreshInPlace()`:
- if a full load is running → ignore (it is already fetching fresh data);
- if the screen shows the network-error state → run the normal `load()` (a retry is the right refresh);
- otherwise fetch silently: on success replace `categories`/`tabs` and keep `selectedTabId` when that tab still exists (else fall back to Todo); on failure keep the current products untouched — no spinner, no error screen, no reset of the idle timer.

The command is acked `success = true` as soon as the signal is emitted: from the merchant's point of view the tablet accepted the refresh, and no screen may be listening (Home), which is also a success.

### D5 — Network shape

`DeviceCommandApiService.ack(deviceToken, commandId, DeviceCommandAckDto(success, detail))` → `NetworkResult<Unit>`, with the same 15 s per-request timeout as the heartbeat. `DeviceCommandRepositoryImpl` reads the DEVICE token from `StoreSessionRepository` (failure without a request when missing) and maps `HttpError`/`NetworkError` to `Result.failure`.

## Risks / Trade-offs

- [A restart while a consumer is mid-checkout loses the cart] → The merchant pressed "Reiniciar"; the staff screen already explains it restarts the app. Open question below.
- [Process exit skips `onDestroy`/DataStore flushes] → DataStore writes are atomic per edit and the cart is intentionally ephemeral; nothing persistent is lost.
- [Silent refresh swaps products under the consumer's finger] → Same tab, same layout; at worst a product's price/stock changes, which is the point of the command.
- [Commands buffered before this change shipped] → They live only in memory; a process started with this change has nothing stale to replay.

## Migration Plan

Additive client change. Ships together with (or after) `device-heartbeat`. Rollback = previous APK; commands go back to expiring server-side.

## Open Questions

- Should `RESTART_APP` wait until the tablet is idle on Home (empty cart) before restarting, acking immediately? Not required by the contract; can be added later behind the same use case without changing the ack semantics.
