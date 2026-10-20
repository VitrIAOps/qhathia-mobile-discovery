## ADDED Requirements

### Requirement: Kiosk lock follows the remote setting

The app SHALL keep the tablet locked on Discovery while the kiosk lock is active. The kiosk lock SHALL be active when, and only when, the device is paired, the stored remote `kioskMode` is `true`, and the merchant has not exited kiosk mode from Configuración in the current app process. While active, the app SHALL pin its task with Android screen pinning (no Device Owner), so the system Home and Recents actions and the notification shade cannot take the customer out of the app.

#### Scenario: Paired tablet with kiosk mode on is pinned

- **WHEN** the device is paired, `kioskMode` is `true` and the app is in the foreground
- **THEN** the app's task is screen-pinned

#### Scenario: Kiosk mode off is not pinned

- **WHEN** the stored `kioskMode` is `false`
- **THEN** the app does not pin its task and the system navigation works normally

#### Scenario: Unpaired tablet is never pinned

- **WHEN** the device is not paired (onboarding), regardless of `kioskMode`
- **THEN** the app does not pin its task

#### Scenario: Never synced uses the default

- **WHEN** the device is paired and its settings were never synced
- **THEN** the default `kioskMode = true` applies and the app is pinned

### Requirement: Full screen, screen always on

While the kiosk lock is active the app SHALL hide the status and navigation bars (immersive; a swipe MAY reveal them transiently) and SHALL keep the screen on. When the lock becomes inactive the app SHALL show the system bars again and stop forcing the screen on.

#### Scenario: Bars hidden while locked

- **WHEN** the kiosk lock is active
- **THEN** the status and navigation bars are hidden and the screen does not time out

#### Scenario: Bars restored when unlocked

- **WHEN** the kiosk lock becomes inactive
- **THEN** the system bars are visible and the screen follows the system timeout

### Requirement: Back does not leave the app

While the kiosk lock is active, Back on the Home screen SHALL be ignored so the app's root screen is never closed. Back on other screens SHALL keep its normal in-app behavior.

#### Scenario: Back on Home while locked

- **WHEN** the kiosk lock is active and the customer presses Back on Home
- **THEN** the app stays on Home and remains pinned

#### Scenario: Back inside the app

- **WHEN** the kiosk lock is active and the customer presses Back on the product detail screen
- **THEN** the app navigates back within the app as usual

### Requirement: Remote changes apply live

The app SHALL apply a change of `kioskMode` as soon as the new settings are stored, without a restart: `false` SHALL unpin immediately; `true` SHALL pin again when the app is in the foreground and the merchant has not exited from Configuración.

#### Scenario: Merchant turns kiosk mode off from staff

- **WHEN** the tablet is pinned and a sync stores `kioskMode = false`
- **THEN** the app unpins and shows the system bars without a restart

#### Scenario: Merchant turns kiosk mode on from staff

- **WHEN** the tablet is on Home, not pinned, and a sync stores `kioskMode = true`
- **THEN** the app pins its task and hides the system bars

### Requirement: Temporary exit and re-lock

Exiting kiosk mode from Configuración SHALL last only until the merchant goes back from Configuración to Home or until the app process restarts. The exit SHALL NOT be persisted. Whenever the app returns to the foreground and the lock is active but the task is not pinned (for example, after the system unpin gesture), the app SHALL pin again.

#### Scenario: Back to Home re-locks

- **WHEN** the merchant exited kiosk mode and goes back from Configuración to Home
- **THEN** the app pins its task again

#### Scenario: Restart re-locks

- **WHEN** the merchant exited kiosk mode and the app restarts (remote `RESTART_APP`, or the app is reopened after being killed)
- **THEN** the app starts locked and pins its task

#### Scenario: Re-pin after system unpin

- **WHEN** the lock is active, the task was unpinned with the system gesture, and the app comes back to the foreground
- **THEN** the app pins its task again
