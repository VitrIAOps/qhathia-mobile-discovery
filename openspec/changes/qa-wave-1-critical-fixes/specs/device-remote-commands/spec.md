## MODIFIED Requirements

### Requirement: Command acknowledgement

Each executed command SHALL be acknowledged with `POST /api/v1/devices/me/commands/{commandId}/ack`, authenticated with the DEVICE token, carrying `success` and an optional `detail` truncated to at most 300 characters. A failed acknowledgement SHALL be retried up to 3 attempts in total, 2 seconds apart, and then abandoned silently. For a restart, the command id SHALL be stored before the app restarts, and the acknowledgement SHALL be sent after the app is running again. If the app does not come back, no acknowledgement is sent and the command expires on the server.

#### Scenario: Ack succeeds first time

- **WHEN** a command is executed and the ack request returns 204
- **THEN** no further ack is sent for that command

#### Scenario: Ack fails transiently

- **WHEN** the first ack attempt fails with a network error and the second succeeds
- **THEN** exactly two ack attempts are made for that command

#### Scenario: Ack keeps failing

- **WHEN** all 3 ack attempts fail
- **THEN** the app stops retrying, shows nothing to the consumer, and keeps processing later commands

#### Scenario: Long failure detail

- **WHEN** a failed command's detail is longer than 300 characters
- **THEN** the acknowledged detail is truncated to 300 characters

#### Scenario: Restart acknowledged after relaunch

- **WHEN** a `RESTART_APP` command is executed and the app is running again
- **THEN** the stored command id is acknowledged with `success = true` and then forgotten

#### Scenario: Restart that never comes back is not acknowledged

- **WHEN** a `RESTART_APP` command is executed and the app is not started again
- **THEN** no acknowledgement is sent for that command

## ADDED Requirements

### Requirement: Restart keeps the tablet on Discovery

Executing `RESTART_APP` SHALL start a new app process and show Discovery again without leaving the tablet on the Android launcher. When the kiosk lock is active, the tablet SHALL remain pinned to Discovery across the restart.

#### Scenario: Restart while pinned

- **WHEN** the kiosk lock is active, the task is pinned, and a `RESTART_APP` command arrives
- **THEN** the app process is replaced by a new one
- **AND** Discovery is shown again and the task is pinned

#### Scenario: Restart while not pinned

- **WHEN** the kiosk lock is not active and a `RESTART_APP` command arrives
- **THEN** the app process is replaced by a new one and Discovery is shown again
