## Context

`PinLockPolicy` locks for a fixed `PIN_LOCK_DURATION` (24 h) when `failedCount` reaches 5, and `normalize` clears to `PinAttempts.NONE` when the lock ends. Attempts live in DataStore with a fingerprint of the exit PIN hash, so a new or removed PIN reads as `NONE`. The UI shows minutes rounded up ("HH:mm h"). Nothing calls `POST /api/v1/devices/logs`. Modules: `core:domain`, `core:network`, `core:data`, `features:settings`.

## Decisions

- **D1 — Lock level persisted.** `PinAttempts(failedCount, lockedAt, lockCount = 0)`. The 5th failure sets `lockedAt = now` and `lockCount + 1`. When a lock ends, `normalize` keeps `lockCount` (`failedCount = 0, lockedAt = null`), so the next lock is longer. Only a correct PIN (`NONE`) or a PIN change (fingerprint) resets it. New key `settings_pin_lock_count`, removed with the others on `NONE`.
- **D2 — Duration.** `lockDuration(count) = min(30 s × 2^(count−1), 15 min)`, with `count` coerced to at least 1: 30 s, 1, 2, 4, 8, 15, 15… The clock-moved-back rule stays.
- **D3 — Countdown.** `PIN_LOCK_REFRESH_INTERVAL = 1 s`. `SettingsUiState.lockedSecondsLeft`, rounded up to at least 1; the dialog formats `m:ss`.
- **D4 — Log.** `DeviceLogApiService.send(token, DeviceLogsRequestDto(logs = [DeviceLogEntryDto(level, code, message, timestamp, context)]))`. Port `DeviceLogRepository.reportPinLockout(failedAttempts, lockSeconds)` uses the DEVICE token. `SettingsViewModel` calls `ReportPinLockoutUseCase` when `registerPinFailure()` moves from Open to Locked, in its own coroutine; a failure changes nothing on screen.
- **D5 — PIN removed while the dialog is open.** `followLock()` also observes whether a PIN is still required (new `SettingsAccessRepository.observePinRequired()` and `ObserveSettingsPinRequiredUseCase`), and emits `Unlocked` when it is not.

## Risks / Trade-offs

- [The log is lost without network] → Accepted: it is advisory, and the backend does not alert the owner yet.
- [A merchant who keeps mistyping waits up to 15 min] → They can still set a new PIN from the staff app, which unlocks at once.
