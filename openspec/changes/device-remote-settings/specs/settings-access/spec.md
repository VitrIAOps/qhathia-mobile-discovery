## MODIFIED Requirements

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
