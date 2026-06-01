## Why

The Discovery Home screen already exposes a microphone button, but it is a UI-only stub: tapping it runs a 30-second countdown that always ends in a "no audio" retry, with no real audio capture or transcription (see the `discovery-home` spec, "Microphone listening window", which explicitly defers speech-to-text). In-store consumers expect to actually speak their search out loud. This change delivers the real speech-to-text (STT) pipeline — microphone capture plus Deepgram transcription — so a consumer can voice a search and have it turned into text, matching the kiosk's hands-free, low-friction promise.

## What Changes

- Introduce a **voice-search STT pipeline**: capture the consumer's microphone audio on-device, request a short-lived Deepgram token from the Qhathia backend (`POST /api/v1/stt/token`), and send the recorded audio to Deepgram for transcription **only after the consumer finishes speaking** (no live streaming).
- Replace the stubbed mic timeout path in **`discovery-home`** with the real flow and its processing states, surfaced through the existing design-system copy + 3-dot animation:
  - *Escuchando…* while recording, *Organizando ideas…* while uploading/transcribing, and *Está tardando más de lo que esperaba…* when a response is slow (>30s).
- Add the **error taxonomy and recovery UX**: dedicated error screens for network loss, audio-too-long, STT processing failure, empty/no-audio, timeout, and a generic fallback — each with its design-system title/message. An error screen left idle for **30 seconds** auto-returns to the Home idle screen.
- Enforce the **business and technical budgets**: ≤30s wait for the consumer to start/produce audio, ≤45s maximum recording length, ≤10s connection, ≤60s audio upload, ≤120s response, ≤150s total operation; use a lightweight audio format for upload.
- Request **microphone permission at app open**, and gate voice search on it.
- Emit **debug logs** of the transcription query and its result.
- **Out of scope:** navigation to / building the search-results screen (the transcribed text is produced but consuming it downstream is a separate change).

## Capabilities

### New Capabilities
- `voice-search-stt`: The end-to-end speech-to-text pipeline — microphone permission, on-device audio capture with the recording/wait length caps, Deepgram token acquisition from the Qhathia API, post-recording transcription, the per-phase operation budgets/timeouts, the STT error taxonomy, and debug logging of the query + result.

### Modified Capabilities
- `discovery-home`: The "Microphone listening window" and "Retry prompts when no audio is detected" requirements change from a UI-only timeout stub to driving the real STT pipeline; the Home experience gains processing-state messaging (listening / organizing / slow-response) and the STT error screens with the 30-second auto-return-to-idle behavior. Mic permission is now requested at app open.

## Impact

- **New module:** `core:network` (Ktor `HttpClient`, STT token API service + DTOs, Deepgram client, `NetworkResult` error mapping) — does not exist yet and is required by the API integration.
- **Modules touched:** `core:domain` (STT use cases, domain models, repository port, timeout/length constants), `core:data` (STT repository impl + audio-format mapping, Hilt wiring), `core:ui` (processing/error message components + 3-dot animation reuse, string/error tokens), `features:home` (extended `MicState`/processing/error states in `HomeUiState`, `HomeViewModel` orchestration, permission handling), `:app` (manifest permissions, mic-permission request at launch).
- **Build / tooling:** `gradle/libs.versions.toml` gains Ktor (client-core, cio, content-negotiation, logging, kotlinx-json) and kotlinx-serialization; `settings.gradle.kts` registers `:core:network`; a `BASE_URL` build-config field for the Qhathia API.
- **Manifest:** add `RECORD_AUDIO`, `INTERNET`, and `ACCESS_NETWORK_STATE` permissions; `RECORD_AUDIO` is requested at runtime on app open.
- **API:** consumes `POST /api/v1/stt/token` (Bearer JWT, returns a temporary Deepgram token, `expiresIn` ~60s) and calls the Deepgram transcription endpoint directly with that token.
- **Out of scope / deferred:** the search-results destination screen and using the transcript to drive a product search; device pairing/auth that issues the JWT the token endpoint requires (assumed already present in the session).
