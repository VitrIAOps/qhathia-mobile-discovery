# device-unlink Specification

## Purpose
How the tablet detects that the merchant unlinked it (`401 DEVICE_UNLINKED` on any call), revokes its local device session and store data, and returns to Welcome, without reacting to other errors.

## Requirements
### Requirement: Detect an unlinked tablet on any call

The app SHALL treat an HTTP `401` whose problem detail has `errorCode` `DEVICE_UNLINKED` as "this tablet was unlinked from the store", whichever service answers (auth, biz or vo) and whichever call made it: heartbeat, settings, command ack, paired device, voice token, catalog, product detail, suggestions, search, order or share. Detection SHALL happen once, for all calls, and each call SHALL still receive its own error result.

#### Scenario: Heartbeat answers DEVICE_UNLINKED

- **WHEN** the tablet is paired and the heartbeat answers `401` with `errorCode` `DEVICE_UNLINKED`
- **THEN** the app revokes the local device session (see "Revoke the local device session")

#### Scenario: A business call answers DEVICE_UNLINKED

- **WHEN** the tablet is paired and opening the catalog answers `401` with `errorCode` `DEVICE_UNLINKED`
- **THEN** the app revokes the local device session, and the catalog call still resolves to its own failure

### Requirement: Only DEVICE_UNLINKED revokes

The app SHALL NOT revoke the device session on any other HTTP error. That includes a `401` with another `errorCode` or none, any `403` (for example `403 FORBIDDEN` for a write the DEVICE token may not do), and errors from third-party services such as the speech-to-text provider.

#### Scenario: Forbidden is not unlink

- **WHEN** a call answers `403` with `errorCode` `FORBIDDEN`
- **THEN** the device session stays as it is

#### Scenario: Bare 401 is not unlink

- **WHEN** a call answers `401` without a problem body or with an `errorCode` other than `DEVICE_UNLINKED`
- **THEN** the device session stays as it is

### Requirement: Revoke the local device session

When an unlink is detected and the token of the rejected request is the device token currently stored, the app SHALL, in this order:

1. end the customer session (basket and consumer forms);
2. clear the stored remote device settings (exit PIN hash, kiosk mode, idle showcase, voice assistant, brightness, auto-sync and settings version);
3. drop the held pairing code and the "exited kiosk mode from Settings" flag;
4. clear the stored device session: device token, device name, pairing code and store name.

As a consequence the app SHALL leave kiosk mode (the pinned task is released), stop the heartbeat, and show the Welcome screen with the unlink notice. It SHALL do so within one rejected call, so at most one heartbeat interval after the backend starts rejecting the token.

#### Scenario: Unlinked tablet returns to Welcome

- **WHEN** the merchant unlinks the tablet from the staff app and the next heartbeat answers `401 DEVICE_UNLINKED`
- **THEN** the tablet shows Welcome with the unlink notice, is no longer pinned, sends no further heartbeats, and has no stored device token

#### Scenario: Nothing of the old store survives

- **WHEN** the tablet was revoked and is then paired again, to the same or another store
- **THEN** Home does not show the previous store name, and Settings does not ask for the previous exit PIN, until the new store's values arrive

#### Scenario: Customer data is cleared

- **WHEN** the tablet is revoked while a customer has products in the basket and a name and cell phone typed
- **THEN** the basket is empty and the consumer forms are blank

### Requirement: Revocation is idempotent and guarded

Revocation SHALL run at most once per stored session. Several `DEVICE_UNLINKED` answers arriving together SHALL revoke once. An answer to a request made with a token other than the one currently stored (for example a late answer to a request sent before a new pairing) SHALL NOT revoke the current session.

#### Scenario: Concurrent 401s

- **WHEN** the heartbeat and a catalog call both answer `401 DEVICE_UNLINKED` at the same time
- **THEN** the session is revoked once, and the second answer finds it already revoked and does nothing

#### Scenario: Late answer after re-pairing

- **WHEN** the tablet has paired again with a new token, and an answer `401 DEVICE_UNLINKED` arrives for a request sent with the old token
- **THEN** the new session stays paired

### Requirement: No retry of a rejected token

An authenticated call that answers `401` SHALL NOT be retried with the same stored token. It SHALL resolve to its usual failure category.

#### Scenario: Catalog 401 is not retried

- **WHEN** opening the catalog answers `401`
- **THEN** exactly one catalog request is sent, and the catalog shows its error state

