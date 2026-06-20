## 1. Network layer — `core:network`

- [x] 1.1 Add `suggestedFromCategory: String? = null` to `ConversationSearchResponseDto` (`ConversationSearchDtos.kt`); defaulted so older responses stay backward-compatible under `ignoreUnknownKeys`

## 2. Domain layer — `core:domain`

- [x] 2.1 Add `suggestedFromCategory: String? = null` to `ProductSearchResult`
- [x] 2.2 Remove `GeneralizeQueryUseCase` and its unit test; drop any Hilt provider/binding that supplied it

## 3. Data layer — `core:data`

- [x] 3.1 Map `suggestedFromCategory` through `ConversationSearchResponseDto.toDomain()` (`ProductMappers.kt`); keep the `noResults = noResults || products.isEmpty()` safety
- [x] 3.2 Update `ProductMappersTest` to assert `suggestedFromCategory` maps through (non-null and null cases)

## 4. Presentation — `features:product-search`

- [x] 4.1 Remove `ProductSearchPhase.SearchingAlternatives`; add `suggestedFromCategory: String?` to `ProductSearchPhase.Unhappy` (the API `fallbackMessage` is not surfaced, so the phase does not carry it)
- [x] 4.2 In `ProductSearchViewModel`: remove the `generalizeQuery` dependency, `startAlternativeSearch`, and the `SearchingAlternatives` handling; branch `performSearch` on `result.noResults` — `false → settleResults`, `true → settleUnhappy(result.products, result.suggestedFromCategory)`
- [x] 4.3 In `ProductSearchViewModel`: on a search (API) error settle the unhappy screen with an empty suggestion list (`suggestedFromCategory = null`); keep the existing Network-error and idle-timeout handling
- [x] 4.4 Update `settleUnhappy`/`settle` and `HistoryEntry` so the unhappy phase carries `suggestedFromCategory`
- [x] 4.5 Remove the `SearchingAlternatives` branch from `ProductSearchContent`'s phase `when` and from `ProductSearchPhase.processingTextRes()`; ensure the `when` over phases is exhaustive
- [x] 4.6 Update `UnhappyBody` in `ProductSearchContent`: a single apology block — constant `ps_unhappy_message` first line + an invite line that reads "Relacionado a '&lt;categoría&gt;' te podría interesar…" (category verbatim) when `suggestedFromCategory` is non-null, else the generic "De pronto te interesaría…"; render the suggested products beneath (no subtitle, no "Te puede interesar" lead-in)
- [x] 4.7 Strings: remove `ps_searching_alternatives`; set `ps_unhappy_message` to the apology line only; add `ps_unhappy_invite` (generic) and `ps_unhappy_invite_category` (`%1$s` = category, verbatim)
- [x] 4.8 Update the affected Compose `@Preview`s (unhappy with `suggestedFromCategory`, and the bare apology-only case; remove the searching-alternatives preview)

## 5. Tests

- [x] 5.1 `ProductSearchViewModelTest` — `noResults=false` settles `Results`; `noResults=true` settles `Unhappy` with the response's products and only **one** search request is issued; a search (API) error settles `Unhappy` with no products; a connectivity error settles `NetworkError`
- [x] 5.2 `ProductSearchViewModelTest` — `suggestedFromCategory` is carried into the `Unhappy` phase (non-null and null cases)
- [x] 5.3 Update `FakeProductSearchRepository`/`ProductFixtures` to return unhappy results (`noResults=true` with up to 3 products and a `suggestedFromCategory`); remove fixtures/assumptions tied to the generalized alternative search
- [x] 5.4 Remove `GeneralizeQueryUseCaseTest` and any test references to `SearchingAlternatives`

## 6. Definition of Done

- [x] 6.1 `./gradlew :features:product-search:test :core:domain:test :core:data:test :core:network:test` pass
- [x] 6.2 `./gradlew :features:product-search:lintDebug` clean for the touched modules (no `detekt` task is configured in this project)
- [ ] 6.3 Manual check on a landscape kiosk preview: a no-match query shows the unhappy screen with the response's up-to-3 suggestions and the personalized lead-in; a matched query shows the results screen; only one request is issued per search
