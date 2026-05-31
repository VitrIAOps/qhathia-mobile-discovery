## 1. Build infrastructure (version catalog & plugins)

- [ ] 1.1 Add Ktor versions/libraries to `gradle/libs.versions.toml`: `ktor` (3.4.3) with `ktor-client-core`, `ktor-client-cio`, `ktor-client-content-negotiation`, `ktor-client-logging`, `ktor-serialization-kotlinx-json`, plus `kotlinx-serialization-json` (`serialization` = 1.10.0)
- [ ] 1.2 Add the `kotlin-serialization` plugin alias (`org.jetbrains.kotlin.plugin.serialization`, version.ref = kotlin) to `[plugins]`
- [ ] 1.3 Confirm Gradle resolves the catalog with no errors (`gradlew help` / `gradlew projects` succeeds)

## 2. core:network module (new)

- [ ] 2.1 Create the module directory `core/network/src/main/java/com/qhathia/mobile/discovery/core/network` (and `src/test/...`)
- [ ] 2.2 Write `core/network/build.gradle.kts` using the Android Library template + `kotlin.serialization` + `ksp` + `hilt` (Ktor client deps, kotlinx-serialization, hilt-android, `ksp(hilt.compiler)` + `ksp(kotlin.metadata.jvm)`)
- [ ] 2.3 Register `include(":core:network")` in `settings.gradle.kts`
- [ ] 2.4 Create `core/network/src/main/AndroidManifest.xml` (`<manifest />`)
- [ ] 2.5 Add `NetworkResult<T>` sealed type + `Result.toNetworkResult()` mapping
- [ ] 2.6 Add `KtorClientFactory` / `provideHttpClient` (CIO; ContentNegotiation JSON; `HttpTimeout` connect ≤10s, upload/request ≤60s, response ≤120s; Logging BODY in debug / NONE otherwise; `BASE_URL` default request)
- [ ] 2.7 Add `@Serializable SttTokenResponseDto` (`token`, `expiresIn`, `provider`, `issuedAt`) and `SttTokenApiService` calling `POST /api/v1/stt/token` with the Bearer JWT, returning `NetworkResult`
- [ ] 2.8 Add `DeepgramClient` that posts the recorded audio (lightweight mime) using the ephemeral token and returns the transcript as `NetworkResult`
- [ ] 2.9 Add `NetworkModule` (`@Provides` HttpClient, `SttTokenApiService`, `DeepgramClient`)
- [ ] 2.10 Verify the module builds in isolation (`gradlew :core:network:assembleDebug`)

## 3. core:domain module (STT ports, models, constants)

- [ ] 3.1 Add `SttErrorCategory` (Network, AudioTooLong, TranscriptionResponse, EmptyAudio, Timeout, Generic) and `VoiceSearchResult`/`SttError` domain models (framework-free)
- [ ] 3.2 Add `SpeechToTextRepository` port (`suspend fun transcribe(audio: <RecordedAudio>): Result<VoiceSearchResult>`) and an `AudioRecorder` port (start/stop/cancel returning a recorded-audio handle)
- [ ] 3.3 Add `TranscribeVoiceSearchUseCase` orchestrating record → transcribe and wrapping the total operation in `withTimeout(150s)`
- [ ] 3.4 Add STT constants alongside `HomeConstants`: input-wait 30s, max-recording 45s, slow-response 30s, error-idle 30s, and the connect/upload/response/total budgets
- [ ] 3.5 Unit-test `TranscribeVoiceSearchUseCase` (success, empty-audio, total-timeout) with fakes + `TestDispatcher`

## 4. core:data module (STT implementation)

- [ ] 4.1 Add `:core:network` to `core/data/build.gradle.kts` dependencies (and serialization if needed)
- [ ] 4.2 Implement `AudioRecorderImpl` (`MediaRecorder` → app-private temp file; OGG/Opus on API 29+, AAC/M4A fallback for 26–28; enforce 45s auto-stop; delete file after use)
- [ ] 4.3 Implement `SpeechToTextRepositoryImpl`: fetch token via `SttTokenApiService`, call `DeepgramClient`, map `NetworkResult` + causes → `SttErrorCategory` (connect/timeout → Timeout/Network, blank transcript → EmptyAudio, size reject → AudioTooLong, provider failure → TranscriptionResponse, else Generic), discard the token after use
- [ ] 4.4 Add debug-only logging of the query + result (guarded by `BuildConfig.DEBUG`; never log the provider key)
- [ ] 4.5 Add `SttDataModule` (`@Binds` repository + `AudioRecorder`)
- [ ] 4.6 Unit-test `SpeechToTextRepositoryImpl` error mapping with a fake API/Deepgram client

## 5. core:ui module (processing & error components)

- [ ] 5.1 Extract the existing animated three-dot label into a reusable `ProcessingMessage` component (processing messages only) with light + dark `@Preview`
- [ ] 5.2 Add `SttErrorContent` component (title + message + try-again action, design-system styled) with light + dark `@Preview`
- [ ] 5.3 Add string resources for the processing copy (Escuchando…, Organizando ideas…, slow-response) and the six error title/message pairs (network, audio-too-long, transcription-response, empty-audio, timeout, generic), plus an error→strings resolver/tokens

## 6. features:home module (state machine + permission)

- [ ] 6.1 Extend the voice-search state in `HomeUiState`/`MicState` with `Transcribing`, `SlowResponse`, `Success(transcript)`, and `Error(category)`; keep `areControlsVisible` derived (hidden during any active attempt)
- [ ] 6.2 Add `:core:data`/use-case dependency wiring needed by `features:home` (inject `TranscribeVoiceSearchUseCase` + `AudioRecorder`)
- [ ] 6.3 Rework `HomeViewModel`: idle → listening(record, 30s input wait) → transcribing → slow-response(after 30s) → success/error; empty-audio → existing `NoAudioRetry` rotating prompts; 30s error-idle auto-returns to idle; cancel/new attempt tears down recorder + in-flight call
- [ ] 6.4 Handle the microphone permission state in `HomeScreen` (gate activation; inform the consumer when denied)
- [ ] 6.5 Update `HomeContent` to render the processing messages and `SttErrorContent` per state; refresh light + dark `@Preview`s for listening / transcribing / slow-response / error states
- [ ] 6.6 Unit-test `HomeViewModel` (record→transcribe→success; slow-response after 30s; each error category surfaces; error-idle returns to idle; cancel restores) with Turbine + fakes

## 7. Wire the app shell (:app)

- [ ] 7.1 Add `implementation(project(":core:network"))` to `app/build.gradle.kts` so its Hilt bindings aggregate into the graph; add the `BASE_URL` build-config field (and serialization plugin if needed)
- [ ] 7.2 Request the `RECORD_AUDIO` permission at app open (in `MainActivity` / app root) before voice search is used
- [ ] 7.3 Confirm navigation to the results screen remains out of scope (transcript is produced + logged only)

## 8. Manifest audit

- [ ] 8.1 Add `<uses-permission android:name="android.permission.RECORD_AUDIO" />`, `INTERNET`, and `ACCESS_NETWORK_STATE` to `app/src/main/AndroidManifest.xml`
- [ ] 8.2 Verify `RECORD_AUDIO` is also requested at runtime (manifest declaration alone is insufficient on API 23+)

## 9. Tests & Definition of Done

- [ ] 9.1 `./gradlew testDebugUnitTest` green (use case, repository error mapping, ViewModel state machine)
- [ ] 9.2 `./gradlew lintDebug` passes with no errors
- [ ] 9.3 Light + dark previews present for the new `core:ui` components and the new `HomeContent` states
- [ ] 9.4 Manually verify (or document) the per-phase budgets and the 30s error-idle auto-return; confirm debug logs print the query + result
