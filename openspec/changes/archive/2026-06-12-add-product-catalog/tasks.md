## 1. Domain layer (`core:domain`)

- [x] 1.1 Add `model/CatalogCategory.kt` — `CatalogCategory(id: String, name: String, products: List<Product>)`, reusing the existing `Product` model for items.
- [x] 1.2 Add `repository/ProductCatalogRepository.kt` — port `suspend fun getCatalog(): Result<List<CatalogCategory>>`; failures carry a domain exception tagged with an error category (reuse/extend the existing search error category or add a small `CatalogException` mirroring `SearchException`). _Added `CatalogErrorCategory` + `CatalogException`._
- [x] 1.3 Add `usecase/GetProductCatalogUseCase.kt` — `operator fun invoke(): Result<List<CatalogCategory>>` delegating to the repository.
- [x] 1.4 Add `CatalogConstants.kt` — `ERROR_IDLE_TIMEOUT_SECONDS = 30`, `MAX_QUERY_CHARS` (reuses `ProductSearchConstants.MAX_QUERY_CHARS`), `MAX_CATEGORY_ROWS = 2`, and `isDiscountCandidate` (the Descuentos predicate, documented).
- [x] 1.5 Surfaced `allowDiscountInquiry` on the domain `Product` (added with default `= false` so existing call sites and mappers keep compiling); the Descuentos section is derived in the ViewModel via `CatalogConstants.isDiscountCandidate`.

## 2. Network layer (`core:network`)

- [x] 2.1 Add `dto/CatalogDtos.kt` — `@Serializable CatalogCategoryDto(categoryId, categoryName, products)` and `CatalogProductDto(id, categoryId, categoryName, name, price, availability, allowDiscountInquiry, primaryImageUrl?, brand?)` with defaults for resilient deserialization.
- [x] 2.2 Add `service/CatalogApiService.kt` — `suspend fun getCatalog(accessToken: String): NetworkResult<List<CatalogCategoryDto>>` calling `GET ${QHATHIA_BASE_URL}/api/v1/catalog/by-category` with `Authorization: Bearer`, wrapped in `runCatching { ... }.toNetworkResult()` (pattern from `ProductApiService`).

## 3. Data layer (`core:data`)

- [x] 3.1 Add `catalog/CatalogMappers.kt` — `CatalogCategoryDto.toDomain()` / `CatalogProductDto.toDomain()` mapping to `CatalogCategory` / `Product` (carry `allowDiscountInquiry`).
- [x] 3.2 Add `catalog/CatalogErrorMapper.kt` — map `NetworkResult.NetworkError`/`HttpError`/token failures to the domain error category (reuses `SearchErrorMapper` shape).
- [x] 3.3 Add `catalog/ProductCatalogRepositoryImpl.kt` — obtain the JWT via the shared `SessionTokenProvider`, call `CatalogApiService`, retry once on `401` (clear + force refresh), map success/failure to `Result<List<CatalogCategory>>` (pattern from `ProductSearchRepositoryImpl`).
- [x] 3.4 Add `di/ProductCatalogDataModule.kt` — `@Module @InstallIn(SingletonComponent::class)` `@Binds ProductCatalogRepositoryImpl → ProductCatalogRepository`.

## 4. Create the feature module (`features:catalog`)

- [x] 4.1 Create the module directory `features/catalog/src/main/java/com/qhathia/mobile/discovery/feature/catalog` (and `src/test/java/...`).
- [x] 4.2 Add `features/catalog/build.gradle.kts` using the Feature module template (android.library + kotlin.compose + ksp + hilt), namespace `com.qhathia.mobile.discovery.feature.catalog`, depending on `core:domain`, `core:ui`, `core:common`, Compose BOM, lifecycle, navigation, hilt + hilt-navigation-compose, and the JUnit5/MockK/Turbine test deps.
- [x] 4.3 Register the module in root `settings.gradle.kts` with `include(":features:catalog")`.
- [x] 4.4 Add `features/catalog/src/main/AndroidManifest.xml` (minimal `<manifest />`).
- [x] 4.5 Add `features/catalog/src/main/res/values/strings.xml` — catalog labels, Descuentos heading, empty message "Productos sin descuentos aún.", store-empty state, and the network-error title/message copy.

## 5. Feature MVI & UI (`features:catalog`)

- [x] 5.1 Add `model/CatalogSection.kt` — a sealed UI section model (`Category` / always-present `Discounts`) wrapping a heading + products.
- [x] 5.2 Add `model/CatalogUiState.kt` — sections, `isLoading`, `showMicAnimation`, `isNetworkError`, `isStoreEmpty`, `cartItemCount`, `showCloseConfirmDialog`, `source`, `query`.
- [x] 5.3 Add `model/CatalogUiEvent.kt` — `Retry`, `ProductTapped(id)`, `CartTapped(id)`, `OpenCart`, `Back`, `NewSearch`, `ConfirmCloseSearch`, `DismissDialog`, `QueryChanged(text)`, `SubmitSearch`, `ActivateMic`.
- [x] 5.4 Add `model/CatalogUiEffect.kt` — `NavigateToDetail(id)`, `OpenCart`, `NavigateHomeIdle`, `NavigateToSearch(query, source)`.
- [x] 5.5 Add `ui/CatalogViewModel.kt` (`@HiltViewModel`) — load catalog on init via `GetProductCatalogUseCase`, build sections + derive Descuentos, observe cart via `ObserveCartUseCase`, add via `AddToCartUseCase`, Volver (cart-aware close / Home), Nueva búsqueda confirmation, search hand-off effects, and the 30s idle timer on the network-error state; cap query at 500 chars; set `showMicAnimation` from `source`.
- [x] 5.6 Add `ui/CatalogContent.kt` — stateless composable: vertical `LazyColumn` of category sections, each a 2-row horizontal `HorizontalRowsGrid` of product cards with stable keys; Descuentos section with empty message; reuses `core:ui` product card + cart button; top bar (Volver/Nueva búsqueda/Ir al carrito with count) and bottom search bar with mic.
- [x] 5.7 Network-error ("Perdí la conexión…") and store-empty states implemented as bodies inside `CatalogContent` (single stateless surface, mirroring product-search).
- [x] 5.8 Add `ui/CatalogScreen.kt` — stateful composable: collect `uiState` with `collectAsStateWithLifecycle()`, consume `uiEffect`, hoist navigation callbacks.
- [x] 5.9 Add `@Preview` (light + dark) for `CatalogContent` (populated, empty-discounts, store-empty, network-error, close-dialog).
- [x] 5.10 Add `navigation/CatalogNavigation.kt` — `CATALOG_ROUTE` with optional `source` arg, `catalogRoute(source)` builder, and `NavGraphBuilder.catalogScreen(onNavigateHomeIdle, onNavigateToDetail, onNavigateToSearch, onOpenCart)`.

## 6. App wiring (`:app`)

- [x] 6.1 Add `implementation(project(":features:catalog"))` to `app/build.gradle.kts`.
- [x] 6.2 In `MainActivity`, replaced the `catalog` placeholder with `catalogScreen(...)`, wiring: home-idle → `popBackStack(HOME_ROUTE, ...)`, detail → `productDetailRoute(id)`, search → `productSearchRoute(query, source)`, cart → `showCart = true`. Updated `onNavigateToCatalog` to use `catalogRoute()` and removed the unused placeholder (+ its now-dead imports).
- [x] 6.3 Confirmed `INTERNET` + `ACCESS_NETWORK_STATE` already in `app/src/main/AndroidManifest.xml`; catalog adds no new permission/component, so no manifest change needed.

## 7. Tests

- [x] 7.1 `CatalogViewModelTest` (JUnit5 + fakes + Turbine + `MainDispatcherExtension`): load builds sections + Descuentos; empty discounts → empty section; no categories → store-empty; network failure → error state + 30s idle emits `NavigateHomeIdle`; Volver branches (cart-empty→Home / cart-with-items→dialog); Nueva búsqueda confirm → `NavigateHomeIdle`; cart add updates count; query cap at 500; submit/mic emit `NavigateToSearch` with the right source; mic-animation from source. _Added `CatalogFixtures` + `FakeProductCatalogRepository` to `core:testing`._
- [x] 7.2 Data-layer test `ProductCatalogRepositoryImplTest` — success mapping, `401` refresh-and-retry, connectivity/timeout/HTTP error mapping (MockK on the token provider + API service, mirroring `ProductSearchRepositoryImplTest`).
- [ ] 7.3 (Optional — deferred) Compose UI test for `CatalogContent`. Skipped per its optional marker; ViewModel + repository tests cover the behavior. Previews validate the layout.

## 8. Definition of Done

- [x] 8.1 `./gradlew :features:catalog:testDebugUnitTest :core:data:testDebugUnitTest` — **BUILD SUCCESSFUL**, all unit tests pass (run with the Android Studio JBR / JDK 21).
- [x] 8.2 `./gradlew :features:catalog:lintDebug` passes; `:app:compileDebugKotlin` (incl. Hilt KSP) succeeds; screen has light + dark `@Preview`s.
- [x] 8.3 Manual smoke — verified on device (2026-06-12, all OK): Home → catalog shows the tab bar + grid; product tap opens detail; cart add + Ir al carrito; Nueva búsqueda/Volver dialogs; search bar hands off to product-search; network-error screen + return to Home idle.
