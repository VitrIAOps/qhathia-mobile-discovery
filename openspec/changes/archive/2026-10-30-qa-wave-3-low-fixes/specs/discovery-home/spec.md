## ADDED Requirements

### Requirement: Store name when known

Home SHALL show the store's name in the header when the app knows it (saved from the device info), and "TIENDA" otherwise.

#### Scenario: Store name known

- **WHEN** the saved store name is "El Martillo"
- **THEN** the Home header shows "El Martillo"

#### Scenario: Store name unknown

- **WHEN** no store name has been saved
- **THEN** the Home header shows "TIENDA"
