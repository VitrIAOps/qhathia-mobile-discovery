## 1. App shell

- [x] 1.1 `BootLaunchPolicy.shouldOpen(isPaired, isDefaultHome)` and `BootLaunchPolicyTest`.
- [x] 1.2 `BootCompletedReceiver` (`@AndroidEntryPoint`, `goAsync()`, `ObserveStoreSessionUseCase`, default-home check, opens `MainActivity` with `FLAG_ACTIVITY_NEW_TASK`).
- [x] 1.3 Manifest: `RECEIVE_BOOT_COMPLETED`, `<receiver exported="false">` for `BOOT_COMPLETED`, second `<intent-filter>` `MAIN` + `HOME` + `DEFAULT` on `MainActivity`; no `launchMode` change.

## 2. Docs

- [x] 2.1 `docs/android/tablet-kiosk-setup.md`: choose Discovery as home ("Siempre"), unlock once after a reboot, how to reach Android's launcher, replace "No arranca sola".

## 3. Build and verify

- [x] 3.1 `:app` tests; `lintDebug`; `:app:assembleProfile`.
- [x] 3.2 `qa-report.md` (X:XRE-033 on API 26; home path on API 36; RESTART_APP regression X:XCR-021); hand the emulator run to the QA team.
