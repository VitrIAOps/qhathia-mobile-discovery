## Context

The Discovery app already ships the `features:home` slice with a mic state machine that is **UI-only**: `HomeViewModel` runs a 30s `viewModelScope` countdown (`MicState.Listening` → `MicState.NoAudioRetry`) and never records or transcribes. The code itself flags this as the seam for the real integration ("Real speech-to-text is out of scope; the Deepgram integration will later replace the timeout-only path").

This change makes voice search real. The architecture is Clean Architecture + MVI across Gradle modules (`docs/android/android-frontend-standards.md`). There is **no `core:network` module yet** — `settings.gradle.kts` currently declares `:app`, `:core:{common,domain,data,ui,testing}`, `:features:home`. The standards mandate that the Ktor `HttpClient`, API services, and DTOs live in `core:network`, mapping to domain at the `core:data` boundary.

Constraints (from the ClickUp story):
- Deepgram is invoked **only after** the consumer finishes speaking — batch upload, not live streaming.
- Length/time budgets: ≤30s wait for input, ≤45s recording, ≤10s connect, ≤60s upload, ≤120s response, ≤150s total.
- Lightweight audio format for upload; mic permission requested at app open; debug-log the query + result.
- The Deepgram API key never reaches the client — the app fetches a short-lived token from `POST /api/v1/stt/token` (Bearer JWT, `expiresIn` ~60s).
- Error screen idle for 30s → return to Home idle.
- Navigating to / building the results screen is out of scope.

## Goals / Non-Goals

**Goals:**
- Stand up `core:network` (Ktor `HttpClient`, `SttTokenApiService`, `DeepgramClient`, DTOs, `NetworkResult`) per the standards.
- Capture microphone audio on-device in a lightweight format, enforce the recording/wait caps, and transcribe through Deepgram using a backend-issued token.
- Model the full voice-search flow as a ViewModel state machine: idle → listening(recording) → transcribing(organizing) → slow-response → success(transcript) / error(taxonomy), with the 30s error-idle auto-return.
- Enforce every per-phase time budget via Ktor timeouts + a total-operation watchdog.
- Keep the domain framework-free and testable with `TestDispatcher` + Turbine + fakes.

**Non-Goals:**
- The search-results destination screen and consuming the transcript downstream (the transcript is produced and logged; navigation is out of scope).
- Device pairing/auth that issues the session JWT — assumed already present in the session.
- Live/streaming transcription, multi-language selection, or wake-word.
- Persisting audio recordings beyond the in-flight operation.

## Decisions

### Module decomposition

| Module | Type | Change | Responsibility |
|---|---|---|---|
| `core:network` | **New** Android library + Hilt + serialization | created | Ktor `HttpClient` singleton (CIO), `SttTokenApiService` (`POST /api/v1/stt/token`), `DeepgramClient` (transcription call), `@Serializable` DTOs, `NetworkResult` + `toNetworkResult()`, `NetworkModule`. `BASE_URL` via build config. |
| `core:domain` | Pure Kotlin | modified | `VoiceSearchResult`/`SttError` domain models, `SttTokenRepository` + `SpeechToTextRepository` ports, `TranscribeVoiceSearchUseCase`, and STT constants (caps/budgets) alongside `HomeConstants`. Zero Android deps. |
| `core:data` | Android library + Hilt | modified | `SpeechToTextRepositoryImpl` (orchestrates token → Deepgram, maps `NetworkResult` → `Result`/`SttError`), audio-format handling, Hilt bindings. Depends on `core:network` + `core:domain`. |
| `core:ui` | Android library + Compose | modified | Reuse the existing 3-dot animated label as a shared `ProcessingMessage` component; add an `SttErrorContent` component + design tokens/strings for the processing and error copy. |
| `features:home` | Android library + Compose + Hilt | modified | Extend `MicState`/`HomeUiState` with `Transcribing`, `SlowResponse`, `Success`, `Error(category)`; `HomeViewModel` orchestrates recording + use case + budgets + 30s error-idle; runtime mic-permission handling in `HomeScreen`. |
| `:app` | App shell | modified | Manifest permissions (`RECORD_AUDIO`, `INTERNET`, `ACCESS_NETWORK_STATE`); request `RECORD_AUDIO` at launch; `BASE_URL` build-config field; depend on `:core:network`. |

Dependency directions stay legal: `core:data → core:network, core:domain`; `core:network` has **no** domain dependency (mapping happens in `core:data`); `features:home → core:domain, core:ui, core:common`; `app → features:home + core:* (incl. core:network for DI aggregation)`.

**Alternative considered:** put the Ktor client + Deepgram call directly in `core:data`. Rejected — the standards require the HTTP client and API services to live in `core:network`, and `core:network` must not know domain types.

### Audio capture: `MediaRecorder` to a temp file, lightweight format

Record with Android `MediaRecorder` to an app-private temp file using a lightweight container/codec suitable for Deepgram batch upload (e.g. OGG/Opus on API 29+, falling back to AAC/M4A on API 26–28, since `minSdk = 26`). The file is uploaded once recording ends, then deleted. The recorder is owned by a `core:data` (or `core:common`) `AudioRecorder` abstraction behind a domain port so the ViewModel and use case stay testable with a fake.

**Rationale:** the story mandates "lightweight audio formats" and "transcribe only after the user finishes" — a batch file upload fits exactly and avoids a streaming socket. `AudioRecord` (raw PCM) would force us to encode ourselves; `MediaRecorder` gives compressed output directly. **Alternative considered:** Deepgram live WebSocket — rejected, contradicts the "invoke only when the user finishes" rule and adds streaming complexity.

### Voice-search state machine in the ViewModel

Extend `MicState` (or introduce a `VoiceSearchState` carried by `HomeUiState`) with the phases that map 1:1 to the spec:
`Idle → Listening(remainingSeconds) → Transcribing → SlowResponse → (Success(transcript) | Error(SttErrorCategory))`, plus the existing `NoAudioRetry` reached from the empty-audio outcome. `areControlsVisible` stays derived (controls hidden whenever a voice-search attempt is active). A single `viewModelScope` job owns the flow; cancellation (user cancel / new attempt) tears down the recorder and any in-flight call.

Timers: the 30s input-wait and the 30s "slow response" message and the 30s error-idle auto-return are `delay`-based within the job/state, so they are deterministic under `TestDispatcher`. The use case returns a `Result<VoiceSearchResult>` mapped to `Success`/`Error`; the ViewModel never touches Ktor or DTOs.

**Rationale:** keeping orchestration in the VM honors "no business logic in composables" and makes every budget/transition unit-testable with Turbine. **Alternative considered:** drive timers from `LaunchedEffect` in the composable — rejected, untestable and pushes timing into UI.

### Time budgets: Ktor timeouts + a total-operation watchdog

Per-phase HTTP budgets are configured on the Ktor client (`HttpTimeout`: connect ≤10s, plus request/socket bounds for upload ≤60s and response ≤120s). The **150s total** budget is enforced by wrapping the whole token→record→transcribe sequence in `withTimeout(150_000)` in the use case/repository. Any timeout maps to the `timeout` (or `network`) category.

**Rationale:** HTTP-level budgets belong in the client per the standards ("timeouts … handled inside Ktor plugin configuration"); the end-to-end cap spans non-HTTP phases (recording) so it needs an outer `withTimeout`.

### Error taxonomy mapping

A domain `SttError` sealed type with the six categories from the `voice-search-stt` spec. `core:data` maps causes → category: `NetworkResult.NetworkError`/connect failure → `Network`; HTTP timeout / total watchdog → `Timeout`; empty/blank transcript → `EmptyAudio`; audio length rejected (local 45s cap exceeded or provider 4xx for size) → `AudioTooLong`; provider transcription failure (`502`/malformed) → `TranscriptionResponse`; anything else → `Generic`. `core:ui` resolves each category to its title/message string resource (the copy from the story), so the mapping is testable independent of UI.

### Token handling

`SpeechToTextRepositoryImpl` calls `SttTokenApiService.getToken()` (Bearer JWT from the session) immediately before each transcription, uses the returned ephemeral token as the Deepgram `Authorization`, and discards it after the call. No token caching beyond the single operation (TTL ~60s makes caching pointless and risky).

### Debug logging

A debug-only log (guarded by `BuildConfig.DEBUG`) records the query (e.g. audio duration/metadata) and the result (transcript text or `SttError` category). The Ktor `Logging` plugin uses `LogLevel.BODY` only in debug, `NONE` otherwise. The real Deepgram key is never logged (the client only ever holds the ephemeral token).

## Risks / Trade-offs

- **`minSdk = 26` audio codec availability** → OGG/Opus via `MediaRecorder` is reliable from API 29; API 26–28 needs AAC/M4A. *Mitigation:* select the container by `Build.VERSION.SDK_INT` behind the `AudioRecorder` abstraction; verify Deepgram accepts the chosen mime type.
- **Token endpoint depends on a session JWT that pairing/auth has not delivered yet** → in the current build the store session is locally seeded and may lack a JWT. *Mitigation:* treat a missing/invalid JWT as a token failure → `Generic`/`Network` error screen; wire the real JWT when pairing lands. Flagged as an open question.
- **Deepgram request shape not in `api-contract.md`** → only the Qhathia token endpoint is documented; the Deepgram call contract must come from Deepgram's docs. *Mitigation:* isolate it in `DeepgramClient` so the exact endpoint/params are a single, swappable surface; confirm during apply.
- **New `core:network` + Ktor on AGP 9 / Kotlin 2.3** → version reconciliation (Ktor 3.4.3, serialization plugin, `kotlin-metadata-jvm` on the Hilt ksp classpath). *Mitigation:* follow the canonical catalog + the metadata-override note; validate with a clean `:core:network:assembleDebug` before wiring consumers.
- **Permission denied / kiosk without mic** → voice search must degrade gracefully. *Mitigation:* spec gates capture on permission; UI informs the consumer and keeps the rest of Home usable.
- **Large surface in one change (new module + 5 modified)** → *Mitigation:* tasks ordered so `core:network` compiles first, then domain ports, then data, then UI/feature, then `:app`; DoD (lint + unit tests + previews) gates completion.

## Migration Plan

1. Add Ktor + serialization to the version catalog; create and register `core:network`; validate it builds in isolation.
2. Add STT domain models/ports/use case + constants to `core:domain`.
3. Implement `core:data` orchestration (token → record → Deepgram), error mapping, and Hilt wiring against a fake first.
4. Add `core:ui` processing/error components + strings; extend `features:home` state machine and wire the use case + permission handling.
5. Update `:app` manifest (permissions), request `RECORD_AUDIO` at launch, add `BASE_URL`, depend on `:core:network`.
6. Rollback is additive: the change is behind no flag; reverting the module + feature edits restores the UI-only mic. No data migration.

## Open Questions

- Exact Deepgram transcription endpoint, request params (model, language, mime type), and response shape — to be confirmed against Deepgram docs during apply (the contract doc only covers the Qhathia token endpoint).
- Which audio format/mime the backend-issued Deepgram token expects, and the API 26–28 fallback choice — resolve during apply.
- Source of the session Bearer JWT for `POST /api/v1/stt/token` before device pairing/auth exists — interim behavior is to surface a token error; confirm with product.
- Whether "audio extension max 45s" is purely the local recording cap or also a provider-side rejection to map — assumed local cap; confirm.
