# QA report — cart-inactivity-budget

**Legend:** ✔ works · ✖ fails (bug present) · ◐ partial · ⏸ blocked / not run · — not applicable.

| | |
|---|---|
| App (before) | `profile` on `490694e` (QA regression 2026-11-03) |
| App (after) | `profile` with this change (branch `fix/cart-inactivity-budget`) |
| Store | QA store `6b89ea64-d315-4294-9c69-cd412cd8b54a`, never El Martillo |

## Status

- **Handed to the QA team (2026-11-06):** the cases below. Verified with unit tests (`:app` 21, `:core:domain` 135), `lintDebug` and a `profile` build.

## Cases for the QA team

| Case | Steps | Expected after | Before (QA 03/11) | After | Who |
|---|---|---|---|---|---|
| D:CAR-042 (typing) | Cart open, type one letter every 9–30 s for more than 60 s | The cart is never emptied | ✔ | ⏸ | QA team |
| D:CAR-042 (idle) | Cart open, stop touching it | Still open at 60 s; at 180 s (±5) the session ends, Home is shown, the cart is empty and **no keyboard** is shown | ◐ closed at ~50 s, keyboard left over Home | ⏸ | QA team |
| Off Home, cart closed | Catalog or Search with items, idle | Session ends at 60 s, as before | ✔ | ⏸ | QA team |
| X:XPE-040 | As in the regression | No `POST /orders {"items":[]}` (unchanged) | ✔ | ⏸ | QA team |
| X:XPE-004 / X:XJO-009 / X:XPE-043 | — | **Not applicable to the tablet** (product decision 2026-11-06: the tablet only sends the order; the customer is notified on their phone) | — | — | — |

## Unit-test coverage of the delta spec

| Spec scenario (`customer-session`) | Test |
|---|---|
| Inactivity off Home ends the session | `MainViewModelTest.given_off_home_when_idle_for_60s_then_session_ends`, `cartDialogClosed_uses60s` |
| Open cart gets the longer budget | `cartDialogOpen_usesLongerBudget` |
| Typing restarts the cart budget | `cartActivity_restartsTheLongBudget` |
| No keyboard left over Home | `MainActivity` timeout collector (`hide()` + `clearFocus(force = true)`); verified by QA (D:CAR-042) |
| Inactivity on Home with items / Home with an empty basket | `given_home_with_items_when_idle_for_60s_then_session_ends`, `given_home_with_empty_basket_when_idle_then_never_ends` |
| Next customer starts clean | existing session-end tests (`CartViewModelTest`, `MainViewModelTest`) |
| Closing the cart goes back to 60 s | `closingTheCart_backTo60s` |
