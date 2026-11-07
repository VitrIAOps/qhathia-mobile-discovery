## 1. Domain (`core:domain`)

- [x] 1.1 `CartConstants.CART_DIALOG_INACTIVITY_SECONDS: Long = 180`, with KDoc; update the `INACTIVITY_TIMEOUT_SECONDS` KDoc to say it applies while the cart dialog is closed.

## 2. App shell (`app`)

- [x] 2.1 `MainViewModel`:
  - `cartDialogOpen = MutableStateFlow(false)`;
  - `fun onCartDialogVisibilityChanged(open: Boolean)` sets it and counts as activity;
  - the inactivity timer combines it and waits `CART_DIALOG_INACTIVITY_SECONDS` while it is open, else `INACTIVITY_TIMEOUT_SECONDS`;
  - update the class KDoc.
- [x] 2.2 `MainActivity.DiscoveryNavHost`:
  - `LaunchedEffect(showCart) { mainViewModel.onCartDialogVisibilityChanged(showCart) }`;
  - capture `LocalSoftwareKeyboardController.current` and `LocalFocusManager.current`, and in the `sessionTimeout` collector call `hide()` and `clearFocus(force = true)` before closing the overlays.
- [x] 2.3 `MainViewModelTest` (virtual time):
  - `cartDialogOpen_usesLongerBudget` (no end at 60 s, end at 180 s);
  - `cartDialogClosed_uses60s`;
  - `cartActivity_restartsTheLongBudget`;
  - `closingTheCart_backTo60s`.

## 3. Build and verify

- [x] 3.1 Run `:core:domain:test` and `:app:testDebugUnitTest`, one at a time, with JDK 21.
- [x] 3.2 `lintDebug` without new findings in touched files; `:app:assembleProfile` builds.
- [x] 3.3 `qa-report.md`:
  - the cases for the QA team (D:CAR-042: pauses while typing never empty the cart, 180 s idle with the dialog open ends the session and Home has no keyboard; X:XPE-040 unchanged);
  - X:XPE-004 / X:XJO-009 / X:XPE-043 marked not applicable (product decision);
  - the table mapping each scenario to its test.
  
  Mark the emulator run as handed to the QA team.
