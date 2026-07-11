# app-onboarding Specification

## Purpose
Define the first-run onboarding experience that routes the app on launch based on device pairing state, guiding an unpaired merchant through a Welcome screen and an initial configuration (pairing) screen, and entering Home only once the device is successfully paired.
## Requirements
### Requirement: Startup routing by pairing state

On launch the app SHALL decide its first destination from the persisted pairing state: a device with no persisted device token SHALL open the onboarding Welcome flow, and an already-paired device SHALL open Home directly. The decision SHALL be made before any authenticated content is shown, and SHALL NOT flash Home for an unpaired device.

#### Scenario: Unpaired device opens onboarding

- **WHEN** the app launches and no device token is persisted
- **THEN** the Welcome screen is shown as the first destination and Home is not shown

#### Scenario: Paired device opens Home

- **WHEN** the app launches and a device token is persisted
- **THEN** the Home (voice search) screen is shown as the first destination and the onboarding flow is not shown

### Requirement: Welcome screen

The onboarding flow SHALL start with a Welcome screen that displays a bienvenida title, the Qhathia application name, a short supporting message, and a "Continuar" button. Activating "Continuar" SHALL navigate to the initial configuration screen. The screen SHALL preserve the existing Material 3 design system and provide a `@Preview` in light and dark themes.

#### Scenario: Welcome content is shown

- **WHEN** the Welcome screen is displayed
- **THEN** it shows the bienvenida title, the Qhathia app name, the supporting message, and a "Continuar" button

#### Scenario: Continue navigates to initial configuration

- **WHEN** the merchant taps "Continuar"
- **THEN** the app navigates to the initial configuration screen

### Requirement: Initial configuration screen

The initial configuration screen SHALL present the pairing ("Vincular") experience without the settings side menu, with its header titled **QHATHIA** (not TIENDA), and with a back affordance that returns to the Welcome screen. All pairing validations and states (code generation, TTL countdown, status subscription, token exchange, error/rejected/expired handling) SHALL behave identically to the pairing section reached from Settings.

#### Scenario: Initial configuration reuses pairing without the side menu

- **WHEN** the initial configuration screen is displayed
- **THEN** it shows the pairing experience with the header titled "QHATHIA" and without the settings side menu

#### Scenario: Back returns to Welcome

- **WHEN** the merchant activates the back affordance on the initial configuration screen
- **THEN** the app returns to the Welcome screen

#### Scenario: Pairing validations are preserved

- **WHEN** the merchant is on the initial configuration screen
- **THEN** the pairing code, TTL countdown, status subscription, and token exchange behave the same as in the Settings pairing section

### Requirement: Navigate to Home only on successful pairing

The initial configuration screen SHALL advance to Home (voice search) only when pairing completes successfully (an approved session whose device-token exchange succeeds and is persisted). Any other outcome — waiting, rejected, expired, generation failure, or token-exchange failure — SHALL keep the merchant in the onboarding flow and SHALL NOT navigate to Home. After navigating to Home, the onboarding flow SHALL NOT remain on the back stack.

#### Scenario: Successful pairing enters Home

- **WHEN** pairing completes successfully and the device token is persisted from the initial configuration screen
- **THEN** the app navigates to Home (voice search)
- **AND** the onboarding destinations are removed from the back stack

#### Scenario: Unsuccessful pairing stays in onboarding

- **WHEN** pairing is rejected, expires, or fails before a device token is persisted
- **THEN** the app remains on the initial configuration screen and does not navigate to Home
