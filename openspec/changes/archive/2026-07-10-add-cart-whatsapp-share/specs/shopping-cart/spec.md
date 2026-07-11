## MODIFIED Requirements

### Requirement: Render-only order and share actions

Both cart actions SHALL now be functional. The **"Realizar pedido"** action SHALL submit a purchase order as defined by the `cart-order-submission` capability and, on success, SHALL also share the WhatsApp quote for the same basket as defined by the `cart-whatsapp-share` capability. The **"Compartir por WhatsApp"** action SHALL NO LONGER be render-only — it SHALL trigger the WhatsApp quote share defined by the `cart-whatsapp-share` capability, submitting the basket and consumer data to the share endpoint and driving the share loading/success/error states. Tapping "Compartir por WhatsApp" SHALL NOT create a purchase order.

#### Scenario: Realizar pedido submits an order

- **WHEN** the consumer taps "Realizar pedido" with valid data
- **THEN** the app submits a purchase order (see the `cart-order-submission` capability)
- **AND** on success the app also shares the WhatsApp quote for the same basket (see the `cart-whatsapp-share` capability)

#### Scenario: Compartir por WhatsApp shares the cart

- **WHEN** the consumer taps "Compartir por WhatsApp" with valid data
- **THEN** the app submits a WhatsApp quote share (see the `cart-whatsapp-share` capability)
- **AND** the app does not create a purchase order
