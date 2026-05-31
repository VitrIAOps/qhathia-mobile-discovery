## ADDED Requirements

### Requirement: Microphone permission requested at app open

The app SHALL request the device microphone (record audio) permission when it is opened. Voice search SHALL be gated on this permission: the speech-to-text pipeline SHALL NOT attempt to capture audio unless the permission has been granted.

#### Scenario: Permission requested on launch

- **WHEN** the app is opened and the microphone permission has not yet been granted
- **THEN** the app requests the microphone permission

#### Scenario: Voice search blocked without permission

- **WHEN** the consumer activates voice search and the microphone permission is not granted
- **THEN** the app does not start audio capture
- **AND** the consumer is informed that microphone access is required rather than the app crashing or silently failing

#### Scenario: Permission already granted

- **WHEN** the app is opened and the microphone permission is already granted
- **THEN** the app does not prompt again and voice search is available

### Requirement: On-device audio capture with length caps

When voice search is activated, the app SHALL capture the consumer's microphone audio on-device. The app SHALL wait at most **30 seconds** for the consumer to provide audio input. A single recording SHALL last at most **45 seconds**; when the recording reaches 45 seconds it SHALL stop automatically and proceed with the captured audio.

#### Scenario: Recording starts on activation

- **WHEN** the consumer activates voice search with microphone permission granted
- **THEN** the app begins capturing microphone audio

#### Scenario: Input wait window elapses with no audio

- **WHEN** the app has waited 30 seconds for the consumer to provide audio and none was captured
- **THEN** the app stops waiting and treats the attempt as having captured no audio

#### Scenario: Recording reaches the maximum length

- **WHEN** an active recording reaches 45 seconds
- **THEN** the app stops the recording automatically
- **AND** proceeds to transcribe the captured audio

### Requirement: Transcription invoked only after the consumer finishes speaking

The app SHALL send audio to the speech-to-text provider (Deepgram) only after the consumer finishes providing their audio. The app SHALL NOT stream audio for live transcription while the consumer is still speaking. Audio SHALL be sent using a lightweight audio format suitable for low-bandwidth upload.

#### Scenario: Transcription deferred until recording ends

- **WHEN** the consumer is actively recording audio
- **THEN** the app does not call the transcription provider until the recording has ended

#### Scenario: Captured audio is transcribed after recording ends

- **WHEN** the recording ends with captured audio
- **THEN** the app uploads the audio to the transcription provider in a lightweight audio format and requests a transcript

### Requirement: Deepgram token acquisition from the Qhathia API

Before transcribing, the app SHALL obtain a short-lived transcription token from the Qhathia backend via `POST /api/v1/stt/token`, authenticated with the session Bearer JWT. The real provider API key SHALL NOT be embedded in the client. The token SHALL be used only for the current transcription and not persisted beyond its lifetime.

#### Scenario: Token requested before transcription

- **WHEN** the app is ready to transcribe captured audio
- **THEN** the app requests a transcription token from `POST /api/v1/stt/token` with the session Bearer JWT
- **AND** uses the returned token to authenticate the transcription request to Deepgram

#### Scenario: Token request fails

- **WHEN** the token request to `POST /api/v1/stt/token` fails or the service is unavailable
- **THEN** the app surfaces a speech-to-text error rather than calling the provider with no valid token

### Requirement: Operation time budgets

The speech-to-text operation SHALL enforce the following budgets and fail with the appropriate error when any is exceeded: connection establishment at most **10 seconds**, audio upload at most **60 seconds**, transcription response at most **120 seconds**, and total end-to-end operation at most **150 seconds**.

#### Scenario: Connection budget exceeded

- **WHEN** establishing the network connection takes longer than 10 seconds
- **THEN** the operation fails and a network/timeout error is surfaced

#### Scenario: Response budget exceeded

- **WHEN** a transcription response is not received within 120 seconds
- **THEN** the operation fails with a timeout error

#### Scenario: Total operation budget exceeded

- **WHEN** the end-to-end operation exceeds 150 seconds
- **THEN** the operation is aborted and a timeout error is surfaced

### Requirement: Speech-to-text error taxonomy

The pipeline SHALL classify every failure into one of the following categories so the UI can present the matching message: network/connection error, audio-too-long error, transcription-response error, empty/no-audio error, timeout error, and a generic fallback for any unclassified failure.

#### Scenario: Network failure classified

- **WHEN** the operation fails because of lost or unstable network connectivity
- **THEN** the failure is classified as a network/connection error

#### Scenario: Empty audio classified

- **WHEN** the captured audio contains no detectable speech
- **THEN** the failure is classified as an empty/no-audio error

#### Scenario: Audio-too-long classified

- **WHEN** the captured audio exceeds the accepted length for transcription
- **THEN** the failure is classified as an audio-too-long error

#### Scenario: Unclassified failure falls back to generic

- **WHEN** the operation fails for a reason that does not match a specific category
- **THEN** the failure is classified as a generic error

### Requirement: Debug logging of the transcription query and result

In debug builds, the app SHALL log the transcription query that was performed together with its result (the transcript or the failure), to aid troubleshooting. Logging SHALL NOT expose the real provider API key.

#### Scenario: Successful transcription logged

- **WHEN** a transcription completes successfully in a debug build
- **THEN** the app writes a debug log entry containing the query and the resulting transcript

#### Scenario: Failed transcription logged

- **WHEN** a transcription fails in a debug build
- **THEN** the app writes a debug log entry containing the query and the failure outcome
