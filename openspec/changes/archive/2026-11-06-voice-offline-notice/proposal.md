## Why

Without network, tapping the microphone in Search does nothing: no "Escuchando…", no message (QA regression 2026-11-03, F-17 / D:VOZ-010). The voice session fails while asking for the STT token, before it starts listening, and the screen is restored silently, so the customer does not know why.

## What Changes

- **A voice error says why.** When a voice search in Search fails, a long Toast explains it: without network, "Perdí la conexión… Parece que la red está inestable…"; any other error, "No pude usar el micrófono. Escribe tu búsqueda o inténtalo otra vez."
- The results on screen are kept, and a search opened by voice from the Catalog still goes back to it, now with the message.
- No speech (nothing heard) stays silent, as today.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `product-search`: a failed voice search in Search tells the consumer why.

## Impact

- `features:product-search`: `ProductSearchUiEffect.VoiceFailed`, `ProductSearchViewModel.onVoiceEvent`, `ProductSearchScreen` (Toast); tests.
- `core:ui`: string `discovery_voice_failed`.
- No new module, library or manifest change.
