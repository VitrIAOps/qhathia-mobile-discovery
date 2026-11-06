## 1. App shell

- [x] 1.1 Home pinning retry loop under `repeatOnLifecycle(RESUMED)`; `startLockTask()` failures logged.
- [x] 1.2 `KioskDeviceAdminReceiver`, `res/xml/device_admin.xml`, manifest `<receiver>`.
- [x] 1.3 `DeviceOwnerKiosk.configure` from `MainActivity.onCreate`.

## 2. Docs

- [x] 2.1 `docs/android/tablet-kiosk-setup.md`: NO THANKS retry and device-owner provisioning.

## 3. Build and verify

- [x] 3.1 `:app` tests; `lintDebug`; `:app:assembleProfile`.
- [x] 3.2 `qa-report.md` (D:EXP-002, D:EXP-007 / X:XRE-030, X:XCR-002; device owner blocked); hand the emulator run to the QA team.
