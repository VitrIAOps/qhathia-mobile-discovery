## MODIFIED Requirements

### Requirement: Render-only purchase actions

The product-detail screen SHALL display a quantity stepper, an **Add-to-cart** button, and a **Share via WhatsApp** button. The quantity stepper SHALL be functional and bound the selectable quantity by the product's stock-derived maximum. Tapping **Add-to-cart** SHALL add the product to the cart using the quantity selected in the stepper. The **Share via WhatsApp** button SHALL remain render-only: tapping it SHALL NOT navigate to a WhatsApp form or perform a share, and that deferred behavior SHALL be marked in code as `TODO`.

#### Scenario: Action buttons are present

- **WHEN** the product-detail screen is shown
- **THEN** a quantity stepper, an Add-to-cart button, and a Share-via-WhatsApp button are visible

#### Scenario: Add-to-cart adds with the selected quantity

- **WHEN** the consumer sets the stepper to N and taps the Add-to-cart button
- **THEN** the product is added to the cart with quantity N and the cart item count increases

#### Scenario: Share via WhatsApp does not act

- **WHEN** the consumer taps the Share-via-WhatsApp button
- **THEN** the app does not navigate to a WhatsApp form or perform a share (the action is a TODO placeholder)

### Requirement: Retained chrome and navigation controls

The product-detail screen SHALL keep the bottom **microphone animation**, and SHALL provide a top bar with a **Volver** control that returns to the search results, a **Nueva búsqueda** control, and a cart control that shows the current cart item count and opens the cart dialog. The **Nueva búsqueda** control SHALL open a confirmation dialog to close the current search; on confirmation the app SHALL return to the Home idle screen.

#### Scenario: Microphone animation retained

- **WHEN** the product-detail screen is shown
- **THEN** the bottom microphone animation is visible

#### Scenario: Volver returns to results

- **WHEN** the consumer taps Volver on the product-detail screen
- **THEN** the app returns to the search results screen

#### Scenario: New search asks for confirmation and returns to idle

- **WHEN** the consumer taps Nueva búsqueda on the product-detail screen
- **THEN** the app opens a confirmation dialog to close the current search
- **AND** on confirmation the app returns to the Home idle screen

#### Scenario: Cart control opens the cart dialog

- **WHEN** the consumer taps the cart control on the product-detail screen
- **THEN** the app opens the cart dialog over the detail screen and the control shows the current cart item count
