## Why

After 5 wrong settings PINs the gate locks for 24 h (wave 1), shows "24:00 h", and tells nobody. The QA regression of 2026-11-03 (D:AJU-011 / X:XCR-013, F-13) expects a 30 s lock with a countdown and a `POST /devices/logs` `SETTINGS_PIN_LOCKOUT`. Product decision (2026-11-06): a **progressive** lock, which passes QA's 30 s and stays strong against guessing (~3 weeks to try every 4-digit PIN, versus ~17 h with a flat 30 s).

## What Changes

- **Progressive lock.** Each lock lasts 30 s, 1, 2, 4, 8 min, then 15 min per lock (cap). A correct PIN resets it. A new or removed PIN from the staff app resets it too (existing fingerprint rule). It survives killing the app. **BREAKING** (behavior) for the 24 h lock.
- **Countdown in m:ss**, refreshed every second.
- **Lockout log.** Each new lock sends `POST /api/v1/devices/logs` (`WARN`, `SETTINGS_PIN_LOCKOUT`, `context {failedAttempts, lockoutSeconds}`), fire-and-forget, never the PIN or its hash. The backend does not alert the owner yet.
- **Unlock when the PIN is removed** while the dialog is open.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `settings-access`: the lock duration is progressive (30 s to 15 min), the countdown is m:ss, and each lock is reported to the backend.

## Impact

- `core:domain`: `SettingsConstants` (`PIN_LOCK_BASE = 30 s`, `PIN_LOCK_MAX = 15 min`, refresh 1 s); `PinAttempts.lockCount`; `PinLockPolicy.lockDuration(count)`; `RegisterPinFailureUseCase`; new port `DeviceLogRepository` and `ReportPinLockoutUseCase`.
- `core:network`: `DeviceLogApiService` and DTOs.
- `core:data`: `settings_pin_lock_count` key in `SettingsPinAttemptsRepositoryImpl`; `DeviceLogRepositoryImpl` and binding.
- `features:settings`: `lockedSecondsLeft`, m:ss format, string, previews, report on a new lock.
- No new module, library or manifest change.
