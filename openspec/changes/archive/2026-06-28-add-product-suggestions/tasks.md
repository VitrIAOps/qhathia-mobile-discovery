## 1. Network layer (`core:network`)

- [x] 1.1 Add `ProductSuggestionsDtos.kt` in `core/network/.../dto/`: `ProductSuggestionsResponseDto(productId, suggestions: List<SuggestedProductDto> = emptyList(), source = "")` and `SuggestedProductDto` mirroring `ProductSummaryResponse` (id, categoryId, categoryName, name, price, availability, allowDiscountInquiry, primaryImageUrl?, brand?) — all `@Serializable` with defaults; rely on `ignoreUnknownKeys` for unmapped fields (`position`, `tags`).
- [x] 1.2 Add `getSuggestions(accessToken: String, id: String): NetworkResult<ProductSuggestionsResponseDto>` to `ProductApiService` calling `GET {QHATHIA_BASE_URL}/api/v1/products/{id}/suggestions` with the Bearer header and the same per-request timeout pattern as `getProduct`.

## 2. Domain layer (`core:domain`)

- [x] 2.1 Add `suspend fun getSuggestions(id: String): Result<List<Product>>` to the `ProductDetailRepository` port.
- [x] 2.2 Create `GetProductSuggestionsUseCase` (`@Inject` constructor; `operator fun invoke(id: String) = repository.getSuggestions(id)`), following the existing use-case style.

## 3. Data layer (`core:data`)

- [x] 3.1 Add a `SuggestedProductDto.toDomain(): Product` mapper (price stays `Double`; `primaryImageUrl.orEmpty()`; `description = ""`) alongside the detail mappers.
- [x] 3.2 Implement `getSuggestions(id)` in `ProductDetailRepositoryImpl`: obtain the token via `SessionTokenProvider`, call `ProductApiService.getSuggestions`, apply the existing 401-clear-refresh-retry-once pattern, map `NetworkResult` → `Result<List<Product>>` (cap at 5), and translate failures to `DetailException`/`DetailErrorCategory` via `DetailErrorMapper` (no `NotFound` special-casing needed for the strip).

## 4. Test fixtures (`core:testing`)

- [x] 4.1 Add suggestion fixtures (a `List<Product>` sample) and extend any fake `ProductDetailRepository` to back `getSuggestions` (success/empty/failure).

## 5. Feature presentation (`features:product-detail`)

- [x] 5.1 Add `ProductDetailUiEffect.NavigateToDetail(productId: String)`.
- [x] 5.2 Replace `relatedProducts: ImmutableList<Product>` inside `Status.Loaded` with an independent top-level `relatedProducts: RelatedProducts` field on `ProductDetailUiState`, modeled as a sealed sub-state: `Loading`, `Loaded(items: ImmutableList<Product>)`, `Empty`.
- [x] 5.3 In `ProductDetailViewModel`: inject `GetProductSuggestionsUseCase`; on init (independent of `load()`) fetch suggestions and update the `relatedProducts` sub-state — non-empty success → `Loaded`, empty success OR failure → `Empty`; change `RelatedProductTapped` to emit `NavigateToDetail(productId)`.
- [x] 5.4 Delete `features/product-detail/.../data/RelatedProductsMock.kt` and remove its usage/imports.
- [x] 5.5 Update `ProductDetailContent.RelatedProductsSection` to drive off `RelatedProducts`: render `Loaded` as a **horizontally scrollable** strip of `ProductRowCard` (max 5, stable `key`), and render `Empty` as a centered message inside a box sized like the products container. Remove the placeholder comment.
- [x] 5.6 Add the empty-state message string to `features/product-detail/src/main/res/values/strings.xml` (e.g. `pd_related_empty`).
- [x] 5.7 Update `productDetailScreen(...)` (navigation) and `ProductDetailScreen` to expose/handle an `onNavigateToDetail: (String) -> Unit` callback mapped from the `NavigateToDetail` effect.
- [x] 5.8 Add an add-to-cart button to `ProductRowCard` (`core:ui`, `cartContentDescription` + `onAddToCart` params, dark circular cart button mirroring `ProductCard`); add a `RelatedProductAddToCart(productId)` event wired in `ProductDetailContent`; handle it in `ProductDetailViewModel` by adding the related `Product` to the cart (quantity 1) via `AddToCartUseCase`.

## 6. App wiring (`:app`)

- [x] 6.1 In `MainActivity`, pass `onNavigateToDetail = { productId -> navController.navigate(productDetailRoute(productId)) }` to the `productDetailScreen(...)` call (mirroring the catalog/search wiring).

## 7. Validation & Definition of Done

- [x] 7.1 Update/extend `ProductDetailViewModelTest` for the suggestions sub-state (loaded / empty / failure → Empty) and `RelatedProductTapped` → `NavigateToDetail`; add a mapper test for `SuggestedProductDto.toDomain()`.
- [x] 7.2 Ensure light + dark `@Preview`s cover the related-products `Loaded` and `Empty` states.
- [x] 7.3 Manifest audit: confirm no new permissions/components are required (`INTERNET` / `ACCESS_NETWORK_STATE` already declared).
- [x] 7.4 Run `./gradlew test` and `./gradlew lint`; confirm both pass.
