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

A pairing code SHALL be generated only while the merchant is viewing the pairing section. The app SHALL request a code (and its session) from the pairing-token endpoint when the section becomes visible and no valid code is held, and SHALL NOT generate codes from any other screen. When the pairing section is recreated (screen turned off and on, leaving and coming back, a configuration change) while the held code is still valid, the app SHALL keep showing that same code with its remaining time instead of generating a new one.

#### Scenario: Code generated on entering the pairing section

- **WHEN** the merchant opens the pairing section and no valid code is currently held
- **THEN** the app requests a new pairing code and session and displays the returned 5-character code

#### Scenario: Screen off and on keeps the code

- **WHEN** a code is shown and the screen is turned off and on again before the code expires
- **THEN** the same code is shown with its remaining time, no new code is requested, and its status keeps being followed

#### Scenario: Leaving and coming back keeps the code

- **WHEN** the merchant leaves the pairing section and comes back before the code expires
- **THEN** the same code is shown and no new code is requested

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

While a pairing session is active and the pairing section is visible, the app SHALL follow the session status through the pairing-session WebSocket and react to the status it pushes (`WAITING`, `APPROVED`, `REJECTED`, `EXPIRED`). The app SHALL send a WebSocket ping every 20 seconds. If the WebSocket closes or fails before a final status while the code is valid, the app SHALL reconnect it with backoff, and SHALL reconnect immediately when network connectivity comes back. Because the server sends the current status on every connect, a status pushed while the tablet was disconnected SHALL be received on reconnect. While reconnecting, the pairing section SHALL keep showing the waiting state, not an error. The app SHALL NOT poll the token exchange endpoint. The subscription SHALL be torn down when the merchant leaves the pairing section.

#### Scenario: Waiting for approval

- **WHEN** the session WebSocket reports `WAITING`
- **THEN** the pairing section keeps showing the code and countdown in a waiting state

#### Scenario: Approval received

- **WHEN** the session WebSocket reports `APPROVED`
- **THEN** the app proceeds to exchange the session for a device token

#### Scenario: Idle connection kept open

- **WHEN** a code is shown and the merchant has not approved it for more than 60 seconds
- **THEN** the WebSocket stays open (or is reopened) and an approval sent afterwards completes pairing

#### Scenario: Approval pushed while the network was down

- **WHEN** the tablet loses connectivity for 20 seconds while the merchant approves the code
- **THEN** once connectivity returns the app reconnects, receives `APPROVED` and completes pairing without the merchant doing anything else

#### Scenario: Socket drop is not an error

- **WHEN** the WebSocket closes or fails while the code is still valid
- **THEN** the pairing section keeps the waiting state and countdown and reconnects in the background

#### Scenario: No polling of the exchange endpoint

- **WHEN** the app is waiting for approval with a working or reconnecting WebSocket
- **THEN** no request is sent to the token exchange endpoint

#### Scenario: Rejection received

- **WHEN** the session WebSocket reports `REJECTED`
- **THEN** the pairing section shows a rejected state and offers to generate a new code

#### Scenario: Expiry received

- **WHEN** the session WebSocket reports `EXPIRED`
- **THEN** the app treats the code as expired and requests a fresh code and session

#### Scenario: Subscription cleanup

- **WHEN** the merchant leaves the pairing section
- **THEN** the WebSocket subscription is closed, reconnection stops, and no further status is processed

### Requirement: Device token exchange and persistence

After an `APPROVED` status, the app SHALL exchange the session for a DEVICE access token via the token endpoint exactly once per session, and SHALL persist that token so it survives app restarts. A connectivity error during the exchange SHALL be retried; a `410 PAIRING_TOKEN_ALREADY_USED` SHALL NOT be retried. A successful exchange SHALL transition the pairing section to the paired state. When the server rejects the WebSocket handshake, the app SHALL call the exchange once as a fallback and act on its answer: paired on 200, keep reconnecting on `409 PAIRING_NOT_APPROVED`, rejected on `409 PAIRING_REJECTED`, and a new code on `410 PAIRING_TOKEN_EXPIRED` or 404.

#### Scenario: Successful token exchange

- **WHEN** the session is approved and the token exchange succeeds
- **THEN** the device access token and device identity are persisted and the paired-device details are shown

#### Scenario: Exchanged only once

- **WHEN** `APPROVED` is received again after a reconnect and the exchange already succeeded
- **THEN** no second exchange request is sent

#### Scenario: Handshake rejected for an approved session

- **WHEN** the server rejects the WebSocket handshake and the fallback exchange answers 200
- **THEN** the device token is persisted and pairing completes

#### Scenario: Handshake rejected for an expired session

- **WHEN** the server rejects the WebSocket handshake and the fallback exchange answers 410 `PAIRING_TOKEN_EXPIRED`
- **THEN** the app requests a fresh code and session

#### Scenario: Token exchange failure

- **WHEN** the token exchange fails after approval with something other than a connectivity error
- **THEN** the pairing section shows an error state and allows the merchant to generate a new code

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

### Requirement: Screen stays on while a code is shown

While the pairing section shows a pairing code, the screen SHALL stay on, in onboarding and in Settings, whether or not kiosk mode is active.

#### Scenario: Code on screen for more than a minute

- **WHEN** a pairing code is shown during onboarding and nobody touches the tablet for 2 minutes
- **THEN** the screen is still on and the code is visible

### Requirement: Paired details show real values

On a paired tablet, "Actividad" SHALL show the last sync time, or the last time the tablet was seen by the server when no sync time is known. "Código usado" SHALL show the code saved when the tablet paired, also after a restart; when no code was saved it SHALL show "No disponible". When the device-info endpoint returns a store name, the section SHALL show it and save it for Home.

#### Scenario: No sync time

- **WHEN** the device info has no `lastSyncAt` and has `lastSeenAt`
- **THEN** "Actividad" shows the time from `lastSeenAt`

#### Scenario: Code after a restart

- **WHEN** the tablet paired with code "2Y567" and the app was restarted
- **THEN** "Código usado" shows "2Y567"

#### Scenario: Store name sent by the server

- **WHEN** the device info includes `storeName` "El Martillo"
- **THEN** the section shows "El Martillo" and Home shows it instead of "TIENDA"

