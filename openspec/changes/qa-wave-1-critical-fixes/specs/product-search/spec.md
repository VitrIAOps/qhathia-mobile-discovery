## ADDED Requirements

### Requirement: Voice hand-off without a query starts listening

When the product-search screen is opened with source Voice and an empty query (for example from the Catalog microphone), it SHALL start in-place voice capture immediately and SHALL NOT send a search request with an empty query.

#### Scenario: Catalog microphone opens voice capture

- **WHEN** the consumer taps the microphone on the Catalog and the search screen opens with an empty query
- **THEN** the search screen shows the listening state and captures the consumer's voice
- **AND** no search request is sent until a transcript is available

#### Scenario: Voice hand-off with a query still searches

- **WHEN** the search screen is opened with source Voice and a non-empty query
- **THEN** it submits that query to the search API as before
