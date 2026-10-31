## Why

While the consumer speaks, the kiosk only shows a static "Escuchando…" message, so they get no confirmation that they are being understood until the whole recording is uploaded and transcribed. Showing the words as they are recognized makes voice search feel responsive and lets the consumer notice a misheard query right away. The backend already issues Deepgram tokens meant for a live WebSocket session (`docs/android/api-contract.md` §6), so the client is the only missing piece.

## What Changes

- **BREAKING (spec)**: Voice search switches from batch transcription (record a file, then `POST /v1/listen`) to **live streaming transcription** over Deepgram's WebSocket (`wss://api.deepgram.com/v1/listen`) with interim results. Audio is streamed while the consumer speaks; the final transcript comes from the same stream, so the separate upload step disappears.
- Microphone capture moves from a compressed file (`MediaRecorder`, AAC/M4A) to raw PCM chunks (`AudioRecord`, 16-bit, 16 kHz, mono) so audio can be streamed as it is captured.
- The existing on-device endpointing stays as is: 30 s input wait, 45 s max recording, 1 s trailing silence after speech. The level is now computed from the PCM chunks.
- On **Home** and on the **product-search** screen (in-place voice search), the "Escuchando…" message is shown until the first words are recognized; from then on it is **replaced by the live transcript**, updated as interim results arrive (no animated dots on the transcript).
- After end-of-speech, the stream is closed and the final transcript drives the search exactly as today. The "Organizando ideas…" phase stays only for the short wait on the final result.
- Error handling keeps the existing taxonomy (network, timeout, empty audio, transcription error, generic). A dropped socket during listening surfaces the matching error screen.

## Capabilities

### New Capabilities

None. The behavior extends existing capabilities.

### Modified Capabilities

- `voice-search-stt`: replaces "Transcription invoked only after the consumer finishes speaking" with live streaming transcription and interim results; capture format becomes PCM; token acquisition happens when listening starts; time budgets are adapted to a streaming session.
- `discovery-home`: "Voice-search processing feedback" — while recording, the listening message is replaced by the live transcript once speech is recognized.
- `product-search`: adds the same live-transcript feedback while capturing voice in place on the results/unhappy screens.

## Impact

- **core:domain**: `AudioRecorder` exposes a PCM chunk stream; new `LiveTranscriptionRepository` port, `TranscriptUpdate` model and `StreamVoiceSearchUseCase`. `TranscribeVoiceSearchUseCase` / `SpeechToTextRepository` are replaced.
- **core:data**: `AudioRecorderImpl` rewritten on `AudioRecord`; new `LiveTranscriptionRepositoryImpl`; `SpeechToTextRepositoryImpl` removed; `SttErrorMapper` reused.
- **core:network**: new `DeepgramLiveClient` (Ktor WebSockets, already a dependency) and live-result DTOs; batch `DeepgramClient` removed.
- **core:ui**: listening label component that switches from `ProcessingMessage` to the live transcript text.
- **features:home**, **features:product-search**: `MicState.Listening` / `ProductSearchPhase.VoiceListening` carry the live transcript; ViewModels collect transcript updates.
- **Network**: ~32 KB/s upstream while listening (uncompressed PCM). The Deepgram token (60 s TTL) must be valid when the socket opens; the session lasts up to 45 s.
- **Tests**: ViewModel tests for both features, new repository/client tests with a fake socket, recorder level tests.
