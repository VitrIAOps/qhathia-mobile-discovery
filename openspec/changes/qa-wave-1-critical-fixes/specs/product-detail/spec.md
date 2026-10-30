## ADDED Requirements

### Requirement: Unavailable product blocks purchase actions

When the loaded product detail reports the product as inactive, or its availability is not `AVAILABLE`, the product-detail screen SHALL disable the Add-to-cart and Share-via-WhatsApp buttons and the quantity stepper, and SHALL show a visible note that the product is not available right now. The rest of the detail (images, name, price, related products) SHALL still be shown.

#### Scenario: Inactive product opened from a stale result

- **WHEN** the consumer opens the detail of a product whose detail response has `active = false`
- **THEN** the detail is shown with a "not available" note
- **AND** Add-to-cart and Share are disabled

#### Scenario: Low-stock product cannot be shared

- **WHEN** the detail of a product with availability `LOW_STOCK` is shown
- **THEN** the Share-via-WhatsApp button is disabled and no share request can be sent

#### Scenario: Available product keeps its actions

- **WHEN** the detail of an active `AVAILABLE` product is shown
- **THEN** Add-to-cart, the stepper and Share are enabled

### Requirement: Product share reports the real delivery outcome

The product share dialog SHALL follow the same delivery rules as the cart share: success only when the endpoint answers `status = SENT`, the not-delivered error for `FAILED` or `STUB`, and retries only after a connectivity error, a timeout, or a 5xx answer.

#### Scenario: FAILED status in the product share

- **WHEN** the consumer confirms the product share and the endpoint answers `201` with `status = FAILED`
- **THEN** the dialog shows the not-delivered error, not the success screen

#### Scenario: Deterministic rejection sent once

- **WHEN** the product share endpoint answers 400
- **THEN** exactly one request is sent and the dialog shows the error state
