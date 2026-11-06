## ADDED Requirements

### Requirement: Progressive settings PIN lock

After 5 consecutive wrong settings PINs the gate SHALL lock. Each lock SHALL last longer than the previous one: 30 seconds, then 1, 2, 4 and 8 minutes, and then 15 minutes for every further lock. When a lock ends the customer gets 5 new attempts. A correct PIN, or a new or removed exit PIN from the staff app, SHALL reset the lock level. The lock SHALL survive closing the dialog and restarting the app. While locked, the dialog SHALL disable the keypad and show the time left as minutes and seconds, refreshed every second. This replaces the previous 24-hour lock.

#### Scenario: First lock lasts 30 seconds

- **WHEN** 5 wrong PINs are entered in a row
- **THEN** the keypad is disabled and the dialog says to try again in 0:30, counting down every second

#### Scenario: Next lock lasts longer

- **WHEN** the first lock ended and 5 more wrong PINs are entered
- **THEN** the gate locks for 1:00

#### Scenario: Lock survives a restart

- **WHEN** the gate is locked and the app is force-stopped and reopened
- **THEN** the gate is still locked with the time left

#### Scenario: Correct PIN resets the level

- **WHEN** a correct PIN is entered after a lock ended
- **THEN** the next lock, if any, lasts 30 seconds again

### Requirement: PIN lockout reported to the backend

Each time the gate locks, the app SHALL send `POST /api/v1/devices/logs` with level `WARN`, code `SETTINGS_PIN_LOCKOUT`, and the failed attempts and lock seconds as context. It SHALL never send the PIN or its hash. A failed report SHALL NOT change what the screen shows.

#### Scenario: Lock is reported once

- **WHEN** the 5th wrong PIN locks the gate
- **THEN** exactly one lockout log is sent
