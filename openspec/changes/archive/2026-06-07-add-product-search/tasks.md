## 1. core:domain (search ports, models, constants)

- [x] 1.1 Add a framework-free `Product` domain model (`id`, `name`, `description`, `price: BigDecimal`/`Double`, `availability`, `categoryName`, `imageUrl`) under `core/domain/.../model`
- [x] 1.2 Add `ProductSearchResult` (`products: List<Product>`, `displayText`, `understood`, `noResults`, `fallbackMessage`) and a `SearchSource` enum (`Voice`, `Text`)
- [x] 1.3 Add a `SearchErrorCategory` (or reuse a generic) distinguishing **Network** (keep-screen + connection message) from **SearchApi/Generic** (unhappy + alternatives); add a `SearchException(category)` if needed
- [x] 1.4 Add `ProductSearchRepository` port: `suspend fun search(query: String, language: String = "es"): Result<ProductSearchResult>`
- [x] 1.5 Add `SearchProductsUseCase` — enforces the ≤500-char cap and calls the repository
- [x] 1.6 Add `GeneralizeQueryUseCase` (or pure function) — deterministic keyword generalization (strip trailing numeric/spec qualifiers e.g. "20v"; reduce to head noun). Unit-testable in isolation
- [x] 1.7 Add `ProductSearchConstants`: `SEARCH_TIMEOUT_SECONDS = 30`, `SLOW_SEARCH_SECONDS = 20`, `UNHAPPY_IDLE_TIMEOUT_SECONDS = 30`, `MAX_QUERY_CHARS = 500`, `ALTERNATIVES_COUNT = 3`, `MAX_RESULT_ROWS = 2`
- [x] 1.8 Unit-test `SearchProductsUseCase` (cap enforcement, success, failure passthrough) and `GeneralizeQueryUseCase` (e.g. "Taladro 20v" → "Taladros") with fakes/virtual time

## 2. core:network (search API service + DTOs)

- [x] 2.1 Add `@Serializable` request DTO for `POST /api/v1/conversations/search` (`transcribedText`, `sessionId?`, `language?`)
- [x] 2.2 Add `@Serializable` response DTOs (`understood`, `results[]` with `id`/`name`/`description`/`price`/`availability`/`categoryName`/`primaryImageUrl`/`relevanceScore`, `noResults`, `displayText`, `fallbackMessage`, `interpretedQuery` optional) with `ignoreUnknownKeys`
- [x] 2.3 Add `ConversationSearchApiService` calling `POST ${QHATHIA_BASE_URL}/api/v1/conversations/search` with `Authorization: Bearer <accessToken>`, applying a **per-request 30s `HttpTimeout`** override, returning `NetworkResult<...>`
- [x] 2.4 Verify `core:network` builds in isolation (`gradlew :core:network:assembleDebug`)

## 3. core:data (search repository implementation)

- [x] 3.1 Add DTO→domain mappers (`SearchResultDto.toDomain()` → `Product`; response → `ProductSearchResult`)
- [x] 3.2 Implement `ProductSearchRepositoryImpl`: obtain the JWT via the existing `SessionTokenProvider.accessToken()`, call `ConversationSearchApiService`, map `NetworkResult` → `Result<ProductSearchResult>` and classify Network vs SearchApi errors; on 401 `clear()` + retry once
- [x] 3.3 Add debug-only logging of the query + outcome (result count or error category), guarded by `BuildConfig.DEBUG`; never log the token
- [x] 3.4 Add a Hilt module (`ProductSearchDataModule`) `@Binds` `ProductSearchRepository`
- [x] 3.5 Unit-test `ProductSearchRepositoryImpl` (success mapping, noResults, network error → Network category, HTTP error → SearchApi category) with MockK'd service

## 4. core:testing (fakes & fixtures)

- [x] 4.1 Add `FakeProductSearchRepository` (programmable result/error) and `ProductFixtures` (sample `Product`/`ProductSearchResult`, incl. a no-results case) to `core:testing`

## 5. core:ui (product card + results grid)

- [x] 5.1 Add a `ProductCard` composable (Coil image with size constraints, name, `S/ {price}` price, cart icon button) using design tokens; light + dark `@Preview`
- [x] 5.2 Add a horizontal **2-row** results grid composable (and confirm a 3-up row layout for alternatives) with stable `key`s; light + dark `@Preview`
- [x] 5.3 Confirm `ProcessingMessage`, `MicButton`/`VoiceWaveform`, and `DiscoverySearchBar` are reused as-is (no duplication)

## 6. features:product-search module (new)

- [x] 6.1 Create the module directory `features/product-search/src/main/java/com/qhathia/mobile/discovery/feature/productsearch` (+ `src/test/...`)
- [x] 6.2 Write `features/product-search/build.gradle.kts` from the Feature template (compose + ksp + hilt; deps on `core:domain`, `core:ui`, `core:common`; `testImplementation(project(":core:testing"))`)
- [x] 6.3 Register `include(":features:product-search")` in `settings.gradle.kts`
- [x] 6.4 Create `features/product-search/src/main/AndroidManifest.xml` (`<manifest />`)
- [x] 6.5 Define MVI contract: `ProductSearchUiState` (source, query, products, displayText, history depth, dialog visibility, a `ProductSearchPhase` for searching/slow-search/searching-alternatives/results/unhappy/network-error), `ProductSearchUiEvent` (Back, NewSearch, ConfirmNewSearch, DismissDialog, QueryChanged, SubmitSearch, ActivateMic, CartTapped, …), `ProductSearchUiEffect` (NavigateHomeIdle, …)
- [x] 6.6 Implement `ProductSearchViewModel` state machine: on entry run `SearchProductsUseCase`; show `Searching`; `SlowSearch` after 20s; on success → results (push to in-memory history); on no-results/SearchApi error → `SearchingAlternatives` (generalized query, first 3) → unhappy screen; on Network error → keep screen + connection message; 30s idle on unhappy → `NavigateHomeIdle`; mic animation retained only when `source == Voice`
- [x] 6.7 Implement Volver (pop history; empty → open close-confirm dialog) and Nueva búsqueda (open confirm dialog → on confirm clear history + `NavigateHomeIdle`); cart taps are no-ops
- [x] 6.8 Add the processing/error string resources + a `ProductSearchPhase`→copy resolver (mirrors `features:home` `SttErrorMessages`): *Revisando catálogo…*, *Buscando minuciosamente…*, *Buscando sugerencias…*, the unhappy apology copy, and the network-error title/message
- [x] 6.9 Build `ProductSearchScreen` (stateful) + `ProductSearchContent` (stateless): top bar (Volver / Nueva búsqueda / Ir al carrito), results headline, 2-row horizontal grid, bottom search bar + mic, unhappy variant with 3 alternatives, and the confirmation dialog. **Exclude** the Figma filters/suggestions sidebar (out of scope)
- [x] 6.10 Add the navigation graph (`productSearchScreen(...)` + route carrying `query` and `source`) with hoisted callbacks
- [x] 6.11 Light + dark `@Preview`s for: searching, slow-search, searching-alternatives, results, unhappy (3 alternatives), network-error, and the confirmation dialog
- [x] 6.12 Unit-test `ProductSearchViewModel` with Turbine + fakes: searching→results; slow-search after 20s; no-results→alternatives→unhappy; SearchApi error→alternatives; Network error keeps screen; STT-origin does NOT trigger alternatives on its own; Volver pops / empty opens dialog; Nueva búsqueda clears + navigates; 30s idle → home idle

## 7. features:home (route query into product search)

- [x] 7.1 Update `HomeViewModel`: on `SubmitSearch` and on a successful `VoiceTranscribed` transcript, emit a navigation effect carrying the query + source (replace the no-op `VoiceTranscribed` handling and the undestined `NavigateToResults`)
- [x] 7.2 Update `HomeUiEffect` / `HomeNavigation` callbacks so the transcript and typed query both route to product-search with the source flag
- [x] 7.3 Update affected `HomeViewModel` tests for the new routing behavior

## 8. Wire the app shell (:app)

- [x] 8.1 Add `implementation(project(":features:product-search"))` to `app/build.gradle.kts`
- [x] 8.2 Register `productSearchScreen(...)` in the app NavHost and wire Home → ProductSearch (pass query + source); wire ProductSearch `NavigateHomeIdle` back to Home idle
- [x] 8.3 Manifest audit: no new permissions (INTERNET / ACCESS_NETWORK_STATE already declared); confirm no new components needed

## 9. Tests & Definition of Done

- [x] 9.1 `./gradlew test` green across all modules (use case + generalization, repository mapping, ViewModel state machine, updated Home tests)
- [x] 9.2 `./gradlew lintDebug` passes with no errors
- [x] 9.3 Light + dark previews present for `core:ui` `ProductCard`/grid and the product-search states (searching / slow-search / alternatives / results / unhappy / network-error / dialog)
- [x] 9.4 Budgets/behaviors verified on virtual time: 30s search timeout (per-request Ktor override), 20s slow-search, 30s unhappy idle auto-return; alternative search excluded for STT errors (only runs on no-results/SearchApi); query capped at 500 chars
- [ ] 9.5 Reconcile the max-5-results vs "no limit" discrepancy with the backend (Open Question) — confirm whether client pagination is needed before release. **Pending external backend coordination** — client renders whatever the API returns (no client-imposed cap); flagged in design.md Open Questions.
