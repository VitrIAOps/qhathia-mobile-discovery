## Context

`MainActivity` provides `LocalVoiceAssistantEnabled` (from the stored device settings) and `MicButton` draws nothing when it is `false`. Home picks its hero copy in `HomeContent.headlineFor(micState)`; the idle branch always returns `home_title` + `home_subtitle`, whose text asks the customer to press the microphone.

## Goals / Non-Goals

**Goals:** the idle subtitle matches what is on screen when voice is off, and switches back live when voice is turned on.

**Non-Goals:** Catalog, Search and Detail copy (they show no voice instruction); a design from Figma for the voice-off Home (none exists; the layout does not change, only one line of text).

## Decisions

### D1. Read the composition local in `headlineFor`

The idle branch picks `home_subtitle_voice_off` when `LocalVoiceAssistantEnabled.current` is `false`. This is the same signal `MicButton` uses, so the text and the button can never disagree, and the change applies without a restart.

*Alternative rejected:* carry a `voiceEnabled` flag in `HomeUiState`. It would duplicate a value the UI already has, and the ViewModel would need the settings repository only to pick a string.

### D2. Copy

*"Escribe lo que buscas, por nombre o detalles"* keeps the length and tone of the current subtitle and only replaces the action ("Presiona aquí y describe" → "Escribe"), so the hero keeps its layout.

## Risks / Trade-offs

- [No automated UI test: `features/home` has only ViewModel tests] → Light and dark previews for the voice-off state, plus an emulator check with the real setting.
