## Context

The inactivity timer lives in `MainViewModel`. `combine(currentRoute, basketHasItems, activityTick)` decides with `isSessionTimed(route, hasItems)` whether the countdown runs. That is off Home always, and on Home only with items. It then `delay`s `INACTIVITY_TIMEOUT_SECONDS` (60 s), ends the customer session and emits `sessionTimeout`. `MainActivity` closes the overlays and pops to Home.

The timer does not know whether the cart dialog is open: the dialog is composed by `MainActivity` (`showCart`), outside the NavHost. Touches and typing inside it already count as activity (`reportUserActivity`, `LocalUserActivity`).

On timeout the dialog window is removed, but nothing hides the IME. QA saw the keyboard stay open over Home (D:CAR-042).

Modules involved: `core:domain` (constant) and `app` (shell). No new module.

## Goals / Non-Goals

**Goals:**
- A customer with the cart open is not cut off after 60 s, while an abandoned cart still resets within 3 minutes.
- No keyboard is left over Home after a timeout.

**Non-Goals:**
- Pausing the timer indefinitely: an abandoned cart must still clear the previous customer's data.
- Order status on the tablet (product decision; see the proposal).
- The Detail share dialog's budget.

## Decisions

### D1 — The budget depends on the cart dialog

`MainViewModel` gains `cartDialogOpen = MutableStateFlow(false)` and `fun onCartDialogVisibilityChanged(open: Boolean)`, which sets it and calls `onUserActivity()`. Opening or closing is an interaction, so the countdown restarts. The timer combines the four sources. While `isSessionTimed(route, hasItems)` holds, it waits `CART_DIALOG_INACTIVITY_SECONDS` (180) if the dialog is open, else `INACTIVITY_TIMEOUT_SECONDS` (60).

`isSessionTimed` does not change. The cart can only be opened off Home, or with items, so the dialog never makes an otherwise untimed screen timed.

`MainActivity.DiscoveryNavHost` reports it with `LaunchedEffect(showCart) { mainViewModel.onCartDialogVisibilityChanged(showCart) }`. Every close path (Dismiss, ReturnHome, navigate to detail, timeout) sets `showCart = false`, so the state follows.

- *Alternative:* the cart ViewModel reports its own visibility. Rejected: the dialog's lifetime is owned by the shell (`showCart`), and the ViewModel is Activity-scoped and outlives the dialog.

### D2 — Hide the keyboard on timeout

`DiscoveryNavHost` captures `LocalSoftwareKeyboardController.current` and `LocalFocusManager.current`. Before closing the overlays, the `sessionTimeout` collector calls `keyboardController?.hide()` and `focusManager.clearFocus(force = true)`.

## Risks / Trade-offs

- [`showCart` is `rememberSaveable`, but a process restart starts a new `MainViewModel` with `cartDialogOpen = false`] → The `LaunchedEffect(showCart)` runs on the first composition and reports the restored value.
- [Hiding the IME from the main window while the dialog window owns it] → `clearFocus(force = true)` plus `hide()` covers both; the dialog window is removed right after.

## Migration Plan

App-only. Rollback = revert. Verified with unit tests. The QA team re-runs D:CAR-042 (and X:XPE-040, unchanged).

## Open Questions

- None.
