## ADDED Requirements

### Requirement: Stock-aware availability and quantity

When the product detail carries a stock count, the availability label and the quantity stepper SHALL follow it:

- stock 1 to 5 → "Pocas unidades (quedan N)", even when the availability is `AVAILABLE`;
- stock 0, or availability `OUT_OF_STOCK` → "Agotado", in the error color;
- otherwise → today's label for the availability.

The stepper SHALL NOT go above the stock. Without a stock count ("a consultar"), the label and the cap SHALL follow the availability, as before.

#### Scenario: Few units left

- **WHEN** the detail of an `AVAILABLE` product with stock 3 is shown
- **THEN** the label says "Pocas unidades (quedan 3)" and the stepper stops at 3

#### Scenario: Out of stock

- **WHEN** the detail of a product with stock 0 or availability `OUT_OF_STOCK` is shown
- **THEN** the label says "Agotado" and the purchase actions are disabled

### Requirement: Product share form limits and errors

The detail's share form SHALL accept at most 200 characters in the name, the server's limit: input beyond that SHALL NOT be entered. When the share fails because the tablet has no connection, the error SHALL say that the quote could not be sent because the tablet has no internet. Any other share failure SHALL keep the simplified share message.

#### Scenario: Name stops at 200 characters

- **WHEN** the consumer types a 262-character name in the share form
- **THEN** the field keeps the first 200 characters

#### Scenario: Share without network

- **WHEN** the tablet has no connection and the consumer submits the share form
- **THEN** the error says the quote could not be sent because the tablet has no internet, with "Entendido" returning to the form

## MODIFIED Requirements

### Requirement: Unavailable product blocks purchase actions

When the loaded product detail reports the product as inactive, its availability is not `AVAILABLE`, or its stock is 0, the product-detail screen SHALL disable the Add-to-cart and Share-via-WhatsApp buttons and the quantity stepper, and SHALL show a visible note that the product is not available right now. The rest of the detail (images, name, price, related products) SHALL still be shown.

#### Scenario: Inactive product opened from a stale result

- **WHEN** the consumer opens the detail of a product whose detail response has `active = false`
- **THEN** the detail is shown with a "not available" note
- **AND** Add-to-cart and Share are disabled

#### Scenario: Low-stock product cannot be shared

- **WHEN** the detail of a product with availability `LOW_STOCK` is shown
- **THEN** the Share-via-WhatsApp button is disabled and no share request can be sent

#### Scenario: Product with no stock left

- **WHEN** the detail of an `AVAILABLE` product with stock 0 is shown
- **THEN** Add-to-cart, the stepper and Share are disabled

#### Scenario: Available product keeps its actions

- **WHEN** the detail of an active `AVAILABLE` product with stock above 0 (or no stock count) is shown
- **THEN** Add-to-cart, the stepper and Share are enabled
