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

### Requirement: Device pairing state observable

The session SHALL expose whether the device is currently paired, so screens can react to pairing and unpairing without a manual refresh.

#### Scenario: Observing the paired state

- **WHEN** a device token is persisted or cleared
- **THEN** observers of the session receive the updated paired state

### Requirement: Revocable device session

The store session SHALL support revoking the device session after the tablet is unlinked. Revoking SHALL remove the device access token, the device name, the pairing code and the store name in a single write, and SHALL record a pending unlink notice. Observers SHALL see the session become unpaired.

#### Scenario: Revoke clears the session and the store name

- **WHEN** a paired session with a store name is revoked
- **THEN** the session has no device token, device name, pairing code or store name, it is not paired, and the unlink notice is pending

#### Scenario: Revocation survives a restart

- **WHEN** the session is revoked and the app is restarted
- **THEN** the session is still unpaired and the unlink notice is still pending

### Requirement: Dismissable unlink notice

The store session SHALL expose whether an unlink notice is pending, and SHALL allow dismissing it. Dismissing SHALL clear only the notice, nothing else. A session that was never revoked SHALL have no pending notice.

#### Scenario: Notice dismissed

- **WHEN** the unlink notice is pending and it is dismissed
- **THEN** the session reports no pending notice

#### Scenario: No notice by default

- **WHEN** the app starts with a session that was never revoked
- **THEN** the session reports no pending notice

