## 1. Network (`core:network`)

- [x] 1.1 Request and response DTOs:
  - `SearchFiltersDto(brands: List<String>? = null, tags: List<String>? = null)`;
  - `ConversationSearchRequestDto.filters: SearchFiltersDto? = null`;
  - `SearchFilterDto.values: List<String> = emptyList()`;
  - `ConversationSearchResponseDto.alternativesOnly: Boolean = false` and `missingCategory: String? = null`.
- [x] 1.2 `ConversationSearchApiService.search(…, filters: SearchFiltersDto? = null)` puts them in the body.
- [x] 1.3 `MockEngine` tests:
  - with filters the body carries `"filters":{"brands":["Bosch"]}` and the bare text;
  - without filters there is no `filters` key;
  - `values`, `alternativesOnly` and `missingCategory` are parsed.

## 2. Domain (`core:domain`)

- [x] 2.1 `SearchFilter.values: List<String> = options`; `ProductSearchResult.alternativesOnly` / `missingCategory`.
- [x] 2.2 `StructuredSearchFilters(brands, tags)` with `isEmpty` and `from(selected, panel)` (D2). `ProductSearchConstants.BRAND_CRITERION = "Marca"`.
- [x] 2.3 `ProductSearchRepository.search(query, language = "es", filters: StructuredSearchFilters? = null)`; `SearchProductsUseCase(query, language, filters)` passes them.
- [x] 2.4 `ComposeSearchQueryUseCase(base, suggestion)`: drop the filters parameter; update its KDoc and test.
- [x] 2.5 Tests for `StructuredSearchFilters.from`:
  - brand label goes to brands;
  - tag goes as its value;
  - brand matched ignoring case;
  - an option missing from the panel is dropped;
  - empty selection gives `isEmpty`.
  
  Also `SearchProductsUseCaseTest` passes the filters through.

## 3. Data (`core:data`) and fakes

- [x] 3.1 Mappers: `values.ifEmpty { options }`, `alternativesOnly`, `missingCategory`. `StructuredSearchFilters` maps to `SearchFiltersDto` (empty lists become `null`). `ProductSearchRepositoryImpl` passes them.
- [x] 3.2 `FakeProductSearchRepository` records `lastQuery` and `lastFilters`.
- [x] 3.3 Tests:
  - `ProductMappersTest`: values default, `alternativesOnly`;
  - `ProductSearchRepositoryImplTest`: filters reach the service as DTO, none when null.

## 4. Search (`features:product-search`)

- [x] 4.1 `ProductSearchPhase.Results` gains `noFilterMatch: Boolean = false` and `missingCategory: String? = null` (set only with `alternativesOnly`).
- [x] 4.2 `ProductSearchViewModel`:
  - `baseFilters` / `baseSuggestions` kept when a fresh search settles into results;
  - `refineSearch()` sends `composeSearchQuery(baseQuery, selectedSuggestion)` plus `StructuredSearchFilters.from(selectedFilters, baseFilters)`, or `null` when empty;
  - refinements settle with the base panel;
  - a refinement with chips and `noResults` settles into `Results(noFilterMatch = true)`;
  - `performSearch` carries the filters.
- [x] 4.3 `ProductSearchContent`:
  - with `noFilterMatch`, the body shows `ps_filters_no_match` instead of the grid, and the panel stays;
  - with `missingCategory`, the headline is `ps_alternatives_only`.
- [x] 4.4 Strings in `features/product-search/src/main/res/values/strings.xml`: `ps_filters_no_match`, `ps_alternatives_only`.
- [x] 4.5 Previews: no filter match with chips selected, alternatives-only headline (light and dark).
- [x] 4.6 `ProductSearchViewModelTest`:
  - `filterToggled_sendsOriginalTextAndBrands`;
  - `tagChip_sendsValueNotLabel`;
  - `suggestion_staysInTextWithFilters`;
  - `clearingAllChips_sendsBaseQueryWithoutFilters`;
  - `refinement_keepsBasePanel`;
  - `twoChips_noResults_keepsChipsAndPanel`;
  - `freshSearch_noResults_stillUnhappy`;
  - `alternativesOnly_setsMissingCategory`;
  - `back_restoresPanelAndChips`.
  
  Update the existing tests that expected concatenated text for filters.

## 5. Build and verify

- [x] 5.1 Run `testDebugUnitTest` (or `test` for `core:domain`) for `:core:network`, `:core:domain`, `:core:data` and `:features:product-search`, one at a time, with JDK 21.
- [x] 5.2 `lintDebug` without new findings in touched files; `:app:assembleProfile` builds.
- [x] 5.3 `qa-report.md`:
  - the cases for the QA team (D:BUS-003, D:EXP-004, X:XBU-032, with the expected logcat body; two chips without match; alternatives-only);
  - the table mapping each delta-spec scenario to its test.
  
  Mark the emulator run as handed to the QA team.
