## 1. Domain

- [x] 1.1 `SettingsConstants`: `PIN_LOCK_BASE = 30.seconds`, `PIN_LOCK_MAX = 15.minutes`, `PIN_LOCK_REFRESH_INTERVAL = 1.seconds`; remove `PIN_LOCK_DURATION`.
- [x] 1.2 `PinAttempts.lockCount`; `PinLockPolicy.lockDuration(count)`, `normalize` keeps `lockCount` when a lock ends; `RegisterPinFailureUseCase` increments it.
- [x] 1.3 Port `DeviceLogRepository.reportPinLockout(failedAttempts, lockSeconds)` and `ReportPinLockoutUseCase`.
- [x] 1.4 Tests (`PinLockUseCasesTest`): 30 s → 1 min → … → 15 min cap; correct PIN resets the level; clock moved back.

## 2. Network and data

- [x] 2.1 `DeviceLogApiService.send` (`POST /api/v1/devices/logs`) and DTOs; `MockEngine` test of the body.
- [x] 2.2 `settings_pin_lock_count` in `SettingsPinAttemptsRepositoryImpl`; `DeviceLogRepositoryImpl` + binding; fakes in `core:testing`; tests.

## 3. Settings feature

- [x] 3.1 `lockedSecondsLeft`, `m:ss` format, string "Demasiados intentos. Vuelve a intentarlo en %1$s"; previews.
- [x] 3.2 Report once per new lock (fire-and-forget); unlock when the PIN is removed while the dialog is open.
- [x] 3.3 `SettingsViewModelTest`: `m:ss`, one report per lock, report failure does not change the UI.

## 4. Build and verify

- [x] 4.1 Tests of `:core:network`, `:core:domain`, `:core:data`, `:features:settings`; `lintDebug`; `:app:assembleProfile`.
- [x] 4.2 `qa-report.md` (D:AJU-011, X:XCR-013, `POST /devices/logs 204` in nginx); hand the emulator run to the QA team.
