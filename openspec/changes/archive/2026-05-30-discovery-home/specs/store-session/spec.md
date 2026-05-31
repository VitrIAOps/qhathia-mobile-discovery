## ADDED Requirements

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
