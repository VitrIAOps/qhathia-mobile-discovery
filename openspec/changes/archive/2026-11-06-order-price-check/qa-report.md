# QA report — order-price-check

**Legend:** ✔ works · ✖ fails (bug present) · ◐ partial · ⏸ blocked / not run.

| | |
|---|---|
| App (before) | `profile` on `490694e` (the QA-tested build; QA regression 2026-11-03) |
| App (after) | `profile` with this change (branch `fix/order-price-check`) |
| Backend | production `http://87.99.156.164` (phases 35–43) |
| Store | QA store `6b89ea64-d315-4294-9c69-cd412cd8b54a` ("QA Discovery — Ferretería"), never El Martillo |

## Status

- **Handed to the QA team (2026-11-06):** every case below. The change was verified with unit tests (575 across the touched modules), `lintDebug` and a `profile` build. Orders in these cases are real orders in the QA store: reject them from the staff app after each case.

## Cases for the QA team

| Case | Steps | Expected after | Before (QA 03/11) | After | Who |
|---|---|---|---|---|---|
| D:CAR-026 / X:XCA-010 | Add a product at S/ 210 → the seller sets S/ 199 → open the cart | Notice "El precio de «…» cambió de S/ 210.00 a S/ 199.00…" and total S/ 199.00; one `GET /api/v1/products` in nginx | ✖ S/ 210 shown, order recorded at S/ 199 | ⏸ | QA team |
| X:XCO-020 / X:XJO-011 | Cart already open → the seller changes the price → "Realizar pedido" | First tap: `POST /orders 409` and the price notice, no error screen; second tap: `201` with `total` = the total shown; the body carries `"expectedUnitPrice"` | ✖ | ⏸ | QA team |
| X:XRE-002 | Airplane mode → "Realizar pedido" | "Perdí la conexión…" with the internet message, "Reintentar" and "Volver al carrito" (no "Cancelar orden"); with the network back, "Reintentar" → one `POST /orders 201` | ✖ generic "Algo salió mal" | ⏸ | QA team |
| Timeout (no case id) | Throttle the network so `POST /orders` times out | "No pudimos confirmar si tu pedido se registró…" with only "Entendido"; no second `POST /orders` | — | ⏸ | QA team |
| X:XCA-004 | Product with stock 3 → Detail → "+" ×5 | "Pocas unidades (quedan 3)", stepper stops at 3 | ✖ "Disponible", "+" to 6 | ⏸ | QA team |
| X:EXP-004 / XCA-031 | Detail open → delete the product from the staff app → "Agregar" → open the cart | The line is gone with "Quitamos de tu carrito lo que ya no está disponible: «…»"; no `POST /orders` | ✖ added silently | ⏸ | QA team |
| Stock shortage (no case id) | 5 units in the cart → set stock to 3 from staff with the cart open → "Realizar pedido" | `422 STOCK_INSUFFICIENT` → the line goes to 3 with "Solo quedan 3 de «…»", never "Algo salió mal" | — | ⏸ | QA team |
| D:CAR-011 | Type or paste a 262-character name | The field keeps 200 characters; the order gives `201` | ✖ `422` → generic error | ⏸ | QA team |
| D:CAR-012 / X:XPE-007 | Product at 20 % off (S/ 100 → S/ 80) → order | Success shows each line's total and "Total del pedido S/ 80.00" | ◐ no total shown | ⏸ | QA team |
| D:CAR-033 | Airplane mode → "Compartir" (cart and Detail) | "No pudimos enviar la cotización porque la tablet no tiene internet…", "Entendido" keeps cart, name and phone | ◐ generic share message | ⏸ | QA team |
| D:DET-013 / D:CAR-023 / X:XCA-030 | As in the regression | Unchanged (OK) | ✔ | ⏸ | QA team |
| D:CAR-024 | Re-run counting **distinct lines**, not units | The 21st distinct product is refused with the line-limit notice (no code change) | ◐ misread | ⏸ | QA team |

## Unit-test coverage of the delta specs

| Spec scenario | Test |
|---|---|
| cart-revalidation — Price lowered / Deleted product removed / Quantity capped | `CartViewModelTest.opened_refreshesPricesAndShowsNotice`, `opened_removesDeletedProductWithNotice`; `CartTest.refresh …` |
| cart-revalidation — Refresh without network | `CartViewModelTest.opened_refreshFailure_keepsBasketWithoutNotice`, `OrderabilityUseCasesTest.refresh use case leaves the basket untouched on failure` |
| cart-revalidation — One request per opening | `opened_refreshesPricesAndShowsNotice` (`pricing.calls == 1`), `ProductApiServiceTest.list_products_hits_the_list_endpoint_and_parses_stock` |
| cart-revalidation — Several price changes / Notice cleared | `CartNotice` rendering (preview `Cart — notice`), `CartViewModelTest.notice_clearedOnQuantityChange` |
| cart-revalidation — Refresh changes a price / nothing during the tap | `placeOrder_refreshChangedDuringTap_sendsNothing`, `placeOrder_waitsForRunningRefresh` |
| cart-order-submission — Price changed while the cart was open / Second tap | `placeOrder_priceChanged_updatesPricesAndReturnsToCart`, `OrderRepositoryImplTest.price_changed_carries_the_changes`, `request_carries_the_rounded_price_the_customer_saw`, `OrderApiServiceTest.sends_the_expected_unit_price` |
| cart-order-submission — Not sellable flags the line / Stock shortage | `placeOrder_notSellable_flagsLineWithoutRecheck`, `placeOrder_stockInsufficient_capsLine`, `OrderRepositoryImplTest.not_sellable_…`, `stock_insufficient_…` |
| cart-order-submission — Recorded total / Different total flagged | `success_showsServerTotal`, `success_totalMismatch_flagged` |
| cart-order-submission — No network retry / Retry once / Timeout not retried / Other failures | `placeOrder_networkError_showsNetworkReason`, `retryOrder_afterNetworkError_createsOnce`, `placeOrder_timeout_offersNoRetry`, `given_failure_when_place_order_then_error`, `OrderRepositoryImplTest.given_timeout_when_created_then_timeout_category` |
| cart-order-submission — Validation | `placeOrder_validation_showsField`, `OrderRepositoryImplTest.validation_error_lists_the_fields` |
| cart-order-submission — Recheck uses one request / Recheck finds nothing | `given_order_400_with_nothing_to_blame_when_placed_then_generic_error` (`pricing.calls == 1`), `OrderabilityUseCasesTest.recheck …` |
| cart-whatsapp-share — Not sellable share / Stock shortage on share | `share_notSellable_flagsNamedLine`, `CartShareRepositoryImplTest.not_sellable_share_names_the_product`, `stock_insufficient_share_carries_the_units_left` |
| cart-whatsapp-share — Share without network | `share_networkFailure_showsNetworkCopy` |
| shopping-cart — Cap follows the stock / No stock count / stock 0 not added | `OrderabilityUseCasesTest.cap follows the stock when known`, `add to cart refuses a product with no stock left` |
| shopping-cart — Name stops at 200 | `CartViewModelTest.name_truncatedAt200` |
| product-detail — Few units left / Out of stock / No stock left | `ProductDetailViewModelTest.stepper_stops_at_the_stock`, `stock_zero_blocks_add_and_share`; previews `Detail few units left`, `Detail sold out` |
| product-detail — Share name / Share without network | `share_name_truncated_at_200`, `share_without_network_says_so` |
