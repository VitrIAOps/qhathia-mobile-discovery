## ADDED Requirements

### Requirement: Stepper resets after adding

After "Agregar al carrito" adds the product, the quantity stepper SHALL return to 1, so a second tap adds one unit and not the previous amount again.

#### Scenario: Add three, then add again

- **WHEN** the consumer sets the stepper to 3 and taps "Agregar al carrito"
- **THEN** the cart gets 3 units and the stepper shows 1

### Requirement: Retry reloads related products

When the consumer taps "Reintentar" after a connection error, the detail SHALL load the product and, when the related products are not loaded, load them as well.

#### Scenario: Retry after being offline

- **WHEN** the detail and its related products failed without network and the consumer taps "Reintentar" with the network back
- **THEN** the product and "Podrías acompañar este producto con…" are both shown
