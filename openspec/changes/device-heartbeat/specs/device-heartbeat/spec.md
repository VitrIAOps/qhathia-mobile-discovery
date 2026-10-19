## ADDED Requirements

### Requirement: Periodic heartbeat while paired

While the device holds a persisted DEVICE access token, the app SHALL send `POST /api/v1/devices/heartbeat` authenticated with that token as a bearer credential. The first heartbeat SHALL be sent immediately when the app process starts paired, or immediately after pairing completes; subsequent heartbeats SHALL be sent every 60 seconds for as long as the process is alive and the device remains paired. The heartbeat SHALL run independently of which screen is shown and SHALL NOT require any user interaction.

#### Scenario: App starts on a paired device

- **WHEN** the app process starts and a DEVICE token is persisted
- **THEN** a heartbeat is sent right away, and another one every 60 seconds after each attempt

#### Scenario: Pairing completes

- **WHEN** the device was unpaired and the pairing token exchange persists a DEVICE token
- **THEN** a heartbeat is sent right away without waiting for the next interval

#### Scenario: Device is not paired

- **WHEN** no DEVICE token is persisted
- **THEN** no heartbeat request is sent

#### Scenario: Device session is cleared

- **WHEN** the persisted DEVICE token is removed while the heartbeat is running
- **THEN** the heartbeat stops and no further requests are sent until a token is persisted again

#### Scenario: Navigating between screens

- **WHEN** the consumer moves between Home, search, catalog, detail, cart or settings
- **THEN** the heartbeat cadence is unaffected and no extra or duplicate heartbeats are sent

### Requirement: Heartbeat telemetry

Each heartbeat SHALL report `appVersion` (the installed app's version name) and, when readable, `batteryLevel` (integer 0–100), `networkStatus`, `model` (manufacturer and model name, at most 100 characters) and `uptimeSeconds` (seconds since the app process started). A telemetry value that cannot be read or is out of range SHALL be omitted from the request, and SHALL NOT prevent the heartbeat from being sent. The app SHALL NOT report `lastSyncVersion` or `lastSyncAt`, as it keeps no local catalog copy.

#### Scenario: All telemetry available

- **WHEN** a heartbeat is sent on a tablet with readable battery and an active Wi-Fi connection
- **THEN** the request body contains `appVersion`, `batteryLevel` between 0 and 100, `networkStatus` `WIFI`, `model` and `uptimeSeconds`

#### Scenario: Battery level unreadable

- **WHEN** the platform reports no valid battery capacity
- **THEN** the heartbeat is still sent, without the `batteryLevel` field

#### Scenario: Network status by transport

- **WHEN** the device is connected through cellular data and not Wi-Fi
- **THEN** `networkStatus` is `MOBILE`

#### Scenario: Long model name

- **WHEN** the manufacturer and model name together exceed 100 characters
- **THEN** `model` is truncated to 100 characters

### Requirement: Heartbeat failure tolerance

A failed heartbeat — network error, timeout, missing token or any HTTP error status — SHALL be silently discarded and retried on the next 60-second tick. A heartbeat failure SHALL NOT crash the app, SHALL NOT show any UI to the consumer, and SHALL NOT stop subsequent heartbeats. A single heartbeat request SHALL time out in at most 15 seconds so a hung request cannot delay the next tick indefinitely.

#### Scenario: Request fails

- **WHEN** a heartbeat request fails with a network error or an HTTP 5xx
- **THEN** nothing is shown to the consumer and the next heartbeat is attempted 60 seconds later

#### Scenario: Token rejected

- **WHEN** the server answers 401 to a heartbeat
- **THEN** the device session is kept, nothing is shown, and heartbeats continue on schedule

#### Scenario: Request hangs

- **WHEN** the server does not answer a heartbeat within 15 seconds
- **THEN** the request is abandoned and the loop waits for the next tick

### Requirement: Heartbeat response publication

Each successful heartbeat response SHALL be parsed into a domain result carrying `serverTime`, `catalogVersion`, `settingsVersion` and the list of `pendingCommands` (each with its id and command type). The latest result SHALL be observable by other parts of the app, and a late subscriber SHALL receive the most recent result. Each pending command SHALL be delivered to command consumers exactly once and SHALL NOT be replayed to a later subscriber. A command type the app does not recognise SHALL be represented as an unknown command instead of failing the response parsing. Missing optional response fields SHALL default to absent values (`pendingCommands` to an empty list).

#### Scenario: Response with a new settings version

- **WHEN** a heartbeat response carries `settingsVersion` 5
- **THEN** observers of the heartbeat result receive a result whose settings version is 5

#### Scenario: Late subscriber

- **WHEN** a component starts observing heartbeat results after a heartbeat already succeeded
- **THEN** it immediately receives the most recent result

#### Scenario: Pending command delivered once

- **WHEN** a heartbeat response contains a `REFRESH_CATALOG` command with id `c1`
- **THEN** a command consumer receives command `c1` once, and a consumer that subscribes afterwards does not receive `c1` again

#### Scenario: Unknown command type

- **WHEN** a heartbeat response contains a command whose type the app does not know
- **THEN** the response is still parsed, the other fields are published, and the command is represented as unknown

#### Scenario: Older backend response

- **WHEN** a heartbeat response contains only `received` and `serverTime`
- **THEN** it is parsed successfully with no settings version, no catalog version and no pending commands
