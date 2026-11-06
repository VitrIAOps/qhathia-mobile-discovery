# QA report — search-back-to-catalog

**Legend:** ✔ works · ✖ fails (bug present) · ◐ partial · ⏸ blocked / not run · — not applicable.

| | |
|---|---|
| App (before) | `profile` on `490694e` (QA regression 2026-11-03) |
| App (after) | `profile` with this change (branch `fix/search-back-to-catalog`) |
| Store | QA store `6b89ea64-d315-4294-9c69-cd412cd8b54a`, never El Martillo |

## Status

- **Handed to the QA team (2026-11-06):** the cases below. Verified with unit tests (`:features:product-search`, `:app`), `lintDebug` and a `profile` build.

## Cases for the QA team

| Case | Steps | Expected after | Before (QA 03/11) | After | Who |
|---|---|---|---|---|---|
| D:BUS-060 | [T] add a product → Catálogo → bottom bar "taladro" → results → "Volver" | Back on the Catalog, no dialog, cart intact | ✖ "¿Cerrar la búsqueda actual?" → Home | ⏸ | QA team |
| D:BUS-060 (system Back) | Same, with system Back | Same as above | ✖ | ⏸ | QA team |
| Home path | Home → search → "Volver" | Today's close dialog; "Sí, cerrar" empties the cart and goes Home | ✔ | ⏸ | QA team |
| From Detail | Detail → mic or search → results → "Volver" | Back on the Detail, cart intact | — | ⏸ | QA team |
| D:BUS-052 | Results → airplane mode → tap a chip → "Perdí la conexión…" → "Volver" | The previous results again | ✖ (by code) | ⏸ | QA team |
| CAT-024 / CAT-025 (regression) | As in the regression | Unchanged | ✔ | ⏸ | QA team |

## Unit-test coverage of the delta spec

| Spec scenario (`product-search`) | Test |
|---|---|
| Back returns to the previous result | existing back-navigation tests (unchanged) |
| Back after a network error | `ProductSearchViewModelTest.given_network_error_after_a_result_when_back_then_previous_result_shown` |
| Back to the Catalog | `given_no_previous_result_when_back_then_asks_the_screen_to_exit`; the screen's `previousIsHome` branch, verified by QA (BUS-060) |
| Back with no previous result from Home asks to close | `given_no_history_when_back_confirmed_then_session_ends` |
