## Why

After the customer taps NO THANKS on "Pin app?", the tablet only asks again if someone touches Home, so an untouched stand stays unpinned (QA regression 2026-11-03, F-10 / D:EXP-002). Pinning without the dialog needs the app to be device owner, which it cannot be today: there is no device admin component.

## What Changes

- **Ask again without a touch.** While Home is shown and the app is in front, a refused pinning is requested again every 15 s (same throttle as today), and never once pinned.
- **Failures in logcat.** A rejected `startLockTask()` is logged instead of swallowed.
- **Ready for device owner.** New `KioskDeviceAdminReceiver`. When operation makes the app device owner, it allows itself in lock task mode (pins at once, no dialog), hides Home/Recents/notifications while pinned, and makes itself the home screen. Without device owner nothing changes.
- **Setup guide**: provisioning command and its limits.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `kiosk-lock-mode`: pinning is requested again on Home without waiting for a touch; with device owner it pins without the dialog.

## Impact

- `:app` shell: `MainActivity` (retry loop, logging, device-owner setup), new `kiosk/KioskDeviceAdminReceiver.kt` and `kiosk/DeviceOwnerKiosk.kt`, `res/xml/device_admin.xml`, manifest `<receiver>` with `BIND_DEVICE_ADMIN`.
- `docs/android/tablet-kiosk-setup.md`.
- No new module or library. Provisioning device owner is operation's job and stays pending.
