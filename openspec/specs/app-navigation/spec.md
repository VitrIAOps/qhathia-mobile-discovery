# app-navigation Specification

## Purpose
The app-navigation capability keeps the screen stack predictable on a shared kiosk: one tap produces at most one navigation (a repeated tap during a transition is dropped), and the system Back runs the same action as each screen's on-screen "Volver", including its confirmations and history steps.

## Requirements
### Requirement: One navigation per tap

A navigation or back action SHALL run only from the screen that is currently shown and settled. A tap that arrives while that screen is already leaving (during a transition) SHALL be ignored. The back stack SHALL never hold two copies of a screen pushed by repeated taps on the same control.

#### Scenario: Double tap on "Ir al catálogo"

- **WHEN** the consumer taps "Ir al catálogo" on Home twice in quick succession
- **THEN** a single Catalog screen is opened
- **AND** one Back from Catalog returns to Home

#### Scenario: Double tap on a related product

- **WHEN** the consumer taps the same related product on the product detail twice in quick succession
- **THEN** a single detail screen for that product is opened

#### Scenario: Repeated "Volver"

- **WHEN** the consumer taps "Volver" on the product detail three times in quick succession
- **THEN** only the detail screen is closed and the previous screen is shown

### Requirement: System Back matches "Volver"

On every screen that shows a "Volver" control (Catalog, product search, product detail), the system Back SHALL run the same action as "Volver", including any confirmation or history step that "Volver" performs. While a confirmation dialog is open, the system Back SHALL dismiss that dialog.

#### Scenario: Back on Catalog with items in the cart

- **WHEN** the cart has at least one product and the consumer presses the system Back on Catalog
- **THEN** the close confirmation dialog is shown, as with "Volver"

#### Scenario: Back on Catalog with an empty cart

- **WHEN** the cart is empty and the consumer presses the system Back on Catalog
- **THEN** the app returns to Home, as with "Volver"

#### Scenario: Back on search with a previous result

- **WHEN** the search screen has a previous result in its history and the consumer presses the system Back
- **THEN** the previous result is shown, as with "Volver"

#### Scenario: Back on search with no previous result

- **WHEN** the search screen has no previous result and the consumer presses the system Back
- **THEN** the close confirmation dialog is shown, as with "Volver"

#### Scenario: Back closes an open confirmation

- **WHEN** a close confirmation dialog is open and the consumer presses the system Back
- **THEN** the dialog is dismissed and the screen stays

