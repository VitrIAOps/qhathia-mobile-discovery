## ADDED Requirements

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
