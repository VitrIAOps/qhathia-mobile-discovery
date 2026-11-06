## Why

After a reboot the tablet stays on Android's launcher: the showcase does not open, nothing is pinned, and no heartbeat is sent, so the staff app shows it as inactive after 10 min (QA regression 2026-11-03, F-12 / X:XRE-033). Nobody watches the stand, so a power cut or a battery restart leaves it unusable and open to other apps. The field tablets run Android 10 or later (product, 2026-11-06), where an app cannot open its own screen from the background.

## What Changes

- **Boot receiver.** On `BOOT_COMPLETED`, a paired tablet opens `MainActivity`, which pins itself as it already does; the process starts, so the heartbeat starts too. It works on API 26–28 (the QA emulator). An unpaired tablet is left alone.
- **Discovery as the home screen.** `MainActivity` also answers `HOME`. On each field tablet, the installer picks "Discovery → Always" once; from then on Android opens the showcase after every boot, on any version. This is the path for Android 10+.
- **No double start.** When Discovery is already the default home, the receiver does nothing, because Android opens it.
- **Setup guide.** `docs/android/tablet-kiosk-setup.md`: choosing Discovery as home, unlocking once after a reboot when the tablet has a lock PIN, and how to reach Android's launcher (change the default home in Settings).
- **BREAKING** (operation): with Discovery as home, the Home button always returns to the showcase, also after "Salir del modo kiosko".

## Capabilities

### New Capabilities
- `kiosk-boot-start`: the showcase comes back by itself after the tablet reboots.

### Modified Capabilities
<!-- none: kiosk-lock-mode rules (pinning on open, re-lock) are reused as they are -->

## Impact

- `:app` shell only: new `BootCompletedReceiver` and a small launch rule; `AndroidManifest.xml` (`RECEIVE_BOOT_COMPLETED`, `<receiver>`, second `<intent-filter>` with `HOME` + `DEFAULT` on `MainActivity`).
- No `launchMode` change and no change to `RestartActivity` (F-09 stays as it is).
- `docs/android/tablet-kiosk-setup.md`.
- No new module, library, endpoint or backend change. Device owner (pinning without the dialog) stays blocked on operation.
