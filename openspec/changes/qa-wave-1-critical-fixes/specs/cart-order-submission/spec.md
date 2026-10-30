## ADDED Requirements

### Requirement: Unavailable products flagged after a server rejection

When an order or share request is rejected because a product is no longer available (HTTP 400 or 404 from the order or share endpoint), the app SHALL recheck every cart line against the product detail endpoint. Each line whose product is inactive, not `AVAILABLE`, or not found SHALL be marked "No disponible" in the cart. The cart SHALL show a message explaining that some products are no longer available, and "Realizar pedido" and "Compartir por WhatsApp" SHALL stay disabled until those lines are removed. If the recheck finds no unavailable line, the app SHALL show the simplified error state as before.

#### Scenario: Inactive product in the basket is flagged

- **WHEN** the consumer places an order, the endpoint answers 400, and one cart product is now inactive
- **THEN** that line is shown as "No disponible"
- **AND** the order and share actions are disabled with an explanatory message

#### Scenario: Removing the flagged line enables the order again

- **WHEN** the consumer removes every line marked "No disponible"
- **THEN** the order and share actions are enabled again (given a valid consumer form)

#### Scenario: No generic error loop

- **WHEN** the order is rejected for an unavailable product and the consumer goes back to the cart
- **THEN** the consumer cannot resubmit the same basket until the unavailable lines are removed

#### Scenario: Recheck finds nothing

- **WHEN** the order endpoint answers 400 and every cart product is still active and `AVAILABLE`
- **THEN** the app shows the simplified error state
