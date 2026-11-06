## 1. Network (`core:network`)

- [x] 1.1 Add `stock: Int? = null` to `CatalogProductDto`, `SearchProductDto`, `SuggestedProductDto` and `ProductDetailResponseDto`.
- [x] 1.2 Add `expectedUnitPrice: Double? = null` to `OrderItemRequestDto` (omitted from the JSON when `null`, as the other optional fields).
- [x] 1.3 `ProductApiService.listProducts(accessToken): NetworkResult<List<CatalogProductDto>>`: `GET ${QHATHIA_BASE_URL}/api/v1/products`, DEVICE bearer, `timeout { requestTimeoutMillis = 15_000 }`.
- [x] 1.4 Tests (`MockEngine`):
  - `listProducts` hits `/api/v1/products` with the bearer and parses `stock`/`discountedPrice`;
  - an order request with `expectedUnitPrice` serializes `"expectedUnitPrice":210.0`;
  - a `409 PRICE_CHANGED` from `createOrder` yields `HttpError.problem.changes`.

## 2. Domain models and rules (`core:domain`)

- [x] 2.1 `stock: Int? = null` on `Product`, `ProductDetail` and `CartItem`. `toCartItem()` passes `stock` and uses the two-argument cap.
- [x] 2.2 `CartConstants`:
  - `maxQuantityFor(availability, stock)` (`stock` when known, coerced to 1..99, else the availability cap);
  - `MAX_CONSUMER_NAME_CHARS = 200`;
  - `LOW_STOCK_LABEL_MAX = 5`;
  - remove the `TODO(inventory-stock)` notes and update the KDoc.
- [x] 2.3 Orderability: `IsOrderableUseCase.isOrderable(availability, active, stock)` treats `stock == 0` as not orderable. `Product.isOrderable` / `ProductDetail.isOrderable` pass `stock`. `AddToCartUseCase` uses it.
- [x] 2.4 New models:
  - `ProductSnapshot(id, unitPrice, availability, stock)`;
  - `CartPriceChange(productId, productName, oldUnitPrice, newUnitPrice)`;
  - `CartQuantityCap(productId, productName, max)`;
  - `CartRefresh(cart, priceChanges, removedNames, capped)` with `hasChanges`.
- [x] 2.5 Pure `Cart.refreshedWith(snapshots): CartRefresh` (D2) and `Cart.withPricesUpdated(changes): Cart`. Prices are compared after rounding to 2 decimals (half up).
- [x] 2.6 Ports:
  - new `CartPricingRepository.snapshots(): Result<Map<String, ProductSnapshot>>`;
  - `CartRepository` gains `refresh(snapshots): CartRefresh` and `updatePrices(changes)`.
- [x] 2.7 Order failures:
  - `OrderErrorCategory` gains `PriceChanged`, `ProductNotSellable`, `StockInsufficient`, `Validation`, `Timeout` (update the KDoc: the copy now depends on the category);
  - `OrderFailureDetails(priceChanges, productId, productName, available, invalidFields: Set<ConsumerField>)` with `enum ConsumerField { Name, Phone, Items }`;
  - `OrderException(category, details = OrderFailureDetails())`.
- [x] 2.8 Share rejections: `ShareRejection` gains `StockInsufficient`. `ShareOutcome.Rejected(kind, productId: String? = null, productName: String? = null, available: Int? = null)`.
- [x] 2.9 Use cases:
  - `RefreshCartUseCase` (snapshots → `cartRepository.refresh`; failure leaves the cart untouched);
  - `UpdateCartPricesUseCase` (→ `cartRepository.updatePrices`);
  - `RevalidateCartUseCase` rewritten over `CartPricingRepository` (D11), dropping `ProductDetailRepository`.
- [x] 2.10 Tests:
  - `Cart.refreshedWith`: price change, unchanged price within S/ 0.005, absent line removed, not `AVAILABLE` removed, stock 0 removed, quantity capped, cap without stock;
  - `withPricesUpdated`;
  - `maxQuantityFor(availability, stock)`;
  - `isOrderable` with stock 0;
  - `AddToCartUseCase` refuses stock 0;
  - `RefreshCartUseCase` (success and failure);
  - `RevalidateCartUseCase` (absent, not orderable, list failure → empty, a single snapshots call).

## 3. Data (`core:data`)

- [x] 3.1 Mappers: `stock` in `CatalogMappers`, `search/ProductMappers` (results and suggestions), `ProductDetailMappers` (detail and related).
- [x] 3.2 `buildCreateOrderRequest` sets `expectedUnitPrice = BigDecimal.valueOf(priceUnit).setScale(2, HALF_UP).toDouble()`.
- [x] 3.3 `OrderErrorMapper.fromHttpError(code, problem)` by `errorCode` (D5 table). `fromNetworkError(Timeout) → Timeout`. `OrderRepositoryImpl` passes `response.problem` and builds `OrderFailureDetails`. `fieldErrors` keys map to `ConsumerField` (`customerName`, `customerPhone`, `items`).
- [x] 3.4 `CartShareErrorMapper.fromHttpError(code, problem)`:
  - `PRODUCT_NOT_SELLABLE` → `Rejected(Unavailable, productId, productName)`;
  - `STOCK_INSUFFICIENT` → `Rejected(StockInsufficient, productId, productName, available)`;
  - status rules as before.
- [x] 3.5 `CartPricingRepositoryImpl` (`SessionTokenProvider` + `ProductApiService.listProducts`, `unitPrice = discountedPrice ?: price`), bound in `CartDataModule` (`@Binds @Singleton`).
- [x] 3.6 `CartSessionLocalDataSource` / `CartRepositoryImpl`: `refresh(snapshots)` and `updatePrices(changes)` inside `state.update { … }`. They do not emit a session end.
- [x] 3.7 `core:testing`:
  - `FakeCartRepository.refresh/updatePrices`;
  - new `FakeCartPricingRepository` (configurable result and call count);
  - fixtures gain `stock`.
- [x] 3.8 Tests:
  - `OrderRepositoryImplTest`: `PRICE_CHANGED` with changes, `PRODUCT_NOT_SELLABLE`, `STOCK_INSUFFICIENT`, `VALIDATION_ERROR` fields, a `409` without body → `Unavailable`, `404` → `NotFound`, timeout → `Timeout`; the request carries `expectedUnitPrice` rounded;
  - `CartShareRepositoryImplTest`: the two error codes;
  - `CartPricingRepositoryImplTest` (maps prices and stock; network failure);
  - `CartRepositoryImplTest.refresh` (atomic with a concurrent quantity change);
  - mapper tests for `stock`.

## 4. Shared UI (`core:ui`)

- [x] 4.1 `SubmissionLine.priceText: String? = null` ("Cantidad: N · S/ X.XX"). `SubmissionSuccessContent(totalText: String? = null, note: String? = null)`: the total row sits above the action button, and the note uses a warning tone.
- [x] 4.2 Previews (light and dark) with total and note; existing callers unchanged.

## 5. Cart (`features:cart`)

- [x] 5.1 Model:
  - `CartNotice(priceChanges, removedNames, capped)`;
  - `CartUiState.notice`;
  - `OrderFailure` (`Network`, `Timeout`, `Generic`, `Validation(fields)`);
  - `OrderSubmission.Error(reason)`, `ShareError(network)`;
  - `Success` gains `total` and `totalMismatch`;
  - `OrderedLine.lineTotal`;
  - events `Opened` and `RetryOrder`.
- [x] 5.2 `CartDialog`: `LaunchedEffect(Unit) { viewModel.onEvent(CartUiEvent.Opened) }`.
- [x] 5.3 `CartViewModel`:
  - `Opened` → `refreshJob = launch { refreshCart() }` (only with a non-empty basket and `submission == Idle`); merge changes into `notice`;
  - `placeOrder()` and `share()` wait for a running refresh before the snapshot, and do not send when it changed the basket (notice instead);
  - failures per D6 (price update + notice; flag by `productId`; stock cap + notice; reasons);
  - `RetryOrder`;
  - success summary and total from the `Order` (D9);
  - `share()` per D6;
  - name `take(MAX_CONSUMER_NAME_CHARS)`;
  - notice cleared per D7;
  - `onDismissRequest` updated for the new classes.
- [x] 5.4 `CartDialogContent`:
  - notice box above `TotalsSection` (warning surface, polite live region, at most 3 price lines + "y N más");
  - error state per reason (D6 table);
  - share error network copy;
  - success with line prices, total and mismatch note.
- [x] 5.5 Strings in `features/cart/src/main/res/values/strings.xml`:
  - `cart_notice_price_changed`, `cart_notice_price_more` (plural), `cart_notice_items_removed`, `cart_notice_quantity_capped`;
  - `cart_error_network_message`, `cart_error_back_to_cart`, `cart_error_timeout_message`;
  - `cart_error_validation_name`, `cart_error_validation_phone`, `cart_error_validation_items`;
  - `cart_share_error_network_message`;
  - `cart_success_total_label`, `cart_success_line_price`, `cart_success_total_mismatch`.
  
  Reuse `UiR.string.discovery_error_network_title`, `discovery_action_retry` and `discovery_price_format`.
- [x] 5.6 Previews: notice (price changes + removed + capped), error network / timeout / validation, share network, success with total and mismatch.
- [x] 5.7 `CartViewModelTest`:
  - `opened_refreshesPricesAndShowsNotice`;
  - `opened_refreshFailure_keepsBasketWithoutNotice`;
  - `placeOrder_waitsForRunningRefresh` and `placeOrder_refreshChangedDuringTap_sendsNothing`;
  - `placeOrder_priceChanged_updatesPricesAndReturnsToCart`;
  - `placeOrder_notSellable_flagsLineWithoutRecheck`;
  - `placeOrder_stockInsufficient_capsLine`;
  - `placeOrder_networkError_showsNetworkReason`, `retryOrder_afterNetworkError_createsOnce`;
  - `placeOrder_timeout_offersNoRetry`;
  - `placeOrder_validation_showsField`;
  - `success_showsServerTotal`, `success_totalMismatch_flagged`;
  - `share_networkFailure_showsNetworkCopy`;
  - `name_truncatedAt200`;
  - `notice_clearedOnQuantityChange`.
  
  Update the existing tests that compare with `OrderSubmission.Error` / `ShareError`.

## 6. Product detail (`features:product-detail`)

- [x] 6.1 `StockRow(availability, stock)`: "Pocas unidades (quedan N)" for stock 1–5 (plural `pd_availability_low_stock_count`) and "Agotado" (`pd_availability_out_of_stock`, error color) for stock 0 or `OUT_OF_STOCK`. The stepper `maxQuantity` and `adjustQuantity` use `maxQuantityFor(availability, stock)`.
- [x] 6.2 Share: `ShareSubmission.Error(network)` with `pd_share_error_network_message`; name `take(MAX_CONSUMER_NAME_CHARS)`.
- [x] 6.3 Previews: low-stock count and "Agotado" (light and dark), share network error.
- [x] 6.4 `ProductDetailViewModelTest`:
  - stepper stops at stock;
  - stock 0 blocks add/share;
  - share without network → `Error(network = true)`;
  - name truncated at 200.

## 7. Build and verify

- [x] 7.1 Run `testDebugUnitTest` (or `test` for `core:domain`) for `:core:network`, `:core:domain`, `:core:data`, `:core:ui`, `:features:cart`, `:features:product-detail`, `:features:catalog`, `:features:product-search` and `:app`, one module at a time, with JDK 21 (Android Studio JBR).
- [x] 7.2 `lintDebug` passes with no new findings in the touched files. `:app:assembleProfile` builds.
- [x] 7.3 Create `qa-report.md`:
  - the cases for the QA team: D:CAR-026, X:XCA-010, X:XCO-020, X:XJO-011, X:XRE-002, X:XCA-004, X:EXP-004, D:CAR-011, D:CAR-012, D:CAR-033, with the expected result from regression §2.2, against the QA store;
  - the table mapping each delta-spec scenario to its test.
  
  Mark the emulator run as handed to the QA team.
