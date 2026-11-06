## Why

The search chips do not filter. In the QA regression of 2026-11-03 (D:BUS-003 / D:EXP-004, F-37, HIGH), searching "taladro" and tapping the "Bosch" chip sent `{"transcribedText":"taladro marca bosch"}`, with no `filters`, and returned a drill with no brand, a DeWalt and a Bosch. With two chips the concatenated sentence ("taladro marca bosch inalámbrico") fell into "no entendí", and the chips were lost.

The cause is that `ComposeSearchQueryUseCase` flattens each chip into the free text, and the LLM reinterprets the whole sentence. Since backend phase 41 (api-contract §41), the search accepts structured `filters` (`brands`, `tags`) next to the original text, and every criterion carries `values` parallel to its `options`.

There is a second problem. The backend computes the chip panel from the results it returns, so with real filters a refined response would shrink the panel to the selected chips only.

## What Changes

- **Chips become structured filters.**
  - Selecting or clearing a chip re-sends the **original text** plus `filters`.
  - Options of the "Marca" criterion go into `filters.brands`.
  - Options of any other criterion go into `filters.tags`, as their `values[i]`, never as the label.
  - The suggestion phrase stays a natural-language refinement appended to the text, as today.
- **Stable chip panel.** The criteria, options and suggestions shown are the ones from the search that opened the results view. A refinement only changes the products and the selection.
- **"No match" keeps the chips.** When the filters leave no product, the results view stays, with the chips still selected, no products, and "No hay productos con esos filtros. Quita alguno para ver más.". It no longer falls into the unhappy "no entendí" screen. **BREAKING** (behavior) for the "empty re-query follows the unhappy path" scenario.
- **Alternatives-only notice.** When the response says `alternativesOnly` (the requested product is not sold here, and the results are alternatives), the results headline says "No tenemos «X»; te mostramos alternativas." (`missingCategory`).

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `product-search`:
  - filter chips are sent as structured `filters` with the original text, replacing the concatenation for filters (suggestions keep it);
  - the panel stays as the fresh search returned it;
  - a refinement without matches keeps the chips and says so;
  - the alternatives-only notice is shown.

## Impact

- `core:network`:
  - `ConversationSearchRequestDto.filters: SearchFiltersDto? = null` (`brands`, `tags`);
  - `SearchFilterDto.values`;
  - `ConversationSearchResponseDto.alternativesOnly` / `missingCategory`;
  - `ConversationSearchApiService.search(…, filters)`.
- `core:domain`:
  - `SearchFilter.values` (defaults to `options`) and `StructuredSearchFilters`;
  - `ProductSearchResult.alternativesOnly` / `missingCategory`;
  - `ProductSearchRepository.search(query, language, filters)` and `SearchProductsUseCase(query, language, filters)`;
  - `ComposeSearchQueryUseCase(base, suggestion)` (filters dropped);
  - `ProductSearchConstants.BRAND_CRITERION = "Marca"`.
- `core:data`: request and response mappers, `ProductSearchRepositoryImpl`.
- `core:testing`: `FakeProductSearchRepository` records the last filters.
- `features:product-search`:
  - the `ProductSearchViewModel` refinement, base panel and no-match handling;
  - `ProductSearchPhase.Results.noFilterMatch` / `missingCategory`;
  - `ProductSearchContent` copy;
  - strings and previews.
- No new module, library, manifest change, or backend change (api-contract §41 is already in `docs/android/api-contract.md`).
- Out of scope:
  - price-range filters from chips (the backend sends price ranges only from the spoken text);
  - showing the 3 unfiltered alternatives of a no-match refinement.
