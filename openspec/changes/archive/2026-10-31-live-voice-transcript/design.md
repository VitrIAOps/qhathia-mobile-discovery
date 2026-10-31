## Context

Voice search today is a **batch** pipeline (change `add-voice-search-stt`, archived 2026-06-01):

1. `AudioRecorderImpl` (`core:data`) records AAC/M4A to a temp file with `MediaRecorder`; `pollLevel()` reads `maxAmplitude` every 200 ms.
2. `HomeViewModel` and `ProductSearchViewModel` each run their **own copy** of the endpointing loop (30 s input wait, 45 s cap, 1 s trailing silence).
3. On stop, the clip goes through `TranscribeVoiceSearchUseCase` → `SpeechToTextRepositoryImpl` → `POST /api/v1/stt/token` → `DeepgramClient.transcribe()` (`POST /v1/listen`).

Nothing is known about the words until the upload finishes, so the screen can only show "Escuchando…". The batch design was chosen on purpose ("transcribe only after the user finishes"). This change reverses that rule.

Already in place:
- `ktor-client-websockets` is installed on the shared `HttpClient` (used by `PairingSocketClient`).
- The backend's `/api/v1/stt/token` is documented for exactly this flow: a WSS to Deepgram with `Authorization: Token <token>` (`api-contract.md` §6).
- `RECORD_AUDIO` and `INTERNET` permissions are declared.

Modules involved (all existing, no new Gradle module):

```
features:home ─┐
               ├─→ core:domain (ports, use case, models) ←─ core:data (impl) ─→ core:network (WSS client, DTOs)
features:product-search ─┘        ↑
               └─→ core:ui (ListeningMessage)          core:testing (fakes)
```

## Goals / Non-Goals

**Goals:**
- Stream microphone audio to Deepgram Live while the consumer speaks and show the interim transcript in place of "Escuchando…" on Home and product-search.
- Use the final transcript from the stream for the search, with no batch upload.
- Keep the current endpointing rules, error taxonomy, UI states and navigation unchanged.
- Remove the duplicated listening loop from the two ViewModels.

**Non-Goals:**
- Changing what happens after the transcript (search request, results, unhappy screens).
- Server-side endpointing (Deepgram `endpointing` / `utterance_end_ms`). Local endpointing stays as the single source of truth.
- Falling back to batch transcription when the socket fails.
- A reactive waveform (`todo/reactive-voice-waveform.md`), although PCM levels make it easy later.
- Language selection or wake words.

## Decisions

### D1 — Deepgram Live WebSocket, final transcript from the stream

`DeepgramLiveClient` (`core:network`) opens:

```
wss://api.deepgram.com/v1/listen?model=nova-3&language=es&encoding=linear16&sample_rate=16000
    &channels=1&interim_results=true&smart_format=true&punctuate=true
Authorization: Token <ephemeral token>
```

It sends binary frames with PCM chunks. When the audio flow completes it sends the text frame `{"type":"CloseStream"}` and keeps reading until the server closes. Each `Results` message is decoded into `DeepgramLiveResultDto` (`is_final`, `speech_final`, `channel.alternatives[0].transcript`, `confidence`). Other message types (`Metadata`, `SpeechStarted`, `UtteranceEnd`) are ignored. `DEEPGRAM_BASE_URL` is reused, with the scheme swapped `https`→`wss`, the same way `PairingSocketClient` does it.

**Why:** this is the only way to get words while the consumer is still speaking, and the backend contract already targets it. Taking the final text from the same stream removes the upload step, so "Organizando ideas…" becomes a short wait.

**Alternatives:**
- Android `SpeechRecognizer` for interim results plus the batch upload for the final text. Rejected: it depends on Google speech services, which may be missing or disabled on a kiosk-locked tablet; it transcribes twice and the two results can disagree; and it fights `AudioRecord` for the microphone.
- Live stream for interim results plus batch for the final text. Rejected: double cost and latency for no gain, since Deepgram's `is_final` segments are the same model output.

### D2 — PCM capture with `AudioRecord`

`AudioRecorderImpl` is rewritten on `AudioRecord` (source `VOICE_RECOGNITION`, 16 kHz, mono, `ENCODING_PCM_16BIT`). A read loop on `Dispatchers.IO` reads ~100 ms buffers (3 200 bytes) and:
- pushes each chunk into an unbounded `Channel<ByteArray>`, exposed once per session as `audioChunks: Flow<ByteArray>`. The channel buffers audio captured while the token/socket is still being set up, so the first words are not lost (worst case 45 s × 32 KB/s ≈ 1.4 MB);
- tracks the peak absolute sample since the last `pollLevel()` call, so `pollLevel()` keeps its current contract (Speech/Silence against a floor, polled every 200 ms).

`stop()` stops reading and closes the channel, which makes the stream send `CloseStream`. `cancel()` stops reading and closes the channel with a cancellation. `RecordedAudio` and the temp file go away.

**Why:** Deepgram Live needs either raw PCM or a streamable container. `MediaRecorder` writes MP4, which is not streamable until the file is finalized. Linear16 is what Deepgram recommends for live audio and needs no encoder.

**Alternatives:** `MediaRecorder` with Ogg/Opus to a pipe. Rejected: it already produced invalid files on several devices (see the archived design), and pipe output is unreliable.

`SILENCE_FLOOR` must be recalibrated. `maxAmplitude` and the PCM peak share the same 0–32767 scale, so the current value (1 000) is the starting point, to be confirmed on the kiosk tablet.

### D3 — One shared session orchestrator in `core:domain`

New `RunVoiceSearchSessionUseCase` (pure Kotlin) replaces `TranscribeVoiceSearchUseCase` and owns the whole attempt: start the recorder, start live transcription, run the endpointing loop now duplicated in both ViewModels, and finalize. Both ViewModels call it.

```kotlin
interface VoiceSearchSession {
    val events: Flow<VoiceSessionEvent>   // cold; collecting starts the session, cancelling aborts it
    fun stop()                           // user tap on the mic: end recording now, keep the transcript
}

sealed interface VoiceSessionEvent {
    data class Listening(val transcript: String, val remainingSeconds: Int) : VoiceSessionEvent
    data object Finalizing : VoiceSessionEvent           // recording ended, waiting for the final result
    data class Transcribed(val transcript: String) : VoiceSessionEvent  // terminal
    data object NoAudio : VoiceSessionEvent              // terminal (input-wait elapsed, no speech)
    data class Failed(val category: SttErrorCategory) : VoiceSessionEvent // terminal
}
```

ViewModel mapping (same states as today):

| Event | Home `MicState` | Product-search phase |
|---|---|---|
| `Listening(t, s)` | `Listening(s, liveTranscript = t)` | `VoiceListening(liveTranscript = t)` |
| `Finalizing` | `Transcribing` (→ `SlowResponse` after 30 s, timer stays in the VM) | `VoiceTranscribing` (→ `VoiceSlowResponse`) |
| `Transcribed(t)` | `Success(t)` + navigate | new in-place search |
| `NoAudio` | `NoAudioRetry` | restore the previous screen |
| `Failed(c)` | `Error(c)` | restore the previous screen |

Cancel (`CancelMic`/`CancelVoice`) cancels the collecting job, which cancels the recorder and closes the socket through structured concurrency.

**Why:** the endpointing logic is a business rule shared by both screens and is already duplicated. Adding the streaming state to two copies would double the risk. Keeping it in pure Kotlin keeps it testable with virtual time and fakes, and the ViewModels become thin mappers. `VoiceSearchConstants` stays the single source of timing values.

**Alternative:** keep the loops in each ViewModel and only add transcript collection. Rejected: the duplication grows, and both copies would need the same socket and error handling.

### D4 — Ports and transcript accumulation

- `AudioRecorder` (domain): `start()`, `audioChunks(): Flow<ByteArray>`, `pollLevel()`, `stop()`, `cancel()`.
- `LiveTranscriptionRepository` (domain): `fun transcribe(audio: Flow<ByteArray>): Flow<TranscriptUpdate>`, where `TranscriptUpdate` is `Partial(text)` or `Final(text)`. The flow completes after `Final`, or fails with `SttException(category)`.
- `LiveTranscriptionRepositoryImpl` (`core:data`): `SessionTokenProvider` → `SttTokenApiService.getToken()` → `DeepgramLiveClient.stream(audio, token)`. A pure `TranscriptAccumulator` keeps the confirmed text (concatenated `is_final` segments) and the current interim segment. It emits `Partial(confirmed + interim)` on every change and `Final(confirmed)` when the socket closes normally. A blank final transcript becomes `SttException(EmptyAudio)`, like the batch path does today.
- `SpeechToTextRepository`, `SpeechToTextRepositoryImpl`, `DeepgramClient` and `DeepgramListenResponseDto` are deleted. `SttTokenApiService`, `SessionTokenProvider` and `SttErrorMapper` are reused. The mapper gains a `fromSocketClose(code)` branch: a Deepgram close code in 1008/4xxx (auth or bad request) → `TranscriptionResponse`, anything else → `Network`.

### D5 — UI: `ListeningMessage` in `core:ui`

New stateless composable `ListeningMessage(transcript: String, listeningText: String, ...)`:
- `transcript.isBlank()` → the existing `ProcessingMessage(listeningText)` (animated dots).
- otherwise → `Text(transcript)` in the same typography and color, centered, with **no dots** (per design-system rule: dots only on processing copy). It is wrapped in a height-capped (≈3 lines), vertically scrolling box that auto-scrolls to the end, so the latest words stay visible. The switch is animated with `AnimatedContent` (crossfade); text updates within the transcript are not animated, to avoid flicker.
- Accessibility: `semantics { liveRegion = LiveRegionMode.Polite }` on the transcript.

Home's `ProcessingHero` (listening branch) and product-search's `ProcessingBody` (`VoiceListening`) use it. `MicState.Listening` gets `liveTranscript: String = ""`. `ProductSearchPhase.VoiceListening` changes from `data object` to `data class VoiceListening(val liveTranscript: String = "")`, and `processingTextRes()` and `when` branches are adjusted.

### D6 — Time budgets

- Token request + socket handshake: **10 s** connect budget (existing `CONNECT_TIMEOUT_MS`).
- Streaming lasts as long as the recording (≤ 45 s, local cap).
- Final-result wait after `CloseStream`: **`RESPONSE_TIMEOUT_MS` (120 s)**; slow-response message after 30 s (unchanged).
- End-to-end: **150 s** outer `withTimeout` in the use case (unchanged).
- The 60 s upload budget no longer applies and is removed from `VoiceSearchConstants`.

### D7 — Keep the heard text while the final transcript is awaited

First emulator run (2026-10-24, one-word query "Amoladora"): the token + socket setup took 0.65–0.9 s, the recording 3–5 s, and the wait for the final transcript 0.7–1.3 s. Results lag behind the audio, so for short queries most interim text arrives *after* the 1 s trailing silence ended the recording. The first version dropped those late partials and swapped the transcript for "Organizando ideas…", so the text flashed briefly or not at all.

Change:
- `VoiceSessionEvent.Finalizing` carries the transcript, and the session keeps emitting it as late partials arrive.
- `MicState.Transcribing` and `ProductSearchPhase.VoiceTranscribing` carry `liveTranscript`. `ListeningMessage` shows it, with "Organizando ideas…" as the fallback only while it is blank.
- Home renders every processing phase from one call site, so the text does not reset when listening turns into finalizing.
- The slow-response message (30 s) still replaces the text, since it signals a problem.
- Debug logs record the session timing (connection open, each partial with its offset and whether it came after the stop, and a summary) to compare runs.

**Alternatives considered:**
- A secondary "Organizando ideas…" line under the text. Rejected as more complex for the same result.
- Holding the final text on screen before navigating, and prefetching the Deepgram token. Deferred until the new timing logs show whether they are needed.

## Risks / Trade-offs

- **Ephemeral token TTL (60 s) vs. session length.** → The token is requested right when listening starts and only used for the handshake. Deepgram does not drop an authenticated socket when its key expires mid-stream; *confirm with backend/Deepgram during apply*. If it does, session length is still ≤ 45 s + finalization.
- **Ktor `HttpTimeout` on WebSockets.** `socketTimeoutMillis = 60 s` / `requestTimeoutMillis = 120 s` are set globally. → Verify on CIO that request timeout does not apply to a WS session; the session is under both limits anyway. If needed, override per request with `timeout { }`.
- **Endpointing regression from the recorder rewrite.** → Same `pollLevel()` contract, unit tests on the peak/level computation, and manual calibration on the kiosk tablet before merge.
- **Bandwidth.** ~32 KB/s up (≈1.4 MB for a 45 s session) on store Wi-Fi. → Acceptable for a kiosk; Opus encoding is a later option if needed.
- **Socket fails while the consumer is still speaking.** → The session fails fast with `Network`/`TranscriptionResponse`, the recorder is cancelled, and the existing error screen shows. No batch fallback (Non-Goal).
- **Interim text flicker** (Deepgram rewrites the interim segment). → Only the interim tail changes; confirmed text is stable; no per-update animation.
- **Microphone contention** with another app holding `AudioRecord`. → `AudioRecord.startRecording()` failure maps to `Generic` and shows the error screen instead of crashing.
- **Tests can't use Ktor `MockEngine` for WebSockets.** → `TranscriptAccumulator` and DTO decoding are tested as pure units; the repository is tested against a `DeepgramLiveClient` seam (interface or open class with a fake); the client itself is verified manually.

## Migration Plan

Client-only change on a feature branch; no backend change. Rollback = revert the branch (the batch implementation remains in git history). Debug builds log the session (token request result, socket open/close codes, final transcript), never the token.

## Open Questions

- ~~Does Deepgram keep the socket open after the ephemeral key's 60 s TTL expires?~~ **Resolved 2026-10-31: yes.** The backend issues a project key (`POST /v1/projects/{id}/keys`, `time_to_live_in_seconds: 60`, `usage:write`; `SttTokenService` / `DeepgramSttTokenProvider` in auth-api). Deepgram validates the key only when the WebSocket opens; an open stream stays up after it expires (Deepgram blog "Protecting your API key", GitHub discussion deepgram#673, confirmed again in 2025). A session is at most 45 s (`MAX_RECORDING_SECONDS`), and each capture asks for a fresh token, so expiry never affects a session in progress. Deepgram does close a stream that receives no audio for about 10 s; the recorder sends audio continuously while listening, so this does not apply.
- ~~`nova-3` vs. `nova-2` for live Spanish~~ **Resolved 2026-10-31: keep `nova-3`.** Deepgram lists Spanish (`es`, `es-419`) as a monolingual Nova-3 language for batch and streaming, and reports a 21.4 % relative WER improvement over Nova-2 for Spanish streaming. The QA of 2026-10-25 opened the `nova-3` live socket in production with a valid token. Quality with real customer voices is still a field check (task 8.2). Option to try then: `language=es-419` (Latin American Spanish) instead of `es`.
- Final `SILENCE_FLOOR` value for PCM peak on the kiosk hardware. **Still open:** needs the kiosk tablet and a person speaking (task 8.2).
