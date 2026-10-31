## MODIFIED Requirements

### Requirement: Session ends the same way on every path

Accepting an order or share success, cancelling from the order or share error state, confirming the close dialog of the Search, Product detail or Catalog screen, the Catalog "Volver" that returns Home with an empty cart, and the inactivity timeout SHALL all end the customer session through the same operation, so the basket, the consumer data and the submission state are cleared together. Opening the Catalog through "Ir al catálogo" SHALL NOT end the session.

#### Scenario: Accept after success clears the consumer data

- **WHEN** the consumer taps "Aceptar" on a success screen and later reopens the cart
- **THEN** the basket and the name and cell-phone fields are empty

#### Scenario: Confirmed close clears the basket for the next customer

- **WHEN** a customer with 2 products in the basket taps Nueva búsqueda, confirms "Sí, cerrar", and the next customer opens the Catalog
- **THEN** the cart control shows 0 and the cart's name and cell-phone fields are empty

#### Scenario: Ir al catálogo keeps the session

- **WHEN** the consumer taps Ir al catálogo on the search screen with products in the basket
- **THEN** the basket and the consumer data are kept
