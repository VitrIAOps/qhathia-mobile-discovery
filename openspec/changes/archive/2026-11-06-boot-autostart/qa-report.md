# QA report — boot-autostart

**Legend:** ✔ works · ✖ fails (bug present) · ◐ partial · ⏸ blocked / not run · — not applicable.

| | |
|---|---|
| App (before) | `profile` on `490694e` (QA regression 2026-11-03) |
| App (after) | `profile` with this change (branch `fix/boot-autostart`) |
| Store | QA store `6b89ea64-d315-4294-9c69-cd412cd8b54a`, never El Martillo |

## Status

- **Handed to the QA team (2026-11-06):** the cases below. Verified with unit tests (`:app`), `lintDebug` and a `profile` build.

## Cases for the QA team

| Case | Steps | Expected after | Before (QA 03/11) | After | Who |
|---|---|---|---|---|---|
| X:XRE-033 (API 26, receiver) | Paired emulator, Discovery **not** chosen as home; `adb reboot`; accept USB debugging | Showcase in front (`dumpsys activity activities \| grep mResumedActivity` → `.MainActivity`), pinning dialog, `POST /devices/heartbeat` in nginx < 30 s after boot | ✖ stays on the launcher, no heartbeat for 7 min | ⏸ | QA team |
| Home path (API 36) | Press Home → "Qhathia Discovery → Always"; `adb reboot` | Showcase opens after boot (Welcome when unpaired; Home + pinning when paired) | ✖ | ⏸ | QA team |
| Home path, no double screen (API 26) | Discovery chosen as home; `adb reboot` | Only one `MainActivity` (`dumpsys activity activities \| grep -c "MainActivity t"`) | — | ⏸ | QA team |
| Unpaired (API 36, not home) | `adb reboot` | The app is not opened | — | ⏸ | QA team |
| Home after leaving kiosk | Discovery as home; Configuración → Salir del modo kiosko; press Home | The showcase comes back and pins again | — | ⏸ | QA team |
| X:XCR-021 (regression) | `RESTART_APP` from the staff app, with Discovery as home and without | Restarts inside the pinned task, as before (F-09) | ✔ | ⏸ | QA team |
| Device owner (no dialog) | — | **Blocked**: operation has not provisioned device owner | ⏸ | ⏸ | Operation |

## Unit-test coverage of the delta spec

| Spec scenario (`kiosk-boot-start`) | Test |
|---|---|
| Paired tablet reboots (Android 9 or earlier) | `BootLaunchPolicyTest.paired_tablet_not_home_is_opened`; launch verified by QA (X:XRE-033) |
| Discovery is the home screen | `discovery_as_home_is_not_opened_twice`; boot as home verified by QA |
| Unpaired tablet reboots | `unpaired_tablet_is_not_opened` |
| Home button with Discovery as home | Manifest `HOME` filter; verified by QA |
