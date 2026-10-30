## ADDED Requirements

### Requirement: Remote catalog refresh in place

While the catalog screen is shown, a remote catalog refresh request SHALL make it re-fetch the catalog in the background. On success the products and category tabs SHALL be replaced with the fresh data, keeping the selected tab when it still exists and falling back to "Todo" otherwise. The refresh SHALL NOT show a loading indicator, SHALL NOT show the network-error screen when it fails (the current products remain), and SHALL NOT reset the inactivity timer. If the screen is showing the network-error state, the refresh SHALL behave like a retry. A refresh requested while no catalog screen is shown SHALL have no effect on a catalog screen opened later.

#### Scenario: Refresh while browsing a category

- **WHEN** the consumer is on the catalog with the "Iluminación" tab selected and a remote refresh is requested
- **THEN** the catalog is fetched again, the fresh products for "Iluminación" are shown, and the "Iluminación" tab stays selected without a loading indicator

#### Scenario: Selected tab no longer exists

- **WHEN** a remote refresh returns a catalog without the currently selected category
- **THEN** the "Todo" tab is selected

#### Scenario: Refresh fails

- **WHEN** the background re-fetch fails
- **THEN** the current products stay on screen and no error screen is shown

#### Scenario: Refresh on the network-error screen

- **WHEN** the catalog shows the network-error screen and a remote refresh is requested
- **THEN** the catalog is loaded again as on a retry

#### Scenario: Catalog opened after the refresh

- **WHEN** a remote refresh is requested while the consumer is on Home and the catalog is opened afterwards
- **THEN** the catalog performs its normal single load and no extra refresh
