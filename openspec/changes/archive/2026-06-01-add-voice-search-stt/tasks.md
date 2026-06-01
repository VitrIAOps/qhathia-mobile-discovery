## 1. Build infrastructure (version catalog & plugins)

- [x] 1.1 Add Ktor versions/libraries to `gradle/libs.versions.toml`: `ktor` (3.4.3) with `ktor-client-core`, `ktor-client-cio`, `ktor-client-content-negotiation`, `ktor-client-logging`, `ktor-serialization-kotlinx-json`, plus `kotlinx-serialization-json` (`serialization` = 1.10.0)
- [x] 1.2 Add the `kotlin-serialization` plugin alias (`org.jetbrains.kotlin.plugin.serialization`, version.ref = kotlin) to `[plugins]` (and declare it `apply false` in the root `build.gradle.kts`)
- [x] 1.3 Confirm Gradle resolves the catalog with no errors (`gradlew help` succeeds on JDK 21)

## 2. core:network module (new)

- [x] 2.1 Create the module directory `core/network/src/main/java/com/qhathia/mobile/discovery/core/network` (and `src/test/...`)
- [x] 2.2 Write `core/network/build.gradle.kts` using the Android Library template + `kotlin.serialization` + `ksp` + `hilt` (Ktor client deps, kotlinx-serialization, hilt-android, `ksp(hilt.compiler)` + `ksp(kotlin.metadata.jvm)`). `buildConfig=true` with `QHATHIA_BASE_URL`/`DEEPGRAM_BASE_URL` + interim dev creds (read by `core:data`)
- [x] 2.3 Register `include(":core:network")` in `settings.gradle.kts`
- [x] 2.4 Create `core/network/src/main/AndroidManifest.xml` (`<manifest />`)
- [x] 2.5 Add `NetworkResult<T>` sealed type + `Result.toNetworkResult()` mapping
- [x] 2.6 Add `KtorClientFactory` / `provideHttpClient` (CIO; ContentNegotiation JSON with `ignoreUnknownKeys`; `HttpTimeout` connect ≤10s, socket ≤60s upload, request ≤120s response; Logging BODY in debug / off otherwise). Services issue absolute URLs (Qhathia + Deepgram are different hosts)
- [x] 2.7 Add `@Serializable LoginRequestDto`/`LoginResponseDto` (read `accessToken`) and `AuthApiService` calling `POST /api/v1/auth/login`, returning `NetworkResult`
- [x] 2.8 Add `@Serializable SttTokenResponseDto` (`token`, `expiresIn`, `provider`, `issuedAt`) and `SttTokenApiService` calling `POST /api/v1/stt/token` with `Authorization: Bearer <accessToken>`, returning `NetworkResult`
- [x] 2.9 Add `DeepgramClient`: `POST https://api.deepgram.com/v1/listen` (Deepgram base URL from build config) with the recorded audio as the **raw body** (`Content-Type` = `audio/ogg` or `audio/mp4`), query params `model=nova-3&smart_format=true&language=es&punctuate=true`, `Authorization: Token <ephemeralToken>` (`AUTH_SCHEME` const; `Bearer` fallback). Return `NetworkResult<DeepgramListenResponseDto>`
- [x] 2.10 Add `@Serializable` DTOs for the Deepgram response (`metadata`, `results.channels[].alternatives[].transcript/confidence`) with `ignoreUnknownKeys`
- [x] 2.11 Add `NetworkModule` (`@Provides` HttpClient; services are constructor-injected)
- [x] 2.12 Verify the module builds in isolation (`gradlew :core:network:assembleDebug`) — BUILD SUCCESSFUL

## 3. core:domain module (STT ports, models, constants)

- [x] 3.1 Add `SttErrorCategory` (Network, AudioTooLong, TranscriptionResponse, EmptyAudio, Timeout, Generic), `RecordedAudio`, `VoiceSearchResult`, and `SttException` (carries the category) domain models (framework-free)
- [x] 3.2 Add `SpeechToTextRepository` port (`suspend fun transcribe(audio: RecordedAudio): Result<VoiceSearchResult>`) and an `AudioRecorder` port (start / hasDetectedAudio / stop → `RecordedAudio?` / cancel)
- [x] 3.3 Add `TranscribeVoiceSearchUseCase`: empty-audio short-circuits to `EmptyAudio`; wraps `repository.transcribe` in `withTimeout(150s)` → `Timeout` on overrun
- [x] 3.4 Add `VoiceSearchConstants` alongside `HomeConstants`: input-wait 30s, max-recording 45s, slow-response 30s, error-idle 30s, and the connect/upload/response/total budgets
- [x] 3.5 Unit-test `TranscribeVoiceSearchUseCase` (success, empty-audio short-circuit, total-timeout) with fakes + virtual time. (Added `FakeSpeechToTextRepository`, `FakeAudioRecorder`, `VoiceSearchFixtures` to `core:testing`; converted `core:testing` to a pure-Kotlin JVM module so the pure-Kotlin `core:domain` can consume it per the standards' template.)

## 4. core:data module (STT implementation)

- [x] 4.1 Add `:core:network` to `core/data/build.gradle.kts` dependencies; enable `buildConfig` (for debug logging) and `unitTests.isReturnDefaultValues` (so `android.util.Log` doesn't throw in JVM tests)
- [x] 4.2 Implement `AudioRecorderImpl` (`MediaRecorder` → app-private temp file; **AAC in MPEG_4/M4A → `audio/mp4` on all API levels** — switched from Ogg/Opus after device testing showed empty files; mono 16 kHz, 64 kbps; `setMaxDuration(45s)`; latched `hasDetectedAudio()` polled per second; debug logging of file size/duration; delete file after use)
- [x] 4.3 Implement the Qhathia auth seam (`SessionTokenProvider`): obtain the access token via `AuthApiService.login()` using the dev credentials (`core:network` `BuildConfig`), cache it in memory for the `stt/token` call; surface auth failure as an `SttException`. (Used a dedicated in-memory provider rather than `StoreSession` — cleaner interim seam, same effect, single replace point when pairing lands.)
- [x] 4.4 Implement `SpeechToTextRepositoryImpl`: ensure a Qhathia JWT (4.3) → fetch ephemeral token via `SttTokenApiService` → call `DeepgramClient`, map via `SttErrorMapper` (`NetworkErrorKind` → Timeout/Network/Generic, token 5xx → TranscriptionResponse, deepgram 413 → AudioTooLong else TranscriptionResponse, blank transcript → EmptyAudio); token used once, not retained
- [x] 4.5 Add debug-only logging of the query (audio mime/duration/size) + result (transcript or error category), guarded by `BuildConfig.DEBUG`; never log the ephemeral token or credentials
- [x] 4.6 Add `SttDataModule` (`@Binds` repository + `AudioRecorder`)
- [x] 4.7 Unit-test `SpeechToTextRepositoryImpl` error mapping with MockK'd services (success, blank-transcript → EmptyAudio, token 5xx → TranscriptionResponse, deepgram connectivity → Network, auth-failure propagation)

## 5. core:ui module (processing & error components)

- [x] 5.1 Extract the animated three-dot label into a reusable `core:ui` `ProcessingMessage` component (processing messages only) with light + dark `@Preview`
- [x] 5.2 Add `core:ui` `SttErrorContent` component (title + message + try-again action, design-system styled, takes plain strings) with light + dark `@Preview`
- [x] 5.3 Add string resources for the processing copy (Escuchando…, Organizando ideas…, slow-response) and the six error title/message pairs, plus an `SttErrorCategory`→strings resolver. (Strings + resolver live in `features:home`, alongside the existing home copy and where `core:domain` is available — `core:ui` stays domain-free; see group 6.)

## 6. features:home module (state machine + permission)

- [x] 6.1 Extend `MicState` with `Transcribing`, `SlowResponse`, `Success(transcript)`, `Error(category)` (+ existing `Idle`/`Listening`/`NoAudioRetry`); `HomeUiState.areControlsVisible` derived (hidden during Listening/Transcribing/SlowResponse/Error)
- [x] 6.2 Inject `AudioRecorder` + `TranscribeVoiceSearchUseCase` into `HomeViewModel` (ports from `core:domain`; impls bound in `core:data`, aggregated by `:app`) — no new module dep needed beyond `core:domain`
- [x] 6.3 Rework `HomeViewModel`: idle → listening(record; 45s cap loop with a 30s no-audio guard via `hasDetectedAudio`) → transcribing → slow-response(after 30s) → success(+`VoiceTranscribed` effect)/error; empty/no-audio → `NoAudioRetry` rotating prompts; 30s error-idle auto-returns to idle; `StopRecording` finishes early; cancel/new attempt tears down recorder + in-flight jobs
- [x] 6.4 Handle the microphone permission in `HomeScreen` (gate `ActivateMic` behind `RequestPermission`; Toast informs the consumer when denied)
- [x] 6.5 Update `HomeContent` to render processing messages (`ProcessingMessage`) and `SttErrorContent` per state; light/dark `@Preview`s for idle / listening / transcribing / slow-response / retry / error
- [x] 6.6 Unit-test `HomeViewModel` (activate hides controls; record→stop→success + transcript effect; no-audio → retry + varies; transcription failure → error category; error-idle → idle; slow-response after 30s; cancel restores + recorder cancelled) with Turbine + fakes

## 7. Wire the app shell (:app)

- [x] 7.1 Add `implementation(project(":core:network"))` to `app/build.gradle.kts` so its Hilt bindings aggregate into the graph. (Build-config fields — `QHATHIA_BASE_URL`, `DEEPGRAM_BASE_URL`, `QHATHIA_DEV_EMAIL`/`QHATHIA_DEV_PASSWORD` — live in `core:network`'s `BuildConfig` instead of `:app`, so they sit next to the network code that reads them and `core:data` can use the creds directly.)
- [x] 7.2 Request the `RECORD_AUDIO` permission at app open (`MainActivity.onCreate` launches `RequestPermission` when not granted)
- [x] 7.3 Navigation to the results screen remains out of scope: success emits `HomeUiEffect.VoiceTranscribed` (observed, no-op) and the data layer logs the transcript

## 8. Manifest audit

- [x] 8.1 Add `<uses-permission android:name="android.permission.RECORD_AUDIO" />`, `INTERNET`, and `ACCESS_NETWORK_STATE` to `app/src/main/AndroidManifest.xml` (+ `android:usesCleartextTraffic="true"` since the Qhathia API is plain HTTP)
- [x] 8.2 `RECORD_AUDIO` is requested at runtime both at app open (`MainActivity`) and on mic activation (`HomeScreen`)

## 9b. Post-apply enhancements (requirements added after the HU)

- [x] 9b.1 End-of-speech auto-stop: `AudioRecorder.pollLevel(): AudioLevel` (replaces `hasDetectedAudio`); ViewModel polls every 200 ms and stops + transcribes after 1 s of trailing silence once speech is detected (`SILENCE_AUTO_STOP_MS`); 30 s no-audio + 45 s cap retained; unit-tested
- [x] 9b.2 Error screens reuse the no-audio-retry hero layout (error headline + `MicButton` + `DiscoverySearchBar`, retry via mic); `MicState.Error` keeps controls visible; removed the unused `core:ui` `SttErrorContent` + `stt_error_retry` string; specs updated

## 9. Tests & Definition of Done

- [x] 9.1 `./gradlew test` green across all modules (use case, repository error mapping, ViewModel state machine — 11 home VM tests, 5 repository tests, 3 use-case tests)
- [x] 9.2 `./gradlew lintDebug` passes with no errors (`BUILD SUCCESSFUL`)
- [x] 9.3 Light + dark previews present: `core:ui` `ProcessingMessage` + `SttErrorContent`; `HomeContent` idle/listening/transcribing/slow-response/retry/error (light) + error/idle (dark)
- [x] 9.4 Budgets/behaviors implemented & covered: per-phase Ktor timeouts (10/60/120s) + `withTimeout(150s)` total; 30s error-idle auto-return and 30s slow-response are unit-tested on virtual time; debug logging of query + result is `BuildConfig.DEBUG`-gated `Log.d` in `SpeechToTextRepositoryImpl`. (On-device run not performed in this environment.)
