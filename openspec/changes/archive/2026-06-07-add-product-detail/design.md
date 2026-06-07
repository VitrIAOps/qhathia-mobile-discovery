## Context

The app is multi-module (Clean Architecture + MVI, Hilt, Ktor, Compose). The product-search slice is complete: `features:product-search` drives a results/unhappy state machine, `core:domain` owns the `Product` model + `ProductSearchRepository` port + use cases, `core:data` implements the repository (calling `ConversationSearchApiService` and reusing `SessionTokenProvider` for the Bearer JWT with a 401-refresh-retry), and `core:ui` provides `ProductCard` / `MicButton` / `ProcessingMessage`. Navigation is a single `NavHost` in `:app/MainActivity`, with each feature contributing a `NavGraphBuilder` extension and hoisting navigation targets as callbacks.

Today a search result is a dead end: `ProductCard` exposes only a no-op cart button, and the search `Product` carries just `id`, `name`, `price`, `availability`, `categoryName`, and a single `imageUrl`. This change adds a product-detail destination reachable by tapping a result, backed by `GET /api/v1/products/{id}` which returns the full `ProductResponse` (including `media[]` and `discounts[]`) — strictly richer than the search projection.

The detail screen must follow Figma node `644-26093`: a main image with lateral thumbnails (gray squares when there are fewer than two photos), name/price/availability/description, render-only Add-to-cart and Share-via-WhatsApp buttons, a mock "related products" row, the retained bottom mic animation, and the Volver / Nueva búsqueda / cart top bar.

## Goals / Non-Goals

**Goals:**
- A `features:product-detail` module with a stateless `*Content` + stateful `*Screen` + `@HiltViewModel` following the project MVI contract.
- A richer `ProductDetail` domain model (with a gallery of images) fed by `GET /api/v1/products/{id}`, mapped at the `core:data` boundary, reusing the existing `SessionTokenProvider` + 401-retry pattern.
- Navigation: result card tap → detail; detail Volver → back to results; detail Nueva búsqueda → confirm → Home idle.
- A reusable image-gallery composable in `core:ui` with the gray-square fallback, and a tappable `ProductCard` (adds `onCardClick`).
- A mock related-products section, clearly marked as a placeholder for the future API.
- Detail load states: loading, not-found (404), network error.

**Non-Goals:**
- Add-to-cart action, cart screen, and cart navigation (button renders only).
- WhatsApp share form/navigation (button renders only).
- Related-products API integration (mock only).
- Rendering discounts/tags math beyond what the Figma shows for this slice.

## Decisions

### Decision: New `features:product-detail` module (not a package in product-search or :app)
Per the frontend standards, every feature is its own Gradle module and feature modules may not depend on each other. The detail screen is a distinct vertical slice, so it gets `:features:product-detail`. Product-search only needs to *emit* a navigation effect with the product id; the actual destination is wired in `:app` (which already depends on every feature). This keeps product-search → product-detail decoupled.
- *Alternative considered:* fold detail into `features:product-search`. Rejected — it is a separate destination/capability and would bloat the search state machine; the standards mandate one module per feature.

### Decision: New `ProductDetail` domain model rather than extending `Product`
`Product` is the search-result projection (single `imageUrl`). The detail needs an ordered image gallery and the full description. Adding gallery fields to `Product` would force every search-result path to carry unused data and complicate the search mappers. Instead introduce `ProductDetail` (id, name, description, price, availability, categoryName, `images: List<ProductImage>`) in `core:domain`, with its own repository port and use case.
- *Alternative considered:* reuse `Product` with a nullable image list. Rejected — muddies the search model and its fixtures.

### Decision: Reuse the search data-layer plumbing (SessionTokenProvider + NetworkResult + 401 retry + error categories)
The detail fetch has the same auth and failure surface as search. `core:data` will add `ProductDetailRepositoryImpl` that mirrors `ProductSearchRepositoryImpl`: obtain the token via `SessionTokenProvider`, call a new `ProductApiService.getProduct(token, id)`, map `NetworkResult` → `Result<ProductDetail>`, and translate failures into a `DetailException`/`DetailErrorCategory` (Network / NotFound / DetailApi) so the ViewModel can choose the recovery UX (mirrors `SearchException`/`SearchErrorCategory`). HTTP `404` maps to a distinct `NotFound` category so the screen can show its not-found state.
- *Alternative considered:* a generic shared error type for both flows. Deferred — keep symmetry with the existing per-feature error category to avoid a refactor outside this change's scope.

### Decision: Pass only the product id through navigation; fetch in the ViewModel
The detail route takes `{productId}` (read via `SavedStateHandle`, exactly like product-search reads `ARG_QUERY`/`ARG_SOURCE`). The ViewModel fetches on init. This keeps navigation arguments small/serializable and means a re-entry/refresh always reloads from source.
- *Alternative considered:* pass the already-loaded `Product` from search. Rejected — `Product` lacks the gallery/full description, and Compose nav args should stay primitive.

### Decision: Gallery component + gray-square fallback live in `core:ui`
A `ProductImageGallery` composable (main image + lateral thumbnails, filling empty slots with `DiscoveryColors.Neutral50`-style gray squares) is added to `core:ui` so it stays design-system-owned and previewable in isolation. `ProductCard` gains an optional `onCardClick: (() -> Unit)? = null` so the whole card surface is clickable while keeping the cart button's separate tap — existing call sites (which pass no `onCardClick`) stay source-compatible.
- *Alternative considered:* build the gallery inside the feature. Rejected — it is a reusable visual component and belongs with the other DS components.

### Decision: Related products as a mock in the feature, marked for replacement
The related-products endpoint does not exist yet. The feature renders the section from a hardcoded in-feature mock list of `Product`, reusing `ProductCard`, with a clear `// TODO/placeholder: replace with related-products API when available` comment (and no repository/use-case for it). This satisfies "leave space, bring components to code with mock" without inventing an API contract.
- *Alternative considered:* a domain port returning mock data via a fake. Rejected as over-engineering for a placeholder; a domain seam can be introduced when the real endpoint is specified.

### Decision: Top-bar / mic behavior reuses product-search patterns, re-implemented in the detail feature
Volver, Nueva búsqueda (+ confirm dialog), the cart control, and the bottom `MicButton` mirror the product-search screen. Because they live in another feature module and cannot be imported across features, the small top-bar/dialog composables are re-created in `features:product-detail` (the shared atoms — `MicButton`, icons, tokens — come from `core:ui`). Volver is handled as a navigation effect (`NavigateBack`) so `:app` pops back to the results entry; Nueva búsqueda emits `NavigateHomeIdle` after confirmation, matching product-search.

### Decision: Layout matches Figma node 644-26093 exactly (reviewed via Figma MCP)
The screen was built against the actual Figma frame, not an approximation. Concrete structure adopted:
- A **"Este es el detalle"** heading row (brand dot + label) above the detail card, mirroring the product-search results headline pattern.
- The detail card is a Row: **square main image + a vertical column of square bordered thumbnails** (`Blue200` border), then a flex **Info Col**. Thumbnails use **3 slots** (`ProductDetailConstants.LATERAL_THUMBNAIL_SLOTS = 3`), padded with neutral gray squares when there are fewer than two photos.
- Info Col order: **title (DS H3)** → **stock row** (colored dot + label) → **price (DS H1, 32sp)** in navy `#0A2540` → **divider** → **description (DS Body/MD)** in `#616161` → **action row**. There is **no "Descripción y características" label** (the Figma shows the description directly under the divider).
- The action row is `space-between`: a **render-only quantity stepper** (− / qty / +) on the left, and on the right a navy **"Agregar al carrito"** button (`Blue700 #112947`) plus a green **"Compartir"** button (`Green500 #22C55E`). The share button label is **"Compartir"** (not "Compartir por WhatsApp") per the Figma.
- Related products use the Figma **horizontal** "Product-cards" component, introduced as a new shared `ProductRowCard` in `core:ui` (image left + category tag / name / price), not the vertical `ProductCard`.

New DS tokens were added to back this: `Type.kt` maps the previously-unused `headlineMedium` to **DS H1 (32sp Bold)**; `DiscoveryColors` gains `Blue700`, `Blue200`, `Green500`, `Divider`, `TextMuted`, `AccentOrange`.

### Decision: The bottom chat-area is the full prompt + search bar + mic, and the search bar is functional
The Figma `chat-area` at the bottom is the same component used on product-search (lightbulb + "¿En qué tipo de proyecto lo usarás?" prompt + search input + `MicButton`), not a lone mic. It is reproduced faithfully. To avoid a dead control, the search input is wired: submitting text emits a `NavigateToSearch(query)` effect that `:app` routes to `productSearchRoute(query, Text)`, starting a fresh search. The microphone keeps its animation; tapping it is a no-op for this slice (in-place voice capture on detail is out of scope and the audio pipeline is not wired into this feature).
- *Alternative considered:* render the search bar as a static, non-functional visual. Rejected — an enabled-looking input that does nothing is worse UX than reusing the existing text-search navigation, which is low-risk and consistent with product-search.

## Risks / Trade-offs

- **[Store-scoped detail endpoint vs. consumer device]** `GET /api/v1/products/{id}` is a BizAPI store endpoint; the device JWT is store-scoped, so a product id from this store's search resolves correctly. A foreign/stale id would yield `404` → handled by the not-found state. → Mitigation: rely on ids coming from the same session's search results; surface `404` cleanly.
- **[Top-bar/dialog duplication across features]** Re-implementing the top bar in product-detail duplicates a little UI code that product-search already has. → Mitigation: acceptable per the "no feature-to-feature dependency" rule; if a third consumer appears, promote the top bar into `core:ui`.
- **[Mock related products diverging from the real API]** The placeholder shape may not match the eventual endpoint. → Mitigation: keep the mock behind a single private provider in the feature and comment it explicitly so the swap is localized.
- **[`media[]` ordering / primary flag]** The gallery order depends on `position`/`primary` in the DTO. → Mitigation: sort by `primary` then `position` in the mapper; fall back to gray squares when fewer than two images.
- **[Coil image sizing on the kiosk]** Large images can waste memory. → Mitigation: constrain image sizes in the gallery composable per the performance guidance.

## Open Questions

Resolved after reviewing Figma node `644-26093` via the Figma MCP:

- **Detail fields:** the frame shows title, a stock row (colored dot + label), price, a divider, and the description — no tags/discount badges in this slice. Availability is rendered as the stock row (dot + "Disponible" / "Pocas unidades" / "Consultar"); the API gives an enum, not a unit count, so the count seen in the mockup ("22 disponibles") is intentionally not reproduced.
- **Thumbnails:** the gallery uses a column of square thumbnails next to the square main image; **3 slots** are rendered, filled with gray squares when there are fewer than two photos.
- **Volver:** unchanged — `popBackStack` returns to the existing product-search entry, preserving its ViewModel state.

Remaining (non-blocking):
- The render-only **quantity stepper** and the **"Compartir"** button are visual placeholders; their real behavior (cart quantity, WhatsApp share) is out of scope and will be specified when the cart / WhatsApp features land.
- The mock **related products** strip stays until the related-products API is defined (`TODO(related-products-api)`).
