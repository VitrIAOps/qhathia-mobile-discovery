## MODIFIED Requirements

### Requirement: Pairing section in settings

The pairing ("Vincular") experience SHALL be a host-agnostic, reusable slice that can be composed by more than one host. It SHALL expose a parameterized header title, an optional back affordance, and a paired-success signal the host can react to. It SHALL be composed by the Settings screen — reachable from the settings side menu alongside the Wi-Fi section, titled TIENDA, with the side menu present and no back affordance — and by the onboarding initial configuration screen — titled QHATHIA, without the side menu, with a back affordance. Switching between the Wi-Fi and Vincular sections in Settings SHALL animate the selection shadow on the side-menu items.

#### Scenario: Switching to the pairing section

- **WHEN** the merchant selects "Vincular" in the settings side menu while the Wi-Fi section is shown
- **THEN** the pairing section is displayed and the selection shadow animates from the Wi-Fi item to the Vincular item

#### Scenario: Switching back to Wi-Fi

- **WHEN** the merchant selects "Wi-Fi" while the pairing section is shown
- **THEN** the Wi-Fi section is displayed and the selection shadow animates back to the Wi-Fi item

#### Scenario: Hosted in Settings

- **WHEN** the pairing slice is composed by the Settings screen
- **THEN** its header is titled TIENDA, the settings side menu is present, and no back affordance is shown

#### Scenario: Hosted in onboarding

- **WHEN** the pairing slice is composed by the onboarding initial configuration screen
- **THEN** its header is titled QHATHIA, the settings side menu is absent, and a back affordance is shown

#### Scenario: Paired-success signal reaches the host

- **WHEN** pairing completes successfully within the pairing slice
- **THEN** the slice emits a paired-success signal that the host can act on (Settings shows the paired-device details in place; onboarding navigates to Home)
