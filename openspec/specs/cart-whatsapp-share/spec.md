# cart-whatsapp-share Specification

## Purpose

The cart-whatsapp-share capability lets an in-store consumer receive a written quote of their basket on their own phone: tapping "Compartir por WhatsApp" sends the basket and consumer data to the share endpoint, which generates a quote PDF and delivers it to the consumer's WhatsApp. It owns the share request assembly (identical body to the order request, cell-phone normalized to the 9-digit mobile format) and its validation gating, the blocking share/loading state, the success summary with return-to-Home-and-session-clear, the simplified error state with its "Entendido" / "Cancelar orden" recovery, the share-phase reset on session clear, and the automatic quote share that accompanies a successful "Realizar pedido". Sharing never creates a purchase order.
## Requirements
### Requirement: Share the cart as a WhatsApp quote

The app SHALL let the consumer share the cart as a WhatsApp quote by tapping **"Compartir por WhatsApp"**. The request SHALL send the consumer's name, the consumer's cell-phone number, and one line per cart product carrying its product identifier and chosen quantity. This action SHALL NOT create a purchase order — it requests a quote PDF that the backend delivers to the consumer's WhatsApp. The share SHALL be submitted only when the consumer-data form is valid.

#### Scenario: Share request carries the basket and consumer data

- **WHEN** the consumer taps "Compartir por WhatsApp" with a valid name, a valid cell-phone, and a non-empty basket
- **THEN** the app submits a share request containing the name, the cell-phone number, and each cart line as a product identifier with its quantity

#### Scenario: Cell-phone normalized before sending

- **WHEN** the consumer entered the cell-phone with spaces or a country prefix
- **THEN** the app normalizes it to the backend's required 9-digit mobile format before submitting

#### Scenario: Invalid data is never submitted

- **WHEN** the consumer-data form is invalid (missing or malformed name or cell-phone)
- **THEN** the "Compartir por WhatsApp" action does not submit a share request

### Requirement: Blocking share state

While a share request is in flight the cart dialog SHALL enter a blocking state: all input controls (form fields, quantity steppers, line removal, and the action buttons) SHALL be disabled, the background SHALL be dimmed/blurred, and a centered loading indicator SHALL be shown. The consumer SHALL NOT be able to submit the share a second time while a request is already in flight.

#### Scenario: Inputs blocked during share

- **WHEN** a share request is in flight
- **THEN** the dialog shows a centered loading indicator over a dimmed background
- **AND** the form fields, steppers, and buttons are disabled

#### Scenario: No duplicate submission

- **WHEN** a share request is already in flight and the consumer attempts to share again
- **THEN** no second share request is submitted

### Requirement: Successful share confirmation

A share SHALL be treated as successful only when the share endpoint answers with `status = SENT`. On a successful share the app SHALL replace the cart dialog with a success screen. The success screen SHALL show a summary of the shared products, each with its photo, name, and quantity. It SHALL show a title conveying that the quote was sent (for example "¡Cotización enviada!"), never a title that says an order was generated, a subtitle telling the consumer to go to the seller to receive the products and complete payment, and a sentence prompting the consumer to check their phone (where the WhatsApp quote arrives). The success screen SHALL present an **"Aceptar"** button that clears the session and returns to the Home screen.

#### Scenario: Success screen lists the shared products

- **WHEN** the share endpoint answers `status = SENT`
- **THEN** the app shows a success screen listing each shared product with its photo, name, and quantity

#### Scenario: Success screen guides the consumer

- **WHEN** the success screen is shown
- **THEN** it shows a quote-sent title, a subtitle to go to the seller, and a prompt to check the consumer's phone

#### Scenario: Share success never says an order was generated

- **WHEN** the success screen is shown after "Compartir por WhatsApp"
- **THEN** its title does not say that an order was generated

#### Scenario: Accept clears the session and returns Home

- **WHEN** the consumer taps "Aceptar" on the success screen
- **THEN** the cart basket and the consumer-data form are cleared
- **AND** the app returns to the Home screen

### Requirement: Share error recovery

On a share failure (rejected or failed after its retries) the app SHALL show a share error state, never the order message:

- **No connection:** "No pudimos enviar la cotización" with a message saying the tablet has no internet and the basket is kept.
- **Any other cause:** "No pudimos enviar la cotización" / "Hubo un problema enviando la cotización. Inténtalo de nuevo en unos momentos."

The share error state, and the not-delivered state, SHALL present a single action, **"Entendido"**, which SHALL return to the cart summary dialog in its prior state (basket and entered data preserved) so the consumer can retry. A share failure SHALL NOT offer "Cancelar orden" and SHALL NOT clear the basket or the consumer data. The order error state keeps its own copy and actions. Product rejections that name the product are handled by "Share rejections decided by error code".

#### Scenario: Share without network says so

- **WHEN** the tablet has no connection and the customer taps "Compartir por WhatsApp"
- **THEN** the share error state says the quote could not be sent because the tablet has no internet

#### Scenario: Other failures show the simplified share message

- **WHEN** the share request fails for another reason (validation, server, PDF generation)
- **THEN** the app shows the simplified share error message rather than the raw cause or the order message

#### Scenario: Entendido returns to the cart summary

- **WHEN** the consumer taps "Entendido" on the share error state
- **THEN** the app returns to the cart summary dialog with the basket and entered consumer data preserved

#### Scenario: No cancel action on a share failure

- **WHEN** the share error state or the not-delivered state is shown
- **THEN** no "Cancelar orden" action is offered, and the basket and consumer data are kept

#### Scenario: Order error keeps its own copy

- **WHEN** a "Realizar pedido" request fails
- **THEN** the order error state shows its order copy and actions, not the share message

### Requirement: Share phase reset on session clear

The share/success/error phase overlaid on the cart dialog SHALL be reset to the idle cart whenever the session is cleared and the flow returns Home. A subsequently reopened cart dialog SHALL show the normal, interactive cart rather than a lingering success or error screen.

#### Scenario: Reopened cart is not stuck on a success or error screen

- **WHEN** the consumer completes or cancels a share and later reopens the cart
- **THEN** the cart dialog shows the normal interactive cart, not the previous success or error screen

### Requirement: Quote shared automatically when placing an order

When the consumer places a purchase order ("Realizar pedido") successfully, the app SHALL also share the WhatsApp quote for the same basket and consumer data. The share SHALL be retried up to three attempts in total only while it fails for a transient reason (connectivity, timeout, or a 5xx answer). The order confirmation SHALL be shown once the order is created, regardless of the share outcome. When the quote is not delivered (the share answered `FAILED` or `STUB`, was rejected, or every attempt failed), the success screen SHALL replace the "check your phone" prompt with a fallback instruction telling the consumer to go to the seller and give their name to review the order.

#### Scenario: Placing an order also shares the quote

- **WHEN** the consumer places an order successfully
- **THEN** the app also submits a WhatsApp quote share for the same basket and consumer data

#### Scenario: Transient share failure retried before giving up

- **WHEN** the quote share fails with a connectivity error, a timeout, or a 5xx answer after the order was created
- **THEN** the app retries the share up to three attempts in total before giving up

#### Scenario: Order confirmed even when the quote is not delivered

- **WHEN** the order was created and the share answered `status = FAILED`
- **THEN** the app shows the order success screen without retrying the share
- **AND** the confirmation instructs the consumer to go to the seller and give their name, instead of prompting to check their phone

### Requirement: Share submission theming and accessibility

The share loading, success, and error states SHALL support light and dark themes and SHALL meet accessibility requirements. Their interactive controls (the "Aceptar", "Entendido", and "Cancelar orden" buttons) SHALL expose content descriptions where the label is not itself descriptive and provide touch targets of at least 48dp. The loading indicator SHALL expose a state description for assistive technology.

#### Scenario: Dark mode

- **WHEN** the device is in dark mode
- **THEN** the loading, success, and error states render with the dark theme

#### Scenario: Interactive controls are accessible

- **WHEN** an assistive technology inspects the success and error state buttons
- **THEN** each exposes a content description and a touch target of at least 48dp

### Requirement: Undelivered quote is never shown as sent

When the share endpoint answers `201` with `status = FAILED` or `status = STUB`, the app SHALL treat the quote as not delivered. "Compartir por WhatsApp" SHALL show the error state with a message saying the quote could not be sent to the consumer's WhatsApp and inviting them to go to the seller. The app SHALL NOT show the success screen or a "check your phone" prompt in that case.

#### Scenario: FAILED status shows the not-delivered message

- **WHEN** the consumer taps "Compartir por WhatsApp" and the endpoint answers `201` with `status = FAILED`
- **THEN** the app shows the error state with the not-delivered message
- **AND** the success screen is not shown

#### Scenario: STUB status is not shown as sent

- **WHEN** the endpoint answers `201` with `status = STUB`
- **THEN** the app treats the quote as not delivered

### Requirement: Deterministic share rejections are not retried

The app SHALL retry a share only after a connectivity error, a timeout, or a 5xx answer, up to three attempts in total. A 4xx answer (400, 404, 422) or a `201` with `status = FAILED` SHALL be final and SHALL NOT be retried.

#### Scenario: 400 is sent once

- **WHEN** the share endpoint answers 400
- **THEN** exactly one share request is sent

#### Scenario: 422 is sent once

- **WHEN** the share endpoint answers 422
- **THEN** exactly one share request is sent

#### Scenario: Timeout is retried

- **WHEN** the first two share attempts time out and the third answers `SENT`
- **THEN** three share requests are sent and the success screen is shown

### Requirement: Share rejections decided by error code

A share rejected with `409 PRODUCT_NOT_SELLABLE` naming a product in the basket SHALL flag that line "No disponible" directly, without a recheck. A share rejected with `422 STOCK_INSUFFICIENT` naming a product SHALL lower that line to `available` units (removing it at 0), show the stock notice, and return to the cart. Neither SHALL be retried.

#### Scenario: Not sellable share flags the line

- **WHEN** "Compartir por WhatsApp" answers `409 PRODUCT_NOT_SELLABLE` for a product in the basket
- **THEN** that line is shown as "No disponible", exactly one share request was sent, and no product request is sent

#### Scenario: Stock shortage on share

- **WHEN** "Compartir por WhatsApp" answers `422 STOCK_INSUFFICIENT` with `available` 2 for a line with 4 units
- **THEN** the line has 2 units, the stock notice is shown, and the cart is shown again

