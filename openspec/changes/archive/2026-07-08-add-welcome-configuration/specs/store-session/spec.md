## MODIFIED Requirements

### Requirement: Persisted device access token

The store session SHALL hold a DEVICE access token obtained from the pairing flow, persisted locally so it survives app restarts. The persisted device token SHALL be the **sole** source of the app's authenticated access token. The app SHALL NOT acquire an access token by logging in with development (or any other) credentials. When no device token has been persisted, the app SHALL have no access token, and authenticated calls SHALL fail cleanly rather than falling back to a login.

#### Scenario: Device token persisted after pairing

- **WHEN** the pairing flow produces a DEVICE access token and stores it in the session
- **THEN** the token is still available from the session after the app is restarted

#### Scenario: Device token used for authentication

- **WHEN** the app needs an authenticated access token and a persisted device token is present
- **THEN** the persisted device token is used

#### Scenario: No token when unpaired

- **WHEN** the app needs an authenticated access token and no device token has been persisted
- **THEN** no token is returned and the authenticated call fails cleanly without attempting a login
