## Why

The product-detail screen already renders a "Podrías acompañar este producto con…" section, but it is fed by a hardcoded in-feature mock (`RelatedProductsMock`) left as a placeholder when the detail slice shipped. The backend now exposes `GET /api/v1/products/{id}/suggestions` (Fase 10), so we can replace the mock with real, store-scoped suggestions — letting an in-store consumer discover complementary products and jump straight into their detail, accelerating their search and orienting the purchase.

## What Changes

- Replace the mock related-products source with a real fetch from `GET /api/v1/products/{id}/suggestions` (Bearer JWT), returning **0–5** complementary products for the base product, mapped to the existing `Product` domain model.
- **Wire navigation from a related product into its own detail**: tapping a suggestion (currently a no-op) navigates to the product-detail screen for that product id (a fresh detail pushed on the back stack), mirroring how the catalog and search cards already open detail. WhatsApp-form navigation stays out of scope.
- Add an **empty/unavailable state** for the section: when there are no suggestions (or the suggestions fetch fails), show a centered message inside a box sized like the products container, instead of hiding the row silently.
- Render the suggestions in a **horizontally scrollable** strip (max 5), reusing the existing `ProductRowCard` design-system component.
- Load suggestions **independently of the main detail fetch** so a suggestions failure never blocks or breaks the product detail itself.
- **Remove** `RelatedProductsMock` and its placeholder wiring once the real source is in place.

## Capabilities

### New Capabilities
<!-- None — this change re-sources and extends an existing capability. -->

### Modified Capabilities
- `product-detail`: The "Related products placeholder" requirement is replaced — related products are now sourced from `GET /api/v1/products/{id}/suggestions` (max 5) instead of mock data, loaded independently of the detail fetch; the section now supports navigation into a tapped suggestion's detail, an empty-state message when there are no suggestions or the fetch fails, and a horizontally scrollable strip.

## Impact

- **Modules touched:**
  - `core:network` — add `getSuggestions(token, id)` to `ProductApiService` plus a `ProductSuggestionsResponseDto` (+ suggested-product DTO mirroring `ProductSummaryResponse`).
  - `core:domain` — add `getSuggestions(id): Result<List<Product>>` to the `ProductDetailRepository` port and a `GetProductSuggestionsUseCase`. Reuse the existing `Product` model and `DetailException`/`DetailErrorCategory`.
  - `core:data` — implement `getSuggestions` in `ProductDetailRepositoryImpl` (same `SessionTokenProvider` + 401-refresh-retry pattern), with a DTO→`Product` mapper.
  - `core:testing` — suggestion fixtures and fake-repository support.
  - `features:product-detail` — inject `GetProductSuggestionsUseCase`, load suggestions independently, model a related-products sub-state (loading/loaded/empty), render the horizontally scrollable strip + empty-state, wire suggestion taps to a new `NavigateToDetail` effect; **delete** `RelatedProductsMock`.
  - `:app` — wire the product-detail `onNavigateToDetail` callback to push `productDetailRoute(productId)` (as catalog/search already do).
- **API:** consumes `GET /api/v1/products/{id}/suggestions` (Bearer JWT, any authenticated role). `source` field is informational; the client renders/hides based on whether `suggestions` is empty. `404`/`401` reuse the existing detail error handling.
- **Manifest:** no new permissions — `INTERNET` / `ACCESS_NETWORK_STATE` already declared.
- **Out of scope:** WhatsApp-form navigation; using the `source`/`brand`/`tags` fields for differentiated copy or client-side filtering.
