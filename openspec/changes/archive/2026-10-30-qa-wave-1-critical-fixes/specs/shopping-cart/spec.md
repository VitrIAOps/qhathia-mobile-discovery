## MODIFIED Requirements

### Requirement: Cart session lifecycle

The cart basket SHALL be cleared when the customer session ends, as defined by the `customer-session` capability: on inactivity (off Home, or on Home while the basket is not empty), when the consumer accepts an order or share success, and when the consumer cancels from the order or share error state. Ending the session SHALL also clear the consumer-data form.

#### Scenario: Inactivity off Home clears the cart

- **WHEN** the consumer is on a screen other than Home and does not interact for 1 minute
- **THEN** the cart basket and the consumer-data form are cleared and the app returns to Home

#### Scenario: Interaction resets the inactivity timer

- **WHEN** the consumer interacts with any screen or dialog before the minute elapses
- **THEN** the 1-minute inactivity countdown restarts

#### Scenario: Home with an empty basket does not clear on inactivity

- **WHEN** the consumer is on the Home screen with an empty basket and does not interact for 1 minute
- **THEN** nothing is cleared by the inactivity rule

#### Scenario: Home with items clears on inactivity

- **WHEN** the consumer is on the Home screen with items in the basket and does not interact for 1 minute
- **THEN** the cart basket and the consumer-data form are cleared

#### Scenario: Successful order clears the cart

- **WHEN** the consumer places an order successfully and accepts the confirmation
- **THEN** the cart basket and the consumer-data form are cleared

## ADDED Requirements

### Requirement: Only orderable products can be added

A product SHALL be addable to the cart only when it is orderable: its availability is `AVAILABLE` and it is not known to be inactive. Add-to-cart controls for a product that is not orderable SHALL be disabled on every screen that offers them (search results, catalog, product detail, related products).

#### Scenario: Low-stock product cannot be added

- **WHEN** a product card or detail shows a product whose availability is `LOW_STOCK` or `CONSULT`
- **THEN** its add-to-cart control is disabled and tapping it adds nothing

#### Scenario: Inactive product cannot be added

- **WHEN** the product detail reports the product as inactive
- **THEN** its add-to-cart control is disabled and tapping it adds nothing

#### Scenario: Available product can be added

- **WHEN** a product is `AVAILABLE` and not inactive
- **THEN** its add-to-cart control is enabled
