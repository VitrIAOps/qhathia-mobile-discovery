## ADDED Requirements

### Requirement: Order carries the price the customer saw

Each line of the order request SHALL carry `expectedUnitPrice`: the unit price shown in the cart, rounded to 2 decimals (half up). When the server answers `409` with `errorCode` `PRICE_CHANGED`, no order exists. The app SHALL then:

- set each listed line to its `currentUnitPrice`;
- show the price-change notice;
- return to the cart, with the basket and consumer data kept, so the customer confirms the new total with another "Realizar pedido".

This SHALL NOT be shown as an error.

#### Scenario: Price changed while the cart was open

- **WHEN** the cart shows S/ 210.00 for a product, the seller changes it to S/ 199.00, and the customer taps "Realizar pedido"
- **THEN** the request carries `expectedUnitPrice` 210.00 and the server answers `409 PRICE_CHANGED`
- **AND** the cart is shown again with the line at S/ 199.00, the new total, and the price-change notice, with no error screen

#### Scenario: Second tap creates the order at the shown price

- **WHEN** after the price-change notice the customer taps "Realizar pedido" again
- **THEN** the request carries `expectedUnitPrice` 199.00 and the order is created with the total the cart shows

### Requirement: Product rejections decided by error code

The app SHALL decide what an order rejection means from its `errorCode`, not from its HTTP status:

- `PRODUCT_NOT_SELLABLE` with a `productId` in the basket SHALL flag that line "No disponible" directly.
- `STOCK_INSUFFICIENT` with a `productId` SHALL lower that line to `available` units (removing it at 0), show the stock notice, and return to the cart.
- `VALIDATION_ERROR` SHALL show the validation error state.

#### Scenario: Not sellable flags the line without a recheck

- **WHEN** the order answers `409 PRODUCT_NOT_SELLABLE` for a product in the basket
- **THEN** that line is shown as "No disponible", the order and share actions are disabled, and no product request is sent

#### Scenario: Stock shortage lowers the quantity

- **WHEN** the basket asks for 5 units, and the order answers `422 STOCK_INSUFFICIENT` with `available` 3
- **THEN** the line has 3 units, the stock notice is shown, and the cart is shown again without an error screen

## MODIFIED Requirements

### Requirement: Successful order confirmation

On a successful order creation the app SHALL replace the cart dialog with a success screen. The success screen SHALL show a summary of the ordered products as the server recorded them, each with its photo, name, quantity and line total, and the order total the server recorded ("Total del pedido S/ X.XX"). If that total differs from the total the cart showed by S/ 0.01 or more, the screen SHALL add a note asking the customer to confirm the total with the seller.

The success screen SHALL show:
- a title conveying that the order was created successfully;
- a subtitle telling the consumer to go to the seller to receive the order and complete payment;
- a sentence prompting the consumer to check their phone.

It SHALL present an **"Aceptar"** button that clears the session and returns to the Home screen.

#### Scenario: Success screen lists the ordered products

- **WHEN** the order is created successfully
- **THEN** the app shows a success screen listing each ordered product with its photo, name, quantity and line total

#### Scenario: Success screen shows the recorded total

- **WHEN** an order with a 20 % discounted product (S/ 100.00 → S/ 80.00) is created
- **THEN** the success screen shows "Total del pedido S/ 80.00", the total the server recorded

#### Scenario: Different total is flagged

- **WHEN** the server records a total different from the one the cart showed
- **THEN** the success screen shows the recorded total and a note to confirm it with the seller

#### Scenario: Success screen guides the consumer

- **WHEN** the success screen is shown
- **THEN** it shows a success title, a subtitle to go to the seller to receive the order and pay, and a prompt to check the consumer's phone

#### Scenario: Accept clears the session and returns Home

- **WHEN** the consumer taps "Aceptar" on the success screen
- **THEN** the cart basket and the consumer-data form are cleared
- **AND** the app returns to the Home screen

### Requirement: Order error recovery

When an order fails, the app SHALL show an error state that depends on the cause:

- **No connection:** title "Perdí la conexión…", a message saying the tablet has no internet and the basket is kept, a primary **"Reintentar"** that sends the same order once, and a secondary **"Volver al carrito"**. It SHALL NOT offer "Cancelar orden".
- **Timeout:** a message saying the app could not confirm whether the order was recorded and that the customer should ask the seller before trying again, with a single **"Entendido"**. The app SHALL NOT offer to retry, because the order may exist.
- **Validation (`422 VALIDATION_ERROR`):** a message for the rejected field (name, phone, or basket), with a single **"Entendido"**.
- **Any other cause:** the simplified message ("Hubo un problema generando la orden. Inténtalo de nuevo en unos momentos"), with **"Entendido"** shown with the higher visual priority, and **"Cancelar orden"**.

"Entendido" and "Volver al carrito" SHALL return to the cart summary dialog in its prior state (basket and entered data preserved). "Cancelar orden" SHALL return to the Home screen, clearing the session. Price changes and product rejections are handled by their own requirements and do not show this error state.

#### Scenario: No network offers a retry

- **WHEN** the tablet has no connection and the customer taps "Realizar pedido"
- **THEN** the error state says the connection was lost and offers "Reintentar" and "Volver al carrito", without "Cancelar orden"

#### Scenario: Retry sends the order once

- **WHEN** the connection is back and the customer taps "Reintentar"
- **THEN** exactly one order request is sent

#### Scenario: Timeout is not retried

- **WHEN** the order request times out
- **THEN** the error state tells the customer to ask the seller whether the order was recorded, offers only "Entendido", and no further order request is sent

#### Scenario: Other failures show the simplified message

- **WHEN** the order request fails for another reason (server error, unknown)
- **THEN** the app shows the simplified error message with "Entendido" and "Cancelar orden"

#### Scenario: Entendido returns to the cart summary

- **WHEN** the consumer taps "Entendido" on the error state
- **THEN** the app returns to the cart summary dialog with the basket and entered consumer data preserved

#### Scenario: Cancelar orden returns Home clearing the session

- **WHEN** the consumer taps "Cancelar orden" on the error state
- **THEN** the app returns to the Home screen
- **AND** the cart basket and the consumer-data form are cleared

#### Scenario: Entendido has the higher priority

- **WHEN** the simplified error state is shown
- **THEN** the "Entendido" action is presented with a higher visual priority than "Cancelar orden"

### Requirement: Unavailable products flagged after a server rejection

When an order or share request is rejected for an unavailable product and the answer does not name it (HTTP 400, 404, or 409 without a usable `productId`), the app SHALL recheck every cart line against the store's current products with a single `GET /api/v1/products` request. Each line whose product is absent, not `AVAILABLE`, or out of stock SHALL be marked "No disponible" in the cart. When the answer names the product (`PRODUCT_NOT_SELLABLE`), that line SHALL be marked directly, without a recheck.

The cart SHALL show a message explaining that some products are no longer available, and "Realizar pedido" and "Compartir por WhatsApp" SHALL stay disabled until those lines are removed. If the recheck finds no unavailable line, or cannot read the products, the app SHALL show the simplified error state.

#### Scenario: Inactive product in the basket is flagged

- **WHEN** the consumer places an order, the endpoint answers 400, and one cart product is now inactive
- **THEN** that line is shown as "No disponible"
- **AND** the order and share actions are disabled with an explanatory message

#### Scenario: Recheck uses one request

- **WHEN** the order endpoint answers 404 for a basket of 4 lines
- **THEN** exactly one `GET /api/v1/products` request is sent and no `GET /api/v1/products/{id}` request is sent

#### Scenario: Removing the flagged line enables the order again

- **WHEN** the consumer removes every line marked "No disponible"
- **THEN** the order and share actions are enabled again (given a valid consumer form)

#### Scenario: No generic error loop

- **WHEN** the order is rejected for an unavailable product and the consumer goes back to the cart
- **THEN** the consumer cannot resubmit the same basket until the unavailable lines are removed

#### Scenario: Recheck finds nothing

- **WHEN** the order endpoint answers 400 and every cart product is still active and `AVAILABLE`
- **THEN** the app shows the simplified error state
