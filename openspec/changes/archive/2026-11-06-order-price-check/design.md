## Context

The basket copies each product's price, availability and a placeholder quantity cap when the product is added (`toCartItem()`: `priceUnit = discountedPrice ?: price`, `maxQuantity = CartConstants.maxQuantityFor(availability)`, 99 for `AVAILABLE`). Nothing reads them again.

`POST /orders` sends only `productId` and `quantity`. A `409` or `400` is always treated as "unavailable": `OrderErrorMapper` triggers `RevalidateCartUseCase`, which calls `GET /products/{id}` for every line. Any other failure (network, timeout, `422`) lands in the single `OrderSubmission.Error` screen with "Cancelar orden". Share failures collapse the same way into `ShareError`.

What the backend guarantees (api-contract §38–§39):

- **`POST /orders`** accepts `items[].expectedUnitPrice` (2 decimals, `HALF_UP`, compared with `discountedPrice ?: price`). It checks, in order:
  - `404 RESOURCE_NOT_FOUND`;
  - `409 PRODUCT_NOT_SELLABLE {productId, productName, reason}`;
  - `422 STOCK_INSUFFICIENT {productId, productName, available, requested}`;
  - `409 PRICE_CHANGED {changes:[{productId, productName, expectedUnitPrice, currentUnitPrice}]}`, with no order created;
  - `422 VALIDATION_ERROR {fieldErrors}`.
  
  The `201` carries `total` and `items[].unitPriceSnapshot` / `lineTotal`.
- **`POST /carts/share-pdf`** answers the same `409` / `422` for products.
- **`GET /api/v1/products`** (DEVICE token) returns a plain array of the store's active products with `price`, `discountedPrice`, `availability` and `stock`. It does **not** count a `PRODUCT_VIEW`. A deleted or deactivated product is absent.
- **`stock`** (`null` = "a consultar") is sent by `GET /products`, `/catalog/by-category`, `/products/{id}`, `/products/{id}/suggestions` and the search results.
- **Sellable** = `AVAILABLE` and, when the product carries stock, more than 0. `LOW_STOCK` is not sellable from the tablet.

Since `device-unlinked-session`, `NetworkResult.HttpError.problem` (`ProblemDetailDto`) already parses `errorCode`, `productId`, `productName`, `reason`, `available`, `changes` and `fieldErrors`.

Modules involved (no new module): `core:network`, `core:domain`, `core:data`, `core:ui`, `core:testing`, `features:cart`, `features:product-detail`. `features:catalog` and `features:product-search` only recompile, because `toCartItem()` carries `stock` now. Dependency rules hold: DTOs and error bodies stay in `core:network`, and `core:data` maps them to domain types.

## Goals / Non-Goals

**Goals:**
- The total the customer sees is the total the store records, or the customer is told before the order is created.
- A product that cannot be ordered, or not in that quantity, is explained by name and never loops on a generic error.
- Each order or share failure shows a message that says what to do: retry when the network is back, ask the seller after a timeout, fix a field.
- The quantity cap follows the real stock.
- The success screen shows what the store recorded.

**Non-Goals:**
- Order status after success (F-26) and the longer cart inactivity budget (F-35): separate changes.
- An idempotent `POST /orders`. The backend has none, so a timeout is never retried.
- Recomputing prices or discounts on the tablet. The server's `discountedPrice` stays the only source.
- A batched `GET /products?ids=` (does not exist; the full list is small for one store).

## Decisions

### D1 — Refresh the basket from `GET /api/v1/products`, once per cart opening

New `ProductApiService.listProducts(token): NetworkResult<List<CatalogProductDto>>`, with a 15 s request timeout, reusing `CatalogProductDto` (same `ProductSummaryResponse`) plus `stock`.

New port `CartPricingRepository { suspend fun snapshots(): Result<Map<String, ProductSnapshot>> }`, implemented in `core:data/cart` with `SessionTokenProvider`. `ProductSnapshot(id, unitPrice = discountedPrice ?: price, availability, stock)`.

- *Why the list and not `GET /products/{id}` per line:* one request instead of up to 20, and no fake `PRODUCT_VIEW` in the merchant's metrics.
- *Why on opening:* that is when the customer looks at the total (X:XCA-010 expects the notice on open). The cart dialog is composed only while shown, so `LaunchedEffect(Unit)` in `CartDialog` sends `CartUiEvent.Opened` once per opening.
- *Alternative:* periodic refresh while the dialog is open. Rejected: the `409 PRICE_CHANGED` on submit already covers a change made while the cart is open.

### D2 — Refreshing is a pure transformation applied atomically in the repository

`Cart.refreshedWith(snapshots): CartRefresh` (pure, `core:domain`). For each line:

- **absent** from `snapshots`, or not orderable (`IsOrderableUseCase.isOrderable(availability)`, or `stock == 0`): the line is removed and listed in `removed`;
- **otherwise:**
  - `priceUnit` is set to the snapshot `unitPrice`; a difference of at least S/ 0.01 goes to `priceChanges`;
  - `availability`, `stock` and `maxQuantity = maxQuantityFor(availability, stock)` are set;
  - when the quantity is above the new cap, it is capped and listed in `capped`.

`CartRefresh(cart, priceChanges: List<CartPriceChange>, removed: List<String> /* names */, capped: List<CartQuantityCap(name, max)>)`, with `hasChanges`.

`CartRepository` gains `suspend fun refresh(snapshots): CartRefresh` and `suspend fun updatePrices(changes: List<CartPriceChange>)`. `CartSessionLocalDataSource` applies them inside `state.update { … }`, so a tap on "+" during the request is never lost (read-modify-write in one step). Neither emits the customer-session end, so the consumer form is kept.

`RefreshCartUseCase` = `snapshots()` → `cartRepository.refresh(it)`. On failure it touches nothing and returns `Result.failure`.

- *Alternative:* `replace(cart)` computed in the use case (regression doc). Rejected: it overwrites any change made between the read and the write.

### D3 — Removed on opening, flagged on rejection

Opening the cart **removes** the lines that can no longer be ordered, with a notice that names them. A rejection while ordering keeps today's behavior: the line is **flagged** "No disponible", and the actions stay blocked until the customer removes it.

The two differ on purpose. On opening the customer has not committed to anything, so a silent "No disponible" line would just be noise. On submit, a line disappearing under their finger would be confusing.

### D4 — The order sends the price the customer saw

`OrderItemRequestDto(productId, quantity, expectedUnitPrice: Double? = null)`. `buildCreateOrderRequest` sets `expectedUnitPrice = BigDecimal.valueOf(priceUnit).setScale(2, HALF_UP)`. The same rounding is used by the backend and by the price-change comparison in D2.

### D5 — Classify by `errorCode`, then by status

`OrderErrorMapper.fromHttpError(code, problem)`:

| `errorCode` | Category | Details carried |
|---|---|---|
| `PRICE_CHANGED` | `PriceChanged` | `changes` → `List<CartPriceChange(productId, productName, oldUnitPrice, newUnitPrice)>` |
| `PRODUCT_NOT_SELLABLE` | `ProductNotSellable` | `productId`, `productName`, `reason` |
| `STOCK_INSUFFICIENT` | `StockInsufficient` | `productId`, `productName`, `available` |
| `VALIDATION_ERROR` | `Validation` | field keys (`customerName`, `customerPhone`, `items`) |
| none, status `404` | `NotFound` | — |
| none, status `400` / `409` | `Unavailable` | today's fallback |
| anything else | `OrderApi` | — |

`fromNetworkError`: `Timeout → Timeout` (new; today it is `OrderApi`), `Connectivity → Network`, `Unknown → OrderApi`.

`OrderException(category, details: OrderFailureDetails = OrderFailureDetails())`. `OrderFailureDetails` is a domain data class with `priceChanges`, `productId`, `productName`, `available` and `invalidFields`. A `PRICE_CHANGED` without `changes` falls back to `OrderApi`, because there is nothing to show.

The share mapper does the same through `ShareOutcome.Rejected(kind, productId?, productName?, available?)`:

- `PRODUCT_NOT_SELLABLE` → `Unavailable` with the product;
- `STOCK_INSUFFICIENT` → new `ShareRejection.StockInsufficient`;
- the rest as today.

Share has no `expectedUnitPrice`, so no price check.

### D6 — What the cart does with each failure

In `CartViewModel.placeOrder().onFailure`:

| Category | Result |
|---|---|
| `PriceChanged` | `updatePrices(changes)`, `notice = prices changed`, `submission = Idle`. Not an error: the customer sees the new total and taps "Realizar pedido" again. |
| `ProductNotSellable` with a `productId` in the basket | flag it (`unavailableIds += id`), `submission = Idle`. No recheck round. |
| `StockInsufficient` with a `productId` | `updateQuantity(id, available)` (0 removes the line), `notice = capped(name, available)`, `submission = Idle` |
| `Unavailable` / `NotFound`, or the two above without a product | today's `flagUnavailableLines` (recheck), with the generic error as its fallback |
| `Network` | `Error(OrderFailure.Network)` |
| `Timeout` | `Error(OrderFailure.Timeout)` |
| `Validation` | `Error(OrderFailure.Validation(fields))` |
| else | `Error(OrderFailure.Generic)` |

Share (cart and Detail) follows the same table for its rejections. `Failed(Network)` becomes `ShareError(network = true)`; everything else is `ShareError()`.

`OrderSubmission.Error` becomes `data class Error(val reason: OrderFailure = OrderFailure.Generic)`, with `sealed interface OrderFailure { Network; Timeout; Generic; data class Validation(fields: Set<ConsumerField>) }`. `ShareError` becomes `data class ShareError(val network: Boolean = false)`. In the Detail, `ShareSubmission.Error` becomes `data class Error(val network: Boolean = false)`.

UI for the order error:

| Reason | Title | Message | Primary | Secondary |
|---|---|---|---|---|
| Network | `discovery_error_network_title` ("Perdí la conexión…") | `cart_error_network_message` | "Reintentar" → `RetryOrder` | "Volver al carrito" → `DismissError` |
| Timeout | `cart_error_title` | `cart_error_timeout_message` | "Entendido" → `DismissError` | — |
| Validation | `cart_error_title` | per field (name, phone, basket) | "Entendido" | — |
| Generic | today's | today's | "Entendido" | "Cancelar orden" |

`RetryOrder` = `submission = Idle` and then `placeOrder()`, on the same guarded path as the button: no double submit.

- *Why no retry after a timeout:* `POST /orders` is not idempotent, and a slow network already created an order once (D:CAR-043). The customer asks the seller instead.

### D7 — Notices

`CartUiState.notice: CartNotice?`, where `CartNotice(priceChanges, removedNames, capped)` merges the result of an opening refresh, a `PRICE_CHANGED`, or a `STOCK_INSUFFICIENT`.

It is drawn in `FooterPanel` above `TotalsSection` on a warning surface:
- one line per price change, up to 3, then "y N más";
- one line for the removed products;
- one line per cap.

It is announced as a polite live region.

The notice is cleared when a quantity changes, when a line is removed, when an order or share starts, and on session reset. A refresh with no changes leaves the current notice as it is.

### D8 — Quantity cap from stock

`CartConstants.maxQuantityFor(availability: String, stock: Int?): Int = (stock ?: maxQuantityFor(availability)).coerceIn(1, DEFAULT_MAX_QUANTITY)`. The single-argument version stays as the "a consultar" fallback, and the `TODO(inventory-stock)` notes are removed. A `stock` of 0 never reaches the cart, because it is not orderable (D2 and `AddToCartUseCase`).

`stock` is mapped on `Product` (catalog, search, suggestions), `ProductDetail` and `CartItem`. `toCartItem()` uses the two-argument cap, and the Detail stepper and `ProductDetailContent` use it too.

The Detail label:
- `stock in 1..5` → "Pocas unidades (quedan N)" (`pd_availability_low_stock_count`, plural), even when `availability` is `AVAILABLE`;
- `stock == 0` or `OUT_OF_STOCK` → "Agotado" in the error color;
- otherwise today's labels.

`AddToCartUseCase` also refuses `stock == 0`, using the same rule as the server.

### D9 — Success screen from the server's order

On `createOrder().onSuccess { order -> }`, the summary is built from `order.items`: name, quantity, `lineTotal`, and the image looked up by `productId` from the basket snapshot. `total = order.total`.

`OrderedLine` gains `lineTotal: Double? = null`, and `OrderSubmission.Success` gains `total: Double? = null` and `totalMismatch: Boolean`. `totalMismatch` is `abs(order.total - shown total) >= 0.01`.

`SubmissionLine` gains `priceText: String? = null`, and `SubmissionSuccessContent` gains `totalText: String? = null` and `note: String? = null`. Quote-only shares keep today's summary, with no prices and no total.

- *Why from the server:* the rule "never recompute what the backend computes". With D4 the two totals match. The mismatch note is only a safety net.

### D10 — Name length

`CartConstants.MAX_CONSUMER_NAME_CHARS = 200` (the server's `@Size(max = 200)`). `CartViewModel.onNameChanged` and `ProductDetailViewModel.onShareNameChanged` truncate the input with `take(200)` before validating, so character 201 cannot be typed. A server `VALIDATION_ERROR` stays as a safety net (D6).

### D11 — Recheck fallback without `GET /products/{id}`

`RevalidateCartUseCase` now takes `CartPricingRepository.snapshots()`. A line is unavailable when it is absent or not orderable. If the list cannot be read, nothing is flagged, as today. It no longer depends on `ProductDetailRepository`.

## Risks / Trade-offs

- [`GET /products` returns every active product of the store, and the cart only needs up to 20] → One store has tens to a few hundred products, about one request of a few KB per cart opening. If it grows, the backend's optional `?ids=` (regression §1.5) can be adopted without changing the port.
- [The refresh is still in flight when the customer taps "Realizar pedido" or "Compartir"] → The action waits for the running refresh (bounded by its 15 s timeout) before taking the basket snapshot. If that refresh changed the basket, nothing is sent: the cart shows the notice, the same as after a `PRICE_CHANGED`, so the customer never sends a total they did not see. If it changed nothing or failed, the request is sent, with `PRICE_CHANGED` as the safety net.
- [A product the server lists but that is not orderable, e.g. `LOW_STOCK`, is removed on opening] → Same rule as the server (§39: `LOW_STOCK` is not sold from the tablet), and the notice names it.
- [Several price changes in a row] → Each `PRICE_CHANGED` updates and shows the new total. The customer is never charged a price they did not see.
- [Changing `OrderSubmission.Error` / `ShareError` from objects to classes] → Every `when` is exhaustive, so the compiler lists each place to update, including the tests that compare with `OrderSubmission.Error`.
- [`stock` absent from older search responses] → It defaults to `null`, so the cap falls back to the availability placeholder, as today.

## Migration Plan

App-only. Older backends ignore `expectedUnitPrice` (optional) and omit `stock`, which defaults to `null`. Rollback = revert the change.

Verify with unit tests. The QA team re-runs D:CAR-026, X:XCA-010, X:XCO-020, X:XJO-011, X:XRE-002, X:XCA-004, X:EXP-004, D:CAR-011, D:CAR-012 and D:CAR-033 against the QA store (see tasks).

## Open Questions

- None blocking. The copy follows the regression doc (§2.2); product can reword strings without code changes.
