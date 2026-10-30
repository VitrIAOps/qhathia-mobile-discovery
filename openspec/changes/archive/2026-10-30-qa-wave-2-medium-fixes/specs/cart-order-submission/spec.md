## ADDED Requirements

### Requirement: Cart dialog stays open while a request runs

While an order or share request from the cart is in flight, the cart dialog SHALL NOT be closed by an outside tap, the system Back or its close control. Once a result is shown, closing the dialog by any of those means SHALL run the result's main action: accepting the success screen (which ends the customer session and returns Home), or "Entendido" on an error (which returns to the cart). A result SHALL never be left pending for the next time the cart opens.

#### Scenario: Outside tap while sending

- **WHEN** an order request is in flight and the consumer taps outside the cart dialog
- **THEN** the dialog stays open with its loading indicator

#### Scenario: Back on the success screen

- **WHEN** the order success screen is shown and the consumer presses the system Back
- **THEN** the app behaves as if the consumer had tapped the accept button

#### Scenario: Reopening the cart after a result

- **WHEN** an order or share has finished and the cart is opened again
- **THEN** the cart shows its summary, not a previous result screen
