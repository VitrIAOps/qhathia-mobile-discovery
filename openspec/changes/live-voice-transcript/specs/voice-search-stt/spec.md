## ADDED Requirements

### Requirement: Live streaming transcription while the consumer speaks

While voice search is recording, the app SHALL stream the captured microphone audio to the speech-to-text provider (Deepgram Live, over a WebSocket) as it is captured, and SHALL receive interim transcripts while the consumer is still speaking. Audio SHALL be streamed as raw 16-bit PCM, 16 kHz, mono. Audio captured before the streaming connection is open SHALL be buffered and sent once the connection opens, so no speech is lost. When the recording ends, the app SHALL signal the end of the audio stream to the provider and use the final transcript returned on that same connection; the app SHALL NOT upload the recording again in a separate request.

#### Scenario: Audio is streamed while recording

- **WHEN** the consumer is speaking during an active recording
- **THEN** the app sends the captured audio to the provider over the live connection without waiting for the recording to end

#### Scenario: Interim transcript is received while speaking

- **WHEN** the provider recognizes words while the consumer is still speaking
- **THEN** the app exposes the current transcript (confirmed segments followed by the current interim segment) to the screen that started the voice search

#### Scenario: Audio captured before the connection opens is not lost

- **WHEN** the consumer starts speaking before the live connection has finished opening
- **THEN** the buffered audio is sent once the connection opens and its words appear in the transcript

#### Scenario: Final transcript comes from the live connection

- **WHEN** the recording ends (trailing silence, maximum length, or the consumer taps the microphone)
- **THEN** the app signals the end of the audio stream on the live connection
- **AND** uses the final transcript returned on that connection as the search text, without a separate upload

#### Scenario: Blank final transcript

- **WHEN** the live connection closes and the final transcript is blank
- **THEN** the attempt fails with an empty/no-audio error

#### Scenario: Live connection drops during recording

- **WHEN** the live connection fails or closes unexpectedly while the consumer is still recording
- **THEN** the app stops recording and surfaces a speech-to-text error classified per the error taxonomy

#### Scenario: Cancelling the attempt closes the stream

- **WHEN** the consumer cancels an in-progress voice-search attempt
- **THEN** the app stops capturing audio, closes the live connection, and discards the transcript

## MODIFIED Requirements

### Requirement: Automatic stop on end-of-speech silence

Once speech has been detected during a recording, a short period of continuous silence SHALL end the recording automatically and finalize the live transcription immediately, without waiting for the maximum recording length. The trailing-silence threshold SHALL be **1 second**. Silence before any speech is detected SHALL NOT end the recording (the input-wait window governs that case). End-of-speech SHALL be detected on-device from the microphone level, independently of the transcripts returned by the provider.

#### Scenario: Trailing silence ends the recording

- **WHEN** the consumer has been speaking and then stays silent for 1 second
- **THEN** the app stops the recording and immediately finalizes the live transcription to obtain the final transcript

#### Scenario: Silence before speaking does not end the recording

- **WHEN** the recording has started but no speech has been detected yet
- **THEN** the trailing-silence auto-stop does not apply, and the input-wait window continues to govern the no-audio outcome

### Requirement: Deepgram token acquisition from the Qhathia API

When a voice-search recording starts, the app SHALL obtain a short-lived transcription token from the Qhathia backend via `POST /api/v1/stt/token`, authenticated with the session Bearer JWT, and use it to authenticate the live connection to Deepgram (`Authorization: Token <token>`). The real provider API key SHALL NOT be embedded in the client. The token SHALL be used only for the current voice-search attempt and not persisted beyond its lifetime.

#### Scenario: Token requested when recording starts

- **WHEN** the consumer starts a voice-search recording
- **THEN** the app requests a transcription token from `POST /api/v1/stt/token` with the session Bearer JWT
- **AND** uses the returned token to authenticate the live connection to Deepgram

#### Scenario: Token request fails

- **WHEN** the token request to `POST /api/v1/stt/token` fails or the service is unavailable
- **THEN** the app stops recording and surfaces a speech-to-text error rather than opening a connection with no valid token

### Requirement: Operation time budgets

The speech-to-text operation SHALL enforce the following budgets and fail with the appropriate error when any is exceeded: connection establishment (token request and live-connection handshake) at most **10 seconds**, final-transcript response after the end of the audio stream at most **120 seconds**, and total end-to-end operation at most **150 seconds**. The audio stream itself lasts no longer than the recording, which is capped at 45 seconds.

#### Scenario: Connection budget exceeded

- **WHEN** establishing the live connection takes longer than 10 seconds
- **THEN** the operation fails and a network/timeout error is surfaced

#### Scenario: Response budget exceeded

- **WHEN** the final transcript is not received within 120 seconds after the end of the audio stream
- **THEN** the operation fails with a timeout error

#### Scenario: Total operation budget exceeded

- **WHEN** the end-to-end operation exceeds 150 seconds
- **THEN** the operation is aborted and a timeout error is surfaced

### Requirement: Debug logging of the transcription query and result

In debug builds, the app SHALL log the transcription session that was performed (audio format, connection open/close outcome) together with its result (the final transcript or the failure), to aid troubleshooting. Logging SHALL NOT expose the real provider API key or the ephemeral transcription token.

#### Scenario: Successful transcription logged

- **WHEN** a live transcription completes successfully in a debug build
- **THEN** the app writes a debug log entry containing the session details and the final transcript

#### Scenario: Failed transcription logged

- **WHEN** a live transcription fails in a debug build
- **THEN** the app writes a debug log entry containing the session details and the failure outcome

## REMOVED Requirements

### Requirement: Transcription invoked only after the consumer finishes speaking

**Reason**: The consumer must see the words being recognized while speaking, which requires streaming audio during the recording. Replaced by "Live streaming transcription while the consumer speaks".

**Migration**: The batch upload (`POST /v1/listen` with the full M4A clip) is removed; the final transcript is taken from the live connection when the recording ends. No backend change is required.
