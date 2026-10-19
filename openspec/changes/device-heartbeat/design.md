## Context

After pairing, the tablet persists a DEVICE JWT in `StoreSessionRepository` (DataStore) and uses it as the only access token (`SessionTokenProvider`). The only DEVICE-authenticated call today is `GET /api/v1/devices/me` (`DevicePairingApiService.getPairedDevice`). Nothing runs in the background: `DiscoveryApp` is an empty `@HiltAndroidApp` and there is no application-scoped coroutine scope (no `DispatcherProvider` binding either — ViewModels use `viewModelScope`).

The backend contract (`docs/android/api-contract.md` §5, §18, §21, §23):

```
POST /api/v1/devices/heartbeat           Authorization: Bearer <deviceAccessToken>
  → { appVersion*, networkStatus?, lastSyncVersion?, lastSyncAt?, batteryLevel?, uptimeSeconds?, model? }
  ← { received, serverTime, catalogVersion, settingsVersion, pendingCommands: [{ id, command }] }
```

- The staff app shows a tablet as "Conectada" when `lastPingAt` is within the last **10 minutes**.
- Telemetry never breaks the heartbeat server-side (an out-of-range value is dropped, still 200).
- Commands in `pendingCommands` are marked `DELIVERED` on delivery and **never re-delivered**.

Constraints (`docs/android/android-frontend-standards.md`): Clean Architecture — `core:domain` is pure Kotlin, `core:network` does not know domain models, `app` holds no business logic; Hilt via KSP2; JUnit5 + MockK + Turbine; `NetworkResult` for every network call.

## Goals / Non-Goals

**Goals:**
- Keep `last_ping_at` fresh (well inside the 10-minute window) for every paired tablet while the app process is alive.
- Report the telemetry the staff "Dispositivos" card shows: battery, network, app version, model.
- Expose each heartbeat response as a domain stream that `device-remote-commands` and `device-remote-settings` subscribe to.
- Be invisible to the consumer: no UI, no crash, no log spam on failures.

**Non-Goals:**
- Executing or acking `pendingCommands` (change `device-remote-commands`).
- Reading/applying `/devices/me/settings` on a `settingsVersion` change (change `device-remote-settings`).
- Catalog sync (`catalogVersion` / `lastSyncVersion`): Discovery has no local catalog cache, so it reports no sync version.
- Heartbeat while the process is dead (boot receiver, foreground service) — the kiosk app is always in the foreground; kiosk lock mode is its own change.
- Handling a revoked device token (401) by unpairing — no backend unpair flow exists yet.

## Decisions

### D1 — A coroutine loop in an application scope, not WorkManager

`PeriodicWorkRequest` has a 15-minute minimum interval, which exceeds the staff app's 10-minute "connected" window — a single late run would flip the card to "Desconectada". A `ForegroundService` would add a persistent notification and a `foregroundServiceType` justification for an app that is always on screen anyway.

Chosen: a suspending loop launched once from `DiscoveryApp.onCreate` in an `@ApplicationScope CoroutineScope` (`SupervisorJob() + Dispatchers.Default`) provided by a new Hilt module in `:app` (`di/AppCoroutineModule.kt`). The qualifier lives in `:app` because nothing else injects the scope. `DiscoveryApp` only does `appScope.launch { runDeviceHeartbeat() }` — the loop logic lives in the domain.

*Alternatives:* WorkManager (interval too long), `ProcessLifecycleOwner`-bound loop (would stop the heartbeat when an overlay/system dialog backgrounds the activity; the kiosk should keep reporting while the process lives).

### D2 — Loop logic as pure-Kotlin use cases, testable with virtual time

- `SendHeartbeatUseCase` — reads telemetry from `DeviceTelemetryProvider` and calls `DeviceHeartbeatRepository.sendHeartbeat(telemetry)`; returns `Result<HeartbeatResult>`.
- `RunDeviceHeartbeatUseCase` — `storeSessionRepository.observe().map { it.isPaired }.distinctUntilChanged().collectLatest { paired -> if (paired) loop() }` where `loop()` is `while (isActive) { sendHeartbeat(); delay(DeviceConstants.HEARTBEAT_INTERVAL) }`. `collectLatest` cancels the running loop the moment the device becomes unpaired and starts a fresh one (sending immediately) right after pairing completes — so the staff card turns "Conectada" seconds after pairing instead of a minute later.
- The interval is fixed-rate after each attempt (send, then wait), not wall-clock aligned: simpler, and a slow request just shifts the next tick.
- Failures are caught inside the loop (`Result.onFailure` → no-op); `CancellationException` is always rethrown.

Both are tested with `runTest` + `advanceTimeBy` against fakes; no Android in the loop.

### D3 — 60-second interval, 15-second request timeout

60 s keeps up to nine consecutive missed ticks inside the 10-minute window and gives remote commands at most ~1 minute of latency (the staff UI says "Enviado" until the ack). The global Ktor `requestTimeoutMillis` is 120 s (sized for uploads), longer than the interval, so `DeviceHeartbeatApiService` sets a per-request `timeout { requestTimeoutMillis = 15_000 }`. Constants live in `DeviceConstants` (`HEARTBEAT_INTERVAL = 60.seconds`) and in the service companion (timeout, which is a transport detail).

*Alternative:* 120 s (handoff allows 1–2 min) — halves traffic but doubles command latency; 60 s is cheap (one tiny JSON request per minute per tablet).

### D4 — Telemetry behind a domain port, every field best-effort

`DeviceTelemetryProvider` (domain interface) → `DeviceTelemetryProviderImpl` (`core:data`, `@ApplicationContext`):

| Field | Source | Null when |
|---|---|---|
| `appVersion` | `PackageManager.getPackageInfo(packageName).versionName` | never — falls back to `"unknown"` (field is required by the API) |
| `batteryLevel` | `BatteryManager.getIntProperty(BATTERY_PROPERTY_CAPACITY)` | value outside 0–100 / `Integer.MIN_VALUE` |
| `networkStatus` | latest `ConnectivityDataSource` snapshot (`first()`) | read fails |
| `model` | `"${Build.MANUFACTURER} ${Build.MODEL}"`, de-duplicated if the model already starts with the manufacturer, capitalized, trimmed to 100 chars | blank |
| `uptimeSeconds` | app process uptime: `SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime()` | — |

`networkStatus` mapping: `WIFI` when a Wi-Fi SSID is connected, `MOBILE` when cellular is the active transport, `OFFLINE` otherwise (in practice the request then fails). The staff contract example (§18) uses `"WIFI"`; see Open Questions.

Each reading is wrapped in `runCatching` so one broken sensor only drops that field. Optional request DTO fields default to `null`; the shared `Json` (`KtorClientFactory`) keeps `encodeDefaults = false`, so unknown values are omitted from the body rather than sent as `null`.

*Alternative:* sticky `ACTION_BATTERY_CHANGED` intent — equivalent, but `BatteryManager.getIntProperty` is one call with no receiver.

### D5 — Response published as a `SharedFlow<HeartbeatResult>`

`DeviceHeartbeatRepositoryImpl` is a `@Singleton` that, on each successful response, emits the mapped `HeartbeatResult` into a `MutableSharedFlow(replay = 1, extraBufferCapacity = 1, onBufferOverflow = DROP_OLDEST)` exposed as `observeHeartbeats(): Flow<HeartbeatResult>` (via `ObserveHeartbeatUseCase`).

- `replay = 1` lets the settings change read the latest `settingsVersion` when it subscribes late.
- **Commands must not be replayed** to a late/re-subscribing collector — a replayed `RESTART_APP` would restart the app twice. The commands change will therefore consume `pendingCommands` from a separate, non-replaying channel; to keep that change additive, this change already exposes `observePendingCommands(): Flow<DeviceCommand>` backed by a `Channel(capacity = 16, onBufferOverflow = DROP_OLDEST)` (`receiveAsFlow`, each command delivered exactly once to one collector). Until a collector exists, commands sit in that bounded buffer and are lost on process death — equivalent to the server-side expiry.
- `DeviceCommandType` maps `REFRESH_CATALOG`, `RESTART_APP`, and `UNKNOWN(raw)` for forward compatibility (a new server command must not break the heartbeat parsing).

*Alternative:* each downstream change calling the heartbeat itself — duplicates the loop and the network traffic, and commands could be delivered to the "wrong" caller.

### D6 — Network layer shape

`DeviceHeartbeatApiService.sendHeartbeat(deviceToken: String, body: HeartbeatRequestDto): NetworkResult<HeartbeatResponseDto>` mirrors `DevicePairingApiService.getPairedDevice` (explicit `bearerAuth`, `runCatching { … }.toNetworkResult()`). DTOs in `dto/DeviceHeartbeatDtos.kt`; response fields other than `received` are defaulted (`pendingCommands = emptyList()`, versions `null`) so an older backend still parses. The repository reads the token from `StoreSessionRepository.deviceAccessToken()` and fails fast (no request) when it is missing.

## Risks / Trade-offs

- [Commands delivered before `device-remote-commands` ships are never executed] → They expire server-side in 10 min and staff shows the expired state; ship both changes in the same release if possible.
- [Battery drain / traffic from a 60 s loop] → One small JSON POST per minute; tablets are mains-powered kiosks. Interval is a single constant.
- [Doze / App Standby defers network while screen is off] → Acceptable: a kiosk is on screen; a sleeping tablet showing "Desconectada" is truthful.
- [Process death stops the heartbeat] → The tablet shows "Desconectada" after 10 min, which is the correct signal; auto-restart belongs to kiosk lock mode.
- [401 with a revoked token loops forever] → Harmless traffic every minute; documented non-goal until an unpair API exists. Log at debug level only.
- [`Build.MODEL` is a code name ("TB330FU"), not the commercial name the staff screen wants ("Lenovo Tab M11")] → Send `manufacturer + model`; a commercial-name lookup table is not worth it now.

## Migration Plan

Additive, client-only. On upgrade the first heartbeat fills `model`, telemetry and `last_ping_at`. Rollback = ship the previous APK; the server simply stops receiving pings (today's behavior).

## Open Questions

- `networkStatus` vocabulary: §5 shows `ONLINE | LIMITED`, §18 shows `WIFI`. Proposed `WIFI | MOBILE | OFFLINE`; confirm with the staff app's mapping (staff `Device.kt`) before release. Decision does not block implementation — it is one mapping function.
