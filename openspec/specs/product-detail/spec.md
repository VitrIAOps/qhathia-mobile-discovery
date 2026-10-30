# product-detail Specification

## Purpose

The product-detail capability lets in-store consumers open a single product from a search result and read its full detail — name, description and characteristics, price, availability, and its photo gallery — closing the discovery loop from "I found something that might match" to "I can read about it and decide". It owns fetching a product by id from the Qhathia backend, the detail screen layout and load states, the purchase actions (Add-to-cart, and Share via WhatsApp which shares the viewed product as a WhatsApp quote per the cart-whatsapp-share capability), the related-products section sourced from the suggestions API, and the retained microphone animation and top-bar navigation controls.
## Requirements
### Requirement: Fetch product detail by id

The app SHALL fetch a single product's full detail from the Qhathia backend via `GET /api/v1/products/{id}`, authenticated with the session Bearer JWT, and present it on the product-detail screen. The fetched detail SHALL include the product name, description, price, availability, and its media (photo) collection. On an authentication failure (`401`) the app SHALL drop the cached session token and retry the request once with a refreshed token, reusing the established session-token seam.

#### Scenario: Detail loaded for a product id

- **WHEN** the product-detail screen is opened for a product id
- **THEN** the app requests `GET /api/v1/products/{id}` with the session Bearer JWT
- **AND** presents the returned product's detail on the screen

#### Scenario: Expired token is refreshed once

- **WHEN** the detail request returns `401`
- **THEN** the app clears the cached session token, refreshes it, and retries the request once

### Requirement: Detail load states

The product-detail screen SHALL show a loading indication while the detail request is in flight, a not-found state when the product does not exist (`404`), and the established network-error message when the request fails due to lost or unstable connectivity.

#### Scenario: Loading while fetching

- **WHEN** the detail request is in progress
- **THEN** the app shows a loading indication

#### Scenario: Product not found

- **WHEN** the detail request returns a not-found (`404`) result
- **THEN** the app shows a not-found state for the product

#### Scenario: Network error keeps the established message

- **WHEN** the detail request fails due to lost or unstable network connectivity
- **THEN** the app shows the established network-error title and message

### Requirement: Product detail layout

When a product's detail is loaded, the app SHALL present it following the detail design: a **main product image** accompanied by **lateral thumbnail images**, together with the product **name**, **price**, **availability**, and its **description and characteristics**. When the product has **fewer than two photos**, the lateral image slots SHALL be filled with **neutral gray placeholder squares**.

#### Scenario: Detail rendered with gallery and information

- **WHEN** a product detail with two or more photos is shown
- **THEN** the app shows the main image, the lateral thumbnail images, and the product name, price, availability, and description & characteristics

#### Scenario: Fewer than two photos fills gray squares

- **WHEN** a product detail has fewer than two photos
- **THEN** the lateral image slots are filled with neutral gray placeholder squares

### Requirement: Purchase actions

The product-detail screen SHALL display a quantity stepper, an **Add-to-cart** button, and a **Share via WhatsApp** button. The quantity stepper SHALL be functional and bound the selectable quantity by the product's stock-derived maximum. Tapping **Add-to-cart** SHALL add the product to the cart using the quantity selected in the stepper. The **Share via WhatsApp** button SHALL NO LONGER be render-only — tapping it SHALL open a share dialog that shares the currently viewed product as a WhatsApp quote, as defined by the "Share this product as a WhatsApp quote" requirement.

#### Scenario: Action buttons are present

- **WHEN** the product-detail screen is shown
- **THEN** a quantity stepper, an Add-to-cart button, and a Share-via-WhatsApp button are visible

#### Scenario: Add-to-cart adds with the selected quantity

- **WHEN** the consumer sets the stepper to N and taps the Add-to-cart button
- **THEN** the product is added to the cart with quantity N and the cart item count increases

#### Scenario: Share via WhatsApp opens the share dialog

- **WHEN** the consumer taps the Share-via-WhatsApp button on a loaded product
- **THEN** the app opens a dialog to collect the consumer's name and cell-phone and share this product as a WhatsApp quote

### Requirement: Share this product as a WhatsApp quote

From the product-detail screen the app SHALL let the consumer share the currently viewed product as a WhatsApp quote. Tapping **"Compartir por WhatsApp"** SHALL open a self-contained dialog with a consumer-data form (name and cell-phone) validated with the same rules the cart uses. On submit the app SHALL share a quote for this single product at the quantity selected in the detail's stepper, retrying up to three times before showing the error state, and SHALL reuse the shared blocking-loading, success, and error states. The share SHALL be submitted only when the form is valid, SHALL NOT create a purchase order, and SHALL NOT require the product to be in the cart.

#### Scenario: Share dialog collects consumer data

- **WHEN** the consumer taps "Compartir por WhatsApp" on a loaded product
- **THEN** the app opens a dialog with a name field and a cell-phone field, disabled for submit until both are valid

#### Scenario: Invalid data is never submitted

- **WHEN** the share dialog's form is invalid (missing or malformed name or cell-phone)
- **THEN** the "Compartir" action does not submit a share request

#### Scenario: Shares the single product at the selected quantity

- **WHEN** the consumer submits the share dialog with valid data
- **THEN** the app submits a share request for this product with the quantity selected in the detail's stepper
- **AND** retries the share up to three times before showing the error state

#### Scenario: Blocking state during share

- **WHEN** a share request from the dialog is in flight
- **THEN** the dialog shows a centered loading indicator over a dimmed background and blocks its inputs
- **AND** no second share request is submitted while one is in flight

#### Scenario: Accept closes and resets the dialog

- **WHEN** the share succeeds and the consumer taps "Aceptar"
- **THEN** the dialog closes and its form is reset

#### Scenario: Error offers retry or cancel

- **WHEN** the share fails after the retries
- **THEN** the dialog shows a simplified error with "Entendido" (returns to the form, preserving entered data) and "Cancelar" (closes the dialog)

### Requirement: Related products from the suggestions API

The product-detail screen SHALL populate its related-products section (*"Podrías acompañar este producto con…"*) from `GET /api/v1/products/{id}/suggestions`, authenticated with the session Bearer JWT, showing up to **5** complementary products for the base product. The suggestions SHALL be fetched and loaded **independently of the main detail fetch**, so a suggestions failure never blocks or breaks the product detail. The suggestions SHALL be presented in a **horizontally scrollable** strip using the shared design-system product card. Tapping a suggestion SHALL navigate to that product's own product-detail screen. Each suggestion card SHALL also expose an **add-to-cart** button that adds that product to the cart (quantity 1). On an authentication failure (`401`) the app SHALL reuse the established session-token refresh-and-retry-once seam.

#### Scenario: Suggestions loaded and shown

- **WHEN** the product-detail screen is opened for a product id
- **THEN** the app requests `GET /api/v1/products/{id}/suggestions` with the session Bearer JWT
- **AND** presents the returned products (up to 5) in a horizontally scrollable related-products strip

#### Scenario: Suggestions load independently of the detail

- **WHEN** the suggestions request fails but the detail request succeeds
- **THEN** the app still presents the product detail
- **AND** the related-products section shows its empty state rather than breaking the screen

#### Scenario: Tapping a suggestion opens its detail

- **WHEN** the consumer taps a product in the related-products strip
- **THEN** the app navigates to the product-detail screen for that product's id

#### Scenario: Adding a suggestion to the cart

- **WHEN** the consumer taps the add-to-cart button on a related-products card
- **THEN** that product is added to the cart with quantity 1 and the cart item count increases

#### Scenario: Expired token is refreshed once

- **WHEN** the suggestions request returns `401`
- **THEN** the app clears the cached session token, refreshes it, and retries the request once

### Requirement: Related-products empty state

When there are no related products for the product — the suggestions response returns an empty list, or the suggestions request fails — the product-detail screen SHALL show a message indicating no related products are available, centered inside a box sized like the related-products container, rather than hiding the section silently.

#### Scenario: Empty list shows the message

- **WHEN** the suggestions response returns an empty list
- **THEN** the related-products section shows a centered "no related products" message in a box the size of the products container

#### Scenario: Fetch failure shows the message

- **WHEN** the suggestions request fails
- **THEN** the related-products section shows the centered "no related products" message rather than disappearing

### Requirement: Retained chrome and navigation controls

The product-detail screen SHALL keep the bottom **microphone animation**, and SHALL provide a top bar with a **Volver** control that returns to the search results, a **Nueva búsqueda** control, and a cart control that shows the current cart item count and opens the cart dialog. The **Nueva búsqueda** control SHALL open a confirmation dialog to close the current search; on confirmation the app SHALL return to the Home idle screen.

#### Scenario: Microphone animation retained

- **WHEN** the product-detail screen is shown
- **THEN** the bottom microphone animation is visible

#### Scenario: Volver returns to results

- **WHEN** the consumer taps Volver on the product-detail screen
- **THEN** the app returns to the search results screen

#### Scenario: New search asks for confirmation and returns to idle

- **WHEN** the consumer taps Nueva búsqueda on the product-detail screen
- **THEN** the app opens a confirmation dialog to close the current search
- **AND** on confirmation the app returns to the Home idle screen

#### Scenario: Cart control opens the cart dialog

- **WHEN** the consumer taps the cart control on the product-detail screen
- **THEN** the app opens the cart dialog over the detail screen and the control shows the current cart item count

### Requirement: Theming and accessibility compliance

The product-detail screen SHALL be built from the shared design system, use Material 3 design tokens (no hardcoded colors, type sizes, or dimensions), support light and dark mode, and meet accessibility requirements.

#### Scenario: Dark mode

- **WHEN** the device is in dark mode
- **THEN** the product-detail screen renders using the design system's dark color scheme

#### Scenario: Interactive controls are accessible

- **WHEN** an assistive technology inspects the top-bar controls, the Add-to-cart and Share-via-WhatsApp buttons, and the microphone
- **THEN** each interactive control exposes a localized content description and a touch target of at least 48dp

### Requirement: Detail microphone opens voice search

The microphone control on the product detail search bar SHALL open the product search screen in voice mode with no query, which starts listening, as the Catalog microphone does.

#### Scenario: Tap the detail microphone

- **WHEN** the consumer taps the microphone on the product detail
- **THEN** the product search screen opens and starts listening

### Requirement: Share dialog stays open while sending

While a product share request is in flight, the share dialog SHALL NOT be closed by an outside tap, the system Back or its close control. Once a result is shown, closing the dialog SHALL run the result's main action (accept on success, "Entendido" on an error).

#### Scenario: Outside tap while sending

- **WHEN** a product share request is in flight and the consumer taps outside the dialog
- **THEN** the dialog stays open with its loading indicator

#### Scenario: Back on the success screen

- **WHEN** the share success screen is shown and the consumer presses the system Back
- **THEN** the dialog closes as if the consumer had accepted the success screen

### Requirement: Unavailable product blocks purchase actions

When the loaded product detail reports the product as inactive, or its availability is not `AVAILABLE`, the product-detail screen SHALL disable the Add-to-cart and Share-via-WhatsApp buttons and the quantity stepper, and SHALL show a visible note that the product is not available right now. The rest of the detail (images, name, price, related products) SHALL still be shown.

#### Scenario: Inactive product opened from a stale result

- **WHEN** the consumer opens the detail of a product whose detail response has `active = false`
- **THEN** the detail is shown with a "not available" note
- **AND** Add-to-cart and Share are disabled

#### Scenario: Low-stock product cannot be shared

- **WHEN** the detail of a product with availability `LOW_STOCK` is shown
- **THEN** the Share-via-WhatsApp button is disabled and no share request can be sent

#### Scenario: Available product keeps its actions

- **WHEN** the detail of an active `AVAILABLE` product is shown
- **THEN** Add-to-cart, the stepper and Share are enabled

### Requirement: Product share reports the real delivery outcome

The product share dialog SHALL follow the same delivery rules as the cart share: success only when the endpoint answers `status = SENT`, the not-delivered error for `FAILED` or `STUB`, and retries only after a connectivity error, a timeout, or a 5xx answer.

#### Scenario: FAILED status in the product share

- **WHEN** the consumer confirms the product share and the endpoint answers `201` with `status = FAILED`
- **THEN** the dialog shows the not-delivered error, not the success screen

#### Scenario: Deterministic rejection sent once

- **WHEN** the product share endpoint answers 400
- **THEN** exactly one request is sent and the dialog shows the error state

