## Why

The product-search flow now lets an in-store consumer find products, but tapping a result goes nowhere — `ProductCard` only exposes a (no-op) cart button and the search results carry just a name, price, and primary image. A consumer who wants to confirm *what a product actually is* — its full description, characteristics, and photos — has no way to do so. This change adds the **product-detail screen**, closing the discovery loop from "I found something that might match" to "I can read about it and decide", which is the core promise of the Discovery experience.

## What Changes

- Introduce a **product-detail capability**: fetch a single product by id from the Qhathia backend (`GET /api/v1/products/{id}`, Bearer JWT) and present its full detail — name, description/characteristics, price, availability, and its photo gallery — following the detail Figma (node `644-26093`).
- Add **navigation into detail from a search result**: tapping a product card on the results (and unhappy) screen navigates to the product-detail screen for that product id. (Today the card only has a cart button; a card tap is added.)
- Add the **detail screen layout** per Figma: a **main product image** with **lateral thumbnail images**; when the product has **fewer than 2 photos**, the lateral slots are filled with **neutral gray placeholder squares**. Below/beside the gallery: the name, price, availability, and the description & characteristics.
- Add the detail-screen **action buttons that render but do not act yet** (out of scope per task): an **Add-to-cart** button (no cart action / no cart navigation) and a **Share via WhatsApp** button (no navigation to the WhatsApp form).
- Add a **"related products" section** (*"Podrías acompañar este producto con…"*) wired in the UI **with mock data only** — the related-products API is not built yet, so the component is rendered from a hardcoded mock and clearly **commented as a placeholder to be replaced** when the endpoint lands. No related-products API call is made.
- Reuse the established **chrome and behaviors** from product-search on the detail screen: the **bottom microphone animation is kept**; the top bar exposes **Volver** (returns to the search results), **Nueva búsqueda** (opens a confirmation dialog that, on confirm, closes the search and returns to Home idle), and the cart control. The text-search-without-mic-animation rule is unchanged (it lives in product-search).
- **Out of scope (deferred):** the Add-to-cart action and cart navigation, the WhatsApp form/navigation, and the related-products API integration (mock only).

## Capabilities

### New Capabilities
- `product-detail`: The end-to-end product-detail experience — fetching a product by id via `GET /api/v1/products/{id}` (Bearer JWT) and mapping it to a domain detail model with its media gallery; the detail screen following the Figma (main image + lateral thumbnails, gray-square fallback when there are fewer than 2 photos, name/price/availability/description & characteristics); the render-only Add-to-cart and Share-via-WhatsApp buttons; the mock "related products" section flagged for later API replacement; the retained bottom microphone animation; and the top-bar Volver (back to results) / Nueva búsqueda (confirm-and-close to Home idle) controls. Loading, not-found, and network-error states for the detail fetch are included.

### Modified Capabilities
- `product-search`: A product card on the results and unhappy screens is now **tappable to open that product's detail** (previously a card exposed only a non-navigating cart button). The product-search screen gains a one-shot navigation effect to hand the selected product id off to the product-detail destination.

## Impact

- **New module:** `features:product-detail` (the detail screen, `ProductDetailViewModel`, UiState/UiEvent/UiEffect, the detail load state machine, navigation graph, and detail copy). Feature modules cannot depend on each other, so shared pieces live in `core:*`.
- **Modules touched:** `core:domain` (`ProductDetail` domain model + `ProductImage`, `ProductDetailRepository` port, `GetProductDetailUseCase`, a `DetailErrorCategory`/exception mirroring the search error model, and detail constants), `core:network` (`ProductApiService` for `GET /api/v1/products/{id}` + `ProductDetailResponseDto`/`MediaDto`/`DiscountDto`), `core:data` (`ProductDetailRepositoryImpl` + DTO→domain mappers + Hilt wiring, reusing `SessionTokenProvider` for the Bearer JWT and the 401-refresh-retry pattern), `core:ui` (a product-image-gallery component with the gray-square fallback; add an optional `onCardClick` to the existing `ProductCard`; reuse `ProductCard`/`MicButton`), `core:testing` (fakes/fixtures for the detail repository and a `ProductDetail` fixture), `features:product-search` (card tap → `NavigateToDetail(productId)` effect), `:app` (register the detail destination in the NavHost and wire ProductSearch→Detail and Detail→Home-idle navigation).
- **Build / tooling:** `settings.gradle.kts` registers `:features:product-detail`; no new third-party dependencies expected (Ktor + serialization + Coil already present).
- **API:** consumes `GET /api/v1/products/{id}` (Bearer JWT, any role) — returns the full `ProductResponse` with `media[]` and `discounts[]`. Only the rendered fields are modeled (`ignoreUnknownKeys`).
- **Manifest:** no new permissions — `INTERNET` / `ACCESS_NETWORK_STATE` are already declared.
- **Out of scope / deferred:** Add-to-cart action, cart screen/navigation, WhatsApp form/navigation, and the related-products API (mock placeholder only).
