## MODIFIED Requirements

### Requirement: Purchase actions

The product-detail screen SHALL display a quantity stepper, an **Add-to-cart** button, and a **Share via WhatsApp** button. The quantity stepper SHALL be functional and bound the selectable quantity by the product's stock-derived maximum. Tapping **Add-to-cart** SHALL add the product to the cart using the quantity selected in the stepper. The **Share via WhatsApp** button SHALL NO LONGER be render-only — tapping it SHALL open a share dialog that shares the currently viewed product as a WhatsApp quote, as defined by the "Share this product as a WhatsApp quote" requirement.

#### Scenario: Action buttons are present

- **WHEN** the product-detail screen is shown
- **THEN** a quantity stepper, an Add-to-cart button, and a Share-via-WhatsApp button are visible

#### Scenario: Add-to-cart adds with the selected quantity

- **WHEN** the consumer sets the stepper to N and taps the Add-to-cart button
- **THEN** the product is added to the cart with quantity N and the cart item count increases

#### Scenario: Share via WhatsApp opens the share dialog

- **WHEN** the consumer taps the Share-via-WhatsApp button on a loaded product
- **THEN** the app opens a dialog to collect the consumer's name and cell-phone and share this product as a WhatsApp quote

## ADDED Requirements

### Requirement: Share this product as a WhatsApp quote

From the product-detail screen the app SHALL let the consumer share the currently viewed product as a WhatsApp quote. Tapping **"Compartir por WhatsApp"** SHALL open a self-contained dialog with a consumer-data form (name and cell-phone) validated with the same rules the cart uses. On submit the app SHALL share a quote for this single product at the quantity selected in the detail's stepper, retrying up to three times before showing the error state, and SHALL reuse the shared blocking-loading, success, and error states. The share SHALL be submitted only when the form is valid, SHALL NOT create a purchase order, and SHALL NOT require the product to be in the cart.

#### Scenario: Share dialog collects consumer data

- **WHEN** the consumer taps "Compartir por WhatsApp" on a loaded product
- **THEN** the app opens a dialog with a name field and a cell-phone field, disabled for submit until both are valid

#### Scenario: Invalid data is never submitted

- **WHEN** the share dialog's form is invalid (missing or malformed name or cell-phone)
- **THEN** the "Compartir" action does not submit a share request

#### Scenario: Shares the single product at the selected quantity

- **WHEN** the consumer submits the share dialog with valid data
- **THEN** the app submits a share request for this product with the quantity selected in the detail's stepper
- **AND** retries the share up to three times before showing the error state

#### Scenario: Blocking state during share

- **WHEN** a share request from the dialog is in flight
- **THEN** the dialog shows a centered loading indicator over a dimmed background and blocks its inputs
- **AND** no second share request is submitted while one is in flight

#### Scenario: Accept closes and resets the dialog

- **WHEN** the share succeeds and the consumer taps "Aceptar"
- **THEN** the dialog closes and its form is reset

#### Scenario: Error offers retry or cancel

- **WHEN** the share fails after the retries
- **THEN** the dialog shows a simplified error with "Entendido" (returns to the form, preserving entered data) and "Cancelar" (closes the dialog)
