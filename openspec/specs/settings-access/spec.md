# settings-access Specification

## Purpose
TBD - created by archiving change add-network-settings. Update Purpose after archive.
## Requirements
### Requirement: PIN-gated settings entry

The app SHALL gate access to the in-app settings area behind the tablet's remote exit PIN ("PIN de salida", set from the staff app). Activating the settings affordance on the Home screen SHALL, when an exit PIN is set, present the settings-PIN dialog **as an overlay before navigating into any settings content** — no settings screen is shown until the correct PIN is entered. When no exit PIN is set (the stored hash is absent, or settings were never synced), the app SHALL open the Network settings screen directly without showing the dialog. On a correct PIN the app SHALL open the Network settings screen directly. The PIN SHALL be validated through a repository port, offline, against the locally stored BCrypt hash; there SHALL be no hard-coded default PIN.

#### Scenario: Settings affordance opens the PIN dialog

- **WHEN** an exit PIN is set and the merchant activates the settings affordance on the Home screen
- **THEN** the app presents the settings-PIN dialog as an overlay
- **AND** the app does not navigate to or show any settings content

#### Scenario: No exit PIN set

- **WHEN** no exit PIN is set and the merchant activates the settings affordance on the Home screen
- **THEN** the app opens the Network settings screen without showing the PIN dialog

#### Scenario: Correct PIN opens Network settings

- **WHEN** the merchant enters the PIN that matches the stored exit PIN hash
- **THEN** the app dismisses the dialog and opens the Network settings screen

#### Scenario: Incorrect PIN is rejected

- **WHEN** the merchant enters a PIN that does not match the stored exit PIN hash
- **THEN** the app keeps the dialog open, clears the entry, and indicates the PIN was not accepted
- **AND** the app does not show any settings content

#### Scenario: Validation works offline

- **WHEN** the tablet has no network and the merchant enters the correct PIN
- **THEN** the app opens the Network settings screen

#### Scenario: Mocked PIN no longer accepted

- **WHEN** an exit PIN other than `1234` is set and the merchant enters `1234`
- **THEN** the PIN is rejected

#### Scenario: Settings re-prompts on every entry

- **WHEN** the merchant opens settings again after a previous session
- **THEN** the app presents the PIN dialog with a blank entry (the unlock is not remembered)

### Requirement: Settings-PIN dialog presentation

The settings-PIN dialog SHALL follow the Figma "Configuración protegida" design: a secure badge, a title and subtitle, PIN indicator dots that fill as digits are entered, and a numeric keypad (digits 1–9, 0, a confirm key and a backspace key). The PIN SHALL be 4 to 6 digits: the dots SHALL show one filled indicator per entered digit with at least four indicators, the confirm key SHALL be enabled from 4 digits, and reaching 6 digits SHALL validate automatically. While a PIN is being validated the keypad SHALL not accept input. The dialog SHALL be dismissable without unlocking settings. The dialog SHALL preserve the existing Material 3 design system.

#### Scenario: Entered digits fill the PIN indicators

- **WHEN** the merchant taps keypad digits
- **THEN** the PIN indicator dots fill to reflect how many digits have been entered

#### Scenario: Confirm a 4-digit PIN

- **WHEN** the merchant enters 4 digits and taps the confirm key
- **THEN** the app validates the entry

#### Scenario: Confirm disabled for short entries

- **WHEN** fewer than 4 digits are entered
- **THEN** the confirm key is disabled

#### Scenario: Six digits validate automatically

- **WHEN** the entry reaches 6 digits
- **THEN** the app validates it without a separate confirm action and ignores further digits

#### Scenario: Dialog can be dismissed without unlocking

- **WHEN** the merchant dismisses the settings-PIN dialog
- **THEN** the app returns to the previous screen without entering settings

### Requirement: Exit kiosk mode from settings

The settings area SHALL offer a "Salir del modo kiosko" action in its side menu while the kiosk lock is active, and SHALL NOT show it otherwise. The action SHALL be reachable only through the existing settings entry, so it is protected by the exit PIN when one is set and asks for nothing when no PIN is set; it SHALL NOT ask for the PIN again. Activating it SHALL unpin the app and show the system bars so the merchant can leave the app, and SHALL tell the merchant that kiosk mode comes back on returning to Home.

#### Scenario: Exit shown while locked

- **WHEN** the kiosk lock is active and the merchant is in the settings area
- **THEN** the side menu shows "Salir del modo kiosko"

#### Scenario: Exit hidden when not locked

- **WHEN** the kiosk lock is not active (`kioskMode` is `false`, or the merchant already exited)
- **THEN** the side menu does not show "Salir del modo kiosko"

#### Scenario: Exit with an exit PIN set

- **WHEN** an exit PIN is set, the merchant enters it at the settings gate and taps "Salir del modo kiosko"
- **THEN** the app unpins, shows the system bars and shows the temporary-exit message, without asking for the PIN again

#### Scenario: Exit with no PIN set

- **WHEN** no exit PIN is set and the merchant opens settings and taps "Salir del modo kiosko"
- **THEN** the app unpins and shows the system bars without asking for any PIN

#### Scenario: Wrong PIN never exits

- **WHEN** an exit PIN is set and the merchant enters a wrong PIN at the settings gate
- **THEN** the settings area does not open and the app stays pinned

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

