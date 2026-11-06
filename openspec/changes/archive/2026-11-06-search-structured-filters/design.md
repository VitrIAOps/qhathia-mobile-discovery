## Context

`ProductSearchViewModel` keeps `baseQuery` (the raw text that opened the results view), the chips the customer selected (`selectedFilters`, any number of them), and one `selectedSuggestion`.

`refineSearch()` calls `composeSearchQuery(baseQuery, selectedFilters, selectedSuggestion)`, which builds `"taladro marca bosch para uso doméstico"`, and sends it as `transcribedText`. `settleResults` replaces the panel with the response's `filters` and `suggestions`. A refinement with `noResults` goes to `settleUnhappy`, the "De pronto te interesaría…" screen, where the chips disappear. Each settled view is stored as a `HistoryEntry`, with its phase and its selection, for "Volver".

Backend phase 41 (api-contract §41):

```json
POST /api/v1/conversations/search
{ "transcribedText": "taladro", "language": "es",
  "filters": { "brands": ["Bosch"], "tags": ["inalambrico"] } }
```

- **Filter rules:** brands in OR; tags in OR within a criterion and AND between criteria.
- **Response `filters[]`:** each criterion is `{criterion, options, values}`. For "Marca", `values == options`. For tag groups ("Voltaje", "Tipo"…), `values[i]` is the tag value matching `options[i]`.
- **No match:** when nothing matches, the response is `noResults: true`, with `fallbackMessage` "No hay productos con esos filtros…" and the best 3 results **without** filters.
- **New response fields:** `alternativesOnly` (the requested product is missing and the results are alternatives) and `missingCategory`.
- **Panel:** the backend builds the panel from the filtered results.

Modules involved: `core:network`, `core:domain`, `core:data`, `core:testing`, `features:product-search`. No new module.

## Goals / Non-Goals

**Goals:**
- A chip filters: the request carries the original text and the chip as a structured filter, and only matching products come back.
- Two or more chips never become "no entendí", and the chips are never lost.
- The panel does not shrink while the customer refines.

**Non-Goals:**
- Price chips (`minPrice` / `maxPrice`): the backend derives price only from the spoken text and sends no price criterion.
- Showing the 3 unfiltered results of a no-match refinement. They would read as matches; the message says what to do instead.
- Changing how suggestions refine: they are natural-language phrases and keep being appended to the text.

## Decisions

### D1 — Domain shape

- `SearchFilter(criterion, options, values: List<String> = options)`: the default keeps every existing caller and older responses working.
- `StructuredSearchFilters(brands: List<String> = emptyList(), tags: List<String> = emptyList())` with `isEmpty`.
- `ProductSearchRepository.search(query, language = "es", filters: StructuredSearchFilters? = null)`, with the same parameter on `SearchProductsUseCase`.
- `ProductSearchResult` gains `alternativesOnly: Boolean = false` and `missingCategory: String? = null`.

### D2 — Building the filters from the selection

A pure function `StructuredSearchFilters.from(selected: Collection<SearchFilterOption>, panel: List<SearchFilter>)` lives in `core:domain`:

- an option of the criterion `ProductSearchConstants.BRAND_CRITERION` ("Marca", compared ignoring case) goes into `brands`, as its label;
- any other option goes into `tags`, as `values[options.indexOf(option)]` of its criterion in the panel;
- an option missing from the panel is dropped, never sent as a label.

- *Why "Marca" by name:* it is vo's fixed criterion for brands (`FilterCriterion`), and the only one whose values are not tags.
- *Alternative:* treat a criterion as brands when `values == options`. Rejected: a tag group can also have identical labels and values.

### D3 — The text keeps only the suggestion

`ComposeSearchQueryUseCase(base, suggestion)` appends only the suggestion; the filter parameter is removed. `refineSearch()` sends:

- `composeSearchQuery(baseQuery, selectedSuggestion)` as the text;
- `StructuredSearchFilters.from(selectedFilters, basePanel)` as filters, or `null` when empty.

So clearing every chip sends exactly the base query again.

### D4 — Stable panel

When a fresh search settles into results, the ViewModel keeps `baseFilters` and `baseSuggestions`. A refinement settles into `Results(filters = baseFilters, suggestions = baseSuggestions)`, whatever the response's own panel says. `HistoryEntry` already stores the phase, so "Volver" restores the panel that was shown.

### D5 — No match stays in results

A refinement whose response has `noResults: true` while chips are selected settles into `Results(products = empty, filters = baseFilters, suggestions = baseSuggestions, noFilterMatch = true)`. The chips stay selected, and the body shows `ps_filters_no_match` ("No hay productos con esos filtros. Quita alguno para ver más.") in place of the grid.

A fresh search, or a refinement without chips (only a suggestion), keeps today's unhappy screen.

### D6 — Alternatives-only notice

When `alternativesOnly` is true and `missingCategory` is known, the results headline becomes `ps_alternatives_only` ("No tenemos «%1$s»; te mostramos alternativas."). Otherwise it is today's "N resultados" headline.

### D7 — Network and mapping

- `ConversationSearchRequestDto` gains `filters: SearchFiltersDto? = null`, with `brands` and `tags` as nullable lists. The shared `Json` does not encode defaults, so a search without chips sends no `filters` key.
- `SearchFilterDto` gains `values: List<String> = emptyList()`, mapped as `values.ifEmpty { options }`.
- `ConversationSearchResponseDto` gains `alternativesOnly` and `missingCategory`.
- `ConversationSearchApiService.search(token, query, language, sessionId = null, filters: SearchFiltersDto? = null)`; the repository maps the domain filters to the DTO.

## Risks / Trade-offs

- [The backend renames "Marca"] → Brands would go into `tags` and match nothing; the no-match message would show. The name is part of vo's contract (§41), and the constant is in one place.
- [A tag option whose `values` list is shorter than `options` (an older backend)] → `values` defaults to `options`, so the label is sent. Older backends ignore `filters` anyway.
- [The panel no longer narrows to what the filtered results contain] → Intended (regression §2.2). A combination with no products says so, and a chip can be removed.

## Migration Plan

App-only. Older backends ignore `filters` (unknown field) and return results for the base text. Rollback = revert.

Verified with unit tests. The QA team re-runs D:BUS-003, D:EXP-004 and X:XBU-032.

## Open Questions

- None.
