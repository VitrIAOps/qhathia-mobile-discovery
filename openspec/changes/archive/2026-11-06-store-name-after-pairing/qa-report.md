# QA report — store-name-after-pairing

**Legend:** ✔ works · ✖ fails (bug present) · ◐ partial · ⏸ blocked / not run · — not applicable.

| | |
|---|---|
| App (before) | `profile` on `490694e` (QA regression 2026-11-03) |
| App (after) | `profile` with this change (branch `fix/store-name-after-pairing`) |
| Store | QA store `6b89ea64-d315-4294-9c69-cd412cd8b54a`, never El Martillo |

## Status

- **Handed to the QA team (2026-11-06):** the cases below. Verified with unit tests (`:core:network`, `:core:data`, `:features:settings`, `:features:pairing`), `lintDebug` and a `profile` build.

## Cases for the QA team

| Case | Steps | Expected after | Before (QA 03/11) | After | Who |
|---|---|---|---|---|---|
| D:VIN-003 / X:XVI-003 (Home) | `pm clear` → pair from the staff app | Home shows "QA DISCOVERY — FERRETERÍA" in ≤ 1 s, without opening Settings | ✖ "TIENDA" until a force-stop | ⏸ | QA team |
| D:VIN-003 (Settings) | Gear → Red Wifi, then Vincular | Both toolbars "QA DISCOVERY — FERRETERÍA"; side menu "QA Discovery — Ferretería" | ✖ "TIENDA" / "Tienda" | ⏸ | QA team |
| Unpaired | `pm clear`, open Settings before pairing | "TIENDA" / "Tienda" | ✔ | ⏸ | QA team |
| AJU-030 / AJU-032 (regression) | As in the regression | "Actividad" and "Código usado" still shown | ✔ | ⏸ | QA team |

## Unit-test coverage of the delta spec

| Spec scenario (`store-session`) | Test |
|---|---|
| Home right after pairing | `DevicePairingRepositoryImplTest.exchange_with_store_name_saves_it_for_home` (Home already reads the session name) |
| Settings titles | `SettingsHostViewModelTest.store_name_follows_the_session`; titles wired in `SettingsScreen`; previews with the name (light + dark) |
| Name unknown | `exchange_without_store_name_keeps_the_session_name`, `blank_store_name_reads_as_unknown`; fallback strings in the composables |
