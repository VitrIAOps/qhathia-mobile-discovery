## ADDED Requirements

### Requirement: Touchable controls carry their own label

Every touchable control without visible text (icon buttons such as the settings gear, close, back, microphone, search, the cart buttons on product cards and the quantity stepper) SHALL expose its description on the touchable element itself. The icon inside it SHALL NOT carry a second description.

#### Scenario: Settings gear

- **WHEN** an accessibility service or `uiautomator` inspects the Home screen
- **THEN** the clickable settings control is described as "Ajustes"

#### Scenario: No unlabelled clickable controls

- **WHEN** the Home, Catalog, Search, Detail, Cart, Pairing and Settings screens are inspected
- **THEN** no clickable element has both an empty text and an empty description (a text field counts as labelled when the label node it contains carries the description)

### Requirement: Text fields expose their label

Every text field SHALL expose its visible label (for example "Nombres", "Número de celular") to accessibility services, also while the field is empty.

#### Scenario: Empty cart form

- **WHEN** the cart form is inspected with both fields empty
- **THEN** the name field is described as "Nombres" and the phone field as "Número de celular"

### Requirement: Grouped values are read as one

A value shown as separate boxes SHALL be exposed as a single description. The pairing code SHALL be read as "Código de vinculación:" followed by its characters, and each detail thumbnail SHALL be described with its position and the product name.

#### Scenario: Pairing code

- **WHEN** the pairing code "2Y567" is on screen
- **THEN** a single element is described as "Código de vinculación: 2 Y 5 6 7" and the five boxes are not exposed separately

#### Scenario: Detail thumbnails

- **WHEN** a product detail shows extra photos
- **THEN** each thumbnail is described as "Foto N de <nombre del producto>"
