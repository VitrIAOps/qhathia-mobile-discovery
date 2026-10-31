# discovery-home Specification

## Purpose
TBD - created by archiving change discovery-home. Update Purpose after archive.
## Requirements
### Requirement: Home screen entry point

The Discovery app SHALL present a Home screen as the launch destination that gives in-store consumers an inviting, low-friction entry point to the app's functions. The screen SHALL display a search bar, a microphone (voice search) button, a catalog shortcut button, and a settings button.

#### Scenario: Home is the launch screen

- **WHEN** the Discovery app is launched
- **THEN** the Home screen is shown as the start destination
- **AND** the search bar, microphone button, catalog button, and settings button are visible in their idle state

#### Scenario: Entrance animations

- **WHEN** the Home screen first appears
- **THEN** its interactive components animate into view with subtle motion rather than appearing abruptly

### Requirement: Store identity is visible

The Home screen SHALL display the name of the store the device is operating for, read from the store session, so the consumer always knows which store they are interacting with.

#### Scenario: Store name displayed

- **WHEN** the store session exposes a store name
- **THEN** the Home screen displays that store name

#### Scenario: Store name not yet available

- **WHEN** the store session has no store name available
- **THEN** the Home screen displays a neutral placeholder and does not crash or show an empty/blank label

### Requirement: Controls hidden during microphone interaction

The search bar and the catalog and settings buttons SHALL be visible only when the consumer is NOT interacting with the microphone button. While a microphone interaction is active, these controls SHALL be hidden.

#### Scenario: Entering microphone interaction hides controls

- **WHEN** the consumer activates the microphone button from the idle state
- **THEN** the search bar, the catalog button, and the settings button are hidden
- **AND** the microphone listening UI is shown

#### Scenario: Leaving microphone interaction restores controls

- **WHEN** the microphone interaction ends (cancelled, completed, or timed out and dismissed)
- **THEN** the search bar, the catalog button, and the settings button become visible again

### Requirement: Microphone listening window

When the consumer activates the microphone, the app SHALL enter a listening state that captures microphone audio and lasts at most 30 seconds while waiting for the consumer to start providing audio. When the consumer finishes speaking (or the recording reaches its maximum length), the app SHALL proceed to transcribe the captured audio through the speech-to-text pipeline. The microphone drives the real speech-to-text flow defined by the `voice-search-stt` capability.

#### Scenario: Listening starts on activation

- **WHEN** the consumer activates the microphone button
- **THEN** the app enters the listening state, begins capturing audio, and shows listening feedback

#### Scenario: Listening times out after 30 seconds

- **WHEN** the app has been waiting in the listening state for 30 seconds without captured audio input
- **THEN** the listening window ends and the app stops listening

#### Scenario: Consumer finishes speaking

- **WHEN** the consumer finishes providing audio before the timeout
- **THEN** the app leaves the listening state and begins transcribing the captured audio

#### Scenario: Consumer cancels listening

- **WHEN** the consumer dismisses or cancels the microphone interaction before transcription begins
- **THEN** the app leaves the listening state and returns to the idle Home state

### Requirement: Retry prompts when no audio is detected

When a listening window ends without detected audio (an empty/no-audio outcome from the speech-to-text pipeline), the assistant SHALL ask the consumer to try again, drawing from a set of multiple distinct retry messages rather than repeating a single fixed message.

#### Scenario: First no-audio outcome shows a retry prompt

- **WHEN** a voice-search attempt ends with no detected audio
- **THEN** the app shows a retry prompt inviting the consumer to try speaking again

#### Scenario: Repeated no-audio outcomes vary the prompt

- **WHEN** consecutive voice-search attempts end with no detected audio
- **THEN** the retry prompt shown varies across the available set of retry messages rather than repeating the same message every time

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

### Requirement: Speech-to-text error screens

When a voice-search attempt fails, the Home screen SHALL present an error screen whose title and message match the failure category from the `voice-search-stt` error taxonomy (network, audio-too-long, transcription-response, empty/no-audio, timeout, or generic). The error screen SHALL use the same layout as the no-audio-retry screen — the error title and message as the headline, with the microphone button and the search bar — so the consumer can retry by voice (the microphone) or type their query. If an error screen remains without consumer activity for 30 seconds, the app SHALL return to the Home idle screen.

#### Scenario: Error category maps to its message

- **WHEN** a voice-search attempt fails with a given error category
- **THEN** the Home screen shows the error title and message that correspond to that category

#### Scenario: Error screen uses the no-audio-retry layout

- **WHEN** an error screen is shown
- **THEN** it presents the microphone button and the search bar (the same layout as the no-audio-retry screen)

#### Scenario: Error screen auto-returns to idle

- **WHEN** an error screen has been shown with no consumer activity for 30 seconds
- **THEN** the app returns to the Home idle screen

#### Scenario: Consumer retries from an error screen

- **WHEN** the consumer taps the microphone on an error screen
- **THEN** the app starts a new voice-search attempt

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

### Requirement: Theming and accessibility compliance

The Home screen SHALL be built from the shared design system, use Material 3 design tokens (no hardcoded colors, type sizes, or dimensions), support light and dark mode, and meet accessibility requirements.

#### Scenario: Dark mode

- **WHEN** the device is in dark mode
- **THEN** the Home screen renders using the design system's dark color scheme

#### Scenario: Interactive controls are accessible

- **WHEN** an assistive technology inspects the microphone, catalog, and settings controls
- **THEN** each interactive control exposes a localized content description and a touch target of at least 48dp

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

### Requirement: Store name when known

Home SHALL show the store's name in the header when the app knows it (saved from the device info), and "TIENDA" otherwise.

#### Scenario: Store name known

- **WHEN** the saved store name is "El Martillo"
- **THEN** the Home header shows "El Martillo"

#### Scenario: Store name unknown

- **WHEN** no store name has been saved
- **THEN** the Home header shows "TIENDA"

