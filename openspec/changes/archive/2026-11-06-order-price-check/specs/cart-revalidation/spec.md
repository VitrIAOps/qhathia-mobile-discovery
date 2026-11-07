## ADDED Requirements

### Requirement: Basket refreshed when the cart opens

Each time the cart dialog opens with products in the basket, the app SHALL read the store's current products with a single `GET /api/v1/products` request and refresh every basket line:

- **Price:** the unit price SHALL become the current price the customer pays (`discountedPrice` when set, else `price`).
- **Removal:** a line whose product is absent from the answer (deleted or deactivated), not orderable (not `AVAILABLE`), or with stock 0 SHALL be removed.
- **Stock cap:** a quantity above the product's stock SHALL be lowered to that stock.

The refresh SHALL NOT clear the consumer-data form, and a change the customer makes while it runs SHALL NOT be lost. If the request fails, the basket SHALL stay unchanged and no notice SHALL be shown. The app SHALL NOT call `GET /api/v1/products/{id}` per line to refresh the basket.

#### Scenario: Price lowered while the product was in the basket

- **WHEN** a product was added at S/ 210.00, the seller changed it to S/ 199.00, and the cart is opened
- **THEN** the line shows S/ 199.00, the total uses it, and the cart shows a notice that the price of that product changed from S/ 210.00 to S/ 199.00

#### Scenario: Deleted product is removed with a notice

- **WHEN** a product in the basket was deleted or deactivated by the merchant and the cart is opened
- **THEN** its line is removed and the cart shows a notice naming the product as no longer available

#### Scenario: Quantity capped to stock

- **WHEN** a line has 6 units, the product's stock is now 3, and the cart is opened
- **THEN** the line has 3 units and the cart shows a notice that only 3 are left of that product

#### Scenario: Refresh without network

- **WHEN** the cart is opened and the products request fails
- **THEN** the basket and the consumer form are unchanged and no notice is shown

#### Scenario: One request per opening

- **WHEN** the cart is opened with 5 lines
- **THEN** exactly one `GET /api/v1/products` request is sent and no `GET /api/v1/products/{id}` request is sent

### Requirement: Basket change notices

The cart SHALL explain every change made to the basket on the customer's behalf, in a notice above the totals:

- each price change ("El precio de «X» cambió de S/ A a S/ B…"), listing at most 3 and then "y N más";
- the products removed for no longer being available;
- each quantity lowered to the stock.

The notice SHALL be announced to accessibility services. It SHALL be cleared when the customer changes a quantity or removes a line, when an order or share starts, and when the customer session ends.

#### Scenario: Several price changes

- **WHEN** a refresh changes the price of 5 products
- **THEN** the notice lists 3 of them and says "y 2 más"

#### Scenario: Notice cleared on the next action

- **WHEN** a notice is shown and the customer changes a quantity
- **THEN** the notice is no longer shown

### Requirement: Order waits for a running refresh

When the customer taps "Realizar pedido" or "Compartir por WhatsApp" while the opening refresh is still running, the app SHALL wait for that refresh to finish (at most its request timeout) before taking the basket snapshot it sends.

- If the refresh changed the basket (a price, a removed line, or a lowered quantity), the app SHALL NOT send the request: it SHALL return to the cart with the change notice, so the customer confirms the new basket.
- If the refresh changed nothing or failed, the request SHALL be sent with the basket as shown.

#### Scenario: Refresh changes a price during the tap

- **WHEN** the cart opens, the refresh is still running, the customer taps "Realizar pedido", and the refresh then lowers a price
- **THEN** no order request is sent and the cart shows the new total with the price-change notice

#### Scenario: Refresh changes nothing during the tap

- **WHEN** the cart opens, the refresh is still running, the customer taps "Realizar pedido", and the refresh changes nothing
- **THEN** exactly one order request is sent after the refresh
