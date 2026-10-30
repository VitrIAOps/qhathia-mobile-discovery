## REMOVED Requirements

### Requirement: Toggle mobile data
**Reason**: Android does not let a non-system app turn mobile data on or off, so the switch always failed with "no permitido" (AJU-041).
**Migration**: Replaced by "Mobile data status and shortcut": a status row with a button to Android's mobile-data screen.

## ADDED Requirements

### Requirement: Mobile data status and shortcut

The Network settings screen SHALL show whether mobile data is active ("Activos" / "Inactivos"), or "Sin SIM" when the tablet cannot use mobile data, with no switch. When mobile data can be used, the row SHALL offer "Abrir ajustes de datos", which SHALL exit kiosk mode temporarily (as "Salir del modo kiosko") and open Android's mobile-data screen: the Internet panel on Android 10 or later, the mobile network settings on Android 8 and 9. When the merchant comes back, the row SHALL show the current state.

#### Scenario: Mobile data off

- **WHEN** mobile data is off on a tablet with a SIM
- **THEN** the row shows "Inactivos" and the "Abrir ajustes de datos" button

#### Scenario: Open Android's mobile-data screen

- **WHEN** the merchant taps "Abrir ajustes de datos" while kiosk mode is active
- **THEN** the app leaves kiosk mode for the moment and Android's mobile-data screen opens

#### Scenario: Back after turning data on

- **WHEN** the merchant turns mobile data on in Android's screen and goes back
- **THEN** the Network settings row shows "Activos"
- **AND** the tablet pins again once the merchant goes back to Home

#### Scenario: Tablet without SIM

- **WHEN** the tablet has no SIM
- **THEN** the row shows "Sin SIM" and no button

### Requirement: Wi-Fi scan on demand and after the permission

"Buscar redes" SHALL start a Wi-Fi scan and refresh the list with the results. When the location permission becomes granted while the screen is open, the app SHALL start a scan right away.

#### Scenario: Buscar redes

- **WHEN** Wi-Fi is on and the merchant taps "Buscar redes"
- **THEN** a scan starts and the list shows the latest results

#### Scenario: First visit

- **WHEN** the merchant opens the screen for the first time and grants the location permission
- **THEN** the list shows the nearby networks without leaving the screen

### Requirement: Location permission can be requested again

When the location permission is missing, the networks area SHALL say that it is needed to list networks and SHALL offer "Permitir ubicación", which requests it again. When Android no longer shows the request, the message SHALL say to allow it from the device settings.

#### Scenario: Permission refused before

- **WHEN** the location permission was refused and the merchant taps "Permitir ubicación"
- **THEN** the permission is requested again
