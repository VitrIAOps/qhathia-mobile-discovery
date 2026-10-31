## MODIFIED Requirements

### Requirement: Results catalog viewed from chat

When a search returns products, the app SHALL present a "catalog viewed from chat" results screen following the results design. The screen SHALL show a results headline derived from the API display text (e.g. *Encontré 8 taladros para ti*), the products laid out in **at most 2 rows with horizontal scroll**, a **cart button** on each product, a top bar with **Volver**, **Ir al catálogo**, **Nueva búsqueda**, and an **Ir al carrito** control that shows the current cart item count, and a bottom search bar with a microphone for starting the next query. When the response carries filters and/or suggestions, the screen SHALL also show the **filters & suggestions section** for refining the search. There SHALL be **no limit** imposed by the app on the number of products displayed.

#### Scenario: Products rendered as a chat catalog

- **WHEN** a search returns one or more products
- **THEN** the app shows the results screen with the results headline and the products in at most 2 rows that scroll horizontally

#### Scenario: Per-product and top-bar controls present

- **WHEN** the results screen is shown
- **THEN** each product exposes a cart button
- **AND** the top bar exposes Volver, Ir al catálogo, Nueva búsqueda, and Ir al carrito controls
- **AND** a search bar with a microphone is available for the next query

#### Scenario: Filters & suggestions section present when returned

- **WHEN** the results screen is shown and the response carried filters and/or suggestions
- **THEN** the app shows the filters & suggestions section alongside the results

#### Scenario: Per-product cart button adds to the cart

- **WHEN** the consumer taps a product's cart button
- **THEN** the product is added to the cart and the Ir al carrito control's item count increases

#### Scenario: Ir al carrito opens the cart dialog

- **WHEN** the consumer taps the Ir al carrito control
- **THEN** the app opens the cart dialog over the results screen without navigating to a separate cart screen

### Requirement: New search resets the session

The **Nueva búsqueda** control SHALL open a confirmation dialog to close the current search. When the cart contains products, the dialog message SHALL warn that the cart will be emptied and SHALL state how many products it contains; when the cart is empty, the dialog SHALL keep the plain close message. On confirmation, the app SHALL end the customer session (clearing the cart and the consumer data), clear the temporary search history and return to the Home idle screen. The same dialog, opened by **Volver** or the system Back when there is no previous result, SHALL behave the same way.

#### Scenario: New search asks for confirmation

- **WHEN** the consumer taps Nueva búsqueda
- **THEN** the app opens a confirmation dialog to close the current search and return to Home

#### Scenario: Dialog warns that the cart will be emptied

- **WHEN** the cart contains 2 products and the consumer opens the close dialog
- **THEN** the dialog message says the cart (2 productos) will be emptied and the consumer will return to the start

#### Scenario: Dialog with an empty cart keeps the plain message

- **WHEN** the cart is empty and the consumer opens the close dialog
- **THEN** the dialog message says the search will be closed and the consumer will return to the start, with no cart warning

#### Scenario: Confirming new search ends the session

- **WHEN** the consumer confirms the close dialog
- **THEN** the cart and the consumer name and cell-phone are cleared
- **AND** the app clears the temporary search history
- **AND** returns to the Home idle screen

#### Scenario: Cancelling new search keeps the current results

- **WHEN** the consumer dismisses the new-search dialog
- **THEN** the app keeps the current search results, history and cart unchanged

## ADDED Requirements

### Requirement: Ir al catálogo opens the catalog

The product-search top bar SHALL show an **Ir al catálogo** control in every search phase (results, no results, error). Tapping it SHALL open the Catalog screen directly, without a confirmation dialog, keeping the cart and the consumer data. After it, the screen stack SHALL be Home followed by the Catalog, so "Volver" from the Catalog follows the Catalog rules.

#### Scenario: Ir al catálogo from results keeps the cart

- **WHEN** the cart contains 2 products and the consumer taps Ir al catálogo on the results screen
- **THEN** the app shows the Catalog with no dialog
- **AND** the cart control still shows 2

#### Scenario: Ir al catálogo from the no-results screen

- **WHEN** a search returned no products and the consumer taps Ir al catálogo
- **THEN** the app shows the Catalog

#### Scenario: Catalog opened from search sits on top of Home

- **WHEN** the consumer went Home → Catalog → Search and taps Ir al catálogo
- **THEN** the app shows a single Catalog screen above Home
- **AND** "Volver" on that Catalog with an empty cart returns to Home

#### Scenario: Double tap opens one catalog

- **WHEN** the consumer taps Ir al catálogo twice quickly
- **THEN** only one Catalog screen is opened
