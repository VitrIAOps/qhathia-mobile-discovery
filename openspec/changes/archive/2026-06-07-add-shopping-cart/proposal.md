## Why

An in-store consumer can now search products and read a product's detail, but the discovery loop dead-ends there: the cart button on the search and detail screens is a no-op, "Agregar al carrito" does nothing, and there is no way to assemble a list of products to request from the merchant. This change adds the **shopping cart** — a session-scoped order-request basket — so a consumer can collect products, enter their contact data, and (when the backend lands) submit a purchase order. This is the step that turns discovery into a sale lead for the merchant.

## What Changes

- Introduce a **shopping-cart capability**: a session-scoped basket holding the products a consumer selected, their per-line quantities, and the computed subtotals/total. The basket is shared across every screen and survives navigation within a session.
- Make the existing **cart controls functional**:
  - The top-bar cart pill on product-search and product-detail (today a no-op) **opens the cart dialog** and **shows the total item count** as a badge.
  - The per-card cart button on search results and the detail **"Agregar al carrito"** button now **add the product to the basket** (the detail screen adds the quantity chosen in its stepper).
- Add the **cart dialog** following Figma node `758-25521`: title **"Carrito"** with the total item count as subtitle, a scrollable **list of cart items** (each with image, name, price, a quantity **stepper**, and a tap target to open that product's detail), a **receipt-styled totals section** (subtotals + total), a **consumer-data form** (name + cell phone, both required and validated), a **"Realizar pedido"** button, a **"Seguir buscando"** button, and a **"Compartir por WhatsApp"** button.
- Enforce the **business rules**: no limit on the number of distinct products; the per-product quantity cap derives from inventory stock; name and cell phone are required to enable the order; the name must contain at least one referential name; the cell phone must match the general mobile-number format.
- Add **session lifecycle rules**: the basket is **cleared when the order is placed** and **cleared after 1 minute of inactivity on any screen other than Home**.
- **Render-only (API pending), commented as TODO:** the **"Realizar pedido"** action and the **"Compartir por WhatsApp"** action perform no network/intent work — no cart or WhatsApp API exists yet.
- **Closing the dialog** (via "Seguir buscando" or the top "X") dismisses **only the dialog**, leaving the underlying screen and the basket intact. Tapping a cart item dismisses the dialog and navigates to that product's detail.

## Capabilities

### New Capabilities
- `shopping-cart`: The end-to-end cart experience — the session-scoped basket (domain model + in-memory session store observed across screens, with totals), adding products (from a search card, from the detail with a chosen quantity), per-line quantity stepping bounded by stock, removing lines, the cart dialog per Figma `758-25521` (item list, receipt-style totals, consumer-data form with name + cell-phone validation), the cart-count badge, the render-only "Realizar pedido" and "Compartir por WhatsApp" buttons (TODO until their APIs land), "Seguir buscando" (dismiss dialog), cart-item tap → product detail, and the session lifecycle (clear on order placed; clear after 1 minute of inactivity off the Home screen).

### Modified Capabilities
- `product-search`: The top-bar cart pill (previously a no-op) now opens the cart dialog and renders the live item-count badge; the per-result cart button (previously a no-op `CartTapped`) now adds that product to the basket.
- `product-detail`: The top-bar cart pill (previously a no-op `onCart`) now opens the cart dialog and renders the live item-count badge; the "Agregar al carrito" button (previously a render-only `AddToCart`) now adds the product with the quantity from the (now functional) stepper.

## Impact

- **New module:** `features:cart` (the cart dialog, `CartViewModel`, its UiState/UiEvent/UiEffect, consumer-form validation surface, and cart copy). Per the standards, feature modules cannot depend on each other, so shared pieces live in `core:*` and the dialog is hosted by `:app`.
- **Modules touched:**
  - `core:domain` — `CartItem` / `Cart` domain models (with computed totals), `CartRepository` port (observe + mutate the session basket), use cases (`AddToCartUseCase`, `ObserveCartUseCase`, `UpdateCartItemQuantityUseCase`, `RemoveCartItemUseCase`, `ClearCartUseCase`), consumer-data validation (`ValidateConsumerNameUseCase`, `ValidateConsumerPhoneUseCase`), and `CartConstants` (inactivity timeout, phone/name rules).
  - `core:data` — `CartSessionLocalDataSource` (`@Singleton` **in-memory** `StateFlow` holder — not DataStore, see design) + `CartRepositoryImpl` + Hilt wiring.
  - `core:ui` — a `CartButton` with a count badge (promoted/shared so both features render identical chrome), a `QuantityStepper` component, and the WhatsApp icon promoted from `features:product-detail` to `core:ui` for reuse.
  - `core:testing` — `FakeCartRepository`, cart fixtures.
  - `features:product-search` — wire `CartTapped` → add product; top-bar cart pill → open-cart effect; show the count badge.
  - `features:product-detail` — wire `AddToCart` → add product with stepper quantity; make the stepper functional; top-bar cart pill → open-cart effect; show the count badge.
  - `:app` — host the cart dialog as an overlay above the `NavHost`, observe the cart count, route open-cart effects from both features, wire cart-item tap → `productDetailRoute(id)`, and run the **1-minute off-Home inactivity timer** (reset on root user interaction) that clears the basket.
- **Build / tooling:** `settings.gradle.kts` registers `:features:cart`; `app/build.gradle.kts` depends on `:features:cart`; no new third-party dependencies (Compose, Hilt, immutable collections, Coil already present).
- **API:** none consumed in this change. "Realizar pedido" and "Compartir por WhatsApp" are render-only with explicit `TODO` markers until their endpoints/contracts exist.
- **Manifest:** no new permissions (the WhatsApp share intent is deferred; `INTERNET` / `ACCESS_NETWORK_STATE` already declared).
- **Out of scope / deferred:** the place-order API call, the WhatsApp share action/intent, product suggestions inside the cart, and any dedicated full-screen cart route (the cart is a dialog).
