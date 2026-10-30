## ADDED Requirements

### Requirement: Pinning requested again on Home

While the kiosk lock is active and the task is not pinned (for example because the "Pin app?" confirmation was refused or dismissed), the app SHALL request pinning again when the app returns to the Home screen and on a touch on Home, at most once every 15 seconds.

#### Scenario: Refused pinning is asked again on Home

- **WHEN** the kiosk lock is active, the pinning confirmation was refused, and the consumer returns to Home
- **THEN** the app requests pinning again

#### Scenario: Not asked in a loop

- **WHEN** the consumer refuses the pinning confirmation and keeps touching Home
- **THEN** pinning is requested at most once every 15 seconds

### Requirement: System bars stay hidden over dialogs and the keyboard

While the kiosk lock is active, the system status and navigation bars SHALL stay hidden when an app dialog is shown, when the on-screen keyboard opens or closes, and when the app window regains focus. A swipe MAY still show them briefly.

#### Scenario: Cart dialog does not reveal the bars

- **WHEN** the kiosk lock is active and the cart dialog opens
- **THEN** the system bars stay hidden and no app control is covered by them

#### Scenario: Keyboard closing does not leave the bars visible

- **WHEN** the consumer types in a dialog and the keyboard closes
- **THEN** the system bars are hidden again

#### Scenario: Focus regained hides the bars

- **WHEN** a system dialog or the pinning confirmation goes away and the app window regains focus
- **THEN** the system bars are hidden again
