## Why

When the merchant turns the voice assistant off from the staff app, Home hides the microphone but keeps the subtitle *"Presiona aquí y describe lo que buscas, por nombre o detalles"*, which points the customer at a button that is no longer there. Found while closing `device-remote-settings` (task 8.4, 2026-10-31). It breaks the project rule that the screen must not promise an action that does not exist.

## What Changes

- With the voice assistant off, the Home idle subtitle reads *"Escribe lo que buscas, por nombre o detalles"*, which points at the search bar, the only way left to search.
- With voice on (or no settings stored) nothing changes.
- The retry and error copy is untouched: those states can only be reached through the microphone.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `device-remote-settings`: the "Voice assistant setting" requirement adds that the Home subtitle invites typing while voice is off.

## Impact

- **Code:** `features/home` only (`HomeContent.headlineFor`, one string, two previews). It reads the existing `LocalVoiceAssistantEnabled` from `core:ui`; no ViewModel, domain or network change.
- **QA:** check on the paired emulator by turning voice off and on from the server.
