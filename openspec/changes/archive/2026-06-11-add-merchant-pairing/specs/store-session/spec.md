## ADDED Requirements

### Requirement: Persisted device access token

The store session SHALL hold a DEVICE access token obtained from the pairing flow, persisted locally so it survives app restarts. When a persisted device token is present, it SHALL be the source of the app's authenticated access token, taking precedence over the development-credential login fallback.

#### Scenario: Device token persisted after pairing

- **WHEN** the pairing flow produces a DEVICE access token and stores it in the session
- **THEN** the token is still available from the session after the app is restarted

#### Scenario: Device token used for authentication

- **WHEN** the app needs an authenticated access token and a persisted device token is present
- **THEN** the persisted device token is used instead of logging in with development credentials

#### Scenario: Fallback when no device token

- **WHEN** the app needs an authenticated access token and no device token has been persisted
- **THEN** the app falls back to the existing development-credential login

### Requirement: Device pairing state observable

The session SHALL expose whether the device is currently paired, so screens can react to pairing and unpairing without a manual refresh.

#### Scenario: Observing the paired state

- **WHEN** a device token is persisted or cleared
- **THEN** observers of the session receive the updated paired state
