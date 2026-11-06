## Context

`ProductSearchViewModel.onVoiceEvent` treats `NoAudio` and `Failed` the same: it restores the phase before voice, or emits `NavigateBack` when the search was opened by voice. Without network the session fails with `Failed(Network)` while asking for the STT token, before `Listening`, so nothing ever changes on screen.

## Decisions

- **D1 — Effect, not state.** New `ProductSearchUiEffect.VoiceFailed(category)`, emitted before the restore. A state field would be lost when the screen navigates back (search opened by the Catalog microphone).
- **D2 — Toast.** The screen shows a long Toast, the same resource QA accepted for the microphone permission (VOZ-008). `Network` → `discovery_error_network_title` + `discovery_error_network_message`; any other category → new `core:ui` string `discovery_voice_failed`.
- **D3 — No speech is silent.** `NoAudio` and `Failed(EmptyAudio)` show nothing; the screen coming back is enough.
- **D4 — No alternative search** and no full-screen `NetworkError`: the results the customer had stay.

- **D5 — Effect buffer 1 → 8.** `VoiceFailed` and `NavigateBack` are emitted back to back; with one slot and `DROP_OLDEST` the second dropped the first before the screen collected it.

## Risks / Trade-offs

- [A Toast is less visible than an in-screen banner] → Accepted (D1); product can switch to a banner later, losing the Catalog case.
