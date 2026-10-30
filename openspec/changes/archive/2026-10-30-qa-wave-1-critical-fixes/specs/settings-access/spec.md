## ADDED Requirements

### Requirement: Settings PIN locks for 24 hours after 5 wrong attempts

The settings PIN gate SHALL lock after 5 consecutive wrong PINs. The lock SHALL last 24 hours from the 5th wrong attempt. While locked, the keypad SHALL not accept input and the dialog SHALL show that there were too many attempts and how much time is left. When the 24 hours have passed, the lock and the count SHALL be cleared and the merchant SHALL get 5 new attempts. A correct PIN entered before the lock SHALL reset the count. The count and the lock SHALL survive closing the dialog, restarting the app and rebooting the tablet. Only wrong PINs SHALL count as attempts; network or validation errors SHALL NOT.

#### Scenario: Fifth wrong PIN locks the gate

- **WHEN** the merchant enters a wrong PIN 5 times in a row
- **THEN** the keypad is disabled and the dialog shows the time left of a 24-hour lock

#### Scenario: Lock survives a restart

- **WHEN** the gate is locked and the app is restarted
- **THEN** reopening the gate still shows it locked with the remaining time

#### Scenario: Lock ends after 24 hours

- **WHEN** 24 hours have passed since the lock started
- **THEN** the keypad accepts input again and the merchant has 5 new attempts

#### Scenario: Clock moved back does not shorten the lock

- **WHEN** the gate is locked and the device clock moves to a time before the lock started
- **THEN** the gate stays locked and the 24 hours count from the current time

#### Scenario: Correct PIN resets the count

- **WHEN** the merchant enters the correct PIN after fewer than 5 wrong attempts
- **THEN** the settings open and the wrong-attempt count goes back to zero

### Requirement: A new PIN from the staff app lifts the lock

When the exit PIN delivered by the remote device settings changes (a new PIN is set or the PIN is removed), the app SHALL clear the settings PIN lock and the wrong-attempt count.

#### Scenario: Merchant resets the PIN remotely

- **WHEN** the gate is locked and the staff app sends a new exit PIN to the tablet
- **THEN** the gate unlocks and accepts the new PIN with 5 attempts available

#### Scenario: PIN removed remotely

- **WHEN** the gate is locked and the staff app removes the exit PIN
- **THEN** the lock is cleared and the settings open without asking for a PIN
