## MODIFIED Requirements

### Requirement: Pinning requested again on Home

While the kiosk lock is active and the task is not pinned (for example because the "Pin app?" confirmation was refused or dismissed), the app SHALL request pinning again when the app returns to the Home screen, on a touch on Home, and every 15 seconds while Home is shown and the app is in the foreground without any touch, at most once every 15 seconds in total. Once the task is pinned it SHALL request nothing.

#### Scenario: Refused pinning is asked again on Home

- **WHEN** the kiosk lock is active, the pinning confirmation was refused, and the consumer returns to Home
- **THEN** the app requests pinning again

#### Scenario: Asked again without a touch

- **WHEN** the kiosk lock is active, the pinning confirmation was refused on Home, and nobody touches the screen
- **THEN** the confirmation is shown again within 15 seconds

#### Scenario: Not asked in a loop

- **WHEN** the consumer refuses the pinning confirmation and keeps touching Home
- **THEN** pinning is requested at most once every 15 seconds

## ADDED Requirements

### Requirement: Pinning without the dialog as device owner

When the app is the device owner, it SHALL allow itself in lock task mode, so pinning happens without the system confirmation, SHALL hide Home, Recents and notifications while pinned (Android 9+), and SHALL make itself the home screen. Leaving kiosk mode from Configuración SHALL keep working. When the app is not the device owner, this SHALL change nothing.

#### Scenario: Device owner pins at once

- **WHEN** the app is device owner and the kiosk lock applies
- **THEN** the task is locked within 2 seconds and no "Pin app?" confirmation is shown

#### Scenario: Not device owner

- **WHEN** the app is not device owner
- **THEN** pinning still uses the system confirmation, as before
