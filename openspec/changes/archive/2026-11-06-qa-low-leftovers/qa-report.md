# QA report — qa-low-leftovers

**Legend:** ✔ works · ✖ fails (bug present) · ◐ partial · ⏸ blocked / not run · — not applicable.

| | |
|---|---|
| App (before) | `profile` on `490694e` (QA regression 2026-11-03) |
| App (after) | `profile` with this change (branch `fix/qa-low-leftovers`) |
| Store | QA store `6b89ea64-d315-4294-9c69-cd412cd8b54a`, never El Martillo |

## Status

- **Handed to the QA team (2026-11-06):** the cases below. Verified with unit tests (`:core:domain`, `:core:data`, `:core:ui`, `:features:product-detail`, `:features:home`, `:app`), `lintDebug` and a `profile` build.

## Cases for the QA team

| Case | Steps | Expected after | Before (QA 03/11) | After | Who |
|---|---|---|---|---|---|
| D:EXP-004 (F-07) | `pm clear` → pair → Home | No permission dialog over Home | ✖ dialog on arrival | ⏸ | QA team |
| D:EXP-004 (tap) | Tap the Home microphone | System dialog → ALLOW → "Escuchando…"; DENY → Toast "Necesitamos acceso al micrófono…" | ✔ | ⏸ | QA team |
| X:XCR-006 / X:XRE-001 (F-24) | Airplane mode 90 s → change a setting from the staff app → airplane off | `POST /devices/heartbeat` in nginx ≤ 3 s after the Wi-Fi validates; the setting applies ≤ 10 s | ◐ 8 s, by luck of the 60 s tick | ⏸ | QA team |
| F-24 (retries) | Stop the backend (or block it) for 1 min | nginx/logcat: attempts at about +5, +15, +35, +75 s, then every 60 s; nothing on screen | — | ⏸ | QA team |
| X:EXP-005 (F-63) | `am force-stop` + reopen with Wi-Fi | First heartbeat in logcat `KtorClient`: `"networkStatus":"WIFI"` and `"uptimeSeconds"` > 0 (≈ time since the emulator booted) | ✖ `0` | ⏸ | QA team |
| D:DET-062 (F-43) | Detail of "QA Foco sin foto" | Placeholder icon + "Sin foto", no thumbnails | ✖ 3 gray squares, no label | ⏸ | QA team |
| D:DET-063 | Detail of "QA Foco imagen rota" | Placeholder icon on the main image (no "Sin foto") | ✔ | ⏸ | QA team |
| One photo | Detail of "Foco LED 9W" | Main photo, no empty squares | ✖ 3 gray squares | ⏸ | QA team |
| X:XCA-005 (regression) | Catalog cards without photo | Placeholder icon, as before | ✔ | ⏸ | QA team |

## Unit-test coverage of the delta specs

| Spec scenario | Test |
|---|---|
| `voice-search-stt` First Home / First microphone tap | Home request removed from `MainActivity`; `rememberMicPermissionGate` unchanged; verified by QA (EXP-004) |
| `device-heartbeat` Request fails | `RunDeviceHeartbeatUseCaseTest.given_failed_heartbeat_then_retried_after_5_10_20_40_s_then_every_interval`, `given_success_after_failures_then_back_to_the_interval`, `given_heartbeat_throws_then_loop_survives` |
| `device-heartbeat` Internet comes back | `given_internet_comes_back_then_heartbeat_sent_at_once`, `given_internet_comes_back_during_the_interval_then_heartbeat_sent_at_once`, `given_connectivity_changes_without_regaining_internet_then_no_extra_heartbeat` |
| `device-heartbeat` First heartbeat after the app opens | `DeviceTelemetryProviderImpl.deviceUptimeSeconds()` (platform clock); verified by QA (EXP-005) |
| `product-detail` One photo / No photo | `ProductImageGallery` previews (light + dark); verified by QA (DET-062) |
