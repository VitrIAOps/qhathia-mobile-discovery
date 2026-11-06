# QA report — pinning-reprompt

**Legend:** ✔ works · ✖ fails (bug present) · ◐ partial · ⏸ blocked / not run · — not applicable.

| | |
|---|---|
| App (before) | `profile` on `490694e` (QA regression 2026-11-03) |
| App (after) | `profile` with this change (branch `fix/pinning-reprompt`) |
| Store | QA store `6b89ea64-d315-4294-9c69-cd412cd8b54a`, never El Martillo |

## Status

- **Handed to the QA team (2026-11-06):** the cases below. Verified with `:app` unit tests, `lintDebug` and a `profile` build. No new unit test: the retry is a Compose effect over the existing, tested `PinningRequestThrottle`.

## Cases for the QA team

| Case | Steps | Expected after | Before (QA 03/11) | After | Who |
|---|---|---|---|---|---|
| D:EXP-002 | Paired, kiosk ON; on Home tap NO THANKS; touch nothing | The dialog comes back within 15 s; GOT IT → `mLockTaskModeState=PINNED` | ✖ never comes back without a touch | ⏸ | QA team |
| D:EXP-002 (no loop) | Keep tapping NO THANKS | At most one dialog every 15 s | ✔ | ⏸ | QA team |
| D:EXP-007 / X:XRE-030 | Salir del modo kiosko → Volver | The dialog appears; `PINNED` after GOT IT | ◐ | ⏸ | QA team |
| X:XCR-002 | Kiosk ON from the staff app with the screen off; turn it on | The dialog, or pinned | — | ⏸ | QA team |
| Background | With the app in the background (kiosk exited, another app open) | No `startLockTask rejected` lines every 15 s in logcat | — | ⏸ | QA team |
| Device owner | `adb shell dpm set-device-owner com.qhathia.mobile.discovery/.kiosk.KioskDeviceAdminReceiver` on a reset emulator without accounts; open the app | `LOCKED` in ≤ 2 s without the dialog; Home button goes to the showcase; Salir del modo kiosko still unpins | ⏸ | ⏸ | Blocked (operation) |

## Coverage of the delta spec

| Spec scenario (`kiosk-lock-mode`) | Test |
|---|---|
| Refused pinning is asked again on Home | unchanged; verified by QA |
| Asked again without a touch | `MainActivity` Home effect (`repeatOnLifecycle(RESUMED)` + 15 s); verified by QA (EXP-002) |
| Not asked in a loop | `PinningRequestThrottleTest` (shared throttle) |
| Device owner pins at once / Not device owner | `DeviceOwnerKiosk` (no-op unless `isDeviceOwnerApp`); device-owner case blocked on operation |
