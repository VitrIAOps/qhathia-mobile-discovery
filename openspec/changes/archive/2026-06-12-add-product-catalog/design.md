## Context

The Discovery (kiosk) app already ships several vertical slices following Clean Architecture + MVI across the multi-module layout (`core:domain`, `core:data`, `core:network`, `core:ui`, `features:*`, thin `:app`). The Home screen exposes a catalog shortcut that today navigates to a placeholder `catalog` destination declared inline in `MainActivity` (`placeholderDestinations()`).

This change turns that placeholder into a real **product-catalog** feature. The backend exposes the data via `GET /api/v1/catalog/by-category` (Fase 8): an array of categories, each with embedded `AVAILABLE` products (`ProductSummaryResponse`). The endpoint is authenticated with the session Bearer JWT and already applies the "only categories with available products" filter server-side.

The closest existing analog is `features:product-search` (results-from-chat catalog with a 2-row horizontal product layout, top bar, cart hand-off, network-error screen, idle timeout). The new feature reuses that visual/behavioral language and the shared seams: `SessionTokenProvider` (auth), `NetworkResult` (error mapping), the `Product` domain model, the cart use cases, the cart dialog overlay, and the product-search hand-off route.

**Constraints**
- New layers/features must be **Gradle modules**, never packages in `:app` (project standards).
- `core:domain` stays framework-free (pure Kotlin/JVM).
- `core:network` must not depend on `core:domain`; DTO→domain mapping happens in `core:data`.
- Hilt via KSP2; JUnit5 + MockK + Turbine for tests; `@Preview` (light/dark) for the screen.

## Goals / Non-Goals

**Goals:**
- A `features:catalog` module presenting the store's products grouped by category, plus a synthetic Descuentos section, with detail navigation, cart access, search hand-off, and the network-error + 30s-idle behavior.
- Reuse existing core seams and the product-search visual language; no duplication of the voice state machine.
- Replace the `:app` catalog placeholder with the real screen.

**Non-Goals:**
- No in-place voice capture on the catalog — the search bar hands off to `features:product-search` (decision below).
- No real discount/pricing model — the Descuentos section is derived from `allowDiscountInquiry` since the endpoint exposes no discount fields (decision below).
- No client-side pagination, filtering (e.g. by `brand`), caching, or offline persistence of the catalog in this change.
- No changes to the `discovery-home` capability — Home already emits catalog navigation.

## Decisions

### Module & layer placement

New module `features:catalog` (`com.qhathia.mobile.discovery.feature.catalog`), Android library + Compose + Hilt, depending on `core:domain`, `core:ui`, `core:common`. Supporting layers extend existing core modules:

- `core:domain` — `CatalogCategory` model, `ProductCatalogRepository` port, `GetProductCatalogUseCase`, `CatalogConstants`.
- `core:network` — `CatalogApiService` + `CatalogDtos` (`CatalogCategoryDto`, reusing/aligning with the existing product summary shape).
- `core:data` — `ProductCatalogRepositoryImpl` + `CatalogMappers` + error mapping + `ProductCatalogDataModule` (`@Binds`).

Dependency flow stays legal: `features:catalog → core:domain ← core:data → core:network`. **Alternative considered:** putting the API call inside the feature module — rejected, violates the layer rules (network/data belong in core).

### Discounts section from `allowDiscountInquiry` (confirmed with stakeholder)

The endpoint returns no discount/price-off data, only `allowDiscountInquiry: Boolean` per product. The Descuentos section aggregates products where `allowDiscountInquiry == true` across all returned categories; empty → centered *"Productos sin descuentos aún."* This is modeled in the domain as a derived section computed in the use case (or repository), so the UI receives a ready-to-render ordered list of sections (categories + a Descuentos section that is always present). **Alternatives considered:** (a) call `GET /api/v1/products/{id}/discounts` per product — rejected, N+1 calls, no batch endpoint, slow; (b) wait for a backend discount field — rejected, blocks delivery on a contract change not yet planned. Trade-off captured under Risks.

### Search bar hands off to product-search (confirmed with stakeholder)

Typed submit and mic tap both navigate to the existing `productSearchRoute(query, source)`; the catalog remains display-only for search. The mic tap carries `SearchSource.Voice` so product-search performs the in-place capture it already implements. **Alternative considered:** replicate the STT state machine on the catalog — rejected, duplicates `ProductSearchViewModel`'s voice logic for no added value.

### Entry source & retained mic animation

The catalog route takes an optional `source` argument (mirroring product-search's `ARG_SOURCE`), defaulting to a direct entry. When entered from a voice/chat context the screen retains the mic animation; a direct entry from the Home shortcut does not show it. Home's existing catalog shortcut navigates with the direct/default source, so no `discovery-home` change is required. The argument keeps a future "catalog from chat" entry a one-line nav change.

### Presentation: category tab bar over a single grid (Figma-driven)

The catalog Figma frame (node 758-25823) presents a **centered category tab bar** — `Todo`, one tab per store category, then `Descuentos` (with a discount icon) — over a **single two-row horizontally-scrolling product grid** that shows the selected tab's products, not stacked per-category sections. The implementation follows the Figma (the design system is the source of truth): the `Todo` tab shows all products, a category tab filters to that category, and `Descuentos` filters to `allowDiscountInquiry` products (empty → *"Productos sin descuentos aún."*). The 2-row grid reuses `core:ui`'s `HorizontalRowsGrid`; the toolbar and bottom chat-area (prompt + search bar + mic) reuse the product-search chrome. A `CatalogTabTrack` color token and an `ic_discount` vector were added to `core:ui`.

### MVI surface

`CatalogUiState` (`tabs`, `selectedTabId`, `visibleProducts`, loading/network-error flags, `cartItemCount`, `showCloseConfirmDialog`, `source`, `query`), `CatalogUiEvent` (Retry, TabSelected, ProductTapped, CartTapped, OpenCart, Back, NewSearch, ConfirmClose, DismissDialog, QueryChanged, SubmitSearch, ActivateMic), `CatalogUiEffect` (NavigateToDetail, OpenCart, NavigateHomeIdle, NavigateToSearch(query, source)). `CatalogViewModel` keeps the loaded categories, builds the tab list, recomputes `visibleProducts` on tab selection, observes the cart via `ObserveCartUseCase`, adds via `AddToCartUseCase`, and runs the 30s idle timer on the network-error state (pattern lifted from `ProductSearchViewModel.startIdleTimer`).

### Cart & error reuse

Cart stays a `:app`-hosted dialog overlay opened via the `onOpenCart` callback (feature modules can't depend on each other) — identical to product-search/detail. Network errors map through `NetworkResult.NetworkError`; the established title/message strings live in the catalog feature's `strings.xml`, matching the product-search error copy.

## Risks / Trade-offs

- **Descuentos semantics are approximate** → `allowDiscountInquiry` means "consumer may ask about discounts", not "has an active discount". The section may over- or under-include vs. true promotions. Mitigation: isolate the predicate in one mapper/use-case function and document it, so swapping to a real discount source later is a localized change; flagged as an open question for product/backend.
- **No caching / full payload each open** → large catalogs re-fetch fully and render every `AVAILABLE` product (no app-side limit, per spec). Mitigation: `LazyRow`/`LazyColumn` with stable `key`s; client pagination deferred as a non-goal; revisit if payloads grow.
- **`source` argument unused by current callers** → only the direct entry exists today, so the retained-mic path is initially dormant. Mitigation: still spec'd and tested via the source param so the "from chat" entry is trivial later; low cost.
- **Endpoint/DTO drift** → catalog reuses the product-summary shape (`brand`, `primaryImageUrl` nullable). Mitigation: `ignoreUnknownKeys` on the Ktor JSON config (already set) and nullable DTO fields with defaults.

## Open Questions

- Should the Descuentos section eventually reflect **actual active discounts** (requires a backend field on `/catalog/by-category` or a batch discounts endpoint), rather than `allowDiscountInquiry`? Deferred to product/backend.
- Is there a near-term "catalog from chat" entry point that should populate the voice `source`, or does it remain dormant until that flow is designed?
