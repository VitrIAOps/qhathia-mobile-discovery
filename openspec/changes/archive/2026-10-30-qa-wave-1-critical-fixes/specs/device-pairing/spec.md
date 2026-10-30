## MODIFIED Requirements

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
