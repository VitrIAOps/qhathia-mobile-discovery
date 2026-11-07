## MODIFIED Requirements

### Requirement: Heartbeat telemetry

Each heartbeat SHALL report `appVersion` (the installed app's version name) and, when readable, `batteryLevel` (integer 0–100), `networkStatus`, `model` (manufacturer and model name, at most 100 characters) and `uptimeSeconds` (seconds since the device booted). A telemetry value that cannot be read or is out of range SHALL be omitted from the request, and SHALL NOT prevent the heartbeat from being sent. The app SHALL NOT report `lastSyncVersion` or `lastSyncAt`, as it keeps no local catalog copy.

#### Scenario: All telemetry available

- **WHEN** a heartbeat is sent on a tablet with readable battery and an active Wi-Fi connection
- **THEN** the request body contains `appVersion`, `batteryLevel` between 0 and 100, `networkStatus` `WIFI`, `model` and `uptimeSeconds`

#### Scenario: First heartbeat after the app opens

- **WHEN** the app is reopened on a device that booted earlier and sends its first heartbeat
- **THEN** `uptimeSeconds` is greater than 0 (the time since the device booted)

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

A failed heartbeat — network error, timeout, missing token or any HTTP error status — SHALL NOT crash the app, SHALL NOT show any UI to the consumer, and SHALL NOT stop subsequent heartbeats. After a failure the next heartbeat SHALL be attempted 5, 10, 20 and 40 seconds after the 1st, 2nd, 3rd and 4th failure in a row, and 60 seconds after later ones; a success returns to the 60-second interval. Whatever the wait, when the device goes from no internet to validated internet the next heartbeat SHALL be sent at once. A single heartbeat request SHALL time out in at most 15 seconds so a hung request cannot delay the next one indefinitely.

#### Scenario: Request fails

- **WHEN** a heartbeat request fails with a network error or an HTTP 5xx
- **THEN** nothing is shown to the consumer and the next heartbeat is attempted 5 seconds later

#### Scenario: Internet comes back

- **WHEN** the device was offline and regains validated internet
- **THEN** a heartbeat is sent within 3 seconds, without waiting for the retry or the interval

#### Scenario: Token rejected

- **WHEN** the server answers 401 to a heartbeat without `DEVICE_UNLINKED`
- **THEN** the device session is kept, nothing is shown, and heartbeats continue with the retry waits

#### Scenario: Request hangs

- **WHEN** the server does not answer a heartbeat within 15 seconds
- **THEN** the request is abandoned and counted as a failure
