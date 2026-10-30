## MODIFIED Requirements

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

## ADDED Requirements

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
