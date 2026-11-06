# QA report — voice-offline-notice

**Legend:** ✔ works · ✖ fails (bug present) · ◐ partial · ⏸ blocked / not run · — not applicable.

| | |
|---|---|
| App (before) | `profile` on `490694e` (QA regression 2026-11-03) |
| App (after) | `profile` with this change (branch `fix/voice-offline-notice`) |
| Store | QA store `6b89ea64-d315-4294-9c69-cd412cd8b54a`, never El Martillo |

## Status

- **Handed to the QA team (2026-11-06):** the cases below. Verified with unit tests (`:features:product-search`), `lintDebug` and a `profile` build.

## Cases for the QA team

| Case | Steps | Expected after | Before (QA 03/11) | After | Who |
|---|---|---|---|---|---|
| D:VOZ-010 | Search "taladro" → 3 results → airplane mode → bottom-bar microphone | Within 1 s a Toast "Perdí la conexión… Parece que la red está inestable…"; the 3 results stay | ✖ nothing at 1 s or 5 s | ⏸ | QA team |
| Catalog microphone offline | Catalog → airplane mode → microphone | Back on the Catalog with the same Toast | ✖ | ⏸ | QA team |
| Nothing heard | With network, tap the microphone and stay silent | Screen comes back, no message | ✔ | ⏸ | QA team |
| VOZ-008 / DET-040 (regression) | As in the regression | Unchanged | ✔ | ⏸ | QA team |

## Unit-test coverage of the delta spec

| Spec scenario (`product-search`) | Test |
|---|---|
| Microphone without network | `ProductSearchViewModelTest.given_no_network_when_mic_tapped_then_says_why_and_keeps_results` |
| Voice search opened from the Catalog without network | `given_voice_entry_without_network_then_says_why_before_going_back`, `given_voice_entry_when_stt_fails_then_navigates_back_without_search` |
| Nothing heard | `given_no_speech_failure_then_no_message` |
