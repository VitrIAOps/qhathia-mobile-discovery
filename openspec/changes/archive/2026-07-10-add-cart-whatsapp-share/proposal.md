## Why

The cart's green **"Compartir por WhatsApp"** button is still a render-only `TODO` — tapping it does nothing. The backend now ships `POST /api/v1/carts/share-pdf` (Fase 14), which generates a quote PDF of the basket and delivers it to the consumer's WhatsApp. Wiring the button lets a consumer walk away with a written quote (photos, quantities, referential prices, and how to reach the store) on their own phone — reinforcing the kiosk's "self-serve, minimal seller back-and-forth" premise. This closes the second cart action alongside "Realizar pedido".

## What Changes

- Wire the cart's **"Compartir por WhatsApp"** action to `POST /api/v1/carts/share-pdf`, sending the consumer's name, the normalized cell-phone (`9XXXXXXXX`), and the basket lines (`productId` + `quantity`) — the **same request body as `POST /api/v1/orders`**. The endpoint generates a quote PDF and sends it to the consumer's WhatsApp; it does **not** create an order.
- Reuse the existing **order-submission lifecycle UX** from the `cart-order-submission` slice for the share flow: the blocking, dimmed **loading** overlay (all inputs disabled, centered loader) while the request is in flight; a **success screen** (product summary of photo/name/quantity, success title, "go to the seller" subtitle, and the "check your phone" prompt — which now literally applies, since the PDF arrives on WhatsApp) with an **"Aceptar"** button that clears the session and returns Home; and an **error screen** with the single simplified message plus **"Entendido"** (higher priority, returns to the cart) and **"Cancelar orden"** (clears the session, returns Home).
- Guard against double-submit and honor the same field validation used by "Realizar pedido" so an invalid basket never reaches the network.
- Fix the lingering-state bug: reset the submission phase to `Idle` when the session is cleared / the flow returns Home, so a reopened cart never shows a stale success/error screen ("el carrito se queda con mensaje de éxito tras envío").
- **BREAKING (spec-level)**: the `shopping-cart` capability's "Compartir por WhatsApp" action is no longer render-only.
- Out of scope: differentiating the `share-pdf` `status` values (`SENT`/`STUB`/`FAILED`), surfacing/opening the returned `pdfUrl` as an in-app fallback, and any change to the "Realizar pedido" order flow.

## Capabilities

### New Capabilities
- `cart-whatsapp-share`: Sharing the cart as a WhatsApp quote PDF from the "Compartir por WhatsApp" button — request assembly (identical body to the order request, phone normalized to `^9\d{8}$`) and validation gating, the blocking share/loading state, the success summary + return-to-Home-with-session-clear, and the error state with its "Entendido" / "Cancelar orden" recovery paths, all reusing the order-submission phase UX.

### Modified Capabilities
- `shopping-cart`: The "Render-only order and share actions" requirement narrows — "Compartir por WhatsApp" is no longer render-only; it now triggers the WhatsApp quote share defined by the `cart-whatsapp-share` capability.

## Impact

- **New code** — `core:domain` (`CartShare` domain model, `CartShareRepository` port, `ShareCartUseCase`, `CartShareErrorCategory`/`CartShareException` mirroring the order error taxonomy), `core:network` (share request/response DTOs, `CartShareApiService` for `POST /api/v1/carts/share-pdf`), `core:data` (`CartShareRepositoryImpl` reusing `SessionTokenProvider`'s DEVICE JWT + 401-retry-once, reusing the order mappers' phone normalization + item mapping, plus a Hilt binding).
- **Modified code** — `features:cart` (`CartViewModel` wires `ShareWhatsApp` through the existing `OrderSubmission` phase and `CreateOrder`-style flow; `finishAndReturnHome` also resets `submission = Idle`). The existing `CartDialogContent` submitting/success/error overlays and `OrderedLine` summary are **reused as-is** — no new screens.
- **API** — consumes `POST /api/v1/carts/share-pdf` (DEVICE auth); no backend changes.
- **Dependencies** — none new; reuses the existing Ktor client, session-token seam, order mappers, and `ClearCartUseCase`.
