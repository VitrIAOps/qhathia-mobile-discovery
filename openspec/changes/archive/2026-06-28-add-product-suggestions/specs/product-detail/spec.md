## ADDED Requirements

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

## REMOVED Requirements

### Requirement: Related products placeholder

**Reason**: Replaced by real related products sourced from `GET /api/v1/products/{id}/suggestions`. The in-feature mock (`RelatedProductsMock`) was an explicit placeholder for the not-yet-built endpoint, which now exists.

**Migration**: Related products are now obtained via `GetProductSuggestionsUseCase` → `ProductDetailRepository.getSuggestions(id)`. The `RelatedProductsMock` object is deleted; no client code references it. Behavior for the consumer is unchanged when suggestions exist; an empty/failed fetch now shows the empty-state message (see "Related-products empty state").
