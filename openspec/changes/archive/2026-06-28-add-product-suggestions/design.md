## Context

The app is multi-module (Clean Architecture + MVI, Hilt, Ktor, Compose). The `product-detail` slice is complete: `features:product-detail` drives a load state machine via `ProductDetailViewModel`, `core:domain` owns `ProductDetail` + `ProductDetailRepository` + `GetProductDetailUseCase` (and the `DetailException`/`DetailErrorCategory` model), `core:data` implements the repository through `ProductApiService.getProduct` reusing `SessionTokenProvider` with a 401-refresh-retry, and `core:ui` provides the horizontal `ProductRowCard` already used by the related-products strip.

Today the related-products section is fed by `features/product-detail/.../data/RelatedProductsMock.kt` — a hardcoded `List<Product>` flagged `TODO(related-products-api)`, rendered in `ProductDetailContent.RelatedProductsSection` as a non-scrolling `Row`, and suggestion taps (`ProductDetailUiEvent.RelatedProductTapped`) are no-ops. The backend now exposes `GET /api/v1/products/{id}/suggestions` (Fase 10) returning `{ productId, suggestions: ProductSummaryResponse[0..5], source }`, store-scoped from the JWT, fail-open on the server (LLM → same-category → store-popular → empty). The suggested-product shape equals the catalog's `ProductSummaryResponse` (already modeled as `CatalogProductDto`).

This change re-sources the section from that endpoint, makes the strip horizontally scrollable, adds an empty state, and wires suggestion taps to open the tapped product's detail.

## Goals / Non-Goals

**Goals:**
- Fetch related products from `GET /api/v1/products/{id}/suggestions` (Bearer JWT), max 5, mapped to the existing `Product` domain model.
- Load suggestions independently of the detail fetch so a suggestions failure never blocks the detail.
- Render a horizontally scrollable strip via the existing `ProductRowCard`; show a centered empty-state message (in a container-sized box) when there are no suggestions or the fetch fails.
- Navigate into a tapped suggestion's own detail (fresh detail pushed on the back stack), reusing the existing detail route.
- Remove `RelatedProductsMock` and its placeholder wiring.

**Non-Goals:**
- WhatsApp-form navigation (still out of scope, render-only elsewhere).
- Using the response `source`/`brand`/`tags` for differentiated copy ("Otros productos del store") or client-side filtering — deferred.
- Any change to the main detail fetch, gallery, purchase actions, or top-bar chrome.

## Decisions

### Decision: Extend `ProductDetailRepository` rather than introduce a new port/module
Suggestions are keyed by the same product id, hit the same host, and need the same auth seam (`SessionTokenProvider` + 401 retry) as the detail fetch. Adding `suspend fun getSuggestions(id: String): Result<List<Product>>` to the existing `ProductDetailRepository` (implemented in `ProductDetailRepositoryImpl`) keeps the cohesive product-detail data surface in one place and avoids a new module just for one call.
- *Alternative considered:* a dedicated `ProductSuggestionsRepository` + module. Rejected — over-engineering for one endpoint that shares all of detail's plumbing; the standards mandate a module per *feature*, and this is the same feature/capability.

### Decision: Map suggestions to the existing `Product` domain model
The related-products UI already consumes `List<Product>` and renders it with `ProductRowCard` (name, category, price, image). The suggestion item is a `ProductSummaryResponse` — a strict superset of what `Product` needs. A dedicated `SuggestedProductDto` in `core:network` (mirroring `CatalogProductDto`) maps to `Product` at the `core:data` boundary, keeping `brand`/`tags`/`position`/`source` out of the domain for now (modeled in the DTO with `ignoreUnknownKeys`, unused).
- *Alternative considered:* reuse `CatalogProductDto` directly. Rejected — couples two unrelated endpoints' DTOs; a small dedicated DTO is clearer and lets the suggestions contract evolve independently.
- *Alternative considered:* a new `SuggestedProduct` domain model. Rejected — `Product` already carries every field the strip renders; a new model would force a parallel UI path.

### Decision: Load suggestions independently of the detail, as a sub-state of the screen
The section is supplementary; it must not gate the detail. `ProductDetailViewModel` fires the suggestions load separately from `load()` (both keyed off the `SavedStateHandle` product id). The related-products state becomes its own field on `ProductDetailUiState` — a `RelatedProducts` sub-state (`Loading` / `Loaded(items)` / `Empty`) — replacing the `relatedProducts: ImmutableList<Product>` currently nested inside `Status.Loaded`. The `Content` renders the section only within the loaded-detail branch but drives it from this independent sub-state. A failed or empty suggestions fetch both resolve to `Empty` (the empty-state message), so the consumer never sees a suggestions error surfaced as a screen error.
- *Alternative considered:* keep suggestions inside `Status.Loaded` and load after the detail succeeds. Rejected — couples the two fetches and complicates retry; an independent field is simpler and lets suggestions and detail resolve in any order.

### Decision: Suggestion tap navigates via a new `NavigateToDetail` effect wired in `:app`
Per the no-feature-to-feature-dependency rule, the feature emits a one-shot `ProductDetailUiEffect.NavigateToDetail(productId)`; `:app` routes it to `navController.navigate(productDetailRoute(productId))`, pushing a fresh detail on the back stack — exactly how `catalogScreen` and `productSearchScreen` already wire `onNavigateToDetail`. `RelatedProductTapped` stops being a no-op and emits this effect. `productDetailScreen(...)` gains an `onNavigateToDetail: (String) -> Unit` callback.
- *Alternative considered:* reuse `NavigateToSearch`. Rejected — semantically wrong; the target is a detail, not a search.

### Decision: Empty/failed suggestions collapse to a single empty-state
The endpoint is fail-open server-side (`source: "empty"` ⇒ `suggestions: []`). The client treats "empty list" and "request failed" identically: a centered message inside a box sized like the products container (per the task's UX note). This avoids a separate suggestions-error UX and keeps the section unobtrusive.

## Risks / Trade-offs

- **[Suggestions failure masked as empty]** A transient suggestions error looks the same as a legitimately empty store. → Mitigation: acceptable for a supplementary section; the main detail still loads and the consumer can retry by re-entering. No retry control is added for the strip in this slice.
- **[Independent fetch races the detail]** Suggestions may resolve before/after the detail. → Mitigation: they are separate `UiState` fields; `Content` only paints the strip inside the loaded-detail branch, so ordering is harmless.
- **[Back-stack growth from suggestion → detail → suggestion …]** Each tap pushes a new detail. → Mitigation: matches existing catalog/search behavior; Volver pops one level, Nueva búsqueda returns to Home idle. No dedup needed for this slice.
- **[DTO drift vs. `ProductSummaryResponse`]** The dedicated `SuggestedProductDto` could drift from the shared backend shape. → Mitigation: `ignoreUnknownKeys` + field defaults keep deserialization resilient; only rendered fields are mapped.

## Open Questions

- Differentiated copy when `source == "store-popular"` ("Otros productos del store") is explicitly deferred (Non-Goal). If product wants it later, the DTO already carries `source` — only the UI copy/state needs extending.
