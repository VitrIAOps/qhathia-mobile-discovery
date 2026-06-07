## ADDED Requirements

### Requirement: Navigation to product detail from a result

A product card on the results screen and on the unhappy screen's alternatives SHALL be tappable to open that product's detail. When the consumer taps a product card, the app SHALL navigate to the product-detail screen for that product's id. This card tap is distinct from the per-card cart button, which remains non-navigating.

#### Scenario: Tapping a result opens its detail

- **WHEN** the consumer taps a product card on the results screen
- **THEN** the app navigates to the product-detail screen for that product's id

#### Scenario: Tapping an alternative opens its detail

- **WHEN** the consumer taps one of the alternative product cards on the unhappy screen
- **THEN** the app navigates to the product-detail screen for that product's id

#### Scenario: Cart button still does not navigate

- **WHEN** the consumer taps a product card's cart button
- **THEN** the app does not navigate to a cart screen and does not open the product detail
