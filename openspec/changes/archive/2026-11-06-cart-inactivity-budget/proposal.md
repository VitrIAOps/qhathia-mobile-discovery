## Why

The customer-session timer ends the session after 60 s without activity on every screen (`CartConstants.INACTIVITY_TIMEOUT_SECONDS`), including the cart dialog. In the QA regression of 2026-11-03 (D:CAR-042, F-35), a customer who stopped typing in the cart lost the basket after about 50 s, and the keyboard stayed open over Home. The regression asks for 180 s while the cart dialog is open: a customer reviewing the basket, filling in the form or reading the order confirmation is not idle the way an abandoned tablet is.

The order's later status (confirmed, rejected, expired) is **not** the tablet's concern. The tablet only sends the order; the customer is notified on their phone. So F-26 (polling `GET /orders/{id}/status`) is out of scope by product decision, and X:XPE-004, X:XJO-009 and X:XPE-043 do not apply to the tablet.

## What Changes

- **Longer inactivity budget with the cart open.** While the cart dialog is open (cart, form, order or share result), the customer session ends after **180 s** without activity instead of 60 s. Every other screen keeps 60 s. Opening or closing the dialog counts as activity. **BREAKING** (behavior) for the "60 seconds" rule of `customer-session`.
- **Keyboard hidden on timeout.** When the session ends on inactivity, the soft keyboard is hidden and input focus cleared before going Home.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `customer-session`: the inactivity budget is 180 s while the cart dialog is open (60 s elsewhere), and the keyboard is hidden when the session times out.

## Impact

- `core:domain`: `CartConstants.CART_DIALOG_INACTIVITY_SECONDS = 180`; KDoc of `INACTIVITY_TIMEOUT_SECONDS`.
- `app`:
  - `MainViewModel` gains a `cartDialogOpen` state and `onCartDialogVisibilityChanged(open)`, and its timer picks the budget;
  - `MainActivity` reports the cart dialog visibility and hides the keyboard in the timeout collector.
- No new module, library, manifest or API change.
- Out of scope:
  - order status on the tablet (F-26, product decision: the customer is notified on their phone);
  - the same budget for the Detail share dialog (optional in the regression).
