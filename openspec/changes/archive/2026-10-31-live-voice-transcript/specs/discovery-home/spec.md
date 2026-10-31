## MODIFIED Requirements

### Requirement: Voice-search processing feedback

While a voice-search attempt is in progress, the Home screen SHALL show the feedback that matches the current phase. While recording, it SHALL show the listening processing message until the speech-to-text provider recognizes the first words; from then on the listening message SHALL be replaced by the live transcript of what the consumer is saying, updated as new interim results arrive. While the final transcript is awaited, it SHALL keep showing the text heard so far, still updated as late results arrive; the organizing message SHALL be shown only while no words have been recognized. It SHALL show a slow-response message when the final transcript has not arrived after 30 seconds. Processing messages SHALL display the animated three-dot ("…") indicator; the live transcript and other non-processing messages SHALL NOT. When the live transcript is longer than the space available, the most recent words SHALL remain visible.

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

#### Scenario: Heard text stays while awaiting the final transcript

- **WHEN** the recording has ended, words have been recognized, and the app is awaiting the final transcript
- **THEN** the Home screen keeps showing the recognized text instead of the organizing message

#### Scenario: Late results update the heard text

- **WHEN** interim results arrive after the recording has ended
- **THEN** the Home screen updates the shown text to the latest transcript

#### Scenario: Final text stays while opening the search results

- **WHEN** the final transcript has arrived and the app navigates to the search results
- **THEN** the Home screen keeps showing the final text with the microphone animation and hidden controls during the transition, instead of the idle Home layout
- **AND** once Home is no longer visible it returns to the idle state, so it is idle when the consumer comes back

#### Scenario: Organizing message when nothing was recognized

- **WHEN** the recording has ended, no words have been recognized yet, and the app is awaiting the final transcript
- **THEN** the Home screen shows the organizing processing message with the animated three dots

#### Scenario: Slow-response message

- **WHEN** the final transcript has not been received after 30 seconds
- **THEN** the Home screen shows the slow-response processing message with the animated three dots
