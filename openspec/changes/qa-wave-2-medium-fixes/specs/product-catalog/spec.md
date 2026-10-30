## ADDED Requirements

### Requirement: Catalog system Back follows "Volver"

The system Back on the Catalog screen SHALL run the Catalog "Volver" action: the close confirmation when the cart has products, and the return to Home when the cart is empty.

#### Scenario: Back with products in the cart

- **WHEN** the cart has products and the consumer presses the system Back on Catalog
- **THEN** the close confirmation dialog is shown and the screen stays on Catalog until the consumer confirms
