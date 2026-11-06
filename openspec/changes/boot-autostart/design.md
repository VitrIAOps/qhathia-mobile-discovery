## Context

The only `MainActivity` filter is `MAIN` + `LAUNCHER`; there is no boot receiver. The heartbeat, commands and settings sync start in `DiscoveryApp.onCreate`, so only when something starts the process. Opening `MainActivity` already applies the kiosk lock (`ApplyKioskLock` → `startLockTask()`). Field tablets run Android 10+ (product, 2026-11-06); the QA emulator for paired cases is API 26. Since API 29 an app cannot start an Activity from the background, `BOOT_COMPLETED` included, unless it is the home app or a device owner.

## Decisions

- **D1 — Two paths.** A `BootCompletedReceiver` covers API 26–28 (and QA). A `HOME` + `DEFAULT` filter on `MainActivity` covers Android 10+: once chosen as the default home, Android opens it after every boot. Device owner (`addPersistentPreferredActivity`) stays out until operation provisions it.
- **D2 — Launch rule.** The receiver opens `MainActivity` (`FLAG_ACTIVITY_NEW_TASK`) only when the tablet is paired (`ObserveStoreSessionUseCase().first().isPaired`) and Discovery is not the default home (`PackageManager.resolveActivity(HOME, MATCH_DEFAULT_ONLY)` is not this package). The rule is a pure function `BootLaunchPolicy.shouldOpen(isPaired, isDefaultHome)`, unit tested. It uses `goAsync()` and reads the session on `Dispatchers.IO`, always calling `finish()`.
- **D3 — Never only the heartbeat.** The receiver does not start the heartbeat without opening the Activity, or the tablet would report "Activo" while showing the launcher. (Starting the process runs `DiscoveryApp.onCreate` anyway; the heartbeat it starts is the normal one.)
- **D4 — No `launchMode` change.** `singleTask` would break the `RestartActivity` trampoline (F-09). As home, a Home press while the showcase is in front reuses its task.
- **D5 — Direct boot.** The receiver listens to `BOOT_COMPLETED` only, not `LOCKED_BOOT_COMPLETED`: DataStore lives in credential-encrypted storage. A tablet with a lock PIN must be unlocked once after a reboot; this goes in the setup guide.

## Risks / Trade-offs

- [With Discovery as home, the Home button never reaches Android's launcher] → Accepted (it is the point of a kiosk). The guide says how to change the default home in Settings.
- [The first Home press shows Android's "which home app" chooser on QA emulators] → QA picks "Just once" or "Always"; documented.
- [The receiver alone does nothing on Android 10+] → Expected; the home path covers it. Documented in the guide.
