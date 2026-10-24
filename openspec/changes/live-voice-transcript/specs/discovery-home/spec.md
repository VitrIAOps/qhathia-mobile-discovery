## MODIFIED Requirements

### Requirement: Voice-search processing feedback

While a voice-search attempt is in progress, the Home screen SHALL show the feedback that matches the current phase. While recording, it SHALL show the listening processing message until the speech-to-text provider recognizes the first words; from then on the listening message SHALL be replaced by the live transcript of what the consumer is saying, updated as new interim results arrive. It SHALL show an organizing message while the final transcript is awaited, and a slow-response message when the final transcript has not arrived after 30 seconds. Processing messages SHALL display the animated three-dot ("…") indicator; the live transcript and other non-processing messages SHALL NOT. When the live transcript is longer than the space available, the most recent words SHALL remain visible.

#### Scenario: Listening message while recording before speech is recognized

- **WHEN** the app is capturing microphone audio and no words have been recognized yet
- **THEN** the Home screen shows the listening processing message with the animated three dots

#### Scenario: Live transcript replaces the listening message

- **WHEN** the speech-to-text provider recognizes the consumer's first words during recording
- **THEN** the Home screen replaces the listening message with the recognized text, without the animated dots

#### Scenario: Live transcript updates while the consumer speaks

- **WHEN** new interim results arrive during recording
- **THEN** the Home screen updates the shown text to the latest transcript

#### Scenario: Long transcript keeps the latest words visible

- **WHEN** the live transcript does not fit in the available space
- **THEN** the most recent words remain visible

#### Scenario: Organizing message while awaiting the final transcript

- **WHEN** the recording has ended and the app is awaiting the final transcript
- **THEN** the Home screen shows the organizing processing message with the animated three dots

#### Scenario: Slow-response message

- **WHEN** the final transcript has not been received after 30 seconds
- **THEN** the Home screen shows the slow-response processing message with the animated three dots
