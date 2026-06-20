## 1. Network layer — `core:network`

- [x] 1.1 Add `SearchFilterDto(criterion: String = "", options: List<String> = emptyList())` (`@Serializable`) in `ConversationSearchDtos.kt`
- [x] 1.2 Add `filters: List<SearchFilterDto> = emptyList()` and `suggestions: List<String> = emptyList()` to `ConversationSearchResponseDto` (defaults keep backward compatibility under `ignoreUnknownKeys`)

## 2. Domain layer — `core:domain`

- [x] 2.1 Add `SearchFilter(criterion: String, options: List<String>)` domain model in `core.domain.model`
- [x] 2.2 Add `filters: List<SearchFilter> = emptyList()` and `suggestions: List<String> = emptyList()` to `ProductSearchResult` (defaulted so existing construction sites compile)
- [x] 2.3 Create `ComposeSearchQueryUseCase` (pure Kotlin) that takes the base query, selected filter terms (`criterion` + `option`), and optional selected suggestion, and returns the natural-language query string (filter term as `"<criterion> <option>"` lowercased; parts joined by single spaces onto the trimmed base; base returned unchanged when no terms)

## 3. Data layer — `core:data`

- [x] 3.1 Map `SearchFilterDto → SearchFilter` and pass `filters`/`suggestions` through in `ConversationSearchResponseDto.toDomain()` (`ProductMappers.kt`); empty on the unhappy path is already satisfied by defaults

## 4. Presentation — `features:product-search`

- [x] 4.1 Extend `ProductSearchPhase.Results` to carry `filters: ImmutableList<SearchFilter>` and `suggestions: ImmutableList<String>`
- [x] 4.2 Add `FilterToggled(criterion: String, option: String)` and `SuggestionSelected(phrase: String)` to `ProductSearchUiEvent` (re-tapping the active suggestion de-selects it)
- [x] 4.3 Expose the criteria/options/suggestions and the current selection (selected filter option set + selected suggestion) on `ProductSearchUiState` so the UI can render selected states
- [x] 4.4 In `ProductSearchViewModel`: hold the immutable base raw query for the current results view and the mutable selection; on `FilterToggled`/`SuggestionSelected`, update the selection (N filters; at most one suggestion, replacing the previous), compose the query via `ComposeSearchQueryUseCase`, and drive the existing `performSearch(composed, currentSource)`
- [x] 4.5 Set/refresh the base query when a non-filter/suggestion search settles into `Results`, and reset the selection on `Nueva búsqueda` (`ConfirmCloseSearch`) and when showing a history entry
- [x] 4.6 Carry `filters`/`suggestions` into `settleResults` when building `ProductSearchPhase.Results`
- [x] 4.7 Render the filters & suggestions section in `ProductSearchContent` on the `Results` phase (default + selected states per Figma frames 722-13413 / 722-13564); hide it when both lists are empty
- [x] 4.8 Add the required string resources (section/criteria labels, content descriptions) to `features:product-search` strings
- [x] 4.9 Add Compose `@Preview`s for the results screen with filters & suggestions (default and with a selected filter + selected suggestion), light and dark

## 5. Tests

- [x] 5.1 `ComposeSearchQueryUseCaseTest` — base only; one filter; multiple filters; suggestion only; filters + suggestion; de-selecting all returns the base
- [x] 5.2 `ProductMappers`/repository test — `filters` and `suggestions` are mapped from the DTO; both empty on the unhappy path
- [x] 5.3 `ProductSearchViewModelTest` — selecting a suggestion re-queries with it appended to the base; selecting N filters re-queries with all appended; selecting a second suggestion replaces the first; de-selecting all re-queries the base; an empty re-query result follows the unhappy path; `Nueva búsqueda` resets the selection
- [x] 5.4 Update `FakeProductSearchRepository`/fixtures to return sample `filters`/`suggestions`

## 6. Definition of Done

- [x] 6.1 `./gradlew :features:product-search:test :core:domain:test :core:data:test` pass
- [x] 6.2 `./gradlew detekt lint` clean for the touched modules
- [x] 6.3 Manual check on a landscape kiosk preview: section renders, selecting filters/suggestion re-queries and repaints, selected state matches Figma
