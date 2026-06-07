## 1. Domain layer (`core:domain`)

- [x] 1.1 Add `ProductDetail` domain model (`id`, `name`, `description`, `price`, `availability`, `categoryName`, `images: List<ProductImage>`) under `core/domain/.../model/`
- [x] 1.2 Add `ProductImage` domain model (`url`, `position`, `isPrimary`) under `core/domain/.../model/`
- [x] 1.3 Add `DetailErrorCategory` (`Network`, `NotFound`, `DetailApi`) and `DetailException(category)` mirroring the search error model
- [x] 1.4 Add `ProductDetailRepository` port (`suspend fun getProduct(id: String): Result<ProductDetail>`) under `core/domain/.../repository/`
- [x] 1.5 Add `GetProductDetailUseCase` (constructor-injected, `operator fun invoke(id)`) under `core/domain/.../usecase/`
- [x] 1.6 Add `ProductDetailConstants` if any budgets/limits are needed (e.g. request timeout), matching the `ProductSearchConstants` style
- [x] 1.7 Unit-test `GetProductDetailUseCase` against a fake repository

## 2. Network layer (`core:network`)

- [x] 2.1 Add `ProductDetailResponseDto` + `MediaDto` + `DiscountDto` (`@Serializable`, only rendered fields, `ignoreUnknownKeys`) under `core/network/.../dto/`
- [x] 2.2 Add `ProductApiService.getProduct(accessToken, id): NetworkResult<ProductDetailResponseDto>` calling `GET {QHATHIA_BASE_URL}/api/v1/products/{id}` with the Bearer header, returning via `toNetworkResult()`
- [x] 2.3 Register `ProductApiService` in the `core:network` Hilt `NetworkModule`

## 3. Data layer (`core:data`)

- [x] 3.1 Add `ProductDetailMappers.kt` mapping `ProductDetailResponseDto` → `ProductDetail` (sort `media[]` by `primary` then `position`; map to `ProductImage`)
- [x] 3.2 Add `DetailErrorMapper` translating token/network failures and HTTP codes (`404` → `NotFound`) into `DetailErrorCategory`, mirroring `SearchErrorMapper`
- [x] 3.3 Add `ProductDetailRepositoryImpl` (reuse `SessionTokenProvider`; 401 → clear + refresh + retry once; map `NetworkResult` → `Result<ProductDetail>`)
- [x] 3.4 Add a Hilt module binding `ProductDetailRepository` → `ProductDetailRepositoryImpl` (with `ksp(libs.kotlin.metadata.jvm)` present in the module)
- [x] 3.5 Unit-test `ProductDetailRepositoryImpl` (success, 404→NotFound, network error, 401-retry) using fakes

## 4. Shared UI (`core:ui`)

- [x] 4.1 Add `ProductImageGallery` composable (main image + lateral thumbnails; fill empty slots with neutral gray squares when fewer than two photos; constrain Coil image sizes) with light + dark `@Preview`s
- [x] 4.2 Add an optional `onCardClick: (() -> Unit)? = null` to `ProductCard` so the whole card is tappable while keeping the cart button's separate tap; keep existing call sites source-compatible
- [x] 4.3 Update/extend `ProductCard` previews to cover the clickable variant

## 5. Shared testing (`core:testing`)

- [x] 5.1 Add a `ProductDetailFixtures` (or extend `ProductFixtures`) with a `productDetail(...)` factory covering 0/1/2+ images
- [x] 5.2 Add `FakeProductDetailRepository` for use-case/ViewModel tests

## 6. Create the feature module (`features:product-detail`)

- [x] 6.1 Create the module directory `features/product-detail/src/main/java/com/qhathia/mobile/discovery/feature/productdetail/`
- [x] 6.2 Add `features/product-detail/build.gradle.kts` from the Feature module template (compose + hilt + ksp; depend on `core:domain`, `core:ui`, `core:common`; `ksp(libs.kotlin.metadata.jvm)`)
- [x] 6.3 Register `include(":features:product-detail")` in `settings.gradle.kts`
- [x] 6.4 Add the module `AndroidManifest.xml` (`<manifest />`)

## 7. Feature presentation (MVI)

- [x] 7.1 Add `ProductDetailUiState` (loading / loaded `ProductDetail` / not-found / network-error; `showCloseConfirmDialog`) under `model/`
- [x] 7.2 Add `ProductDetailUiEvent` (`Retry`, `Back`, `NewSearch`, `ConfirmCloseSearch`, `DismissDialog`, `AddToCart`, `ShareWhatsApp`, `ActivateMic`, related-product tap) under `model/`
- [x] 7.3 Add `ProductDetailUiEffect` (`NavigateBack`, `NavigateHomeIdle`) under `model/`
- [x] 7.4 Add `ProductDetailViewModel` (`@HiltViewModel`): read `productId` from `SavedStateHandle`, fetch via `GetProductDetailUseCase` on init, expose `StateFlow`/`SharedFlow`, handle the confirm-close dialog and Volver/Nueva búsqueda effects
- [x] 7.5 Add the mock related-products provider in-feature, clearly commented as a placeholder to replace with the related-products API
- [x] 7.6 Add detail copy strings (`strings.xml`) — title, description label, add-to-cart, share via WhatsApp, related-products headline, not-found, content descriptions, dialog text

## 8. Feature UI

- [x] 8.1 Add `ProductDetailContent` (stateless): top bar (Volver / Nueva búsqueda / cart), `ProductImageGallery`, name/price/availability/description & characteristics, Add-to-cart + Share-via-WhatsApp buttons (render-only), mock related-products row (reusing `ProductCard`), bottom `MicButton` animation, and the close-confirmation `AlertDialog`
- [x] 8.2 Add loading, not-found, and network-error bodies in `ProductDetailContent`
- [x] 8.3 Add `ProductDetailScreen` (stateful): bind the ViewModel, `collectAsStateWithLifecycle`, forward `NavigateBack` / `NavigateHomeIdle` effects
- [x] 8.4 Add light + dark `@Preview`s for loaded (2+ photos), single-photo (gray squares), loading, not-found, and network-error states
- [x] 8.5 Unit-test `ProductDetailViewModel` (init load success/404/network, retry, confirm-close emits NavigateHomeIdle, Back emits NavigateBack)

## 9. Navigation wiring

- [x] 9.1 Add `ProductDetailNavigation.kt` in the feature: `ARG_PRODUCT_ID`, `PRODUCT_DETAIL_ROUTE`, `productDetailRoute(productId)`, and `NavGraphBuilder.productDetailScreen(onNavigateBack, onNavigateHomeIdle)`
- [x] 9.2 In `features:product-search`, add `ProductSearchUiEffect.NavigateToDetail(productId)` and emit it from a new card-tap event (results + unhappy); wire `onCardClick` on `ProductCard` in `ProductSearchContent`; surface the effect in `ProductSearchScreen` via a new `onNavigateToDetail` callback
- [x] 9.3 In `:app/MainActivity`, register `productDetailScreen(...)`, wire product-search `onNavigateToDetail` → `navController.navigate(productDetailRoute(id))`, detail `onNavigateBack` → `popBackStack`, detail `onNavigateHomeIdle` → `popBackStack(HOME_ROUTE, inclusive = false)`
- [x] 9.4 Add `:features:product-detail` to `:app` dependencies in `app/build.gradle.kts`

## 10. Definition of Done

- [x] 10.1 Confirm no new manifest permissions are needed (`INTERNET` / `ACCESS_NETWORK_STATE` already declared); manifest audit complete
- [x] 10.2 `./gradlew test` passes (domain, data, feature unit tests)
- [x] 10.3 `./gradlew lint` passes
- [x] 10.4 Light + dark previews verified for the detail screen states; all interactive controls expose content descriptions and ≥48dp touch targets
