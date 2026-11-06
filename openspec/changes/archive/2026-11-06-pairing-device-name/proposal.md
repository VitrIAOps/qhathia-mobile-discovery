## Why

Settings → Vincular does not show the name the store knows the tablet by, so with two tablets nobody can tell which card in the staff app is which (QA regression 2026-11-03, F-59 / X:XVI-030). `/devices/me` already returns `deviceName` and the app already reads it; the screen just does not show it.

## What Changes

- The paired-device details show **"Nombre en la app del comercio"** with `deviceName`, above "Actividad".
- A rename from the staff app shows when Vincular is opened again (it reads `/devices/me` each time).
- The name is not sent in the heartbeat (the backend ignores it).

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `device-pairing`: the paired details show the tablet's name in the staff app.

## Impact

- `features:pairing`: `PairingContent.PairedState`, string `pairing_device_name_label`.
- No new module, library, endpoint or manifest change. Repeated "Tablet N" names are a backend matter.
