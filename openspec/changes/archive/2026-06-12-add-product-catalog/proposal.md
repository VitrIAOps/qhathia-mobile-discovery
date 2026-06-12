## Why

In-store consumers who don't want to search by voice or text need a way to browse everything the store offers, grouped by category, as a general overview. Today the Home "catalog" shortcut lands on a placeholder screen; this change replaces it with a real, API-backed product catalog so consumers can explore the store's available products and jump into a product's detail or start a search.

## What Changes

- Add a **product catalog** screen reachable from the Home catalog shortcut, listing the store's `AVAILABLE` products grouped by the merchant's categories (only categories that have products), each category rendered as a horizontally-scrolling band of **at most 2 rows**.
- Add a synthetic **"Descuentos"** section that aggregates products flagged with `allowDiscountInquiry`; when none exist it shows the empty message *"Productos sin descuentos aún."*
- Fetch the catalog from the new `GET /api/v1/catalog/by-category` endpoint, authenticated with the session Bearer JWT, and show the **network-error ("Unhappy") screen** (*Perdí la conexión… / Parece que la red está inestable…*) when connectivity fails; the error screen auto-returns to **Home idle after 30 seconds** of inactivity.
- Provide top-bar controls — **Volver**, **Nueva búsqueda** (confirmation dialog → Home idle), and **Ir al carrito** (opens the cart dialog with the live item count) — and per-product **cart** buttons; tapping a product card opens its **detail**.
- Provide a **bottom search bar** with a microphone that hands off to the existing **product-search** flow (carrying query + source) when the consumer types a query or taps the mic; the **microphone animation is retained** when the catalog is entered from a voice/chat context.
- Replace the `:app` placeholder `catalog` destination with the new feature module's navigation.

## Capabilities

### New Capabilities
- `product-catalog`: Browsing the store's available products grouped by category (plus a discounts section), with navigation to product detail, hand-off to product search, cart access, and the network-error/idle-timeout behavior.

### Modified Capabilities
<!-- None. Home already emits catalog navigation (discovery-home spec unchanged); the catalog was previously a placeholder, not a specified capability. -->

## Impact

- **New module:** `features:catalog` (Android library + Compose + Hilt), registered in `settings.gradle.kts` and wired into `:app`'s `NavHost` (replacing `placeholderDestinations()`'s `catalog` route).
- **`core:domain`:** new `CatalogCategory` domain model, `ProductCatalogRepository` port, `GetProductCatalogUseCase`, and a `CatalogConstants` (idle-timeout, discounts label). Reuses the existing `Product` model.
- **`core:network`:** new `CatalogApiService` calling `GET /api/v1/catalog/by-category`, plus `@Serializable` catalog DTOs.
- **`core:data`:** new `ProductCatalogRepositoryImpl` (+ mappers and error mapping), reusing the shared `SessionTokenProvider` auth seam and `NetworkResult` handling, with a Hilt `@Binds` module.
- **`:app`:** `MainActivity` NavHost wires the real catalog screen (Home → catalog → detail / product-search / cart); removes the catalog placeholder.
- **Reuse:** existing cart use cases (`AddToCartUseCase`, `ObserveCartUseCase`), the cart dialog overlay, the network-error visual language, and the product-search hand-off route — no changes to those capabilities.
- **API dependency:** `GET /api/v1/catalog/by-category` (Fase 8). No discount fields are exposed by the endpoint; the Descuentos section is derived from `allowDiscountInquiry`.
