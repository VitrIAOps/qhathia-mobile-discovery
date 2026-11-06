## ADDED Requirements

### Requirement: Abandoned pairing codes are cancelled

The tablet SHALL keep the session of the code it shows across restarts. Each new code request SHALL ask the backend to replace that session. When the app process starts and the tablet is not paired, the kept session SHALL be cancelled on the backend and forgotten; a failed cancel SHALL NOT block the pairing screen. A successful exchange SHALL forget the kept session. Going back and continuing within the same process SHALL keep reusing the code on screen. A code the backend reports as cancelled SHALL be replaced by a new one on screen.

#### Scenario: Code abandoned by a force-stop

- **WHEN** a code A is shown, the app is force-stopped and opened again
- **THEN** the tablet cancels A on the backend, and approving A from the staff app fails

#### Scenario: New code replaces the old one

- **WHEN** a code A is shown and a new code B is requested (expiry or "Generar nuevo código")
- **THEN** the request for B asks the backend to replace A

#### Scenario: Back and continue reuses the code

- **WHEN** a code is shown, the merchant taps Volver and then Continuar in the same process
- **THEN** the same code is shown and no new code is requested

#### Scenario: Cancelled code on screen

- **WHEN** the pairing status reports the shown code as cancelled
- **THEN** the tablet requests and shows a new code

#### Scenario: Paired tablet

- **WHEN** the exchange succeeds
- **THEN** the kept session is forgotten and nothing is cancelled on the next start
