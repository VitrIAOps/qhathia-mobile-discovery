## ADDED Requirements

### Requirement: Version-driven settings sync

The app SHALL read the tablet's remote settings with `GET /api/v1/devices/me/settings` (DEVICE token) whenever a heartbeat reports a `settingsVersion` different from the locally stored one, and SHALL persist the returned settings, their version and the exit PIN hash locally. It SHALL NOT read the settings when the reported version equals the stored one, and SHALL NOT poll the endpoint on its own. A failed read SHALL be retried on a later heartbeat and SHALL NOT show any UI. Stored settings SHALL survive app restarts and be available without network.

#### Scenario: First heartbeat after install

- **WHEN** no settings are stored and a heartbeat reports `settingsVersion` 4
- **THEN** the app reads the remote settings once and stores them with version 4

#### Scenario: Version unchanged

- **WHEN** the stored version is 4 and a heartbeat reports `settingsVersion` 4
- **THEN** the app does not read the remote settings

#### Scenario: Merchant changes a setting

- **WHEN** the stored version is 4 and a heartbeat reports `settingsVersion` 5
- **THEN** the app reads the remote settings and stores version 5

#### Scenario: Read fails

- **WHEN** reading the remote settings fails
- **THEN** nothing is shown, the stored settings stay as they were, and the next heartbeat with a different version triggers a new read

#### Scenario: Restart offline

- **WHEN** the app restarts without network after settings were stored
- **THEN** the stored settings are applied

### Requirement: Voice assistant setting

When the stored `voiceAssistantEnabled` is `false`, the app SHALL hide every microphone control (Home, Catalog, Product search, Product detail) so voice search cannot be started. When it is `true` or no settings are stored, microphone controls SHALL be shown as today. A change SHALL apply without restarting the app.

#### Scenario: Voice assistant turned off

- **WHEN** the stored settings have `voiceAssistantEnabled = false`
- **THEN** no microphone button is shown on Home, Catalog, Product search or Product detail, and text search keeps working

#### Scenario: Voice assistant turned back on

- **WHEN** a later sync stores `voiceAssistantEnabled = true`
- **THEN** the microphone buttons reappear without restarting the app

### Requirement: Brightness setting

The app SHALL apply the stored `brightnessMode` to its window: `AUTO` SHALL leave the system brightness in control; `LOW`, `MEDIUM` and `HIGH` SHALL set the window brightness to 30 %, 60 % and 100 % respectively. With no stored settings the mode SHALL be `AUTO`. An unknown value SHALL be treated as `AUTO`.

#### Scenario: High brightness

- **WHEN** the stored `brightnessMode` is `HIGH`
- **THEN** the app window brightness is set to the maximum

#### Scenario: Back to automatic

- **WHEN** the stored `brightnessMode` changes to `AUTO`
- **THEN** the app stops overriding the brightness and the system setting applies

### Requirement: Settings without an applied effect are stored

`kioskMode`, `idleShowcaseEnabled` and `autoSyncEnabled` SHALL be stored with the other settings so later changes can apply them, and SHALL have no visible effect in this version.

#### Scenario: Kiosk mode toggled

- **WHEN** a sync stores `kioskMode = false`
- **THEN** the value is persisted and the app behaves as before
