## MODIFIED Requirements

### Requirement: Temporary search history with back navigation

The app SHALL store the history of search results temporarily (in memory) so the consumer can go back to a previous result. The **Volver** control SHALL return to the immediately previous search result when one exists. While a network error is shown over a result, **Volver** SHALL return to that result. When no previous result exists, **Volver** SHALL return to the previous screen (Catalog or Product detail) keeping the cart and the customer session; only when the search was opened from Home SHALL it open the new-search confirmation dialog.

#### Scenario: Back returns to the previous result

- **WHEN** the consumer has performed more than one search and taps Volver
- **THEN** the app shows the immediately previous search result

#### Scenario: Back after a network error

- **WHEN** a result is shown, a later search fails for lack of network, and the consumer taps Volver
- **THEN** the app shows that result again

#### Scenario: Back to the Catalog

- **WHEN** the search was opened from the Catalog, there is no previous search result, and the consumer taps Volver or system Back
- **THEN** the app returns to the Catalog without a dialog, and the cart is kept

#### Scenario: Back with no previous result from Home asks to close

- **WHEN** the search was opened from Home, there is no previous search result, and the consumer taps Volver
- **THEN** the app opens the confirmation dialog to close the current search
