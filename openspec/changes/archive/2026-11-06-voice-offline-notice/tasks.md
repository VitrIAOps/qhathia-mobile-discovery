## 1. Search feature

- [x] 1.1 `ProductSearchUiEffect.VoiceFailed`; emit it on `Failed` (not `EmptyAudio`) before restoring.
- [x] 1.2 `ProductSearchScreen` Toast; `core:ui` string `discovery_voice_failed`.
- [x] 1.3 Tests: mic without network keeps results and says why; voice entry says why before going back; no speech is silent.

## 2. Build and verify

- [x] 2.1 Tests of `:features:product-search`; `lintDebug`; `:app:assembleProfile`.
- [x] 2.2 `qa-report.md` (D:VOZ-010, Catalog mic offline, VOZ-008, DET-040); hand the emulator run to the QA team.
