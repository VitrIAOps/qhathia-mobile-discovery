# QA report — pairing-device-name

**Legend:** ✔ works · ✖ fails (bug present) · ◐ partial · ⏸ blocked / not run · — not applicable.

| | |
|---|---|
| App (before) | `profile` on `490694e` (QA regression 2026-11-03) |
| App (after) | `profile` with this change (branch `fix/pairing-device-name`) |
| Store | QA store `6b89ea64-d315-4294-9c69-cd412cd8b54a`, never El Martillo |

## Status

- **Handed to the QA team (2026-11-06):** the cases below. Verified with `:features:pairing` unit tests, `lintDebug` and a `profile` build. UI-only change (one row), covered by the previews.

## Cases for the QA team

| Case | Steps | Expected after | Before (QA 03/11) | After | Who |
|---|---|---|---|---|---|
| X:XVI-030 / X:EXP-011 | Two paired tablets; on each, Ajustes → Vincular | "Nombre en la app del comercio" matches its card in the staff app | ✖ no name shown | ⏸ | QA team |
| Rename | `PATCH /api/v1/devices/{id} {"name":"Mostrador"}`; reopen Vincular | "Mostrador" | ✖ | ⏸ | QA team |
| X:XCR-034 (regression) | As in the regression | "Versión de la app" = versionName from the first heartbeat | ✔ | ⏸ | QA team |
| Repeated "Tablet N" | — | Backend matter, not the tablet | ✖ | — | Backend |
