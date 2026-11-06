## ADDED Requirements

### Requirement: Store name known right after pairing

When the pairing exchange returns a store name, the tablet SHALL save it in the store session at once, so Home shows it without opening Settings. The Settings area SHALL title the Wi-Fi and pairing panels with the store name in capitals and the side menu with the store name, and SHALL fall back to "TIENDA" / "Tienda" while the name is unknown.

#### Scenario: Home right after pairing

- **WHEN** the tablet is paired and the exchange returned "QA Discovery — Ferretería"
- **THEN** Home shows the store name without opening Settings

#### Scenario: Settings titles

- **WHEN** the store name is known and the merchant opens Settings
- **THEN** the Red Wifi and Vincular toolbars show "QA DISCOVERY — FERRETERÍA" and the side menu shows "QA Discovery — Ferretería"

#### Scenario: Name unknown

- **WHEN** no store name is known (older backend, or not paired)
- **THEN** the titles show "TIENDA" and "Tienda"
