## Why

The tablet never calls `POST /api/v1/devices/heartbeat`: after pairing it only reads `GET /devices/me`, so the server's `last_ping_at` freezes at the pairing instant. That single missing call is the downlink channel for five server features that are already deployed (Phases 18, 21 and 23) and have no client: the "Conectada" status and tablet count in the staff app, battery/network telemetry, remote commands ("Actualizar catálogo" / "Reiniciar"), remote tablet settings (`settingsVersion`) and the kiosk exit PIN. It is the highest-return item of the 2026-10-18 triage (BUG-025, `docs/handoff/android-pendientes.md` §1).

## What Changes

- Add a **periodic device heartbeat**: while the device is paired, the app sends `POST /api/v1/devices/heartbeat` authenticated with the DEVICE token, once immediately and then every 60 seconds, for the lifetime of the process. It stops when the device has no device token and restarts as soon as pairing completes.
- Send the **telemetry** the tablet already has at hand: `appVersion` (required), `batteryLevel` (0–100), `networkStatus`, `model` (manufacturer + model, ≤100 chars) and `uptimeSeconds`. Every telemetry field is best-effort: a value that cannot be read is omitted, never blocks the heartbeat.
- **Parse and publish the heartbeat response** — `serverTime`, `catalogVersion`, `settingsVersion` and `pendingCommands` — as an observable domain stream, so the follow-up changes `device-remote-commands` and `device-remote-settings` can react to it without making their own network calls.
- A failed heartbeat (network error, timeout, 4xx/5xx) is swallowed and retried on the next tick; it never crashes the app nor surfaces UI to the consumer.

Out of scope (follow-up changes): executing/acking `pendingCommands`, reading `/devices/me/settings` on a `settingsVersion` bump, kiosk lock mode. Until `device-remote-commands` ships, delivered commands are not executed and expire server-side after 10 minutes — the same outcome as today.

## Capabilities

### New Capabilities
- `device-heartbeat`: Periodic DEVICE-authenticated heartbeat while paired, the telemetry it reports, its failure tolerance, and the publication of the heartbeat response (`settingsVersion`, `pendingCommands`, `catalogVersion`) for downstream consumers.

### Modified Capabilities
<!-- None: pairing and store-session requirements are unchanged; the heartbeat only reads the persisted device token. -->

## Impact

- `core/network` — new `DeviceHeartbeatApiService` (`POST /api/v1/devices/heartbeat`, bearer DEVICE token, per-request timeout shorter than the interval) and `DeviceHeartbeatDtos` (request, response, pending command). Registered in `NetworkModule`.
- `core/domain` — new models (`DeviceTelemetry`, `HeartbeatResult`, `DeviceCommand`, `DeviceCommandType`), ports (`DeviceHeartbeatRepository`, `DeviceTelemetryProvider`), use cases (`SendHeartbeatUseCase`, `RunDeviceHeartbeatUseCase`, `ObserveHeartbeatUseCase`) and `DeviceConstants` (interval, model max length).
- `core/data` — `DeviceHeartbeatRepositoryImpl` (DTO ↔ domain mapping, latest-result stream), `DeviceTelemetryProviderImpl` (`PackageManager` version, `BatteryManager`, `ConnectivityDataSource`, `Build`, process uptime), Hilt module.
- `app` — an application-scoped `CoroutineScope` provided via Hilt and `DiscoveryApp.onCreate` launching `RunDeviceHeartbeatUseCase`. No business logic in the app shell.
- `core/testing` — `FakeDeviceHeartbeatRepository`, `FakeDeviceTelemetryProvider`, heartbeat fixtures.
- No new permissions (battery capacity and network state need none beyond the existing `ACCESS_NETWORK_STATE`). No new dependencies. No new Gradle module: the change fits the existing `core:*` layers.
- Backend: none — the endpoint is deployed in production since 2026-10-18.
