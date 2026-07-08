> **Module note:** No new Gradle module is required — every module this change touches (`:core:domain`, `:core:network`, `:core:data`, `:features:cart`, `:app`) already exists and is registered in `settings.gradle.kts`. Order code layers onto these following the existing product-detail vertical slice. Do **not** add any of it as packages inside `:app`.

## 1. Domain layer (`core:domain`)

- [x] 1.1 Add `Order` and `OrderItem` domain models (framework-free) capturing the fields the UI needs: id/status/total plus per-item productId, name, quantity, unit-price snapshot, line total.
- [x] 1.2 Add the `OrderRepository` port: `suspend fun createOrder(name: String, phone: String, items: List<CartItem>): Result<Order>`.
- [x] 1.3 Add `OrderErrorCategory` enum + `OrderException` (mirroring `DetailErrorCategory`/`DetailException`); a single generic category is sufficient given the simplified message.
- [x] 1.4 Add `CreateOrderUseCase` delegating to `OrderRepository.createOrder`.

## 2. Network layer (`core:network`)

- [x] 2.1 Add `OrderDtos.kt` — `@Serializable` `CreateOrderRequestDto` (customerName, customerPhone, items, optional sessionId/customerNote), `OrderItemRequestDto` (productId, quantity), `OrderResponseDto`, `OrderItemResponseDto` (productId, productName, quantity, unitPriceSnapshot, lineTotal) — matching `POST /api/v1/orders` in api-contract §9.
- [x] 2.2 Add `OrderApiService.createOrder(accessToken, request)` — `client.post("${QHATHIA_BASE_URL}/api/v1/orders")` with `Authorization: Bearer`, JSON body, a per-request timeout, returning `NetworkResult<OrderResponseDto>` via `toNetworkResult()`.

## 3. Data layer (`core:data`)

- [x] 3.1 Add `OrderMappers.kt` — build `CreateOrderRequestDto` from name/phone/`List<CartItem>` (normalize phone to `^9\d{8}$`: strip whitespace and any leading `+51`/`51`); map `OrderResponseDto` → domain `Order`.
- [x] 3.2 Add `OrderErrorMapper` folding `NetworkResult.HttpError`/`NetworkError`/token failure into `OrderErrorCategory`.
- [x] 3.3 Add `OrderRepositoryImpl` — resolve the DEVICE JWT via `SessionTokenProvider`, submit via `OrderApiService`, and on `401` do the `clear()` + `forceRefresh` retry-once (as in `ProductDetailRepositoryImpl`); return `Result<Order>`.
- [x] 3.4 Add a Hilt `@Binds` for `OrderRepository → OrderRepositoryImpl` (extend `CartDataModule` or add an `OrderDataModule`).

## 4. Cart feature — state & logic (`features:cart`)

- [x] 4.1 Add an `OrderSubmission` sealed interface (`Idle`, `Submitting`, `Success(summary)`, `Error`) and an `OrderedLine` model (photo, name, quantity); add `submission: OrderSubmission = Idle` to `CartUiState`.
- [x] 4.2 Extend `CartUiEvent` with `AcceptSuccess`, `DismissError`, `CancelOrder`; extend `CartUiEffect` with `ReturnHome`.
- [x] 4.3 Wire `CartViewModel`: inject `CreateOrderUseCase` + `ClearCartUseCase`; implement `PlaceOrder` (guard `isOrderEnabled` and `submission==Idle`, snapshot summary, set `Submitting`, call the use case, set `Success`/`Error`); `AcceptSuccess`/`CancelOrder` → clear cart + emit `ReturnHome`; `DismissError` → `submission=Idle`.

## 5. Cart feature — UI (`features:cart`)

- [x] 5.1 In `CartDialogContent`, render the `Submitting` overlay: dim/blur the background, disable all inputs (form fields, steppers, line removal, buttons), show a centered loading indicator with a state description.
- [x] 5.2 Add the success screen: per-product summary (photo/name/quantity), success title, seller-pickup-and-pay subtitle, check-your-phone prompt, and an "Aceptar" button → `AcceptSuccess`.
- [x] 5.3 Add the error screen: single simplified message, "Entendido" (higher visual priority) → `DismissError`, "Cancelar orden" → `CancelOrder`.
- [x] 5.4 Add the Spanish string resources for the new titles/subtitles/messages/button labels and content descriptions; reuse `core:ui` components (`PrimaryActionButton`, etc.) and design tokens.
- [x] 5.5 Add/adjust `@Preview`s (light + dark) covering the Submitting, Success, and Error phases.

## 6. App shell (`app`)

- [x] 6.1 Handle the `ReturnHome` effect in `CartDialog`/`MainActivity`: dismiss the cart overlay (`showCart = false`) and pop to `HOME_ROUTE`.

## 7. Tests & Definition of Done

- [x] 7.1 `CreateOrderUseCase` / `OrderRepositoryImpl` unit tests (success, 401-retry-once, error mapping) using fakes from `core:testing`; add a `FakeOrderRepository` if needed.
- [x] 7.2 `OrderMappers` unit tests — phone normalization variants (`+51 987 654 321`, `51987654321`, `987654321`) and request/response mapping.
- [x] 7.3 `CartViewModel` tests (Turbine): `PlaceOrder` emits `Submitting`→`Success`/`Error`; double-submit is ignored; `AcceptSuccess`/`CancelOrder` clear the cart and emit `ReturnHome`; `DismissError` returns to `Idle`.
- [x] 7.4 Validate Definition of Done: `./gradlew lint` and `./gradlew test` pass, and the cart dialog previews render in light and dark for every phase.
