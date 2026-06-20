## Why

The Discovery search-results screen shows products but gives the in-store consumer no way to narrow them. The Qhathia search API (Fase 9) already returns `filters` and `suggestions` alongside the results, but the app drops both fields on the floor. Surfacing them lets the consumer refine a search by tapping common characteristics (criterion/option filters) or contextual phrases (suggestions) — turning a flat result list into a guided, specific exploration of what the store offers.

## What Changes

- Parse `filters` (`[{criterion, options[]}]`) and `suggestions` (`List<String>`) from the `POST /api/v1/conversations/search` response (currently ignored) and carry them through the network → data → domain layers into `ProductSearchResult`.
- Add a **filters & suggestions section** to the results screen on the happy (has-results) phase, following the Figma frames (default + selected states). Filters and suggestions are empty on the unhappy / no-results phase, so the section is hidden there.
- Selection behavior:
  - **N filter options** can be selected at once (across criteria).
  - **At most one suggestion** can be selected at a time; selecting a new suggestion replaces the previous selection.
- Tapping a filter or suggestion **re-queries** `POST /api/v1/conversations/search`, concatenating the selected terms naturally onto the **original raw query** (e.g. raw query *busco un taladro* + suggestion *para uso doméstico* → *busco un taladro para uso doméstico*). The re-query refreshes the products **and** the filters/suggestions lists from the new response.
- The original raw query is retained as the immutable concatenation base for the current results view, so de-selecting terms returns to the base query.
- Re-queries reuse the existing search state machine: an empty re-query result follows the established alternative-search / unhappy path.

## Capabilities

### New Capabilities
<!-- none — this extends an existing capability -->

### Modified Capabilities
- `product-search`: adds a requirement for surfacing and selecting search filters and suggestions, and for re-querying with the selected terms concatenated onto the original query. The existing query-submission and results requirements are amended to carry filters/suggestions and to support filter/suggestion-driven re-queries.

## Impact

- **`core:network`** — `ConversationSearchResponseDto` gains `filters` and `suggestions`; new `SearchFilterDto` (`criterion`, `options`).
- **`core:domain`** — new domain models `SearchFilter` and (suggestions as `List<String>`); `ProductSearchResult` gains `filters` and `suggestions`. New use case to compose the re-query string from the base query + selected terms.
- **`core:data`** — `ProductMappers` maps the new DTO fields; `ProductSearchRepository.search` signature unchanged (composition happens in domain/presentation).
- **`features:product-search`** — `ProductSearchUiState` / `ProductSearchPhase.Results` carry filters, suggestions, and the current selection; new `ProductSearchUiEvent`s for selecting/deselecting filters and suggestions; `ProductSearchViewModel` tracks the base query and selection and drives re-queries; `ProductSearchContent` renders the new section. New string resources.
- **API**: no contract change — consumes existing `filters` / `suggestions` from `POST /api/v1/conversations/search`.
- No new Gradle module is required; all affected modules already exist.
