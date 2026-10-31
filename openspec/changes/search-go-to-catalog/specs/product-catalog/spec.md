## MODIFIED Requirements

### Requirement: Catalog top-bar controls

The catalog screen SHALL show a top bar with **Volver**, **Nueva búsqueda**, and an **Ir al carrito** control that displays the current cart item count.

The **Volver** control SHALL return to the previous screen state when one exists; when no previous state exists and the cart contains items, **Volver** SHALL open a confirmation dialog to close the current session; when no previous state exists and the cart is empty, **Volver** SHALL end the customer session and return to the Home idle screen.

The **Nueva búsqueda** control SHALL open a confirmation dialog to close the current session. When the cart contains products, the dialog message SHALL warn that the cart will be emptied and SHALL state how many products it contains; when the cart is empty, the dialog SHALL keep the plain close message. On confirmation the app SHALL end the customer session (clearing the cart and the consumer data) and return to the Home idle screen.

The **Ir al carrito** control SHALL open the cart dialog over the catalog without navigating to a separate screen.

#### Scenario: Top-bar controls present

- **WHEN** the catalog screen is shown
- **THEN** the top bar exposes Volver, Nueva búsqueda, and Ir al carrito controls
- **AND** the Ir al carrito control shows the current cart item count

#### Scenario: Volver with no previous state and items in cart asks to close

- **WHEN** the consumer taps Volver, there is no previous screen state, and the cart contains items
- **THEN** the app opens a confirmation dialog to close the current session
- **AND** the dialog warns that the cart will be emptied

#### Scenario: Volver with no previous state and empty cart returns to Home

- **WHEN** the consumer taps Volver, there is no previous screen state, and the cart is empty
- **THEN** the consumer data is cleared and the app returns to the Home idle screen

#### Scenario: New search asks for confirmation then returns to Home

- **WHEN** the consumer taps Nueva búsqueda and confirms the dialog
- **THEN** the cart and the consumer data are cleared
- **AND** the app returns to the Home idle screen

#### Scenario: Cancelling new search keeps the catalog

- **WHEN** the consumer dismisses the Nueva búsqueda dialog
- **THEN** the app keeps the catalog and the cart unchanged

#### Scenario: Ir al carrito opens the cart dialog

- **WHEN** the consumer taps the Ir al carrito control
- **THEN** the app opens the cart dialog over the catalog without navigating to a separate cart screen

#### Scenario: Per-product cart button updates the count

- **WHEN** the consumer taps a product's cart button
- **THEN** the product is added to the cart and the Ir al carrito control's item count increases

### Requirement: Network-error screen and idle timeout

When the catalog request fails due to lost or unstable network connectivity, the app SHALL present the established network-error ("Unhappy") screen showing the title *"Perdí la conexión..."*, the message *"Parece que la red esta inestable. Verifica tu internet e intenta nuevamente."* and a **Reintentar** button that loads the catalog again. If the network-error screen remains without consumer activity for **30 seconds**, the app SHALL return to the Home idle screen.

#### Scenario: Network failure shows the error screen

- **WHEN** the catalog request fails due to lost or unstable connectivity
- **THEN** the app shows the network-error screen with the established title and message
- **AND** a Reintentar button

#### Scenario: Reintentar reloads the catalog

- **WHEN** the network-error screen is shown, the network is back, and the consumer taps Reintentar
- **THEN** the app shows the loading state and then the catalog

#### Scenario: Reintentar fails again

- **WHEN** the consumer taps Reintentar and the request fails again
- **THEN** the network-error screen is shown again and its 30-second idle countdown starts over

#### Scenario: Error screen auto-returns to idle

- **WHEN** the network-error screen has been shown with no consumer activity for 30 seconds
- **THEN** the app returns to the Home idle screen
