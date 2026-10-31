No new Gradle module is needed: every change lands in existing modules (`core:domain`, `core:network`, `core:data`, `core:ui`, `core:testing`, `features:home`, `features:product-search`).

## 1. Domain contracts (`core:domain`)

- [x] 1.1 Add `TranscriptUpdate` (`Partial(text)` / `Final(text)`) and `VoiceSessionEvent` (`Listening(transcript, remainingSeconds)`, `Finalizing`, `Transcribed`, `NoAudio`, `Failed(category)`) models
- [x] 1.2 Change the `AudioRecorder` port: add `audioChunks(): Flow<ByteArray>`, make `stop()` return `Unit`, keep `start()`, `pollLevel()`, `cancel()`; update KDoc
- [x] 1.3 Add the `LiveTranscriptionRepository` port: `fun transcribe(audio: Flow<ByteArray>): Flow<TranscriptUpdate>` (fails with `SttException`)
- [x] 1.4 Add the `VoiceSearchSession` interface (`events`, `stop()`) and `RunVoiceSearchSessionUseCase` that starts the recorder and live transcription, runs the endpointing loop (30 s input wait, 45 s cap, 1 s trailing silence) and finalizes, under the 150 s outer `withTimeout`
- [x] 1.5 Remove the 60 s upload budget from `VoiceSearchConstants`; document the connect / final-response / total budgets for the streaming session
- [x] 1.6 Delete `SpeechToTextRepository`, `TranscribeVoiceSearchUseCase`, `RecordedAudio` and their tests
- [x] 1.7 Unit-test `RunVoiceSearchSessionUseCase` with virtual time: interim → `Listening`; trailing silence → `Finalizing` → `Transcribed`; input wait → `NoAudio`; 45 s cap; `stop()`; repository failure → `Failed`; cancellation cancels the recorder; blank final → `Failed(EmptyAudio)`

## 2. Live client (`core:network`)

- [x] 2.1 Add `DeepgramLiveResultDto` (`type`, `is_final`, `speech_final`, `channel.alternatives[0].transcript/confidence`) with `ignoreUnknownKeys`
- [x] 2.2 Implement `DeepgramLiveClient.stream(audio: Flow<ByteArray>, token: String): Flow<DeepgramLiveResultDto>` on the shared `HttpClient` WebSockets: `wss` URL from `DEEPGRAM_BASE_URL`, query params from design D1, `Authorization: Token`, binary frames for chunks, `{"type":"CloseStream"}` when audio completes, read until server close; surface close reason/code as a typed failure
- [x] 2.3 Verify `HttpTimeout` does not cut the WS session on CIO; add a per-request `timeout { }` override if it does
- [x] 2.4 Delete the batch `DeepgramClient` and `DeepgramListenResponseDto`; update `NetworkModule` bindings
- [x] 2.5 Unit-test DTO decoding for `Results` (interim and final), `Metadata` and unknown message types

## 3. Data layer (`core:data`)

- [x] 3.1 Rewrite `AudioRecorderImpl` on `AudioRecord` (`VOICE_RECOGNITION`, 16 kHz, mono, PCM 16-bit): IO read loop of ~100 ms buffers into an unbounded `Channel`, peak-since-last-poll for `pollLevel()`, `stop()` closes the channel, `cancel()` aborts; map `startRecording()` failure to `SttException(Generic)`
- [x] 3.2 Extract the PCM peak/level computation into a pure function and unit-test it (silence buffer, speech buffer, little-endian decoding)
- [x] 3.3 Implement the pure `TranscriptAccumulator` (confirmed `is_final` segments + current interim → `Partial`; normal close → `Final`) and unit-test it
- [x] 3.4 Implement `LiveTranscriptionRepositoryImpl`: `SessionTokenProvider` → `SttTokenApiService.getToken()` → `DeepgramLiveClient.stream()` → accumulator; map token HTTP errors, network errors and socket close codes through `SttErrorMapper` (add `fromSocketClose`); blank final → `EmptyAudio`; debug logs without the token
- [x] 3.5 Introduce a test seam for `DeepgramLiveClient` (interface in `core:network` or fake-able open class) and unit-test the repository: success, token failure, socket failure mid-stream, blank final
- [x] 3.6 Update `SttDataModule`: bind `LiveTranscriptionRepository`, remove the `SpeechToTextRepository` binding; delete `SpeechToTextRepositoryImpl` and its test

## 4. Test fakes (`core:testing`)

- [x] 4.1 Update `FakeAudioRecorder` to the new port (scripted levels + chunk flow, records `stop`/`cancel` calls)
- [x] 4.2 Replace `FakeSpeechToTextRepository` with `FakeLiveTranscriptionRepository` (scripted `TranscriptUpdate` sequence or failure)

## 5. Shared UI (`core:ui`)

- [x] 5.1 Add the stateless `ListeningMessage(transcript, listeningText, ...)`: blank → `ProcessingMessage`; otherwise centered text without dots in a ~3-line height-capped box that auto-scrolls to the end; `AnimatedContent` crossfade between the two; `liveRegion = Polite`
- [x] 5.2 Add previews (empty, short transcript, long transcript)

## 6. Home (`features:home`)

- [x] 6.1 Add `liveTranscript: String = ""` to `MicState.Listening`; update previews
- [x] 6.2 Refactor `HomeViewModel` to use `RunVoiceSearchSessionUseCase`: map events per design D3, keep the 30 s slow-response timer for `Finalizing`, `StopRecording` → `session.stop()`, `CancelMic` → cancel the collecting job; remove the local endpointing loop
- [x] 6.3 Use `ListeningMessage` in `HomeContent`'s listening branch
- [x] 6.4 Update `HomeViewModelTest` with the fakes: live transcript reaches `Listening`, success navigates, no audio, error, cancel, user stop

## 7. Product search (`features:product-search`)

- [x] 7.1 Change `ProductSearchPhase.VoiceListening` to `data class VoiceListening(val liveTranscript: String = "")`; update `processingTextRes()` and every `when` branch
- [x] 7.2 Refactor `ProductSearchViewModel` to use `RunVoiceSearchSessionUseCase` (same mapping; failure/no-audio restores the previous screen); remove the local endpointing loop
- [x] 7.3 Use `ListeningMessage` in `ProductSearchContent`'s `ProcessingBody` for `VoiceListening`
- [x] 7.4 Update `ProductSearchViewModelTest`: live transcript in `VoiceListening`, final transcript starts an in-place search, failure restores the previous screen, cancel

## 8. Verification

- [x] 8.1 Run unit tests for all touched modules and a debug build with JDK 21 (`./gradlew testDebugUnitTest assembleDebug`)
- [ ] 8.2 On the kiosk tablet: calibrate `SILENCE_FLOOR` for PCM, confirm the first words are not lost, transcript appears within ~1 s of speaking, end-of-speech still triggers at 1 s, and cancel closes the socket
  - **Pending field check (decision 2026-10-31):** the change is archived without it. It needs the kiosk tablet (or the same model) by USB and someone speaking; the emulator cannot calibrate the threshold for the real microphone. Run it before rolling out to stores and record the `SILENCE_FLOOR` value and the measured latency here.
- [x] 8.3 Confirm open questions from design.md (token TTL vs. open socket, `nova-3` live quality in Spanish) and record the outcome in design.md
  - Done 2026-10-31 from the Deepgram docs and the backend code (see design.md, Open Questions): the socket survives the 60 s key expiry; `nova-3` supports Spanish streaming. Live quality with real voices moves to the field check in 8.2.
- [x] 8.4 Update `docs/android/api-contract.md` §6 client notes if the final query params or auth scheme differ from the documented example

## 9. Keep the heard text while finalizing (design D7)

- [x] 9.1 Debug timing logs in `LiveTranscriptionRepositoryImpl`: connection open offset, each partial with its offset and whether it arrived after the stop, and a session summary
- [x] 9.2 `VoiceSessionEvent.Finalizing(transcript)`; the session keeps emitting late partials after the recording ends instead of dropping them
- [x] 9.3 `MicState.Transcribing(liveTranscript)` and `ProductSearchPhase.VoiceTranscribing(liveTranscript)`; ViewModels refresh the text on later `Finalizing` events and keep the slow-response message once shown
- [x] 9.4 UI shows the heard text in place of "Organizando ideas…" (fallback only while blank); Home renders all processing phases from one call site
- [x] 9.5 Tests: late partials in the session use case, Home and product-search ViewModels; `FakeLiveTranscriptionRepository.latePartials`
- [x] 9.6 Home keeps the final text (mic animated, controls hidden) in `MicState.Success` during the navigation transition; `HomeUiEvent.ScreenStopped` (lifecycle `ON_STOP`) resets it to idle
- [x] 9.7 On the emulator/tablet: compare the new timing logs, confirm the text stays visible for short queries and that there is no idle-Home flash before "Revisando catálogo…"
