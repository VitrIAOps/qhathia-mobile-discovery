## ADDED Requirements

### Requirement: Share rejections decided by error code

A share rejected with `409 PRODUCT_NOT_SELLABLE` naming a product in the basket SHALL flag that line "No disponible" directly, without a recheck. A share rejected with `422 STOCK_INSUFFICIENT` naming a product SHALL lower that line to `available` units (removing it at 0), show the stock notice, and return to the cart. Neither SHALL be retried.

#### Scenario: Not sellable share flags the line

- **WHEN** "Compartir por WhatsApp" answers `409 PRODUCT_NOT_SELLABLE` for a product in the basket
- **THEN** that line is shown as "No disponible", exactly one share request was sent, and no product request is sent

#### Scenario: Stock shortage on share

- **WHEN** "Compartir por WhatsApp" answers `422 STOCK_INSUFFICIENT` with `available` 2 for a line with 4 units
- **THEN** the line has 2 units, the stock notice is shown, and the cart is shown again

## MODIFIED Requirements

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
