## Context

- `device-heartbeat` publishes every successful heartbeat as `HeartbeatResult` (`settingsVersion: Long?`) on a replay-1 flow (`ObserveHeartbeatUseCase`).
- `GET /api/v1/devices/me/settings` (DEVICE) returns `idleShowcaseEnabled`, `voiceAssistantEnabled`, `brightnessMode` (`AUTO|LOW|MEDIUM|HIGH`), `kioskMode`, `exitPinHash` (BCrypt or `null`), `autoSyncEnabled`, `settingsVersion`. Each staff-side write bumps `settingsVersion`. A device paired before Phase 21 gets defaults on first read.
- The settings gate today: `SettingsViewModel` caps input at `SETTINGS_PASSWORD_LENGTH = 4`, auto-validates at 4 digits through `SettingsAccessRepository` → `MockSettingsPasswordDataSource` (`== "1234"`). The keypad has no submit key; `PinDots` draws exactly 4 dots.
- The session (incl. the DEVICE JWT) lives in a single Preferences DataStore (`store_session`), unencrypted, app-private.
- Microphone buttons are the shared `core:ui` `MicButton`, used by Home, Catalog, Product search and Product detail.

## Goals / Non-Goals

**Goals:**
- Fetch settings only when the server says they changed, keep them across restarts and offline.
- Make "PIN de salida" and "Asistente por voz" and "Brillo" do what their labels say.
- No PIN set → settings open without a prompt (product decision, 2026-10-18).

**Non-Goals:**
- Kiosk lock (`kioskMode`) — `kiosk-lock-mode`, which will also reuse the exit PIN to leave lock task.
- `idleShowcaseEnabled` / `autoSyncEnabled` — no showcase or sync feature exists to toggle; stored for later.
- Encrypting local storage (see D3).
- Hiding voice-specific copy on Home when voice is off (only the mic control is removed).

## Decisions

### D1 — Version-driven sync, in the application scope

`SyncDeviceSettingsUseCase` collects `ObserveHeartbeatUseCase()`, maps to `settingsVersion`, drops `null`s, `distinctUntilChanged()`, and for each version that differs from the stored one calls `DeviceSettingsRepository.refresh()`. A failed refresh is ignored; because the stored version is unchanged, the next heartbeat with the same version retries (so the distinct filter is applied against the **stored** version, not the previous emission: `filter { it != repository.storedVersion() }`). Launched from `DiscoveryApp` like the heartbeat.

*Alternative:* poll `/devices/me/settings` on a timer — the contract explicitly avoids new polling.

### D2 — Domain model without the hash

`DeviceSettings(idleShowcaseEnabled, voiceAssistantEnabled, brightnessMode, kioskMode, hasExitPin, autoSyncEnabled, version)` with `DeviceSettings.DEFAULT` mirroring the server defaults (`true, true, AUTO, true, false, true, null`). The BCrypt hash never leaves `core:data`: only `SettingsAccessRepositoryImpl` reads it, via `DeviceSettingsLocalDataSource.exitPinHash()`. Unknown `brightnessMode` strings map to `AUTO`.

### D3 — Persist in the existing DataStore, not encrypted storage

The contract suggests encrypted storage for the hash. The DEVICE JWT — a bearer credential far more valuable than a one-way BCrypt hash of a 4–6 digit PIN — already lives unencrypted in the same app-private DataStore; encrypting only the hash adds Keystore code without changing the threat model (the contract itself notes the PIN only stops a curious consumer). Store everything under new keys in `store_session`. Encrypting the session as a whole is a separate hardening change.

*Alternative:* Jetpack `security-crypto` — deprecated; hand-rolled Keystore AES-GCM — real code with device-specific failure modes, for no gain here.

### D4 — BCrypt with `at.favre.lib:bcrypt`

Spring Security hashes are `$2a$10$…`. `at.favre.lib:bcrypt` (pure Java, no native code, verifies `$2a/$2b/$2y`) does `BCrypt.verifyer().verify(pin.toCharArray(), hash).verified`. Verification costs ~100–300 ms on a tablet at cost 10, so `ExitPinVerifier` runs on `Dispatchers.Default`. A malformed hash verifies as `false` (never throws to the UI).

*Alternative:* `org.mindrot:jbcrypt` — unmaintained since 2015, `$2a` only.

### D5 — Gate behavior

- `SettingsAccessRepository.isPinRequired()` = a stored hash exists.
- `SettingsViewModel` starts in `isResolvingAccess = true`; on `Reset` it asks `IsSettingsPinRequiredUseCase`: not required → emit `Unlocked` immediately (the gate never renders the dialog); required → show the keypad.
- Input accepts up to `MAX_PIN_LENGTH = 6`; a confirm key (bottom-left cell, today an empty spacer) is enabled at `MIN_PIN_LENGTH = 4`; reaching 6 digits validates automatically. The dots show one per entered digit, with a minimum of 4 placeholders, so the PIN length is not revealed in advance.
- While validating, `isValidating = true` disables the keypad (BCrypt takes a moment).
- Wrong PIN → clear + inline error (unchanged).

### D6 — Applying settings in the app shell

`MainViewModel` exposes `deviceSettings: StateFlow<DeviceSettings>` (`ObserveDeviceSettingsUseCase`, initial `DEFAULT`).

- **Voice**: `core:ui` declares `val LocalVoiceAssistantEnabled = staticCompositionLocalOf { true }`; `MicButton` returns early when it is `false`. `DiscoveryAppRoot` provides it from the settings. One change hides all six mic buttons in four features without touching their ViewModels; voice search can only be started from those buttons, so hiding them is sufficient.
- **Brightness**: a `LaunchedEffect(brightnessMode)` in `DiscoveryAppRoot` sets `window.attributes.screenBrightness`: `AUTO` → `BRIGHTNESS_OVERRIDE_NONE`, `LOW` 0.3, `MEDIUM` 0.6, `HIGH` 1.0 (window-level, needs no `WRITE_SETTINGS` permission, reverts when the app leaves the foreground).

*Alternative for voice:* a `isVoiceEnabled` field in four ViewModels' UiState — four times the code for the same behavior.

## Risks / Trade-offs

- [Settings never synced (offline since install) → no PIN, settings open] → Matches the "no PIN" decision; the first online heartbeat closes the gap.
- [Merchant removes the PIN remotely] → Next sync drops the hash; settings open directly — intended.
- [BCrypt verify latency] → Off the main thread, keypad disabled meanwhile.
- [Removing `1234` breaks muscle memory of QA/dev] → Documented in the proposal (**BREAKING**); dev builds can set a PIN from the staff app.
- [`staticCompositionLocalOf` recomposes the whole tree on change] → Changes are rare (a merchant toggle).

## Migration Plan

On upgrade the first heartbeat carries a `settingsVersion` while none is stored → one settings read. Until then the gate opens without PIN (the mocked `1234` is gone). Rollback = previous APK (mock PIN returns).

## Open Questions

- None blocking. Brightness levels (0.3/0.6/1.0) can be tuned after a field check.
