## 1. Shared UI (D5)

- [x] 1.1 `core/ui`: strings `discovery_action_go_to_catalog` and `discovery_cd_go_to_catalog` ("Ir al catálogo"). Check the Figma DS for a catalog icon; otherwise add `ic_catalog` (Material `grid_view` outline vector) next to the other top-bar icons.
- [x] 1.2 Shared `plurals` `discovery_close_dialog_message_with_cart` in `core/ui` for the close dialog with a non-empty cart ("Se vaciará el carrito (%d producto/productos) y volverás al inicio.").

## 2. Search: "Ir al catálogo" (D1)

- [x] 2.1 `ProductSearchUiEvent.GoToCatalog` and `ProductSearchUiEffect.NavigateToCatalog`. The ViewModel cancels timers, voice jobs and the search job (no history or cart change) and emits the effect.
- [x] 2.2 `TopBar` adds the "Ir al catálogo" button between "Volver" and "Nueva búsqueda", in every phase. `ProductSearchScreen` / `productSearchScreen(...)` hoist `onNavigateToCatalog`.
- [x] 2.3 `MainActivity`: wire `onNavigateToCatalog` as `fromSettledScreen { navigate(catalogRoute()) { popUpTo(HOME_ROUTE) { inclusive = false } } }`.
- [x] 2.4 Unit tests: `GoToCatalog` emits `NavigateToCatalog`, does not open the dialog and does not end the session (the fake `EndCustomerSessionUseCase` dependencies are not called).

## 3. Close dialog ends the session (D2, D3)

- [x] 3.1 Search: inject `EndCustomerSessionUseCase`; `confirmCloseSearch()` calls it before emitting `NavigateHomeIdle`. `CloseConfirmDialog` takes `cartItemCount` and picks the plain or the cart-warning message.
- [x] 3.2 Product detail: same in `confirmCloseSearch()` and its `CloseConfirmDialog`.
- [x] 3.3 Catalog: same in `confirmClose()` (covers "Nueva búsqueda", "Volver" with items, and "Volver" with an empty cart) and its dialog.
- [x] 3.4 Unit tests per ViewModel: confirm → basket cleared and `sessionEnded` emitted before `NavigateHomeIdle`; dismiss → nothing cleared. Search: "Volver" with no history → dialog → confirm clears too. Catalog: "Volver" with an empty cart clears and goes Home without a dialog. Update the existing `NewSearch` tests (`ProductSearchViewModelTest` :200, :586; `ProductDetailViewModelTest` :213, :225; `CatalogViewModelTest` :149).
- [x] 3.5 Previews (light and dark) of each dialog with an empty cart and with 2 products.

## 4. Catalog "Reintentar" (D4): A-12

- [x] 4.1 `NetworkErrorBody(onRetry)` renders a "Reintentar" button (`UiR.string.discovery_action_retry`), following the product-detail error body; it sends `CatalogUiEvent.Retry`.
- [x] 4.2 Unit test: from the error state, `Retry` loads again; success shows the catalog; a second failure shows the error and restarts the 30 s timer. Update the network-error preview.

## 5. Build checks and emulator QA

- [x] 5.1 `./gradlew testDebugUnitTest` (JDK 21 JBR) green.
- [x] 5.2 `./gradlew lint assembleDebug assembleProfile` green.
- [x] 5.3 Emulator (paired API 26 AVD): BUS-032 ("Nueva búsqueda" with the warning), DET-010, CAR-040 (2 items → "Nueva búsqueda" → "Sí, cerrar" → Home → "Ir al catálogo": badge 0, cart form empty), BUS-060 and CAT-024/025 (Back = "Volver" still holds), "Ir al catálogo" from results and from the no-results screen (badge kept, one Catalog above Home, double tap opens one), Catalog without network → "Reintentar" with the network back. Check that the 4-button top bar fits at 1024 dp and 2560×1600 and that the label script shows no unlabelled controls.
  - Done 2026-10-30 on `Medium_Tablet`: every case passes (`qa-report.md`). The Catalog network error could not be produced (Wi-Fi cannot be turned off from adb on this image); "Reintentar" is covered by unit tests and previews.
- [x] 5.4 Write `qa-report.md` in the change and update `docs/handoff/qa-2026-10-25-casos-observados-por-flujo.md` (CAR-040, plus a row for A-12) and the Discovery status in `docs/handoff/android-pendientes-v2.md` (§5.10 done).
