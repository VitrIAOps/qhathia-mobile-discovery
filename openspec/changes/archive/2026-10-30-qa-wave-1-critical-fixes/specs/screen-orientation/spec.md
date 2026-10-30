## ADDED Requirements

### Requirement: App fixed to landscape

The app SHALL always be shown in landscape on every screen (onboarding, pairing, Home, catalog, search, detail, cart, settings and the restart transition) and SHALL NOT rotate to portrait, whatever the device's rotation setting or how the tablet is held. This SHALL also hold on Android 16 and later tablets, where the system ignores orientation locks for apps that target SDK 36 unless the app opts out.

#### Scenario: Tablet turned to portrait

- **WHEN** the tablet is physically turned to portrait while any screen of the app is shown
- **THEN** the app stays in landscape and its screen is not recreated

#### Scenario: Rotation forced by the system setting

- **WHEN** auto-rotate is off and the system rotation is set to portrait
- **THEN** the app is still shown in landscape

#### Scenario: Android 16 large screen

- **WHEN** the app runs on an Android 16 tablet with a smallest width of 600 dp or more
- **THEN** the app is shown in landscape and does not rotate

#### Scenario: App opened while the tablet is in portrait

- **WHEN** the app is launched (or restarted remotely) while the tablet is held in portrait
- **THEN** the first screen is shown in landscape
