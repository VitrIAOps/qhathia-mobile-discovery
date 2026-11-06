## Why

The QA regression of 2026-11-03 found that the tablet can promise a customer one price and record another. In D:CAR-026 / X:XCA-010 (F-30, CRITICAL), a product was added at S/ 210 and the seller changed it to S/ 199. The cart kept showing S/ 210 with no notice, and `POST /orders` (sent without the price the customer saw) recorded the order at S/ 199.

Around that, every order or share problem that is not "product unavailable" ends in the same generic "Algo salió mal" screen:

- no network (F-44, X:XRE-002; "Cancelar orden" even empties the cart);
- a timeout;
- a `422` (F-33, a name over 200 characters);
- stock shortage (F-32, `422 STOCK_INSUFFICIENT`);
- a share without network (F-34, D:CAR-033).

The quantity cap is still a placeholder (99 for any `AVAILABLE` product), so a product with 3 units lets the customer pick 6 (X:XCA-004). A product deleted while its Detail is open still enters the cart unnoticed (X:EXP-004). The success screen never shows the total the store recorded (F-29, D:CAR-012).

The backend already does its part (api-contract §38–§39):
- it accepts `items[].expectedUnitPrice`;
- it answers `409 PRICE_CHANGED` with `changes[]`, `409 PRODUCT_NOT_SELLABLE` with `reason`, and `422 STOCK_INSUFFICIENT` with `available`;
- it sends `stock` on every product;
- it exposes `GET /products` to refresh the basket without counting fake product views.

Since `device-unlinked-session`, `NetworkResult.HttpError.problem` carries those error bodies, so the app can now react to each case.

## What Changes

- **Basket refresh when the cart opens.** One `GET /api/v1/products` call refreshes every line:
  - the current unit price (`discountedPrice ?: price`);
  - availability and stock;
  - lines that are gone or no longer orderable are removed;
  - quantities above the stock are capped.
  
  The cart shows a notice: "El precio de «X» cambió de S/ 210.00 a S/ 199.00…", "Quitamos de tu carrito lo que ya no está disponible: «X».", or "Solo quedan N de «X»; ajustamos la cantidad.". If the refresh fails (no network), the basket is left as it is.
- **The order carries the price the customer saw.** `POST /orders` sends `items[].expectedUnitPrice`. A `409 PRICE_CHANGED` is not an error: the cart updates each changed line to `currentUnitPrice`, shows the price notice and goes back to the cart so the customer confirms the new total.
- **Errors decided by `errorCode`, not by status.**
  - `PRODUCT_NOT_SELLABLE` flags that line directly.
  - `STOCK_INSUFFICIENT` caps the line to `available` (removes it at 0) with a notice.
  - `VALIDATION_ERROR` shows a field-specific message.
  - `404` and unreadable bodies keep today's recheck as a fallback. The recheck now uses the single `GET /products` call instead of one `GET /products/{id}` per line (each of those counted a fake `PRODUCT_VIEW`).
  - The same mapping applies to "Compartir" in the cart and in the Detail.
- **Order error by cause** (**BREAKING** for the "single message for every cause" rule):
  - *No network*: "Perdí la conexión…" with "Reintentar" and "Volver al carrito", never "Cancelar orden".
  - *Timeout*: "No pudimos confirmar si tu pedido se registró…" with "Entendido" only. The order may exist, so it is never retried.
  - *Validation*: a message for the field.
  - *Anything else*: today's generic screen.
- **Share error without network** says the tablet has no internet, in the cart and in the Detail.
- **Real stock cap.** `stock` is read from catalog, search, related products, detail and the refresh. The cap is `stock` when known (still at most 99), and the placeholder only for "a consultar" (`stock = null`). The Detail shows "Pocas unidades (quedan N)" for 1–5 units and "Agotado" at 0 or `OUT_OF_STOCK`.
- **Name limited to 200 characters** in the cart and the Detail share form (the server limit).
- **Success screen with the recorded total.** It shows each line's total and "Total del pedido S/ X.XX" from the server's `Order`. If that total differs from what the cart showed, it adds a note telling the customer to confirm it with the seller.

## Capabilities

### New Capabilities
- `cart-revalidation`: refreshing the basket against the store (prices, availability, stock) when the cart opens and after a price-change rejection, and the notices that explain what changed.

### Modified Capabilities
- `cart-order-submission`:
  - the success screen shows the recorded line totals and order total;
  - order errors are shown by cause (network with retry, timeout, validation, generic);
  - unavailable products are flagged from the error code, with the list-based recheck only as a fallback.
- `cart-whatsapp-share`: a share that failed for lack of network says so, and an error code naming a product flags that line.
- `shopping-cart`:
  - the quantity cap comes from the real stock;
  - the consumer name is limited to 200 characters;
  - the order request carries the price the customer saw.
- `product-detail`:
  - the availability label uses the stock ("Pocas unidades (quedan N)", "Agotado") and the stepper stops at the stock;
  - the share form name is limited to 200 characters;
  - a share without network says so.

## Impact

- `core:network`:
  - `OrderItemRequestDto.expectedUnitPrice`;
  - `stock` in `CatalogProductDto`, `SearchProductDto`, `SuggestedProductDto` and `ProductDetailResponseDto`;
  - `ProductApiService.listProducts()` (`GET /api/v1/products`).
- `core:domain`:
  - `stock` on `Product`, `ProductDetail` and `CartItem`;
  - `CartConstants.maxQuantityFor(availability, stock)` and `MAX_CONSUMER_NAME_CHARS`;
  - `Cart.refreshedWith(...)` / `withPricesUpdated(...)`, with `CartRefresh`, `CartPriceChange` and `ProductSnapshot`;
  - port `CartPricingRepository`;
  - `RefreshCartUseCase`;
  - `OrderErrorCategory` gains `PriceChanged`, `ProductNotSellable`, `StockInsufficient`, `Validation`, `Timeout`, and `OrderException` carries the details;
  - `ShareRejection` / `ShareOutcome.Rejected` carry the product and the available units;
  - `RevalidateCartUseCase` reads the snapshots;
  - `CartRepository.replace(cart)`.
- `core:data`:
  - order and share error mappers read `HttpError.problem`;
  - order request mapper;
  - catalog, search, detail and suggestion mappers (`stock`);
  - new `CartPricingRepositoryImpl` and its binding;
  - `CartSessionLocalDataSource.replace`.
- `core:ui`: `SubmissionLine.priceText`, plus `SubmissionSuccessContent` total and note slots.
- `core:testing`: fakes and fixtures (`FakeCartRepository.replace`, `FakeCartPricingRepository`, `stock` in fixtures).
- `features:cart`:
  - `CartUiEvent.Opened` / `RetryOrder`;
  - `CartNotice`;
  - `OrderSubmission.Error(reason)`, `ShareError(network)`, and `Success` with line totals and order total;
  - the `CartDialog` open hook;
  - copy.
- `features:product-detail`:
  - stock-aware label and cap;
  - name limit;
  - share network copy;
  - share flags by error code.
- `features:catalog`, `features:product-search`: no behavior change; they pass `stock` through `toCartItem()`.
- No new Gradle module, no new library, no manifest change. API contract: api-contract §38–§39 (already in `docs/android/api-contract.md`).
- Out of scope:
  - order status polling after success (F-26, change `order-status-tracking`);
  - the 180 s cart inactivity budget (F-35);
  - re-running D:CAR-024, where QA counted units rather than lines; the 20-line limit already works.
