## ADDED Requirements

### Requirement: Remote command execution

The app SHALL execute every remote command delivered by the device heartbeat exactly once, in the order received, for as long as the app process is alive. Commands SHALL be processed one at a time; a command SHALL NOT be cancelled by the arrival of the next one. Executing a command SHALL NOT show any UI to the consumer beyond the command's own effect, and a failure SHALL NOT crash the app.

#### Scenario: Refresh catalog command

- **WHEN** the heartbeat delivers a `REFRESH_CATALOG` command
- **THEN** the app requests a catalog refresh and acknowledges the command as successful

#### Scenario: Restart command

- **WHEN** the heartbeat delivers a `RESTART_APP` command and the app can relaunch itself
- **THEN** the app acknowledges the command as successful and then restarts in a new process, opening the start destination that matches the persisted pairing state

#### Scenario: Restart not possible

- **WHEN** the heartbeat delivers a `RESTART_APP` command and the app cannot resolve its own launch entry point
- **THEN** the app does not exit and acknowledges the command as failed with the detail "No se pudo reiniciar la app"

#### Scenario: Unknown command

- **WHEN** the heartbeat delivers a command type the app does not support
- **THEN** the app performs no action and acknowledges the command as failed with the detail "Comando no soportado: <command>"

#### Scenario: Execution throws

- **WHEN** executing a command raises an unexpected error
- **THEN** the app keeps running, acknowledges the command as failed with the error message as detail, and continues processing later commands

#### Scenario: Several commands in one heartbeat

- **WHEN** one heartbeat delivers a `REFRESH_CATALOG` followed by a `RESTART_APP`
- **THEN** the refresh is executed and acknowledged before the restart is acknowledged and executed

### Requirement: Command acknowledgement

Each executed command SHALL be acknowledged with `POST /api/v1/devices/me/commands/{commandId}/ack`, authenticated with the DEVICE token, carrying `success` and an optional `detail` truncated to at most 300 characters. A failed acknowledgement SHALL be retried up to 3 attempts in total, 2 seconds apart, and then abandoned silently. For a restart, the acknowledgement attempts SHALL complete before the app process exits.

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

#### Scenario: Restart waits for the ack

- **WHEN** a `RESTART_APP` command is executed
- **THEN** the app exits only after its ack attempts have finished
