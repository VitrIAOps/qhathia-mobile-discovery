# device-pairing Specification

## Purpose
TBD - created by archiving change add-merchant-pairing. Update Purpose after archive.
## Requirements
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

### Requirement: Pairing code generation gated to the pairing screen

A pairing code SHALL be generated only while the merchant is viewing the pairing section. The app SHALL request a code (and its session) from the pairing-token endpoint when the section becomes visible and SHALL NOT generate codes from any other screen.

#### Scenario: Code generated on entering the pairing section

- **WHEN** the merchant opens the pairing section and no valid code is currently held
- **THEN** the app requests a new pairing code and session and displays the returned 5-character code

#### Scenario: No code generated outside the pairing section

- **WHEN** the merchant is anywhere other than the pairing section
- **THEN** the app does not request a pairing code

#### Scenario: Generation failure

- **WHEN** the pairing-token request fails
- **THEN** the pairing section shows an error state with a retry affordance and no code

### Requirement: Pairing code TTL countdown

Each generated code SHALL display a countdown reflecting its remaining lifetime, which SHALL not exceed 5 minutes. When the countdown reaches zero while the merchant is still on the pairing section, the code SHALL be treated as expired.

#### Scenario: Countdown while waiting

- **WHEN** a code has been generated and the merchant remains on the pairing section
- **THEN** a countdown decreases toward zero based on the code's expiry time

#### Scenario: Code expires on screen

- **WHEN** the countdown reaches zero while the pairing section is still visible
- **THEN** the displayed code is marked expired and the app requests a fresh code and session

### Requirement: Pairing status subscription

While a pairing session is active, the app SHALL subscribe to the pairing-session WebSocket and react to the terminal status it pushes (`WAITING`, `APPROVED`, `REJECTED`, `EXPIRED`). The subscription SHALL be torn down when the merchant leaves the pairing section.

#### Scenario: Waiting for approval

- **WHEN** the session WebSocket reports `WAITING`
- **THEN** the pairing section keeps showing the code and countdown in a waiting state

#### Scenario: Approval received

- **WHEN** the session WebSocket reports `APPROVED`
- **THEN** the app proceeds to exchange the session for a device token

#### Scenario: Rejection received

- **WHEN** the session WebSocket reports `REJECTED`
- **THEN** the pairing section shows a rejected state and offers to generate a new code

#### Scenario: Expiry received

- **WHEN** the session WebSocket reports `EXPIRED`
- **THEN** the app treats the code as expired and requests a fresh code and session

#### Scenario: Subscription cleanup

- **WHEN** the merchant leaves the pairing section
- **THEN** the WebSocket subscription is closed and no further status is processed

### Requirement: Device token exchange and persistence

After an `APPROVED` status, the app SHALL exchange the session for a DEVICE access token via the token endpoint and SHALL persist that token so it survives app restarts. A successful exchange SHALL transition the pairing section to the paired state.

#### Scenario: Successful token exchange

- **WHEN** the session is approved and the token exchange succeeds
- **THEN** the device access token and device identity are persisted and the paired-device details are shown

#### Scenario: Token exchange failure

- **WHEN** the token exchange fails after approval
- **THEN** the pairing section shows an error state and allows the merchant to retry the exchange or generate a new code

### Requirement: Paired-device details

When the device is paired, the pairing section SHALL display the paired-device details obtained from the device-info endpoint: device name, platform/model, and last sync time, together with the code used for pairing and the merchant (store) name. Values not available from any API SHALL be shown as pending rather than blank or failing.

#### Scenario: Showing paired details

- **WHEN** the device is paired and the device-info request succeeds
- **THEN** the section shows the device name, platform, last sync time, and the pairing code

#### Scenario: Unavailable values are pending

- **WHEN** a detail (such as the merchant name) is not provided by any API
- **THEN** that field is shown in a pending state rather than empty or erroring

### Requirement: Unpair confirmation (stubbed)

The paired-device view SHALL provide an "Unpair" button that opens a confirmation dialog. Because no unpair API exists yet, confirming SHALL invoke a stubbed data-layer operation marked as a TODO and SHALL NOT silently appear to succeed against the backend.

#### Scenario: Opening the unpair confirmation

- **WHEN** the merchant taps "Unpair"
- **THEN** a confirmation dialog is shown asking the merchant to confirm

#### Scenario: Cancelling unpair

- **WHEN** the merchant dismisses or cancels the confirmation dialog
- **THEN** no unpair action is performed and the paired-device details remain

#### Scenario: Confirming unpair (no API yet)

- **WHEN** the merchant confirms unpairing
- **THEN** the stubbed unpair operation is invoked and the UI communicates that server-side unpairing is not yet available (TODO)

### Requirement: One pairing code request at a time

While a pairing code request is in flight, further requests to generate a code (repeated taps on "Generar nuevo código") SHALL be ignored. Leaving the pairing section SHALL cancel a request in flight.

#### Scenario: Fast taps on "Generar nuevo código"

- **WHEN** the merchant taps "Generar nuevo código" several times in quick succession
- **THEN** a single code request is sent and a single code is shown, with no error

### Requirement: Paired-device loading failure

On a paired tablet, when the paired-device details cannot be loaded, the pairing section SHALL show a connection error ("No pudimos cargar los datos del dispositivo. Revisa la conexión e inténtalo de nuevo.") and its retry action SHALL load the paired-device details again. A paired tablet SHALL never request a pairing code from that section.

#### Scenario: Offline on a paired tablet

- **WHEN** the tablet is paired, has no network and the merchant opens "Vincular"
- **THEN** the connection error is shown, not the code-generation error

#### Scenario: Retry on a paired tablet

- **WHEN** the merchant taps "Reintentar" on that error
- **THEN** the app requests the paired-device details again and does not request a pairing code

