## Why

The staff app's "Dispositivos" card has two buttons — "Actualizar catálogo" and "Reiniciar" — that queue `REFRESH_CATALOG` / `RESTART_APP` commands (api-contract.md §23, Phase 23). Since `device-heartbeat`, the tablet receives them in `pendingCommands`, but nothing executes or acknowledges them: the merchant sees "Enviado" until the command expires. A delivered command is never re-delivered, so the tablet must act on it and report back (handoff §1, BUG-025).

## What Changes

- **Execute delivered commands** in arrival order, once each, for the lifetime of the process:
  - `REFRESH_CATALOG` → an open Catálogo screen re-fetches the catalog in the background and swaps in the fresh products, keeping the selected tab; nothing visible happens on other screens (they already read live data when opened).
  - `RESTART_APP` → the app acknowledges first, then relaunches itself (fresh process, back at the start destination).
  - Any other command → acknowledged as failed with "Comando no soportado: <command>".
- **Acknowledge** each command with `POST /api/v1/devices/me/commands/{commandId}/ack` (`success`, optional `detail` ≤ 300 chars), retrying a failed ack a few times before giving up. The staff card turns "Listo" or "Falló: <detail>".
- A command failure or an ack failure never crashes the app nor shows UI to the consumer.

## Capabilities

### New Capabilities
- `device-remote-commands`: Execution of heartbeat-delivered remote commands (`REFRESH_CATALOG`, `RESTART_APP`, unknown), exactly-once processing, acknowledgement with retries, and ack-before-restart ordering.

### Modified Capabilities
- `product-catalog`: An open catalog screen refreshes its products in place when a remote catalog refresh is requested, without resetting the selected tab or showing a loading/error state.

## Impact

- `core/network` — new `DeviceCommandApiService.ack(deviceToken, commandId, body)` and `DeviceCommandAckDto`.
- `core/domain` — ports `DeviceCommandRepository` (ack), `CatalogRefreshSignal` (request/observe), `AppRestarter`; use cases `ProcessDeviceCommandsUseCase`, `ObserveCatalogRefreshUseCase`; `DeviceConstants` gains ack retry/detail-length constants.
- `core/data` — `DeviceCommandRepositoryImpl`, in-memory `CatalogRefreshSignalImpl`, `AppRestarterImpl` (launch intent + process exit), Hilt bindings.
- `features/catalog` — `CatalogViewModel` subscribes to the refresh signal and reloads silently.
- `app` — `DiscoveryApp` also launches `ProcessDeviceCommandsUseCase` in the application scope.
- `core/testing` — fakes for the new ports.
- No new permissions, no new dependencies, no new Gradle module. Backend already deployed.
