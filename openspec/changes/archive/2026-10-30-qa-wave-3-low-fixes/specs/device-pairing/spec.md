## ADDED Requirements

### Requirement: Screen stays on while a code is shown

While the pairing section shows a pairing code, the screen SHALL stay on, in onboarding and in Settings, whether or not kiosk mode is active.

#### Scenario: Code on screen for more than a minute

- **WHEN** a pairing code is shown during onboarding and nobody touches the tablet for 2 minutes
- **THEN** the screen is still on and the code is visible

### Requirement: Paired details show real values

On a paired tablet, "Actividad" SHALL show the last sync time, or the last time the tablet was seen by the server when no sync time is known. "Código usado" SHALL show the code saved when the tablet paired, also after a restart; when no code was saved it SHALL show "No disponible". When the device-info endpoint returns a store name, the section SHALL show it and save it for Home.

#### Scenario: No sync time

- **WHEN** the device info has no `lastSyncAt` and has `lastSeenAt`
- **THEN** "Actividad" shows the time from `lastSeenAt`

#### Scenario: Code after a restart

- **WHEN** the tablet paired with code "2Y567" and the app was restarted
- **THEN** "Código usado" shows "2Y567"

#### Scenario: Store name sent by the server

- **WHEN** the device info includes `storeName` "El Martillo"
- **THEN** the section shows "El Martillo" and Home shows it instead of "TIENDA"
