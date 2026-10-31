## MODIFIED Requirements

### Requirement: Retained chrome and navigation controls

The product-detail screen SHALL keep the bottom **microphone animation**, and SHALL provide a top bar with a **Volver** control that returns to the search results, a **Nueva búsqueda** control, and a cart control that shows the current cart item count and opens the cart dialog. The **Nueva búsqueda** control SHALL open a confirmation dialog to close the current search. When the cart contains products, the dialog message SHALL warn that the cart will be emptied and SHALL state how many products it contains; when the cart is empty, the dialog SHALL keep the plain close message. On confirmation the app SHALL end the customer session (clearing the cart and the consumer data, including the share form) and return to the Home idle screen.

#### Scenario: Microphone animation retained

- **WHEN** the product-detail screen is shown
- **THEN** the bottom microphone animation is visible

#### Scenario: Volver returns to results

- **WHEN** the consumer taps Volver on the product-detail screen
- **THEN** the app returns to the search results screen

#### Scenario: New search asks for confirmation and returns to idle

- **WHEN** the consumer taps Nueva búsqueda on the product-detail screen
- **THEN** the app opens a confirmation dialog to close the current search
- **AND** on confirmation the cart and the consumer data are cleared and the app returns to the Home idle screen

#### Scenario: Dialog warns that the cart will be emptied

- **WHEN** the cart contains 1 product and the consumer taps Nueva búsqueda on the product-detail screen
- **THEN** the dialog message says the cart (1 producto) will be emptied

#### Scenario: Cancelling keeps the detail and the cart

- **WHEN** the consumer dismisses the Nueva búsqueda dialog on the product-detail screen
- **THEN** the detail screen and the cart stay unchanged

#### Scenario: Cart control opens the cart dialog

- **WHEN** the consumer taps the cart control on the product-detail screen
- **THEN** the app opens the cart dialog over the detail screen and the control shows the current cart item count
