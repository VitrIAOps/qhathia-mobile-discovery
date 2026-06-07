## MODIFIED Requirements

### Requirement: Navigation intents from Home

The Home screen SHALL emit navigation intents toward the catalog and the settings destinations when the corresponding controls are used. When the consumer submits a typed query, or a voice search produces a transcript, the Home screen SHALL hand the query off to the product-search flow, carrying the query text and its source (voice or text). For a voice search, the handoff SHALL continue the same conversation so the product-search processing messages follow the speech-to-text phases without a visible break.

#### Scenario: Catalog button emits catalog navigation

- **WHEN** the consumer taps the catalog button
- **THEN** the app emits a navigation intent toward the catalog destination

#### Scenario: Settings button emits settings navigation

- **WHEN** the consumer taps the settings button
- **THEN** the app emits a navigation intent toward the settings destination

#### Scenario: Submitting a typed search starts a product search

- **WHEN** the consumer submits a query from the search bar
- **THEN** the app hands the query off to the product-search flow with source = text

#### Scenario: Voice transcript starts a product search

- **WHEN** a voice search produces a transcript
- **THEN** the app hands the transcript off to the product-search flow with source = voice
- **AND** the product-search processing messages continue the conversation after the speech-to-text phases
