No new Gradle module is needed: every change lands in existing modules (`core:domain`, `core:network`, `core:data`, `core:ui`, `core:testing`, `features:home`, `features:product-search`).

## 1. Domain contracts (`core:domain`)

- [ ] 1.1 Add `TranscriptUpdate` (`Partial(text)` / `Final(text)`) and `VoiceSessionEvent` (`Listening(transcript, remainingSeconds)`, `Finalizing`, `Transcribed`, `NoAudio`, `Failed(category)`) models
- [ ] 1.2 Change the `AudioRecorder` port: add `audioChunks(): Flow<ByteArray>`, make `stop()` return `Unit`, keep `start()`, `pollLevel()`, `cancel()`; update KDoc
- [ ] 1.3 Add the `LiveTranscriptionRepository` port: `fun transcribe(audio: Flow<ByteArray>): Flow<TranscriptUpdate>` (fails with `SttException`)
- [ ] 1.4 Add the `VoiceSearchSession` interface (`events`, `stop()`) and `RunVoiceSearchSessionUseCase` that starts the recorder and live transcription, runs the endpointing loop (30 s input wait, 45 s cap, 1 s trailing silence) and finalizes, under the 150 s outer `withTimeout`
- [ ] 1.5 Remove the 60 s upload budget from `VoiceSearchConstants`; document the connect / final-response / total budgets for the streaming session
- [ ] 1.6 Delete `SpeechToTextRepository`, `TranscribeVoiceSearchUseCase`, `RecordedAudio` and their tests
- [ ] 1.7 Unit-test `RunVoiceSearchSessionUseCase` with virtual time: interim → `Listening`; trailing silence → `Finalizing` → `Transcribed`; input wait → `NoAudio`; 45 s cap; `stop()`; repository failure → `Failed`; cancellation cancels the recorder; blank final → `Failed(EmptyAudio)`

## 2. Live client (`core:network`)

- [ ] 2.1 Add `DeepgramLiveResultDto` (`type`, `is_final`, `speech_final`, `channel.alternatives[0].transcript/confidence`) with `ignoreUnknownKeys`
- [ ] 2.2 Implement `DeepgramLiveClient.stream(audio: Flow<ByteArray>, token: String): Flow<DeepgramLiveResultDto>` on the shared `HttpClient` WebSockets: `wss` URL from `DEEPGRAM_BASE_URL`, query params from design D1, `Authorization: Token`, binary frames for chunks, `{"type":"CloseStream"}` when audio completes, read until server close; surface close reason/code as a typed failure
- [ ] 2.3 Verify `HttpTimeout` does not cut the WS session on CIO; add a per-request `timeout { }` override if it does
- [ ] 2.4 Delete the batch `DeepgramClient` and `DeepgramListenResponseDto`; update `NetworkModule` bindings
- [ ] 2.5 Unit-test DTO decoding for `Results` (interim and final), `Metadata` and unknown message types

## 3. Data layer (`core:data`)

- [ ] 3.1 Rewrite `AudioRecorderImpl` on `AudioRecord` (`VOICE_RECOGNITION`, 16 kHz, mono, PCM 16-bit): IO read loop of ~100 ms buffers into an unbounded `Channel`, peak-since-last-poll for `pollLevel()`, `stop()` closes the channel, `cancel()` aborts; map `startRecording()` failure to `SttException(Generic)`
- [ ] 3.2 Extract the PCM peak/level computation into a pure function and unit-test it (silence buffer, speech buffer, little-endian decoding)
- [ ] 3.3 Implement the pure `TranscriptAccumulator` (confirmed `is_final` segments + current interim → `Partial`; normal close → `Final`) and unit-test it
- [ ] 3.4 Implement `LiveTranscriptionRepositoryImpl`: `SessionTokenProvider` → `SttTokenApiService.getToken()` → `DeepgramLiveClient.stream()` → accumulator; map token HTTP errors, network errors and socket close codes through `SttErrorMapper` (add `fromSocketClose`); blank final → `EmptyAudio`; debug logs without the token
- [ ] 3.5 Introduce a test seam for `DeepgramLiveClient` (interface in `core:network` or fake-able open class) and unit-test the repository: success, token failure, socket failure mid-stream, blank final
- [ ] 3.6 Update `SttDataModule`: bind `LiveTranscriptionRepository`, remove the `SpeechToTextRepository` binding; delete `SpeechToTextRepositoryImpl` and its test

## 4. Test fakes (`core:testing`)

- [ ] 4.1 Update `FakeAudioRecorder` to the new port (scripted levels + chunk flow, records `stop`/`cancel` calls)
- [ ] 4.2 Replace `FakeSpeechToTextRepository` with `FakeLiveTranscriptionRepository` (scripted `TranscriptUpdate` sequence or failure)

## 5. Shared UI (`core:ui`)

- [ ] 5.1 Add the stateless `ListeningMessage(transcript, listeningText, ...)`: blank → `ProcessingMessage`; otherwise centered text without dots in a ~3-line height-capped box that auto-scrolls to the end; `AnimatedContent` crossfade between the two; `liveRegion = Polite`
- [ ] 5.2 Add previews (empty, short transcript, long transcript)

## 6. Home (`features:home`)

- [ ] 6.1 Add `liveTranscript: String = ""` to `MicState.Listening`; update previews
- [ ] 6.2 Refactor `HomeViewModel` to use `RunVoiceSearchSessionUseCase`: map events per design D3, keep the 30 s slow-response timer for `Finalizing`, `StopRecording` → `session.stop()`, `CancelMic` → cancel the collecting job; remove the local endpointing loop
- [ ] 6.3 Use `ListeningMessage` in `HomeContent`'s listening branch
- [ ] 6.4 Update `HomeViewModelTest` with the fakes: live transcript reaches `Listening`, success navigates, no audio, error, cancel, user stop

## 7. Product search (`features:product-search`)

- [ ] 7.1 Change `ProductSearchPhase.VoiceListening` to `data class VoiceListening(val liveTranscript: String = "")`; update `processingTextRes()` and every `when` branch
- [ ] 7.2 Refactor `ProductSearchViewModel` to use `RunVoiceSearchSessionUseCase` (same mapping; failure/no-audio restores the previous screen); remove the local endpointing loop
- [ ] 7.3 Use `ListeningMessage` in `ProductSearchContent`'s `ProcessingBody` for `VoiceListening`
- [ ] 7.4 Update `ProductSearchViewModelTest`: live transcript in `VoiceListening`, final transcript starts an in-place search, failure restores the previous screen, cancel

## 8. Verification

- [ ] 8.1 Run unit tests for all touched modules and a debug build with JDK 21 (`./gradlew testDebugUnitTest assembleDebug`)
- [ ] 8.2 On the kiosk tablet: calibrate `SILENCE_FLOOR` for PCM, confirm the first words are not lost, transcript appears within ~1 s of speaking, end-of-speech still triggers at 1 s, and cancel closes the socket
- [ ] 8.3 Confirm open questions from design.md (token TTL vs. open socket, `nova-3` live quality in Spanish) and record the outcome in design.md
- [ ] 8.4 Update `docs/android/api-contract.md` §6 client notes if the final query params or auth scheme differ from the documented example
