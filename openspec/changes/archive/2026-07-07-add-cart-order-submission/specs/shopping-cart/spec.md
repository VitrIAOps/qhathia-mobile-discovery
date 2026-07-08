## MODIFIED Requirements

### Requirement: Render-only order and share actions

The **"Compartir por WhatsApp"** action SHALL be render-only because its backend does not exist yet. Tapping "Compartir por WhatsApp" SHALL NOT open WhatsApp or perform a share, and this deferred behavior SHALL be marked in code as `TODO` to be implemented when its API is available. The **"Realizar pedido"** action SHALL NO LONGER be render-only — it submits a purchase order as defined by the `cart-order-submission` capability.

#### Scenario: Realizar pedido submits an order

- **WHEN** the consumer taps "Realizar pedido" with valid data
- **THEN** the app submits a purchase order (see the `cart-order-submission` capability)

#### Scenario: Share performs no action

- **WHEN** the consumer taps "Compartir por WhatsApp"
- **THEN** the app does not open WhatsApp or perform a share (the action is a TODO placeholder)

### Requirement: Cart session lifecycle

The cart basket SHALL be cleared after 1 minute of inactivity on any screen that is not the Home screen. On the Home screen the inactivity clear SHALL NOT apply. The basket SHALL also be cleared when the consumer places an order successfully and when the consumer cancels the order from the order-error state.

#### Scenario: Inactivity off Home clears the cart

- **WHEN** the consumer is on a screen other than Home and does not interact for 1 minute
- **THEN** the cart basket is cleared

#### Scenario: Interaction resets the inactivity timer

- **WHEN** the consumer interacts with a non-Home screen before the minute elapses
- **THEN** the 1-minute inactivity countdown restarts

#### Scenario: Home screen does not clear on inactivity

- **WHEN** the consumer is on the Home screen and does not interact for 1 minute
- **THEN** the cart basket is not cleared by the inactivity rule

#### Scenario: Successful order clears the cart

- **WHEN** the consumer places an order successfully and accepts the confirmation
- **THEN** the cart basket is cleared
