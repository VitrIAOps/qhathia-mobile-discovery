## ADDED Requirements

### Requirement: Detail microphone opens voice search

The microphone control on the product detail search bar SHALL open the product search screen in voice mode with no query, which starts listening, as the Catalog microphone does.

#### Scenario: Tap the detail microphone

- **WHEN** the consumer taps the microphone on the product detail
- **THEN** the product search screen opens and starts listening

### Requirement: Share dialog stays open while sending

While a product share request is in flight, the share dialog SHALL NOT be closed by an outside tap, the system Back or its close control. Once a result is shown, closing the dialog SHALL run the result's main action (accept on success, "Entendido" on an error).

#### Scenario: Outside tap while sending

- **WHEN** a product share request is in flight and the consumer taps outside the dialog
- **THEN** the dialog stays open with its loading indicator

#### Scenario: Back on the success screen

- **WHEN** the share success screen is shown and the consumer presses the system Back
- **THEN** the dialog closes as if the consumer had accepted the success screen
