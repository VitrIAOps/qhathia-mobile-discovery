# product-catalog Specification

## Purpose

The product-catalog capability lets in-store consumers browse everything the store offers — the available products grouped by the merchant's categories — as a general overview, without going through search. It owns loading the catalog from the Qhathia catalog API, the category tab bar (Todo + one tab per category + Descuentos) over a single two-row product grid, navigation to product detail, cart access, the hand-off to product search, and the network-error / idle-timeout behavior.
## Requirements
### Requirement: Catalog retrieval grouped by category

The app SHALL load the store's catalog via `GET /api/v1/catalog/by-category`, authenticated with the session Bearer JWT, and present the returned products through a category **tab bar** (following the catalog Figma frame). The tab bar SHALL open with an **"Todo"** tab first, followed by one tab per category returned by the endpoint (the backend already returns only categories that have `AVAILABLE` products), preserving the order in which they are returned, and ending with the **"Descuentos"** tab. Selecting a tab SHALL filter the product grid: **Todo** shows every available product across categories; a **category** tab shows that category's products. The app SHALL impose **no limit** on the number of products shown.

#### Scenario: Catalog loads and renders tabs

- **WHEN** the consumer opens the catalog and the request succeeds
- **THEN** the app shows the tab bar with Todo first, one tab per returned category in order, and Descuentos last
- **AND** the Todo tab is selected, showing every available product

#### Scenario: Only categories with products are shown

- **WHEN** the catalog response contains categories
- **THEN** the app renders exactly one tab per category present in the response, without adding empty-category tabs

#### Scenario: Selecting a category filters the grid

- **WHEN** the consumer selects a category tab
- **THEN** the grid shows only that category's products

#### Scenario: Empty catalog

- **WHEN** the catalog response contains no categories
- **THEN** the app shows an empty state indicating the store has no available products yet

### Requirement: Descuentos tab

The app SHALL present a dedicated **"Descuentos"** tab (shown with a discount icon) that, when selected, filters the grid to the products whose `allowDiscountInquiry` flag is true, aggregated across all categories. When no such products exist, the grid SHALL show the centered message *"Productos sin descuentos aún."* The Descuentos tab SHALL always be present whenever the catalog has categories.

#### Scenario: Discounted products listed

- **WHEN** the consumer selects the Descuentos tab and one or more products have `allowDiscountInquiry` true
- **THEN** the grid lists those products

#### Scenario: No discounted products

- **WHEN** the consumer selects the Descuentos tab and no product has `allowDiscountInquiry` true
- **THEN** the grid shows the centered message "Productos sin descuentos aún."

### Requirement: Products laid out in two rows

For the selected tab, the app SHALL lay the products out in **at most 2 rows** that **scroll horizontally**.

#### Scenario: Two-row horizontal layout

- **WHEN** a tab's products are shown
- **THEN** they are arranged in at most 2 rows
- **AND** the products scroll horizontally

### Requirement: Navigation to product detail from the catalog

A product card on the catalog SHALL be tappable to open that product's detail. When the consumer taps a product card, the app SHALL navigate to the product-detail screen for that product's id. This card tap is distinct from the per-card cart button, which adds the product to the cart rather than opening the detail.

#### Scenario: Tapping a product opens its detail

- **WHEN** the consumer taps a product card on the catalog
- **THEN** the app navigates to the product-detail screen for that product's id

#### Scenario: Cart button adds without opening the detail

- **WHEN** the consumer taps a product card's cart button
- **THEN** the product is added to the cart and the app does not open the product detail

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

### Requirement: Bottom search bar hands off to product search

The catalog screen SHALL show a bottom search bar with a microphone. When the consumer submits a typed query or taps the microphone, the app SHALL start the product-search flow, carrying the query text and its source (text or voice). The typed query SHALL be capped at **500 characters**; input beyond that limit SHALL NOT be accepted.

#### Scenario: Typed query hands off to product search

- **WHEN** the consumer submits a typed query from the catalog search bar
- **THEN** the app starts the product-search flow with that query and a text source

#### Scenario: Microphone hands off to product search

- **WHEN** the consumer taps the microphone on the catalog search bar
- **THEN** the app starts the product-search flow with a voice source

#### Scenario: Query length is capped

- **WHEN** the consumer enters a query in the catalog search bar
- **THEN** the app limits the query to at most 500 characters

### Requirement: Microphone animation retained when entered from chat

When the catalog is entered from a voice/chat context, the app SHALL retain the microphone animation on the catalog screen. When the catalog is entered directly (e.g. from the Home catalog shortcut), the microphone animation SHALL NOT be shown.

#### Scenario: Catalog from chat keeps the mic animation

- **WHEN** the catalog is opened from a voice/chat context
- **THEN** the microphone animation is visible on the catalog screen

#### Scenario: Catalog opened directly hides the mic animation

- **WHEN** the catalog is opened directly from the Home catalog shortcut
- **THEN** the microphone animation is not shown

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

### Requirement: Theming and accessibility compliance

The catalog screens SHALL be built from the shared design system, use Material 3 design tokens (no hardcoded colors, type sizes, or dimensions), support light and dark mode, and meet accessibility requirements.

#### Scenario: Dark mode

- **WHEN** the device is in dark mode
- **THEN** the catalog screens render using the design system's dark color scheme

#### Scenario: Interactive controls are accessible

- **WHEN** an assistive technology inspects the top-bar controls, the product cart buttons, and the microphone
- **THEN** each interactive control exposes a localized content description and a touch target of at least 48dp

### Requirement: Catalog system Back follows "Volver"

The system Back on the Catalog screen SHALL run the Catalog "Volver" action: the close confirmation when the cart has products, and the return to Home when the cart is empty.

#### Scenario: Back with products in the cart

- **WHEN** the cart has products and the consumer presses the system Back on Catalog
- **THEN** the close confirmation dialog is shown and the screen stays on Catalog until the consumer confirms

### Requirement: Remote catalog refresh in place

While the catalog screen is shown, a remote catalog refresh request SHALL make it re-fetch the catalog in the background. On success the products and category tabs SHALL be replaced with the fresh data, keeping the selected tab when it still exists and falling back to "Todo" otherwise. The refresh SHALL NOT show a loading indicator, SHALL NOT show the network-error screen when it fails (the current products remain), and SHALL NOT reset the inactivity timer. If the screen is showing the network-error state, the refresh SHALL behave like a retry. A refresh requested while no catalog screen is shown SHALL have no effect on a catalog screen opened later.

#### Scenario: Refresh while browsing a category

- **WHEN** the consumer is on the catalog with the "Iluminación" tab selected and a remote refresh is requested
- **THEN** the catalog is fetched again, the fresh products for "Iluminación" are shown, and the "Iluminación" tab stays selected without a loading indicator

#### Scenario: Selected tab no longer exists

- **WHEN** a remote refresh returns a catalog without the currently selected category
- **THEN** the "Todo" tab is selected

#### Scenario: Refresh fails

- **WHEN** the background re-fetch fails
- **THEN** the current products stay on screen and no error screen is shown

#### Scenario: Refresh on the network-error screen

- **WHEN** the catalog shows the network-error screen and a remote refresh is requested
- **THEN** the catalog is loaded again as on a retry

#### Scenario: Catalog opened after the refresh

- **WHEN** a remote refresh is requested while the consumer is on Home and the catalog is opened afterwards
- **THEN** the catalog performs its normal single load and no extra refresh

