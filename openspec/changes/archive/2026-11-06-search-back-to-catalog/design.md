## Context

`ProductSearchViewModel.onBack` opens the close dialog whenever `history.size <= 1`, without knowing which screen is below. A network error is an overlay that is not pushed to `history`, so with one result plus an error Volver also opens the dialog.

## Decisions

- **D1 — The screen decides the exit.** `onBack` emits `ProductSearchUiEffect.ExitRequested` instead of opening the dialog. `ProductSearchScreen` receives `previousIsHome: () -> Boolean`: if true it sends `NewSearch` (today's dialog, which ends the session on confirm); else `onNavigateBack()`. The view model stays free of navigation state.
- **D2 — `previousIsHome`** is `navController.previousBackStackEntry?.destination?.route == HOME_ROUTE`, read at the moment of Back, so it is right whatever path led to the search (Home, Catalog, Detail).
- **D3 — Network error.** When the phase is `NetworkError` and `history` is not empty, Volver shows `history.last()` without popping it.
- **D4 — Session.** Going back to the Catalog or Detail never ends the customer session (cart kept); only the dialog does, as today (CAR-040).

## Risks / Trade-offs

- [A search reached from Detail goes back to Detail, not Home] → Intended: it is the screen the customer came from.
