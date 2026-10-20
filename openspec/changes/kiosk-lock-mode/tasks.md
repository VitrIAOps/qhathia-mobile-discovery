## 1. Platform spike (decides D6)

- [ ] 1.1 **(manual — no device connected; pending)** On the target tablet (and an API 26 + API 34 emulator), with a throwaway `startLockTask()` call: note whether the "Pin app?" dialog appears, whether mic/location permission dialogs work while pinned, whether a `WifiNetworkSuggestion` approval is reachable, and whether `Intent.makeRestartActivityTask` + `exitProcess` relaunches while pinned. Record the results in `design.md` → Open Questions.

## 2. Domain

- [x] 2.1 Add `KioskLockState(active: Boolean)` to `core:domain` model.
- [x] 2.2 Add `KioskSessionRepository` port: `exitedFromSettings: StateFlow<Boolean>`, `setExited(Boolean)`.
- [x] 2.3 Add `ObserveKioskLockUseCase` (`isPaired && kioskMode && !exitedFromSettings`, distinct), `ExitKioskModeUseCase`, `ResumeKioskModeUseCase`.
- [x] 2.4 Unit tests for `ObserveKioskLockUseCase`: unpaired → inactive; paired + default settings → active; `kioskMode=false` → inactive; exited → inactive; resume → active again.

## 3. Data

- [x] 3.1 Add `KioskSessionRepositoryImpl` (in-memory `MutableStateFlow(false)`, `@Singleton`) and bind it in a Hilt module in `core:data`.
- [x] 3.2 Unit test: starts `false`, `setExited` round-trips, a new instance starts `false` (not persisted).

## 4. App shell

- [x] 4.1 `MainViewModel` exposes `kioskLock: StateFlow<KioskLockState>` and `resumeKioskMode()`.
- [x] 4.2 Add `ApplyKioskLock(state)` in `MainActivity`, next to `ApplyWindowBrightness`: on resume and on state change, `startLockTask()` only when `lockTaskModeState == NONE`, `stopLockTask()` only when pinned; hide/show system bars with `WindowInsetsControllerCompat` (`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`); add/clear `FLAG_KEEP_SCREEN_ON`.
- [x] 4.3 `BackHandler(enabled = kioskLock.active)` on the Home destination.
- [x] 4.4 Call `resumeKioskMode()` whenever the current route becomes Home (covers the side-menu "Volver" and system Back from settings).
- [ ] 4.5 If task 1.1 shows the relaunch is blocked while pinned: add a pre-restart release signal (`AppRestarter` → shell calls `stopLockTask()` → restart) and a test in `ProcessDeviceCommandsUseCase`; otherwise mark N/A.

## 5. Settings feature

- [x] 5.1 Add a ViewModel (or extend the settings host state) exposing `isKioskLockActive` and `onExitKioskMode` via `ObserveKioskLockUseCase` / `ExitKioskModeUseCase` (MVI: UiState + UiEvent).
- [x] 5.2 `SettingsSideMenu`: "Salir del modo kiosko" item shown only while active; after tapping, show "Modo kiosko desactivado hasta volver al inicio".
- [x] 5.3 Side menu: hint, shown above the exit item while the lock is active, to exit kiosk mode before connecting a new Wi-Fi network (the approval notification is unreachable while pinned).
- [x] 5.4 Strings in `features/settings` resources; previews for the menu with and without the exit item.
- [x] 5.5 ViewModel unit tests: item visibility follows the lock state; exit calls the use case.

## 6. Docs

- [x] 6.1 Tablet setup guide in `docs/`: set a device screen lock, enable "App pinning" + "Ask for PIN before unpinning", no autostart after reboot (open Discovery manually).
- [x] 6.2 Update `docs/handoff/android-pendientes.md` §1 row "PIN de salida del modo kiosko" to point to this change.

## 7. Verification

- [x] 7.1 `./gradlew assembleDebug testDebugUnitTest lint` pass (JDK 21).
- [ ] 7.2 Manual check on a paired tablet: Home/Recents/shade blocked; bars hidden; screen stays on; Back on Home does nothing; staff toggles kiosk off/on → unpins/pins live; with PIN → Configuración → "Salir del modo kiosko" → can leave the app; Back to Home → pinned again; without PIN → settings open directly and exit works; `RESTART_APP` → relaunches locked; unpaired tablet → onboarding not pinned.
