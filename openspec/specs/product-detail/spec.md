# product-detail Specification

## Purpose

The product-detail capability lets in-store consumers open a single product from a search result and read its full detail — name, description and characteristics, price, availability, and its photo gallery — closing the discovery loop from "I found something that might match" to "I can read about it and decide". It owns fetching a product by id from the Qhathia backend, the detail screen layout and load states, the render-only purchase actions (Add-to-cart and Share via WhatsApp), the mock related-products placeholder, and the retained microphone animation and top-bar navigation controls.

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

### Requirement: Render-only purchase actions

The product-detail screen SHALL display an **Add-to-cart** button and a **Share via WhatsApp** button. Tapping either button SHALL NOT perform the action: the Add-to-cart button SHALL NOT add the product to a cart or navigate to a cart, and the Share-via-WhatsApp button SHALL NOT navigate to a WhatsApp form. These actions are out of scope for this change.

#### Scenario: Action buttons are present

- **WHEN** the product-detail screen is shown
- **THEN** an Add-to-cart button and a Share-via-WhatsApp button are visible

#### Scenario: Action buttons do not act

- **WHEN** the consumer taps the Add-to-cart button or the Share-via-WhatsApp button
- **THEN** the app does not add to a cart, navigate to a cart, or navigate to a WhatsApp form

### Requirement: Related products placeholder

The product-detail screen SHALL render a **related-products** section (*"Podrías acompañar este producto con…"*) populated from **mock data**. The app SHALL NOT call any related-products API. The mock SHALL be implemented so it is clearly marked in code as a placeholder to be replaced once the related-products endpoint is available.

#### Scenario: Related products shown from mock

- **WHEN** the product-detail screen is shown
- **THEN** the related-products section is displayed using mock data
- **AND** no related-products API request is made

### Requirement: Retained chrome and navigation controls

The product-detail screen SHALL keep the bottom **microphone animation**, and SHALL provide a top bar with a **Volver** control that returns to the search results, a **Nueva búsqueda** control, and a cart control. The **Nueva búsqueda** control SHALL open a confirmation dialog to close the current search; on confirmation the app SHALL return to the Home idle screen.

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

### Requirement: Theming and accessibility compliance

The product-detail screen SHALL be built from the shared design system, use Material 3 design tokens (no hardcoded colors, type sizes, or dimensions), support light and dark mode, and meet accessibility requirements.

#### Scenario: Dark mode

- **WHEN** the device is in dark mode
- **THEN** the product-detail screen renders using the design system's dark color scheme

#### Scenario: Interactive controls are accessible

- **WHEN** an assistive technology inspects the top-bar controls, the Add-to-cart and Share-via-WhatsApp buttons, and the microphone
- **THEN** each interactive control exposes a localized content description and a touch target of at least 48dp
