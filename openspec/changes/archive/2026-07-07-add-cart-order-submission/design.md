## Context

The cart is fully built across the existing modules (`features:cart`, `core:domain` cart models/use cases, `core:data` in-memory `CartRepository`). Today `CartViewModel.onEvent(PlaceOrder)` is a `TODO` no-op and the spec calls it render-only. Fase 13 shipped `POST /api/v1/orders` (DEVICE-authenticated, N items with quantity, server-side totals, `201` with the created order), so we can now implement the real submission.

All modules this change touches already exist and are registered in `settings.gradle.kts` (`:core:domain`, `:core:network`, `:core:data`, `:core:ui`, `:features:cart`, `:app`). **No new Gradle module is required** — this is a feature that layers onto existing modules following the established vertical-slice pattern (mirroring how product-detail is wired: `SessionTokenProvider` → `*ApiService` → `*RepositoryImpl` → `UseCase` → `ViewModel`).

Key existing seams reused:
- `SessionTokenProvider` (`core:data`) resolves the DEVICE JWT (persisted device token, dev-login fallback) and exposes the 401-retry-once + `clear()` pattern.
- `NetworkResult` + `toNetworkResult()` (`core:network`) classify Ktor outcomes without leaking Ktor types.
- `ClearCartUseCase` (`core:domain`) already empties the basket; the inactivity timer and `MainViewModel.clearCart()` already call it.
- `CartConstants.PHONE_REGEX` and the `ValidateConsumerName/PhoneUseCase` already encode the field rules.

## Goals / Non-Goals

**Goals:**
- Submit the basket + consumer data to `POST /api/v1/orders` and drive the loading/success/error UX from the result.
- Keep the domain framework-free and map DTOs/errors at the `core:data` boundary, consistent with the product-detail slice.
- Collapse every failure into one simplified consumer-facing message.
- Clear the session (cart + form) on success-accept and on error-cancel, returning to Home.

**Non-Goals:**
- WhatsApp share (stays a render-only `TODO`; subtask `86e22dq6y`).
- Order confirm/reject/list (merchant-side endpoints) — the kiosk only creates.
- Real per-product stock counts (the existing `CartConstants` availability→cap placeholder is untouched).
- Surfacing the specific `422 STOCK_INSUFFICIENT` / validation detail to the consumer (deliberately hidden behind the single message).

## Decisions

### Module placement (dependency graph)

```
features:cart ─┐
               ├─▶ core:domain (Order, OrderItem, OrderRepository, CreateOrderUseCase, OrderException)
core:data ─────┤        ▲
  OrderRepositoryImpl ──┘ implements
  (uses SessionTokenProvider + OrderApiService, maps DTO→domain)
        │
        ▼
core:network (OrderApiService, order DTOs)   app ─▶ features:cart (hosts the effect)
```

- **`core:domain`** (pure Kotlin): `Order` and `OrderItem` domain models; `OrderRepository` port with `suspend fun createOrder(name: String, phone: String, items: List<CartItem>): Result<Order>`; `CreateOrderUseCase`; an `OrderException`/`OrderErrorCategory` taxonomy mirroring `DetailException`. Because every failure collapses to one message, the category set can be minimal (e.g. a single `Generic`), but modeling it as an enum keeps parity with the codebase and room to differentiate later.
- **`core:network`**: `OrderDtos.kt` (`CreateOrderRequestDto`, `OrderItemRequestDto`, `OrderResponseDto`, `OrderItemResponseDto`, all `@Serializable`) and `OrderApiService.createOrder(accessToken, request)` doing `client.post(".../api/v1/orders")` with the `Authorization: Bearer` header and a per-request timeout, returning `NetworkResult<OrderResponseDto>` via `toNetworkResult()`.
- **`core:data`**: `OrderRepositoryImpl` resolves the token via `SessionTokenProvider`, submits, and on `401` does the `clear()` + `forceRefresh` retry-once exactly like `ProductDetailRepositoryImpl`; `OrderMappers.kt` builds the request from the `Cart` lines and maps the response to the domain `Order`; `OrderErrorMapper` folds `HttpError`/`NetworkError`/token failure into `OrderErrorCategory`; an `OrderDataModule` `@Binds` the repository. Registered as a new `@Module` (or added alongside `CartDataModule`).

**Alternative considered:** a dedicated `:features:order` or `:core:order` module. Rejected — the flow is an extension of the cart dialog, has no independent screen, and a feature module may not depend on another feature module. Keeping it in `features:cart` + the shared `core:*` layers matches the taxonomy and avoids a premature module.

### Phone normalization at the data boundary

The regex accepts `+51`/`51` prefixes and spaces, but the API requires exactly `^9\d{8}$`. `OrderMappers` strips whitespace and any leading `+51`/`51` before building the request, so the ViewModel keeps the raw user string and the network layer always sends the canonical form. Rationale: normalization is a serialization concern, kept out of domain/UI.

### UI state: an explicit submission phase on `CartUiState`

Add `val submission: OrderSubmission = OrderSubmission.Idle` to `CartUiState`, where `OrderSubmission` is a sealed interface: `Idle`, `Submitting`, `Success(val summary: ImmutableList<OrderedLine>)`, `Error`. The stateless `CartDialogContent` renders overlays purely from this phase — `Submitting` disables inputs + shows the blurred centered loader, `Success`/`Error` swap the dialog body — keeping the composable a pure function of state and trivially previewable (light/dark/each phase). `OrderedLine` carries photo/name/quantity captured from the basket at submit time so the summary survives the cart being cleared.

**Alternative considered:** a second ViewModel/dialog for the result screens. Rejected — one `CartViewModel` already owns the basket and form; a phase field is less machinery and keeps the one-shot `Dismiss`/navigation effects in one place.

### New events and effects

- `CartUiEvent`: add `AcceptSuccess` (Aceptar), `DismissError` (Entendido → back to summary = set phase `Idle`), `CancelOrder` (Cancelar orden). `PlaceOrder` stops being a no-op.
- `CartUiEffect`: add `ReturnHome` (return to Home clearing the session). `app`/`MainActivity` handles it by popping to `HOME_ROUTE` and setting `showCart = false`; the ViewModel calls `ClearCartUseCase` and the VM's form state dies with the dialog. Both success-accept and error-cancel emit `ReturnHome`.

**Session clearing:** the "session data" is the in-memory cart plus the transient form fields in `CartUiState`. Success-accept and error-cancel call `ClearCartUseCase` (clears the basket) and emit `ReturnHome`; the form state is discarded when the dialog/VM is torn down. This reuses the exact clearing path the inactivity timer already uses.

### ViewModel flow

`PlaceOrder` → guard `isOrderEnabled` and `submission == Idle` (prevents double submit) → capture the summary lines → set `Submitting` → `viewModelScope.launch { createOrder(name, phone, items) }` → on success set `Success(summary)`, on failure set `Error`. `AcceptSuccess`/`CancelOrder` → `clearCart()` + emit `ReturnHome`. `DismissError` → set `submission = Idle`.

## Risks / Trade-offs

- **Hiding the real error (esp. `422 STOCK_INSUFFICIENT`)** → the consumer only sees "try again", which for a genuine stock shortage never resolves. Mitigation: acceptable per the task's explicit "simplify all errors" rule; the merchant reconciles stock on confirm. Revisit if stock races prove common.
- **Duplicate orders from rapid taps / retry after a timed-out-but-succeeded request** → the request may have created an order the client never saw the `201` for. Mitigation: the `Submitting` phase blocks re-taps within a session; full idempotency would need a client order key the API doesn't yet accept — out of scope.
- **Summary shown after cart cleared** → mitigated by snapshotting `OrderedLine`s at submit time into the `Success` state rather than reading the (now-empty) basket.
- **`ReturnHome` effect vs. dialog host coupling** → the cart is hosted in `MainActivity`, not a nav destination; the effect must both pop to Home and dismiss the overlay. Mitigation: handle both in the single `CartDialog` effect collector callback, consistent with the existing `NavigateToDetail` handling.

## Open Questions

- Should `customerNote`/`sessionId` (optional API fields) be populated now? Current plan: omit both (no UI for a note; no session id plumbed to the cart). Trivial to add later — the DTO fields are nullable.
- Does the DEVICE JWT reliably carry the store scope in the current dev-login fallback? The product-detail slice relies on the same seam, so this rides on its behavior; flagged only because orders are a write.
