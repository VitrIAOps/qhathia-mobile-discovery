## Context

- `device-remote-settings` syncs `DeviceSettings.kioskMode` (default `true`) and exposes it through `ObserveDeviceSettingsUseCase`; `MainViewModel.deviceSettings` already feeds the app shell, which applies `brightnessMode` in `MainActivity` (`ApplyWindowBrightness`). This change follows that pattern.
- The settings gate (`SettingsPasswordGate`, app-hosted overlay) validates the exit PIN offline, or opens settings directly when no PIN is set. Settings are `SettingsScreen` (side menu + Wi-Fi / Vincular sections) at `NETWORK_SETTINGS_ROUTE`; going back pops to `HOME_ROUTE`.
- `RESTART_APP` (`AppRestarterImpl`) launches the launcher activity with `Intent.makeRestartActivityTask` and calls `exitProcess(0)`.
- Network settings connect through `WifiNetworkSuggestion`; on Android 11+ the user approves suggestions from a system notification.
- Constraints from the merchant: **no Device Owner provisioning**, **the app is not a launcher**, the only way out is Configuración + exit PIN, and no PIN means nothing is asked.

## Goals / Non-Goals

**Goals:**
- While `kioskMode` is on and the tablet is paired, a customer cannot leave Discovery through the normal UI (Home, Recents, notification shade, Back).
- Full screen with no system bars, screen always on.
- One sanctioned exit: Configuración → "Salir del modo kiosko".
- React to remote changes of `kioskMode` without a restart.

**Non-Goals:**
- Device Owner / `setLockTaskPackages`, auto-granted permissions, lock task feature flags.
- Being the HOME launcher or auto-starting after a reboot (Android 10+ blocks activity starts from `BOOT_COMPLETED`; the app is not a launcher by decision).
- Blocking the system unpin gesture — not possible without Device Owner (see Risks).
- Idle showcase (`idleShowcaseEnabled`).

## Decisions

### D1 — Screen pinning via `Activity.startLockTask()`

Without an allow-list Android runs the task in `LOCK_TASK_MODE_PINNED`: Home and Recents do nothing, the notification shade and other apps cannot be opened. The app calls `startLockTask()` only when `ActivityManager.lockTaskModeState == LOCK_TASK_MODE_NONE`, so it doesn't ask again while already pinned, and `stopLockTask()` only when it is pinned. Pinning is requested when the activity is resumed and the lock should be active — `startLockTask` requires a foreground activity.

*Alternative:* overlay/accessibility tricks to intercept Home — fragile, need special permissions, and Play policy issues. Rejected.

### D2 — Lock state is derived in the domain

`ObserveKioskLockUseCase` combines three inputs into `KioskLockState(active: Boolean)`:

`active = session.isPaired && settings.kioskMode && !exitedFromSettings`

- `isPaired` from `ObserveStoreSessionUseCase` — onboarding is never pinned (the default `kioskMode = true` would otherwise trap an unpaired tablet before any settings sync).
- `exitedFromSettings` from `KioskSessionRepository`, an **in-memory** `StateFlow<Boolean>` in `core:data` (process-scoped singleton). Not persisted on purpose: any process start (cold start, `RESTART_APP`, reboot + manual open) begins locked.

`ExitKioskModeUseCase` sets the flag; `ResumeKioskModeUseCase` clears it.

*Alternative:* persist the exit in DataStore — a forgotten exit would leave the tablet unlocked indefinitely, which is what the "removeExitPin must be explicit" rule in the contract tries to avoid. Rejected.

### D3 — Exit lives in the settings side menu, behind the existing gate

`SettingsSideMenu` gets a third item, "Salir del modo kiosko", rendered only while the lock is active. It needs no extra PIN prompt: being in settings already means the gate passed (or no PIN is set — then nothing is asked, as the merchant decided). Tapping it calls `ExitKioskModeUseCase`; the shell reacts to the state (unpin, show bars) and the merchant can leave with the Home gesture. A short confirmation text ("Modo kiosko desactivado hasta volver al inicio") explains that it's temporary.

`features:settings` gets a small `KioskExitViewModel` (or extends the side-menu host) exposing `isLockActive` and `onExit`.

### D4 — Re-lock when back on Home

The app shell calls `ResumeKioskModeUseCase` whenever the current route becomes `HOME_ROUTE`. That covers every way back from settings: the side-menu "Volver" and system Back, which pops the destination without going through the `onNavigateBack` callback. If the merchant leaves the app from settings and comes back, the task is still on the settings screen and stays unlocked until they go back to Home. A cold start always begins locked (D2).

### D5 — Shell applies the state in `MainActivity`

A `ApplyKioskLock(state)` composable, next to `ApplyWindowBrightness`, with a lifecycle-aware effect (`LifecycleResumeEffect` / `repeatOnLifecycle(RESUMED)`) keyed on `state.active`:

- active → `startLockTask()` (D1), `WindowInsetsControllerCompat.hide(systemBars())` with `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`, add `FLAG_KEEP_SCREEN_ON`.
- inactive → `stopLockTask()` if pinned, show system bars, clear `FLAG_KEEP_SCREEN_ON`.

Re-applying on every resume also covers the cases where the user removed pinning with the system gesture: the app pins again next time it comes back to the foreground.

**Back on Home**: a `BackHandler(enabled = state.active)` on the Home destination swallows Back, so the root activity is never finished (finishing the last activity ends pinning and drops to the launcher). Other screens keep their normal Back.

### D6 — `RESTART_APP` while pinned

`makeRestartActivityTask` targets the same task affinity, so the relaunch should land in the locked task; the new process starts locked (D2) and pins again on resume. **This must be verified on the target tablet first (task 1.1).** If the platform refuses the relaunch while pinned (the "lock task violation" path), `AppRestarter` gets a pre-restart hook: the shell observes a one-shot "release" signal, calls `stopLockTask()`, then the restart proceeds.

## Risks / Trade-offs

- **[The system unpin gesture always works]** Holding Back + Overview (or the gesture equivalent) unpins any non-Device-Owner app. → Tablet setup guide: set a device screen lock and enable *Settings → Security → App pinning → Ask for PIN before unpinning*; then unpinning drops to the lock screen. The app also pins again on its next resume (D5).
- **[Pinning confirmation dialog]** On a non-allow-listed app, Android may show a "Pin app?" dialog each time pinning starts, which a customer could dismiss. → Only request when not already pinned (D1); re-request on resume. Verify the behavior on the target model/Android version (task 1.1); if the dialog appears, it only shows after a cold start, a remote toggle or a return from settings — moments when staff is usually present.
- **[Wi-Fi approval notification is unreachable while pinned]** `WifiNetworkSuggestion` approval comes as a notification and the shade is blocked. → The exit is in the same settings area: the merchant taps "Salir del modo kiosko" before connecting. The side menu shows a hint above "Salir del modo kiosko" while the lock is active.
- **[Runtime permission dialogs]** Mic (on start) and location (Wi-Fi screen) dialogs are launched into the app's own task and are expected to work while pinned. → Verify in task 1.1.
- **[No launcher, no autostart]** After a reboot the tablet shows the system launcher until someone opens Discovery. Accepted by decision; noted in the setup guide.
- **[Settings left unlocked]** If the merchant exits kiosk and leaves the tablet on settings, it stays unlocked until Back to Home or a restart. → Accepted; the temporary-exit message says so. A timeout could be added later.

## Pending team validation

Accepted for this implementation, to be validated with the team (product/ops) — **not blockers**:

- [ ] **System unpin gesture** (Back + Overview) always unpins without Device Owner. Is the per-tablet mitigation (screen lock + "Ask for PIN before unpinning") acceptable operationally?
- [ ] **Wi-Fi approval while pinned**: the merchant must exit kiosk mode before connecting to a new network. Acceptable?
- [ ] **No autostart after a reboot**: someone must open Discovery by hand (the app is not a launcher). Acceptable?
- [ ] **Exit is temporary** (until Back to Home or a restart) and **not persisted**. Confirm this rule.
- [ ] **Pinning confirmation dialog** (if the target tablet shows it): a customer could dismiss it; the app asks again on next resume. Acceptable?

## Emulator findings (API 26, 2560×1600)

- `startLockTask()` shows the "Screen is pinned" confirmation (GOT IT / NO THANKS). A touch outside it dismisses it **without pinning** and is not delivered to the app; the app asks again on the next resume.
- While pinned, the navigation-bar window keeps its touch region (`[0,1504][2560,1600]`) even with the bars hidden. Hidden bars report zero insets, so edge-to-edge content slid into that strip and the settings gear stopped receiving taps. Fix: the app `Scaffold` uses `WindowInsets.systemBarsIgnoringVisibility`.
- Unrelated race found in the process: `SettingsPasswordGate` sent `Reset` before collecting effects; with no PIN, `Unlocked` could be emitted before the collector subscribed and was lost, leaving the gate stuck open and the gear dead. Fixed by subscribing (undispatched) before `Reset`. Verified with 10/10 settings ↔ Home round trips while pinned.

## Open Questions

- Does the target tablet (Lenovo Tab M11) show the pinning confirmation dialog on `startLockTask()`, and does `RESTART_APP` relaunch while pinned? (task 1.1 decides D6.)
- Should the lock also come back after a period of inactivity on the settings screen? Out of scope unless requested.
