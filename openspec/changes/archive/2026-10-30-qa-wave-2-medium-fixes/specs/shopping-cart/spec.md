## ADDED Requirements

### Requirement: Basket limit of 20 distinct products

The basket SHALL hold at most 20 distinct products, the limit the order and share endpoints accept. Adding a product that is not yet in a full basket SHALL be refused and the consumer SHALL be told why ("Tu carrito ya tiene 20 productos distintos. Realiza tu pedido o quita alguno para agregar más."). Adding units of a product already in the basket SHALL still be allowed up to that product's quantity cap.

#### Scenario: 21st distinct product

- **WHEN** the basket has 20 distinct products and the consumer adds a different product from any screen
- **THEN** the product is not added and the limit message is shown

#### Scenario: More units of a product in a full basket

- **WHEN** the basket has 20 distinct products and the consumer adds a product already in it
- **THEN** that line's quantity increases, up to its cap

### Requirement: Cart actions need a basket and valid data

"Realizar pedido" and "Compartir" SHALL be enabled only when the basket has at least one product, the consumer name and cell-phone are valid, and no line is flagged as unavailable. When disabled, both SHALL look disabled.

#### Scenario: Empty basket

- **WHEN** the cart dialog is open with no products
- **THEN** "Realizar pedido" and "Compartir" are disabled

#### Scenario: Empty form

- **WHEN** the basket has products and the name or cell-phone is empty or invalid
- **THEN** "Realizar pedido" and "Compartir" are disabled

#### Scenario: Ready to submit

- **WHEN** the basket has products, the data is valid and no line is flagged
- **THEN** "Realizar pedido" and "Compartir" are enabled
