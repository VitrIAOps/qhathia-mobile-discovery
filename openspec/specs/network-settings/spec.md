# network-settings Specification

## Purpose
TBD - created by archiving change add-network-settings. Update Purpose after archive.
## Requirements
### Requirement: Network settings screen layout

The Network settings screen SHALL follow the Figma "Red Wifi" structure and preserve the existing Material 3 design system. It SHALL be the settings landing reached after the PIN gate and SHALL present a **two-pane layout**: a fixed side menu (store title + a selected **Wi-Fi** section and a **Vincular** section) and a main panel with a toolbar (store name + a **Buscar redes** action), a heading and description, the retained connectivity controls, and the accessible-network list. The main panel SHALL present a **mobile data** switch positioned **above** the **Wi-Fi** on/off control, and the list of accessible Wi-Fi networks beneath them. Any processing or loading indication SHALL use the shared design-system components.

#### Scenario: Two-pane layout with side menu

- **WHEN** the Network settings screen is shown
- **THEN** a side menu with the Wi-Fi section and a main panel with the network list are displayed

#### Scenario: Mobile data switch sits above Wi-Fi

- **WHEN** the Network settings screen is shown
- **THEN** the mobile data switch is displayed above the Wi-Fi section

#### Scenario: Wi-Fi section shows control and network list

- **WHEN** the Network settings screen is shown
- **THEN** a Wi-Fi on/off control is displayed
- **AND** the list of accessible Wi-Fi networks is displayed beneath it

### Requirement: Toggle Wi-Fi

The app SHALL let the merchant turn Wi-Fi on or off from the Network settings screen. When Wi-Fi is off, the accessible-networks list SHALL be empty or hidden. When Wi-Fi is turned on, the app SHALL begin discovering accessible networks.

#### Scenario: Turn Wi-Fi on starts discovery

- **WHEN** Wi-Fi is off and the merchant turns Wi-Fi on
- **THEN** the app begins discovering accessible Wi-Fi networks
- **AND** the Wi-Fi control reflects the on state

#### Scenario: Turn Wi-Fi off clears the list

- **WHEN** Wi-Fi is on and the merchant turns Wi-Fi off
- **THEN** the accessible-networks list is empty or hidden
- **AND** the Wi-Fi control reflects the off state

### Requirement: List accessible Wi-Fi networks

When Wi-Fi is enabled, the app SHALL list the accessible Wi-Fi networks discovered through standard Android APIs. Each entry SHALL identify the network by name (SSID) and SHALL indicate whether the network is secured. The currently connected network SHALL be distinguishable in the list.

#### Scenario: Discovered networks are listed

- **WHEN** Wi-Fi is on and accessible networks are discovered
- **THEN** each network is listed with its name and a secured/open indication

#### Scenario: Connected network is marked

- **WHEN** the device is connected to a Wi-Fi network that appears in the list
- **THEN** that network is shown as the connected network

### Requirement: Wi-Fi password dialog for secured networks

When the merchant selects a secured Wi-Fi network that needs a password, the app SHALL present a Wi-Fi password dialog. The dialog SHALL include a close ("X") control in the top-right that dismisses it, a Wi-Fi icon of approximately 56dp, the selected network's name, a password `TextField`, and a submit button that sends the password for validation. Selecting an open network SHALL NOT show the password dialog.

#### Scenario: Secured network prompts for a password

- **WHEN** the merchant selects a secured Wi-Fi network
- **THEN** the app shows the Wi-Fi password dialog displaying that network's name

#### Scenario: Dialog can be closed without connecting

- **WHEN** the merchant activates the close control on the Wi-Fi password dialog
- **THEN** the dialog is dismissed and no connection attempt is made

#### Scenario: Open network skips the password dialog

- **WHEN** the merchant selects an open (unsecured) Wi-Fi network
- **THEN** the app does not show the Wi-Fi password dialog

### Requirement: Connect to a Wi-Fi network

The app SHALL attempt to connect to the Wi-Fi network selected by the merchant — using the submitted password for a secured network, or directly for an open network. The screen SHALL reflect the connection progress and the resulting connected or failed state using the design system.

#### Scenario: Submit password connects to a secured network

- **WHEN** the merchant submits the password in the Wi-Fi password dialog
- **THEN** the app attempts to connect to that network using the submitted password
- **AND** the screen reflects the connection outcome

#### Scenario: Connect to an open network

- **WHEN** the merchant selects an open network
- **THEN** the app attempts to connect to that network without a password

### Requirement: Airplane-mode guard on network selection

A network connection SHALL only be selectable when the device is not in airplane mode. While airplane mode is active, the app SHALL prevent selecting or changing a network connection and SHALL make this state evident to the merchant.

#### Scenario: Selection blocked in airplane mode

- **WHEN** the device is in airplane mode
- **THEN** the app does not allow selecting or changing a network connection
- **AND** the screen indicates that airplane mode is active

#### Scenario: Selection allowed when airplane mode is off

- **WHEN** the device is not in airplane mode
- **THEN** the merchant can select a network connection

### Requirement: Mobile data status and shortcut

The Network settings screen SHALL show whether mobile data is active ("Activos" / "Inactivos"), or "Sin SIM" when the tablet cannot use mobile data, with no switch. When mobile data can be used, the row SHALL offer "Abrir ajustes de datos", which SHALL exit kiosk mode temporarily (as "Salir del modo kiosko") and open Android's mobile-data screen: the Internet panel on Android 10 or later, and Android's "Red e Internet" settings (one step from mobile data) on Android 8 and 9. When the merchant comes back, the row SHALL show the current state.

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

