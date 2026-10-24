## ADDED Requirements

### Requirement: Live transcript while capturing voice in place

When the consumer captures voice in place from the results or unhappy screen, the product-search screen SHALL show the listening processing message (with the animated three dots) until the speech-to-text provider recognizes the first words; from then on the listening message SHALL be replaced by the live transcript of what the consumer is saying, updated as new interim results arrive, without the animated dots. When the live transcript is longer than the space available, the most recent words SHALL remain visible. Once the recording ends, the screen SHALL continue with the existing voice processing phases (organizing, slow response) and search phases.

#### Scenario: Listening message before speech is recognized

- **WHEN** the consumer taps the bottom microphone and no words have been recognized yet
- **THEN** the screen shows the listening processing message with the animated three dots

#### Scenario: Live transcript replaces the listening message

- **WHEN** the speech-to-text provider recognizes the consumer's first words during the in-place recording
- **THEN** the screen replaces the listening message with the recognized text, without the animated dots

#### Scenario: Live transcript updates while the consumer speaks

- **WHEN** new interim results arrive during the in-place recording
- **THEN** the screen updates the shown text to the latest transcript

#### Scenario: Recording ends and the search continues

- **WHEN** the in-place recording ends with a non-blank final transcript
- **THEN** the screen leaves the live transcript, shows the organizing message while the final transcript is awaited, and then starts a new in-place search with the final transcript
