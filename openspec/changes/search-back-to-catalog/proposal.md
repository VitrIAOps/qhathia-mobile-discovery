## Why

A search opened from the Catalog ends on Home: with only one result, "Volver" and system Back open "¿Cerrar la búsqueda actual?", and confirming it empties the cart (QA regression 2026-11-03, F-38 / D:BUS-060). The customer expected to be back on the Catalog with the cart intact. Also, after a network error over a result, Volver opens the dialog instead of returning to that result (D:BUS-052).

## What Changes

- **Volver goes where the customer came from.** With no previous result, the search asks the screen to exit: if Home is below, it opens today's close dialog; otherwise (Catalog, Detail) it goes back there and keeps the cart and the session.
- **Back after a network error** returns to the result shown before the error.
- System Back behaves like Volver, as today.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `product-search`: with no previous result, Volver returns to the previous screen; only from Home it opens the close dialog. After a network error it returns to the previous result.

## Impact

- `features:product-search`: `ProductSearchUiEffect.ExitRequested`, `ProductSearchViewModel.onBack`, `ProductSearchScreen` / `productSearchScreen(previousIsHome)`; tests.
- `:app`: `MainActivity` passes `previousIsHome`.
- No new module, library or manifest change.
