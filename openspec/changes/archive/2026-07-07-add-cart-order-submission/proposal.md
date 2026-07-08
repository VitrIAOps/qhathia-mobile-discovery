## Why

The cart dialog already collects products and the consumer's name + cell-phone, but "Realizar pedido" is a render-only placeholder — tapping it does nothing. The backend order endpoint now exists (`POST /api/v1/orders`, Fase 13), so consumers can finally self-generate a purchase order from the basket and skip the back-and-forth with the seller, which is the whole point of the kiosk. This closes the primary consumer journey (discover → cart → order).

## What Changes

- Wire the cart's **"Realizar pedido"** action to `POST /api/v1/orders`, sending the consumer's name, normalized cell-phone (`9XXXXXXXX`), and the basket lines (`productId` + `quantity`). Totals are computed and snapshotted server-side.
- Add an **order-submission lifecycle** to the cart dialog: a blocking, blurred **loading** state (inputs disabled, centered loader) while the request is in flight.
- Add a **success screen** shown on `201`: an order summary (each product's photo, name, quantity), a success title, a subtitle telling the consumer to go to the seller to receive the order and pay, and a line prompting them to check their phone. An **"Aceptar"** button clears the session (cart + form) and returns to Home.
- Add an **error screen** shown on any failure, with a single simplified message ("Hubo un problema generando la orden. Inténtalo de nuevo en unos momentos") and two buttons: **"Entendido"** (higher visual priority) which returns to the cart summary, and **"Cancelar orden"** which returns to Home clearing the session.
- Validate the required fields before submitting (reusing the existing name/phone validation) so an invalid basket never reaches the network.
- **BREAKING (spec-level)**: the `shopping-cart` capability's "Realizar pedido" is no longer render-only, and the cart is now actually cleared on successful order placement.
- Out of scope: **"Compartir por WhatsApp"** stays render-only (tracked separately in subtask `86e22dq6y`).

## Capabilities

### New Capabilities
- `cart-order-submission`: Generating a purchase order from the cart basket — request assembly and field validation, the blocking submit/loading state, the success summary + return-to-Home-with-session-clear, and the error state with its "Entendido" / "Cancelar orden" recovery paths.

### Modified Capabilities
- `shopping-cart`: The "Render-only order and share actions" requirement narrows to WhatsApp only ("Realizar pedido" now submits an order). The "Cart session lifecycle" requirement's deferred order-placement clear is now active.

## Impact

- **New code** — `core:domain` (Order/OrderItem models, `OrderRepository` port, `CreateOrderUseCase`, order error taxonomy), `core:network` (order request/response DTOs, `OrderApiService` for `POST /api/v1/orders`), `core:data` (`OrderRepositoryImpl` + mappers + Hilt binding, reusing `SessionTokenProvider` for the DEVICE JWT and its 401-retry-once pattern).
- **Modified code** — `features:cart` (`CartUiState`/`CartUiEvent`/`CartUiEffect`, `CartViewModel` wiring `PlaceOrder`, `CartDialogContent` loading/success/error overlays), `app` (`MainActivity` handles the return-to-Home-and-clear effect).
- **API** — consumes `POST /api/v1/orders` (DEVICE auth); no backend changes.
- **Dependencies** — none new; reuses the existing Ktor client, session-token seam, and `ClearCartUseCase`.
