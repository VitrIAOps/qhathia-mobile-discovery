## Context

The `cart-order-submission` slice (archived `2026-07-07`) already built the full submission-lifecycle UX in the cart dialog: an `OrderSubmission` phase (`Idle`/`Submitting`/`Success`/`Error`) on `CartUiState`, the blocking `SubmittingOverlay`, the `OrderSuccessContent` summary (photo/name/quantity + success title + "go to the seller" subtitle + "check your phone" prompt), the `OrderErrorContent` recovery screen ("Entendido" higher-priority + "Cancelar orden"), the `OrderedLine` snapshot model, and the return-to-Home-with-session-clear path. In that slice `CartViewModel.onEvent(ShareWhatsApp)` was deliberately left a no-op `TODO` because the share backend did not exist.

It now exists: `POST /api/v1/carts/share-pdf` (api-contract §9.5, DEVICE-authenticated) takes the **exact same request body as `POST /api/v1/orders`** (`customerName`, `customerPhone` `^9\d{8}$`, `items[{productId, quantity}]`), generates a quote PDF, uploads it to R2, and sends it to the consumer's WhatsApp. It does **not** create an order. It returns `201 { status, messageId, pdfUrl, error }` where `pdfUrl` is always present (fail-soft), or a non-`201` error (`400/404/422/503/403/401`).

All modules this change touches already exist and are registered in `settings.gradle.kts` (`:core:domain`, `:core:network`, `:core:data`, `:core:ui`, `:features:cart`, `:app`). **No new Gradle module is required** — this layers a second vertical slice onto the same modules, mirroring the order slice exactly (`SessionTokenProvider` → `*ApiService` → `*RepositoryImpl` → `UseCase` → `ViewModel`).

Key existing seams reused verbatim:
- `SessionTokenProvider` (`core:data`) — DEVICE JWT + `401` `clear()`/`forceRefresh` retry-once + `clear()`.
- `NetworkResult` + `toNetworkResult()` (`core:network`) — classify Ktor outcomes without leaking Ktor types.
- `buildCreateOrderRequest` / `String.toApiMobileNumber()` (`core:data` `OrderMappers.kt`) — the share request body is byte-for-byte the order request body, so the phone-normalization + item-mapping logic is reused.
- The whole `OrderSubmission` phase and the `CartDialogContent` overlays/`OrderedLine` — the share flow drives the **same** phase and renders the **same** success/error/loading screens.
- `ClearCartUseCase` (`core:domain`) — empties the basket on accept/cancel.

## Goals / Non-Goals

**Goals:**
- Wire the existing "Compartir por WhatsApp" button to `POST /api/v1/carts/share-pdf` and drive the existing loading/success/error UX from the result.
- Reuse the order slice's phase machinery and screens rather than adding a second, parallel UI.
- Collapse every failure into the one simplified consumer-facing message.
- Fix the lingering-phase bug so a reopened cart never shows a stale success/error screen.

**Non-Goals:**
- Differentiating the `share-pdf` `status` (`SENT`/`STUB`/`FAILED`) in the UI, or surfacing/opening the returned `pdfUrl` as an in-app fallback (all `201`s are treated as success — see Open Questions).
- Any change to the "Realizar pedido" order flow, its DTOs, or its endpoint.
- New success/error copy or new screens — the existing order-submission screens are reused as-is.
- Renaming `OrderSubmission`/`OrderedLine`/`OrderSuccessContent` to share-neutral names (kept as-is to minimize churn; see Trade-offs).

## Decisions

### Module placement (dependency graph)

No new module. A share slice mirrors the order slice across the same modules:

```
features:cart ─┐
               ├─▶ core:domain (CartShare, CartShareRepository, ShareCartUseCase, CartShareException)
core:data ─────┤        ▲
  CartShareRepositoryImpl ──┘ implements
  (uses SessionTokenProvider + CartShareApiService, reuses OrderMappers for the request body)
        │
        ▼
core:network (CartShareApiService, share DTOs)
```

- **`core:domain`** (pure Kotlin): `CartShare` domain model (`status`, `pdfUrl`, `messageId`); `CartShareRepository` port with `suspend fun shareCart(name: String, phone: String, items: List<CartItem>): Result<CartShare>`; `ShareCartUseCase`; a `CartShareErrorCategory` enum + `CartShareException` mirroring `OrderErrorCategory`/`OrderException`. A single generic category suffices given the simplified message, but the enum keeps parity and room to differentiate later.
- **`core:network`**: `CartShareDtos.kt` (`@Serializable` `ShareCartRequestDto`, `ShareCartItemRequestDto`, `ShareCartResponseDto`) and `CartShareApiService.shareCart(accessToken, request)` doing `client.post("${QHATHIA_BASE_URL}/api/v1/carts/share-pdf")` with `Authorization: Bearer`, a per-request timeout, returning `NetworkResult<ShareCartResponseDto>` via `toNetworkResult()`.
- **`core:data`**: `CartShareRepositoryImpl` resolves the token via `SessionTokenProvider`, submits via `CartShareApiService`, and on `401` does the `clear()` + `forceRefresh` retry-once exactly like `OrderRepositoryImpl`; a `CartShareMappers.kt` builds the request (delegating to `buildCreateOrderRequest`'s field logic / `toApiMobileNumber()`) and maps `ShareCartResponseDto → CartShare`; a `CartShareErrorMapper` folds `HttpError`/`NetworkError`/token failure into `CartShareErrorCategory`; a Hilt `@Binds` (extend `OrderDataModule` or add a `CartShareDataModule`).

**Reuse of the order request body:** because `share-pdf` and `orders` accept the identical body, the share DTOs could technically reuse `CreateOrderRequestDto`. Decision: keep a distinct `ShareCartRequestDto` (parity with the slice structure and decoupling if the two endpoints diverge), but reuse `String.toApiMobileNumber()` and the item-mapping logic from `OrderMappers` so the normalization rule lives in exactly one place.

**Alternative considered:** a dedicated `:features:cart-share` or `:core:share` module. Rejected for the same reasons as the order slice — the flow is an extension of the cart dialog with no independent screen, and a feature module may not depend on another feature module.

### Reuse the `OrderSubmission` phase for the share flow

The share flow's loading/success/error UX is identical to the order flow's, and the two are mutually exclusive (one button tap at a time). So the share drives the **same** `CartUiState.submission: OrderSubmission` field and renders the **same** `CartDialogContent` overlays. `ShareWhatsApp` gets a `shareCart()` handler mirroring `placeOrder()`: guard `isOrderEnabled` and `submission == Idle` (blocks double-submit and blocks sharing mid-order), snapshot the `OrderedLine` summary from the basket, set `Submitting`, `viewModelScope.launch { shareCart(name, phone, items) }`, then set `Success(summary)` or `Error`. `AcceptSuccess`/`CancelOrder`/`DismissError` already do the right thing for both flows.

**Alternative considered:** a separate `ShareSubmission` phase and a second set of success/error composables. Rejected — it would duplicate the entire overlay UI for pixel-identical screens the task explicitly asks to reuse.

### Fix the lingering submission phase on session clear

Today `finishAndReturnHome()` clears the cart and emits `ReturnHome` but leaves `submission` at `Success`/`Error`; since the `CartViewModel` outlives the dismissed dialog, reopening the cart shows the stale screen ("el carrito se queda con mensaje de éxito tras envío"). Fix: `finishAndReturnHome()` also resets `submission = OrderSubmission.Idle`. This corrects the same latent bug for the order flow. No new events/effects are needed.

### Treat every `201` as success

`share-pdf` is fail-soft: a `201` always carries a `pdfUrl` even when `status = FAILED` (WhatsApp delivery failed but the PDF exists). Given the task's single-success/single-error model and the "simplify all errors" rule inherited from the order slice, the repository maps any `201` to `Result.success(CartShare(...))` and any non-`201`/transport failure to `Result.failure(CartShareException(...))`. The `status` and `pdfUrl` are carried on the domain `CartShare` but not surfaced yet (see Open Questions).

## Risks / Trade-offs

- **`status = FAILED` shown as success** → on a `201/FAILED` the consumer sees "check your phone" but no WhatsApp message actually arrived. Mitigation: acceptable for a first cut per the simplified-UX rule; the `status`/`pdfUrl` are already on the domain model so a later change can branch on them (e.g. offer the `pdfUrl`). Flagged as an Open Question.
- **Naming drift** → reusing `OrderSubmission`/`OrderedLine`/`OrderSuccessContent`/the `cart_success_*` strings for a share that creates no order is slightly misleading in code and copy ("go to the seller to receive the order"). Mitigation: the task explicitly asks to reuse the order-submission flow and its screens; renaming to share-neutral identifiers is deferred to avoid churn across a shipped slice.
- **Double action across the two buttons** → the `submission == Idle` guard already prevents starting a share while an order is submitting (and vice-versa), since both share one phase field. No extra guarding needed.
- **Duplicate sends from a timed-out-but-succeeded request** → same as the order slice; the `Submitting` phase blocks re-taps within a session, full idempotency is out of scope (the endpoint accepts no client key).

## Open Questions

- Should `status = FAILED` (PDF generated but WhatsApp not delivered) be treated differently — e.g. a distinct message or an in-app "ver PDF" using `pdfUrl`? Current plan: no, treat all `201`s as success; revisit if Meta test-mode delivery failures prove common.
- Should the success copy be forked for the share flow (quote vs. order wording) rather than reusing the `cart_success_*` strings? Current plan: reuse; trivial to fork later if product wants share-specific wording.
