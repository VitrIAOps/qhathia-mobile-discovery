## Context

The app is multi-module (Clean Architecture + MVI, Hilt, Ktor, Compose). Two consumer-facing slices are complete: `features:product-search` (results/unhappy state machine, top-bar cart pill, per-card cart button) and `features:product-detail` (detail screen with a render-only "Agregar al carrito" button and a render-only quantity stepper). In **both** features the cart controls are deliberate no-ops today:

- `features:product-search` → `ProductSearchUiEvent.CartTapped(productId)` only resets the idle timer; the top-bar `onCart` lambda is `{ /* cart navigation out of scope */ }`.
- `features:product-detail` → `ProductDetailUiEvent.AddToCart` is a no-op `Unit` branch; the top-bar `onCart` is `{ /* cart navigation out of scope */ }`; `QuantityStepper` keeps purely local state.

Navigation is a single `NavHost` in `:app/MainActivity`; each feature contributes a `NavGraphBuilder` extension and hoists navigation targets as callbacks. The session pattern in the repo is `StoreSession` (a Preferences-DataStore-backed `@Singleton` repository observed as a `Flow`). Currency is the Peruvian Sol, formatted `S/ %.2f`. The search/detail screens already implement per-screen idle timers via `viewModelScope` + `delay`.

This change adds the **cart** as a session-scoped basket plus a **dialog** (Figma node `758-25521`) reachable from the cart controls on both screens, and wires the previously dead controls.

## Goals / Non-Goals

**Goals:**
- A `core:domain` cart model + ports + use cases (framework-free), a `core:data` **in-memory session** store, and a `features:cart` dialog following the project MVI contract.
- Make the existing cart controls functional: open the dialog, show the item-count badge, and add products (search card adds the product; detail adds the product with the stepper quantity).
- The cart dialog per Figma: item list with steppers and detail-tap, receipt-style totals, consumer-data form (name + phone, validated), and the "Realizar pedido" / "Seguir buscando" / "Compartir por WhatsApp" buttons.
- Session lifecycle: clear on order placed; clear after 1 minute of inactivity on any screen other than Home.

**Non-Goals:**
- The "Realizar pedido" network call and the "Compartir por WhatsApp" action/intent (render-only, `TODO` — no API exists).
- Product suggestions inside the cart.
- A dedicated full-screen cart route (the cart is a dialog overlay).
- Persisting the cart across process death (ephemeral by design — see Decisions).

## Decisions

### Decision: New `features:cart` module, dialog hosted by `:app`
Per the frontend standards every feature is its own Gradle module and feature modules may not depend on one another. The cart is a distinct capability used by **two** features, so it becomes `:features:cart` containing the cart dialog (`CartDialog` stateless content + a stateful host), `CartViewModel`, and its `UiState`/`UiEvent`/`UiEffect`. Because `product-search` and `product-detail` cannot depend on `features:cart`, the dialog is **hosted in `:app`** as an overlay above the `NavHost` (app already depends on every feature). The search/detail screens only *emit* an "open cart" intent that `:app` turns into showing the overlay.
- *Alternative considered:* a dedicated cart **nav destination**. Rejected — the task explicitly specifies a dialog ("Diálogo de carrito", "se debe cerrar solo el diálogo"), and a route would obscure the underlying screen and complicate "Seguir buscando" (dismiss-only).
- *Alternative considered:* put the dialog in `core:ui`. Rejected — it carries a ViewModel, validation, and business behavior, which `core:ui` must not own.

### Decision: In-memory `@Singleton` session store, not DataStore
The basket must be observable across every screen, must clear after 1 minute of inactivity, and must clear when an order is placed — i.e. it is **ephemeral session state**, not durable user data. A `@Singleton` `CartSessionLocalDataSource` holding a `MutableStateFlow<Cart>` (mirroring how `StoreSessionLocalDataSource` exposes a `Flow`, but in memory) is the right fit; `CartRepositoryImpl` delegates to it and is bound in a `core:data` Hilt module. This also makes "clear" trivial (reset the StateFlow) and avoids persisting a basket that the inactivity rule is meant to discard.
- *Alternative considered:* Preferences DataStore like `StoreSession`. Rejected — DataStore survives process death, contradicting the inactivity/clear semantics and adding serialization overhead for throwaway state.

### Decision: `Cart` / `CartItem` domain model with computed totals
`core:domain` gains `CartItem(productId, name, priceUnit, imageUrl, availability, quantity, maxQuantity)` and `Cart(items: List<CartItem>)` with derived helpers: `lineSubtotal = priceUnit * quantity`, `totalItemCount = sum(quantity)`, `totalAmount = sum(lineSubtotal)`. A `CartItem` is buildable from **both** the search `Product` and the detail `ProductDetail` (they share `id`, `name`, `price`, `availability`, and a primary image), so either entry point can add to the basket. Totals live in the domain model (pure, unit-testable), not the UI.
- *Alternative considered:* compute totals in the ViewModel. Rejected — totals are a business rule (receipt) and belong in the framework-free layer where they are trivially tested.

### Decision: Per-product quantity cap derives from stock — placeholder until inventory API exposes a count
The business rule caps quantity by inventory stock, but neither the search projection nor `GET /api/v1/products/{id}` returns a numeric stock count — only an `availability` enum (`AVAILABLE` / `LOW_STOCK` / `CONSULT`). `CartItem` therefore carries a `maxQuantity: Int` resolved from `availability` via a single `CartConstants` mapping, with an explicit `// TODO(inventory-stock): replace with real stock count when the API exposes it`. Initial mapping: `AVAILABLE` → `CartConstants.DEFAULT_MAX_QUANTITY` (a large placeholder cap), `LOW_STOCK` → `CartConstants.LOW_STOCK_MAX_QUANTITY` (a small cap), `CONSULT` → 1. The stepper "+" disables at `maxQuantity`. This keeps the rule expressible and testable today and isolates the one place to change when stock lands. *(See Open Questions.)*
- *Alternative considered:* allow unbounded quantity now. Rejected — it ignores the stated rule and would need a behavioral change later; a single mapped cap is cheaper to evolve.

### Decision: Add-to-cart entry points and quantity semantics
- **Search per-card cart button** (`CartTapped(productId)`): adds the product (qty +1; if already in the basket, increments by 1, bounded by `maxQuantity`). The card already has `name`/`price`/`imageUrl`/`availability` to build the `CartItem`.
- **Detail "Agregar al carrito"** (`AddToCart`): adds the loaded `ProductDetail` with the **stepper's** current quantity; the stepper becomes functional and bounded by `maxQuantity`.
- **Cart dialog steppers**: adjust an existing line's quantity (1..maxQuantity); decrementing below 1 removes the line.
Adding is idempotent on `productId` (same product merges into one line).

### Decision: Cross-feature "open cart" via a hoisted callback/effect, count via observed state
Each feature gains an `OpenCart` UiEffect (search and detail) surfaced through a new `onOpenCart` screen callback, exactly like the existing `onNavigateToDetail` wiring. `:app` collects it and flips an app-level `showCart` state to render the overlay. The **count badge** is driven by observing the cart: the shared `CartButton` takes an `itemCount` parameter; `:app` (and/or each screen) reads `ObserveCartUseCase` to supply it. The cart dialog's own content is driven by `CartViewModel` observing the same repository, so the badge and the dialog never diverge.
- *Alternative considered:* let each feature own the dialog. Rejected — duplicates the dialog three times and violates "no feature-to-feature dependency"; hosting once in `:app` is the established pattern for cross-cutting destinations.

### Decision: Cart-item tap closes the dialog and navigates to detail
Tapping a cart line emits a `NavigateToDetail(productId)` effect from `CartViewModel`; `:app` dismisses the overlay and calls `navController.navigate(productDetailRoute(id))`. "Seguir buscando" and the top "X" emit a `Dismiss` effect that only hides the overlay (basket untouched), satisfying "se debe cerrar solo el diálogo".

### Decision: Consumer-data validation lives in `core:domain` use cases
`ValidateConsumerNameUseCase` and `ValidateConsumerPhoneUseCase` return a small result (valid / specific error) so the dialog can show inline field errors and enable "Realizar pedido" only when both pass.
- **Name**: non-blank and contains at least one referential name token (≥1 alphabetic word of length ≥ 2).
- **Phone**: matches the general mobile format. Default to the Peruvian mobile rule (9 digits beginning with `9`, optionally prefixed by `+51`/`51`), centralized as `CartConstants.PHONE_REGEX` so it can be widened later. *(See Open Questions.)*

### Decision: 1-minute off-Home inactivity clear at the app level
Because the rule spans "any screen that isn't Home", it cannot live in one feature's ViewModel. `:app` wraps the `NavHost` in a root `Modifier.pointerInput` that resets an inactivity timer on any pointer interaction. The timer runs only while the current `NavController` destination is **not** `HOME_ROUTE`; on Home it is cancelled. On expiry it invokes `ClearCartUseCase` and returns to Home idle (consistent with the existing search 30s idle → Home behavior). This complements (does not replace) the existing per-screen search idle timer.
- *Alternative considered:* a `ProcessLifecycleOwner`/global timer in the session store. Rejected — "user interaction" and "current screen" are presentation concerns the Activity composition can observe directly and cheaply.

### Decision: Shared chrome promoted to `core:ui`
A `CartButton(itemCount, onClick, contentDescription)` with a count badge is added to `core:ui` so search and detail render identical chrome (each currently defines a private `CartButton`); those private copies are replaced. The WhatsApp icon (`ic_whatsapp`, today in `features:product-detail`) is promoted to `core:ui` so the cart's "Compartir por WhatsApp" button can reuse it. A reusable `QuantityStepper(quantity, onIncrease, onDecrease, canIncrease)` is added to `core:ui` (the detail screen and the cart dialog both need it).

## Risks / Trade-offs

- **[In-memory basket lost on process death]** A backgrounded/killed app loses the basket. → Acceptable and aligned with the ephemeral/inactivity-clear intent for a shared kiosk; documented as a non-goal.
- **[No real stock count]** The quantity cap is a placeholder mapping from `availability`. → Mitigation: isolate the mapping in `CartConstants` with a `TODO(inventory-stock)` so the swap is one-line; cover the mapping with unit tests.
- **[Cross-module dialog hosting]** Hosting the dialog in `:app` adds app-level state/overlay wiring. → Mitigation: follow the existing effect→callback pattern already used for detail navigation; keep all cart business logic in `CartViewModel`/domain, leaving `:app` as thin glue.
- **[Root interaction listener vs. gestures]** A root `pointerInput` for inactivity must not swallow child taps/scrolls. → Mitigation: use `awaitPointerEvent` in the `Initial` pass (observe-only, no consume) so it resets the timer without intercepting events.
- **[Phone format assumption]** Defaulting to the Peruvian mobile format may reject valid foreign numbers. → Mitigation: centralize the regex in `CartConstants`; revisit if multi-country support is required.
- **[Badge/dialog divergence]** Two observers of the cart could show different counts. → Mitigation: a single `@Singleton` source of truth (the in-memory store) observed by both; no local copies.

## Open Questions

Non-blocking (sensible defaults chosen above, isolated for easy change):
- **Inventory stock cap** — the API exposes only an `availability` enum, not a unit count, so the per-product cap is a placeholder mapping (`TODO(inventory-stock)`). The exact caps (`DEFAULT_MAX_QUANTITY`, `LOW_STOCK_MAX_QUANTITY`) are tunable constants. Confirm the intended caps when the inventory/stock count is available.
- **Phone format scope** — defaults to the Peruvian mobile rule (9 digits starting with `9`, optional `+51`). Confirm whether other countries must be accepted.
- **"Realizar pedido" / "Compartir por WhatsApp"** — render-only with `TODO` until their backends/contracts exist; the order payload (products + totals + consumer data) and the WhatsApp message format will be specified when those APIs land.
