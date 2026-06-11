# store-session Specification

## Purpose
TBD - created by archiving change discovery-home. Update Purpose after archive.
## Requirements
### Requirement: Persisted store session

The app SHALL maintain a store (merchant) session that holds values needed across the app for the store the device operates for, starting with the store name. The session SHALL be persisted locally so its values survive app restarts.

#### Scenario: Stored values survive restart

- **WHEN** a store session value has been saved and the app is restarted
- **THEN** the previously saved value is still available from the store session

#### Scenario: Updating a session value

- **WHEN** a new store name is written to the store session
- **THEN** the persisted store session reflects the new store name

### Requirement: Observable store session

The store session SHALL be exposed to the presentation layer as an observable stream, so screens that display session values update reactively when those values change.

#### Scenario: Observers receive the current value

- **WHEN** a screen starts observing the store session
- **THEN** it receives the current session values immediately

#### Scenario: Observers receive updates

- **WHEN** a store session value changes while a screen is observing it
- **THEN** the observing screen receives the updated value without a manual refresh

### Requirement: Empty session is well defined

When no values have been stored yet, the store session SHALL expose a well-defined empty state rather than failing.

#### Scenario: Empty session

- **WHEN** the store session is read before any value has been written
- **THEN** it returns an empty session (e.g., a null/blank store name) without throwing

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

