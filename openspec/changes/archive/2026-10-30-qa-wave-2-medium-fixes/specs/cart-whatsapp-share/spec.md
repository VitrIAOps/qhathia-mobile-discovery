## MODIFIED Requirements

### Requirement: Share error recovery

On any share failure (rejected or failed after its retries) the app SHALL show a share error state with a single simplified share message ("No pudimos enviar la cotización" / "Hubo un problema enviando la cotización. Inténtalo de nuevo en unos momentos.") regardless of the underlying cause. The share error state, and the not-delivered state, SHALL present a single action, **"Entendido"**, which SHALL return to the cart summary dialog in its prior state (basket and entered data preserved) so the consumer can retry. A share failure SHALL NOT offer "Cancelar orden" and SHALL NOT clear the basket or the consumer data. The order error state keeps its own copy and its "Cancelar orden" action.

#### Scenario: All failures show the same simplified share message

- **WHEN** the share request fails for any reason (validation, not found, server, PDF generation, or connectivity)
- **THEN** the app shows the single simplified share error message rather than the raw cause or the order message

#### Scenario: Entendido returns to the cart summary

- **WHEN** the consumer taps "Entendido" on the share error state
- **THEN** the app returns to the cart summary dialog with the basket and entered consumer data preserved

#### Scenario: No cancel action on a share failure

- **WHEN** the share error state or the not-delivered state is shown
- **THEN** no "Cancelar orden" action is offered, and the basket and consumer data are kept

#### Scenario: Order error keeps its cancel action

- **WHEN** a "Realizar pedido" request fails
- **THEN** the order error state shows its order message with "Entendido" and "Cancelar orden"
