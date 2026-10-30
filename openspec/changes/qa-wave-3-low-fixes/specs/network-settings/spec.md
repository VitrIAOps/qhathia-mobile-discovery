## REMOVED Requirements

### Requirement: Toggle mobile data
**Reason**: Android does not let a non-system app turn mobile data on or off, so the switch always failed with "no permitido" (AJU-041).
**Migration**: Replaced by "Mobile data status", a read-only row.

## ADDED Requirements

### Requirement: Mobile data status

The Network settings screen SHALL show whether mobile data is active as a read-only row ("Activos" / "Inactivos"), with no switch.

#### Scenario: Mobile data active

- **WHEN** mobile data is active
- **THEN** the row shows "Activos" and offers no control to change it

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
