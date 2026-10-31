## Context

- Search, Product detail and Catalog each own a close-confirmation dialog (`showCloseConfirmDialog` in their UiState, a private `CloseConfirmDialog` built on `core/ui` `ConfirmationDialog`, with feature strings `ps_close_dialog_*`, `pd_close_dialog_*`, `catalog_close_dialog_*`). Confirming emits `NavigateHomeIdle`; the app pops to Home (`goHome` in `MainActivity`). Nothing is cleared: the basket survives (CAR-040).
- Wave 1 (`qa-wave-1-critical-fixes`, D2) introduced `EndCustomerSessionUseCase` (clears the basket and emits `sessionEnded`, which resets the cart and share forms). It is called by the inactivity timer (`MainViewModel`) and by the cart result actions (`CartViewModel`).
- The three ViewModels already observe the cart (`ObserveCartUseCase` → `cartItemCount`, which is `Cart.totalItemCount`, i.e. units).
- The search top bar (`ProductSearchContent.TopBar`) is shown in every phase, including "unhappy" (no results / error), which is where "Ir al catálogo" helps most.
- Catalog: `CatalogUiEvent.Retry -> load()` exists, but `NetworkErrorBody` has no button (A-12). Product detail already renders a "Reintentar" button with `UiR.string.discovery_action_retry`.
- Navigation taps go through `navController.fromSettledScreen { … }` (INI-020: one tap, one navigation). The catalog route is `catalog/{source}`, built with `catalogRoute(source)`.

Product decisions (2026-10-30): keep both buttons in Search ("Ir al catálogo" + "Nueva búsqueda"); confirming the close ends the session in all three screens, and the dialog warns about the cart. KIO-032 is on hold.

## Goals / Non-Goals

**Goals:**
- From any search phase, one tap to the catalog, keeping the cart.
- Every explicit "close and go Home" ends the customer session with the same operation as the timeout.
- The customer is told before confirming that the cart will be emptied.
- The Catalog network-error screen can be retried.

**Non-Goals:**
- "Ir al catálogo" on Product detail or as a replacement for "Nueva búsqueda".
- Changing what "Volver" does (history steps, confirmation when there is no history) or the system Back mapping (`app-navigation`).
- Ending the session on the 30 s unhappy/error idle return to Home; the 60 s inactivity timer still covers a basket left on Home.
- KIO-032, `discountEndsAt`, B-01/B-02.

## Decisions

### D1. "Ir al catálogo" is a navigation effect, not a session event

`ProductSearchUiEvent.GoToCatalog` → cancel timers and voice jobs (same cleanup as closing, without clearing anything shared) → `ProductSearchUiEffect.NavigateToCatalog`. The screen hoists `onNavigateToCatalog`; `MainActivity` wires it as:

```kotlin
navController.fromSettledScreen {
    navigate(catalogRoute()) { popUpTo(HOME_ROUTE) { inclusive = false } }
}
```

`popUpTo(HOME)` makes the stack `Home → Catalog` whatever the path (Home → Search, or Home → Catalog → Search → Detail → Search). A Catalog already in the stack is replaced by a fresh one, so it reloads and starts on "Todo". "Volver" from that Catalog then follows its existing rule (Home directly with an empty cart, confirmation otherwise).

*Alternative rejected:* `popBackStack(CATALOG_ROUTE)` when the catalog is in the stack, else navigate. It keeps the old tab, but needs two code paths and leaves a stale catalog. A fresh load is cheap and predictable.

*Alternative rejected:* a dialog before going to the catalog. Nothing is lost (cart kept, history is per-search anyway), so a dialog would only add a tap.

### D2. Confirming a close ends the session in the ViewModel, before navigating

Each of the three ViewModels injects `EndCustomerSessionUseCase`. On `ConfirmCloseSearch`:

```kotlin
viewModelScope.launch {
    endCustomerSession()          // clear basket + emit sessionEnded
    emit(NavigateHomeIdle)
}
```

Ending first guarantees Home shows an empty basket and the form reset has already been broadcast. The Catalog "Volver" with an empty cart (which calls `confirmClose()` without a dialog) goes through the same function, so the customer data typed in a dialog is also cleared. It follows the `customer-session` rule that every path ends the session through the same operation.

*Alternative rejected:* end the session in `MainActivity` when `goHome` runs. `goHome` is also used by the unhappy 30 s idle return and by Settings, which must not clear anything; the decision belongs to the screen that knows the customer confirmed.

*Alternative rejected:* only clear when the basket is not empty. The name and phone can be typed with an empty basket (share dialog in Detail); clearing always is simpler and matches the timeout.

### D3. Dialog message depends on the cart

The dialog keeps its feature title. The message becomes:

- basket empty → current message (`Se cerrará la búsqueda y volverás al inicio.` / `Se cerrará el catálogo y volverás al inicio.`).
- basket not empty → one shared `plurals` resource in `core/ui` (the sentence is the same on the three screens), e.g. *"Se vaciará el carrito (2 productos) y volverás al inicio."* / *"(1 producto)"*.

The count is `cartItemCount`, already in each UiState and already shown on the cart badge, so the number in the warning matches the badge. The dialog composable gets `cartItemCount` as a parameter; no new state.

The confirm label stays "Sí, cerrar". The warning is in the message, which TalkBack reads with the title.

### D4. Catalog retry button

`NetworkErrorBody` gets an `onRetry` parameter and renders a button with `UiR.string.discovery_action_retry`, following the product-detail error body. The button sends `CatalogUiEvent.Retry`. `load()` already cancels the idle timer and shows loading. A failed retry re-enters the error state and restarts the 30 s timer.

### D5. Strings and icon

- `core/ui`: `discovery_action_go_to_catalog` = "Ir al catálogo" and `discovery_cd_go_to_catalog` = "Ir al catálogo". They are shared because Home has its own `home_catalog` and we do not touch it.
- New vector `ic_catalog` (Material "grid_view" outline) in `core/ui/res/drawable`, tinted like the other top-bar icons. If the Figma DS has a catalog icon, use that instead (see Open Questions).
- Shared `plurals` `discovery_close_dialog_message_with_cart` in `core/ui`, next to `discovery_dialog_confirm_close`.

## Risks / Trade-offs

- [The top bar gets crowded: Volver · Ir al catálogo · Nueva búsqueda · cart] → Landscape only (D12 of wave 1); check at 1024 dp and at the emulator's 2560×1600. Labels stay; no icon-only buttons (accessibility labels capability).
- [A customer taps "Sí, cerrar" by mistake and loses the cart] → That is the purpose of the explicit warning with the count; "Cancelar" keeps everything.
- [The fresh Catalog loses the previously selected tab] → Accepted (D1).
- [Ending the session runs while the share/order submission is in flight in an overlay] → Not reachable: the cart and share overlays cover the top bar and block outside taps while sending (CAR-043/044).

## Migration Plan

App-only change, no API or storage changes. Ships in the next build; rollback = revert the change.

## Open Questions

- Figma: is there a design for the 4-button top bar and a catalog icon in Qhatia-DS? If not, use `grid_view` and validate with design after the build.
- Should Product detail also get "Ir al catálogo"? Left out by decision; revisit after QA.
