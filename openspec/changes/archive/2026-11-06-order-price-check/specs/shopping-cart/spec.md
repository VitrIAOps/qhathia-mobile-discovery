## MODIFIED Requirements

### Requirement: Per-product quantity bounded by stock

The per-product quantity SHALL be bounded by the product's stock: the stock count when the product carries one (never more than 99, the server's per-line limit), or the availability-derived cap when it does not ("a consultar"). The quantity stepper for a product SHALL NOT increase the quantity beyond that product's maximum. Decreasing a cart line's quantity below 1 SHALL remove the line from the cart. A product with stock 0 SHALL NOT be added to the cart.

#### Scenario: Increase is blocked at the maximum

- **WHEN** a product's cart-line quantity is at its stock-derived maximum
- **THEN** the stepper's increase control does not raise the quantity further

#### Scenario: Cap follows the real stock

- **WHEN** a product with stock 3 is in the cart
- **THEN** its line cannot go above 3 units

#### Scenario: No stock count keeps the availability cap

- **WHEN** a product without a stock count ("a consultar") is in the cart
- **THEN** its line is capped by its availability as before

#### Scenario: Decreasing below one removes the line

- **WHEN** the consumer decreases a cart line's quantity below 1
- **THEN** the line is removed from the cart

### Requirement: Consumer-data form validation

The cart dialog SHALL provide a consumer-data form requiring a **name** and a **cell-phone number**, both of which are required to generate the order.

- The name SHALL be required to contain at least one referential name, and SHALL accept at most 200 characters (the server's limit); input beyond 200 characters SHALL NOT be entered.
- The cell-phone number SHALL be required to match the general mobile-number format.

The **"Realizar pedido"** action SHALL be enabled only when both the name and the cell-phone number are valid.

#### Scenario: Order disabled until data is valid

- **WHEN** the name or the cell-phone number is missing or invalid
- **THEN** the "Realizar pedido" action is not enabled

#### Scenario: Invalid name rejected

- **WHEN** the entered name does not contain at least one referential name
- **THEN** the form marks the name as invalid and the order remains disabled

#### Scenario: Name stops at 200 characters

- **WHEN** the consumer types or pastes a 262-character name
- **THEN** the field keeps the first 200 characters and an order with that name is accepted by the server

#### Scenario: Invalid phone rejected

- **WHEN** the entered cell-phone number does not match the general mobile-number format
- **THEN** the form marks the phone as invalid and the order remains disabled

#### Scenario: Valid data enables the order

- **WHEN** a valid name and a valid cell-phone number are entered
- **THEN** the "Realizar pedido" action becomes enabled
