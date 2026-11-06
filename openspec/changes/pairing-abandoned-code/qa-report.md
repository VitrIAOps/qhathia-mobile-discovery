# QA report — pairing-abandoned-code

**Legend:** ✔ works · ✖ fails (bug present) · ◐ partial · ⏸ blocked / not run · — not applicable.

| | |
|---|---|
| App (before) | `profile` on `490694e` (QA regression 2026-11-03) |
| App (after) | `profile` with this change (branch `fix/pairing-abandoned-code`) |
| Store | QA store `6b89ea64-d315-4294-9c69-cd412cd8b54a`, never El Martillo |

## Status

- **Handed to the QA team (2026-11-06):** the cases below. Verified with unit tests (`:core:network`, `:core:domain`, `:core:data`, `:features:pairing`, `:app`), `lintDebug` and a `profile` build.

## Cases for the QA team

| Case | Steps | Expected after | Before (QA 03/11) | After | Who |
|---|---|---|---|---|---|
| D:VIN-022 / X:XVI-011 | Unpaired [T]: code A → `am force-stop` → reopen | nginx: `POST /pairing-session/{A}/cancel 204` at start | ✖ no cancel | ⏸ | QA team |
| D:VIN-022 (new code) | Then "Continuar" | Code B; logcat `KtorClient` shows `{"replacesSessionId":"A"}` | ✖ no body | ⏸ | QA team |
| D:VIN-022 (approve A) | Approve `A` by curl | `400 PAIRING_TOKEN_CANCELLED`; `GET /devices` has no new device | ✖ `200 APPROVED`, orphan tablet | ⏸ | QA team |
| VIN-030 | Rotate while a code is shown | No extra `POST /pairing-token` | ✔ | ⏸ | QA team |
| Volver + Continuar | Code shown → Volver → Continuar | Same code, countdown continues, no new request | ✔ | ⏸ | QA team |
| Expiry | Let a code expire | New code; its request carries `replacesSessionId` of the expired one | — | ⏸ | QA team |
| Paired restart | Pair, then force-stop and reopen | No `…/cancel` call | — | ⏸ | QA team |

## Unit-test coverage of the delta spec

| Spec scenario (`device-pairing`) | Test |
|---|---|
| Code abandoned by a force-stop | `DevicePairingRepositoryImplTest.abandoned_code_is_cancelled_and_forgotten`, `abandoned_code_unknown_to_the_backend_is_forgotten`, `failed_cancel_keeps_the_code_for_the_next_request`, `late_cancel_does_not_forget_a_newer_code`; `DevicePairingApiServiceTest.cancel_posts_to_the_session_cancel_path` |
| New code replaces the old one | `first_code_replaces_nothing_and_is_kept`, `second_code_replaces_the_first_even_after_a_restart`; `generate_with_previous_session_sends_replaces_session_id`, `generate_without_previous_session_sends_no_body` |
| Back and continue reuses the code | existing `PairingViewModelTest` reuse tests (unchanged) |
| Cancelled code on screen | `cancelled_status_reads_as_expired` (the view model already requests a new code on `Expired`) |
| Paired tablet | `successful_exchange_forgets_the_code`, `paired_tablet_forgets_the_code_without_cancelling`, `nothing_kept_means_no_request` |
