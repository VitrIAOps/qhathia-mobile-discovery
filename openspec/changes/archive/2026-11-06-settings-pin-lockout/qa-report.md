# QA report — settings-pin-lockout

**Legend:** ✔ works · ✖ fails (bug present) · ◐ partial · ⏸ blocked / not run · — not applicable.

| | |
|---|---|
| App (before) | `profile` on `490694e` (QA regression 2026-11-03) |
| App (after) | `profile` with this change (branch `fix/settings-pin-lockout`) |
| Store | QA store `6b89ea64-d315-4294-9c69-cd412cd8b54a`, never El Martillo |

## Status

- **Handed to the QA team (2026-11-06):** the cases below. Verified with unit tests (`:core:domain`, `:core:network`, `:core:data`, `:features:settings`), `lintDebug` and a `profile` build.

## Cases for the QA team

| Case | Steps | Expected after | Before (QA 03/11) | After | Who |
|---|---|---|---|---|---|
| D:AJU-011 (first lock) | Exit PIN set from the staff app; open Settings; enter 5 wrong PINs | Keypad disabled; "Demasiados intentos. Vuelve a intentarlo en 0:30", counting down every second; at 0:00 the keypad comes back with 5 attempts | ✖ 24 h lock, "24:00 h" | ⏸ | QA team |
| D:AJU-011 (next lock) | After the first lock ends, 5 more wrong PINs | Lock shows 1:00 (then 2:00, 4:00, 8:00, 15:00, 15:00…) | ✖ | ⏸ | QA team |
| D:AJU-011 (reset) | After a lock ends, enter the correct PIN; later 5 wrong PINs | Settings open; the next lock is 0:30 again | — | ⏸ | QA team |
| D:AJU-011 (restart) | While locked, force-stop and reopen the app, open Settings | Still locked with the time left | ✔ (24 h) | ⏸ | QA team |
| D:AJU-011 (PIN removed) | While locked with the dialog open, remove the PIN from the staff app | Settings open without asking | — | ⏸ | QA team |
| X:XCR-013 | 5 wrong PINs; check nginx | One `POST /api/v1/devices/logs` → `204`, body `level WARN`, `code SETTINGS_PIN_LOCKOUT`, `context {failedAttempts: 5, lockoutSeconds: 30}`, no PIN or hash | ✖ nothing sent | ⏸ | QA team |
| X:XCR-013 (offline) | Wi-Fi off; 5 wrong PINs | Lock and countdown as usual; no error on screen | — | ⏸ | QA team |

## Unit-test coverage of the delta spec

| Spec scenario (`settings-access`) | Test |
|---|---|
| First lock lasts 30 seconds | `PinLockUseCasesTest.fifth_failure_locks_for_30_seconds`, `SettingsViewModelTest.given_fifth_wrong_pin_then_locked_for_30_seconds_and_keypad_ignores_input`, `given_locked_then_time_left_refreshes_every_second`, `lock_time_is_formatted_as_minutes_and_seconds` |
| Next lock lasts longer | `each_lock_doubles_up_to_the_15_minute_cap`, `given_30_seconds_passed_then_unlocked_with_five_attempts_and_next_lock_is_1_minute` |
| Lock survives a restart | `SettingsPinAttemptsRepositoryImplTest.lock_is_read_back_by_a_new_instance`, `ended_lock_keeps_its_level_on_disk`, `given_locked_when_gate_reopened_then_still_locked` |
| Correct PIN resets the level | `correct_pin_after_a_lock_resets_the_level`, `new_pin_also_resets_the_lock_level` |
| Lock is reported once | `given_each_new_lock_then_reported_once_with_its_duration`, `given_reopened_while_locked_then_not_reported_again`, `given_report_fails_then_screen_is_unchanged`, `DeviceLogRepositoryImplTest`, `DeviceLogApiServiceTest` |
| PIN removed while the dialog is open | `given_pin_removed_while_dialog_open_then_unlocks` |
