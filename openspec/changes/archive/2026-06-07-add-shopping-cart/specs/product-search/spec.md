## MODIFIED Requirements

### Requirement: Results catalog viewed from chat

When a search returns products, the app SHALL present a "catalog viewed from chat" results screen following the results design. The screen SHALL show a results headline derived from the API display text (e.g. *Encontré 8 taladros para ti*), the products laid out in **at most 2 rows with horizontal scroll**, a **cart button** on each product, a top bar with **Volver**, **Nueva búsqueda**, and an **Ir al carrito** control that shows the current cart item count, and a bottom search bar with a microphone for starting the next query. There SHALL be **no limit** imposed by the app on the number of products displayed.

#### Scenario: Products rendered as a chat catalog

- **WHEN** a search returns one or more products
- **THEN** the app shows the results screen with the results headline and the products in at most 2 rows that scroll horizontally

#### Scenario: Per-product and top-bar controls present

- **WHEN** the results screen is shown
- **THEN** each product exposes a cart button
- **AND** the top bar exposes Volver, Nueva búsqueda, and Ir al carrito controls
- **AND** a search bar with a microphone is available for the next query

#### Scenario: Per-product cart button adds to the cart

- **WHEN** the consumer taps a product's cart button
- **THEN** the product is added to the cart and the Ir al carrito control's item count increases

#### Scenario: Ir al carrito opens the cart dialog

- **WHEN** the consumer taps the Ir al carrito control
- **THEN** the app opens the cart dialog over the results screen without navigating to a separate cart screen

### Requirement: Navigation to product detail from a result

A product card on the results screen and on the unhappy screen's alternatives SHALL be tappable to open that product's detail. When the consumer taps a product card, the app SHALL navigate to the product-detail screen for that product's id. This card tap is distinct from the per-card cart button, which adds the product to the cart rather than opening the detail.

#### Scenario: Tapping a result opens its detail

- **WHEN** the consumer taps a product card on the results screen
- **THEN** the app navigates to the product-detail screen for that product's id

#### Scenario: Tapping an alternative opens its detail

- **WHEN** the consumer taps one of the alternative product cards on the unhappy screen
- **THEN** the app navigates to the product-detail screen for that product's id

#### Scenario: Cart button adds without opening the detail

- **WHEN** the consumer taps a product card's cart button
- **THEN** the product is added to the cart and the app does not open the product detail
