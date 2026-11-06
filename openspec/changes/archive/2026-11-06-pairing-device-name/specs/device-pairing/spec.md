## MODIFIED Requirements

### Requirement: Paired details show real values

On a paired tablet, "Nombre en la app del comercio" SHALL show the device name the server returns, so it matches the tablet's card in the staff app. "Actividad" SHALL show the last sync time, or the last time the tablet was seen by the server when no sync time is known. "Código usado" SHALL show the code saved when the tablet paired, also after a restart; when no code was saved it SHALL show "No disponible". When the device-info endpoint returns a store name, the section SHALL show it and save it for Home.

#### Scenario: Device name

- **WHEN** the device info has `deviceName` "Tablet 3"
- **THEN** "Nombre en la app del comercio" shows "Tablet 3"

#### Scenario: Renamed from the staff app

- **WHEN** the tablet was renamed to "Mostrador" from the staff app and Vincular is opened again
- **THEN** "Nombre en la app del comercio" shows "Mostrador"

#### Scenario: No sync time

- **WHEN** the device info has no `lastSyncAt` and has `lastSeenAt`
- **THEN** "Actividad" shows the time from `lastSeenAt`

#### Scenario: Code after a restart

- **WHEN** the tablet paired with code "2Y567" and the app was restarted
- **THEN** "Código usado" shows "2Y567"

#### Scenario: Store name sent by the server

- **WHEN** the device info includes `storeName` "El Martillo"
- **THEN** the section shows "El Martillo" and Home shows it instead of "TIENDA"
