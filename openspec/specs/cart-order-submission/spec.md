# cart-order-submission Specification

## Purpose

The cart-order-submission capability lets an in-store consumer turn the cart basket into a real purchase order. It owns the "Realizar pedido" submission flow: building the order request from the consumer-data form and the basket (name, cell-phone number, and one line per product with its identifier and quantity), the blocking in-flight state over the cart dialog, the success confirmation screen that summarizes the ordered products and guides the consumer to the seller, and the simplified error-recovery state with retry ("Entendido") and cancel ("Cancelar orden") actions. The server owns subtotal and total — the client never computes or trusts the order total. All of its loading, success, and error states support light/dark theming and accessibility.
## Requirements
### Requirement: Generate a purchase order from the cart

The app SHALL let the consumer generate a purchase order from the cart basket by tapping **"Realizar pedido"**. The request SHALL send the consumer's name, the consumer's cell-phone number, and one line per cart product carrying its product identifier and chosen quantity. The order total SHALL NOT be computed or trusted on the client — the server owns subtotal and total. The order SHALL be submitted only when the consumer-data form is valid.

#### Scenario: Order request carries the basket and consumer data

- **WHEN** the consumer taps "Realizar pedido" with a valid name, a valid cell-phone, and a non-empty basket
- **THEN** the app submits an order request containing the name, the cell-phone number, and each cart line as a product identifier with its quantity

#### Scenario: Cell-phone normalized before sending

- **WHEN** the consumer entered the cell-phone with spaces or a country prefix
- **THEN** the app normalizes it to the backend's required 9-digit mobile format before submitting

#### Scenario: Invalid data is never submitted

- **WHEN** the consumer-data form is invalid (missing or malformed name or cell-phone)
- **THEN** the "Realizar pedido" action does not submit an order request

### Requirement: Blocking submission state

While an order request is in flight the cart dialog SHALL enter a blocking state: all input controls (form fields, quantity steppers, line removal, and the action buttons) SHALL be disabled, the background SHALL be dimmed/blurred, and a centered loading indicator SHALL be shown. The consumer SHALL NOT be able to submit the order a second time while a request is already in flight.

#### Scenario: Inputs blocked during submission

- **WHEN** an order request is in flight
- **THEN** the dialog shows a centered loading indicator over a dimmed background
- **AND** the form fields, steppers, and buttons are disabled

#### Scenario: No duplicate submission

- **WHEN** an order request is already in flight and the consumer attempts to place the order again
- **THEN** no second order request is submitted

### Requirement: Successful order confirmation

On a successful order creation the app SHALL replace the cart dialog with a success screen. The success screen SHALL show a summary of the ordered products, each with its photo, name, and quantity. It SHALL show a title conveying that the order was created successfully, a subtitle telling the consumer to go to the seller to receive the order and complete payment, and a sentence prompting the consumer to check their phone. The success screen SHALL present an **"Aceptar"** button that clears the session and returns to the Home screen.

#### Scenario: Success screen lists the ordered products

- **WHEN** the order is created successfully
- **THEN** the app shows a success screen listing each ordered product with its photo, name, and quantity

#### Scenario: Success screen guides the consumer

- **WHEN** the success screen is shown
- **THEN** it shows a success title, a subtitle to go to the seller to receive the order and pay, and a prompt to check the consumer's phone

#### Scenario: Accept clears the session and returns Home

- **WHEN** the consumer taps "Aceptar" on the success screen
- **THEN** the cart basket and the consumer-data form are cleared
- **AND** the app returns to the Home screen

### Requirement: Order error recovery

On any order-generation failure the app SHALL show an error state with a single simplified message ("Hubo un problema generando la orden. Inténtalo de nuevo en unos momentos") regardless of the underlying cause. The error state SHALL present two actions: **"Entendido"**, shown with the higher visual priority, and **"Cancelar orden"**. Tapping "Entendido" SHALL return to the cart summary dialog in its prior state (basket and entered data preserved) so the consumer can retry. Tapping "Cancelar orden" SHALL return to the Home screen, clearing the session.

#### Scenario: All failures show the same simplified message

- **WHEN** the order request fails for any reason (validation, not found, server, or connectivity)
- **THEN** the app shows the single simplified error message rather than the raw cause

#### Scenario: Entendido returns to the cart summary

- **WHEN** the consumer taps "Entendido" on the error state
- **THEN** the app returns to the cart summary dialog with the basket and entered consumer data preserved

#### Scenario: Cancelar orden returns Home clearing the session

- **WHEN** the consumer taps "Cancelar orden" on the error state
- **THEN** the app returns to the Home screen
- **AND** the cart basket and the consumer-data form are cleared

#### Scenario: Entendido has the higher priority

- **WHEN** the error state is shown
- **THEN** the "Entendido" action is presented with a higher visual priority than "Cancelar orden"

### Requirement: Order submission theming and accessibility

The order loading, success, and error states SHALL support light and dark themes and SHALL meet accessibility requirements. Their interactive controls (the "Aceptar", "Entendido", and "Cancelar orden" buttons) SHALL expose content descriptions where the label is not itself descriptive and provide touch targets of at least 48dp. The loading indicator SHALL expose a state description for assistive technology.

#### Scenario: Dark mode

- **WHEN** the device is in dark mode
- **THEN** the loading, success, and error states render with the dark theme

#### Scenario: Interactive controls are accessible

- **WHEN** an assistive technology inspects the success and error state buttons
- **THEN** each exposes a content description and a touch target of at least 48dp

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

