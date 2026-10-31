## Why

When a search does not find what the customer wants, the only exits are "Volver" and "Nueva búsqueda", and both end on Home: the customer cannot jump to the full catalog (handoff `android-pendientes-v2.md` §5.10). At the same time, confirming "Sí, cerrar" returns Home but keeps the cart, so the next customer inherits it (QA 2026-10-25, CAR-040, ALTO; wave 1 only covered the 60 s inactivity timeout). The QA also confirmed that the Catalog network-error screen has no way to retry (A-12): the customer waits 30 s to be sent Home.

## What Changes

- **Search: new "Ir al catálogo" control** in the top bar, next to "Volver", "Nueva búsqueda" and the cart. It opens the catalog directly, with no dialog, and keeps the cart. "Nueva búsqueda" stays.
- **Closing a search, detail or catalog ends the customer session.** Confirming the close dialog ("Sí, cerrar") in Search, Product detail and Catalog, whether it was opened from "Nueva búsqueda", "Volver" or the system Back, clears the cart and the customer data (same operation as the inactivity timeout) before returning Home. The Catalog "Volver" with an empty cart, which goes Home without a dialog, also ends the session.
- **The close dialog warns about the cart.** With products in the cart, the dialog message says the cart will be emptied and how many products it has. With an empty cart, the current message is kept.
- **Catalog network-error screen gets "Reintentar"**, wired to the existing retry event.

Out of scope: "Ir al catálogo" on Product detail; KIO-032 (`idleShowcaseEnabled` / `autoSyncEnabled`, on hold until product answers); "Termina en X" with `discountEndsAt`; backend B-01/B-02 (order and PDF at full price).

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `product-search`: the top bar adds "Ir al catálogo"; confirming the close dialog ends the customer session; the dialog warns that the cart will be emptied.
- `product-catalog`: confirming the close dialog (and "Volver" with an empty cart) ends the customer session; the dialog warns about the cart; the network-error screen offers "Reintentar".
- `product-detail`: confirming the "Nueva búsqueda" dialog ends the customer session; the dialog warns about the cart.
- `customer-session`: closing a screen through its confirmation is a new path that ends the session through the same operation as the timeout and the order/share results.

## Impact

- **Code:** `features/product-search` (event, effect, top bar, dialog, navigation callback), `features/product-detail` and `features/catalog` (dialog message, session end on confirm), `features/catalog` (retry button), `app/MainActivity.kt` (wire "Ir al catálogo" navigation), `core/ui` (strings, catalog icon).
- **Domain:** reuses `EndCustomerSessionUseCase`; no new use case, repository or API call.
- **Tests:** ViewModel tests for the three features (session end on confirm, no session end on dismiss or on "Ir al catálogo", catalog retry).
- **QA to repeat:** BUS-032, DET-010, CAR-040, BUS-060, CAT-024/025, plus a network-error retry on Catalog.
