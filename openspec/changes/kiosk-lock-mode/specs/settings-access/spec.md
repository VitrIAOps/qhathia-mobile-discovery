## ADDED Requirements

### Requirement: Exit kiosk mode from settings

The settings area SHALL offer a "Salir del modo kiosko" action in its side menu while the kiosk lock is active, and SHALL NOT show it otherwise. The action SHALL be reachable only through the existing settings entry, so it is protected by the exit PIN when one is set and asks for nothing when no PIN is set; it SHALL NOT ask for the PIN again. Activating it SHALL unpin the app and show the system bars so the merchant can leave the app, and SHALL tell the merchant that kiosk mode comes back on returning to Home.

#### Scenario: Exit shown while locked

- **WHEN** the kiosk lock is active and the merchant is in the settings area
- **THEN** the side menu shows "Salir del modo kiosko"

#### Scenario: Exit hidden when not locked

- **WHEN** the kiosk lock is not active (`kioskMode` is `false`, or the merchant already exited)
- **THEN** the side menu does not show "Salir del modo kiosko"

#### Scenario: Exit with an exit PIN set

- **WHEN** an exit PIN is set, the merchant enters it at the settings gate and taps "Salir del modo kiosko"
- **THEN** the app unpins, shows the system bars and shows the temporary-exit message, without asking for the PIN again

#### Scenario: Exit with no PIN set

- **WHEN** no exit PIN is set and the merchant opens settings and taps "Salir del modo kiosko"
- **THEN** the app unpins and shows the system bars without asking for any PIN

#### Scenario: Wrong PIN never exits

- **WHEN** an exit PIN is set and the merchant enters a wrong PIN at the settings gate
- **THEN** the settings area does not open and the app stays pinned
