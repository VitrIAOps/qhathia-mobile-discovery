## Context

`DeviceInfoResponseDto.deviceName` → `PairedDevice.deviceName` (non-null) already arrives; `PairingViewModel` reads `/devices/me` each time Vincular opens. `PairedState` shows store, Actividad, Plataforma and Código usado only.

## Decisions

- **D1 — One row.** `DetailRow("Nombre en la app del comercio", device.deviceName)` first in the device card. No "Modelo" row: not asked by QA.
- **D2 — No heartbeat field.** The backend does not read `deviceName` from the heartbeat.

## Risks / Trade-offs

- [The backend may repeat "Tablet N"] → Backend matter; the tablet shows what the server says.
