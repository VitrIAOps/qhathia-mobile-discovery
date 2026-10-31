## 1. Implementation

- [x] 1.1 `features/home`: string `home_subtitle_voice_off` ("Escribe lo que buscas, por nombre o detalles").
- [x] 1.2 `HomeContent.headlineFor`: the idle branch uses `home_subtitle_voice_off` when `LocalVoiceAssistantEnabled.current` is `false` (D1).
- [x] 1.3 Previews "Home idle, voice off" in light and dark.

## 2. Verification

- [x] 2.1 `./gradlew :features:home:testDebugUnitTest :features:home:lint assembleDebug assembleProfile` (JDK 21 JBR) green.
- [x] 2.2 Emulator (`Medium_Tablet`, paired): voice off from the server → after the next heartbeat Home shows no microphone and the new subtitle; voice back on → microphone and original subtitle return without restarting. Leave the tablet's settings as they were.
  - Done 2026-10-31 on the `profile` build: voice off (`settingsVersion` 28) → Home without microphone, subtitle "Escribe lo que buscas, por nombre o detalles"; voice on (29) → "Buscar por voz" and "Presiona aquí y describe lo que buscas…" back, no restart. Tablet left as before (voice on, brightness LOW, merchant PIN, pinned).
