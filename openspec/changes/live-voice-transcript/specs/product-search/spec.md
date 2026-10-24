## ADDED Requirements

### Requirement: Live transcript while capturing voice in place

When the consumer captures voice in place from the results or unhappy screen, the product-search screen SHALL show the listening processing message (with the animated three dots) until the speech-to-text provider recognizes the first words; from then on the listening message SHALL be replaced by the live transcript of what the consumer is saying, updated as new interim results arrive, without the animated dots. When the live transcript is longer than the space available, the most recent words SHALL remain visible. Once the recording ends and while the final transcript is awaited, the screen SHALL keep showing the text heard so far (updated as late results arrive), showing the organizing message only while no words have been recognized; the slow-response and search phases then follow as before.

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
- **THEN** the screen keeps showing the heard text (or the organizing message if no words were recognized yet) while the final transcript is awaited, and then starts a new in-place search with the final transcript

#### Scenario: Late results update the heard text

- **WHEN** interim results arrive after the in-place recording has ended
- **THEN** the screen updates the shown text to the latest transcript
