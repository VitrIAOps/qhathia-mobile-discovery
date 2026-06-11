# settings-access Specification

## Purpose
TBD - created by archiving change add-network-settings. Update Purpose after archive.
## Requirements
### Requirement: PIN-gated settings entry

The app SHALL gate access to the in-app settings area behind a numeric PIN. Activating the settings affordance on the Home screen SHALL present the settings-PIN dialog **as an overlay before navigating into any settings content** — no settings screen is shown until the correct PIN is entered. On a correct PIN the app SHALL open the Network settings screen directly. The PIN SHALL be validated through a repository port; because the backend password API does not yet exist, the implementation SHALL validate against a mocked default value of `1234`.

#### Scenario: Settings affordance opens the PIN dialog

- **WHEN** the merchant activates the settings affordance on the Home screen
- **THEN** the app presents the settings-PIN dialog as an overlay
- **AND** the app does not navigate to or show any settings content

#### Scenario: Correct PIN opens Network settings

- **WHEN** the merchant enters the PIN `1234`
- **THEN** the app dismisses the dialog and opens the Network settings screen

#### Scenario: Incorrect PIN is rejected

- **WHEN** the merchant enters a PIN other than `1234`
- **THEN** the app keeps the dialog open, clears the entry, and indicates the PIN was not accepted
- **AND** the app does not show any settings content

#### Scenario: Settings re-prompts on every entry

- **WHEN** the merchant opens settings again after a previous session
- **THEN** the app presents the PIN dialog with a blank entry (the unlock is not remembered)

### Requirement: Settings-PIN dialog presentation

The settings-PIN dialog SHALL follow the Figma "Configuración protegida" design: a secure badge, a title and subtitle, four PIN indicator dots that fill as digits are entered, and a numeric keypad (digits 1–9, 0, and a backspace key). Because the keypad has no separate submit control, the PIN SHALL be validated automatically once the entry reaches the expected length. The dialog SHALL be dismissable without unlocking settings. The dialog SHALL preserve the existing Material 3 design system.

#### Scenario: Entered digits fill the PIN indicators

- **WHEN** the merchant taps keypad digits
- **THEN** the PIN indicator dots fill to reflect how many digits have been entered

#### Scenario: Complete PIN validates automatically

- **WHEN** the entry reaches the expected PIN length
- **THEN** the app validates it without a separate submit action

#### Scenario: Dialog can be dismissed without unlocking

- **WHEN** the merchant dismisses the settings-PIN dialog
- **THEN** the app returns to the previous screen without entering settings

