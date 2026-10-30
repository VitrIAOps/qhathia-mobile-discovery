## ADDED Requirements

### Requirement: No-audio prompt returns to idle

The no-audio retry prompt ("No te escuché…" and its variants) SHALL return Home to its idle state after 30 seconds without interaction, the same timeout as the speech-to-text error screens. Any microphone interaction SHALL cancel that timeout.

#### Scenario: Prompt left alone

- **WHEN** the no-audio retry prompt is shown and the consumer does not interact for 30 seconds
- **THEN** Home returns to its idle state with the microphone button and the search bar

#### Scenario: Consumer retries before the timeout

- **WHEN** the no-audio retry prompt is shown and the consumer taps the microphone before 30 seconds
- **THEN** a new listening window starts and the idle timeout is cancelled

### Requirement: Leaving Home stops voice capture

When Home stops being visible (another screen opens on top or the app goes to the background), any voice-search attempt in progress (listening, transcribing or waiting for a slow response) SHALL be cancelled, the microphone SHALL be released, and Home SHALL be idle when it is shown again.

#### Scenario: App sent to the background while listening

- **WHEN** Home is listening and the app goes to the background
- **THEN** the recording stops and the microphone is released
- **AND** Home is idle when the app comes back

#### Scenario: Another screen opens while transcribing

- **WHEN** a transcription is in progress and Home is covered by another screen
- **THEN** the transcription is cancelled and no search is started from it
