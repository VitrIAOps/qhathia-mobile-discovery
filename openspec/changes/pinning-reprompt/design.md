## Context

`MainActivity` asks for pinning on resume (`applyKioskLock`), on arriving at Home, and on touches on Home, throttled to 15 s (`PinningRequestThrottle`). `requestPinning` returns early when pinned and swallows `startLockTask()` errors. There is no device admin component, so the app cannot be device owner or call `setLockTaskPackages`.

## Decisions

- **D1 — Timer while Home is resumed.** The Home `LaunchedEffect` runs `repeatOnLifecycle(RESUMED) { while (true) { requestPinning(throttled = true); delay(15 s) } }`. Resumed only, so a backgrounded app never calls `startLockTask()` (rejected on API 26). The shared throttle still caps all sources (arrival, touch, timer) to one request every 15 s.
- **D2 — Log, do not swallow.** `startLockTask()` failures go to `Log.w`.
- **D3 — `DeviceOwnerKiosk.configure(activity)`** in `MainActivity.onCreate`: no-op unless `isDeviceOwnerApp`; then `setLockTaskPackages`, `setLockTaskFeatures(NONE)` on API 28+, and `addPersistentPreferredActivity` for `HOME` (so F-12 needs no manual "Siempre", and the boot receiver sees Discovery as the default home and does not open it twice). `SecurityException` is logged.
- **D4 — `KioskDeviceAdminReceiver`** with an empty `<uses-policies/>`, exported and guarded by `BIND_DEVICE_ADMIN`, as Android requires.
- **D5 — No `launchMode` change** (F-09).

## Risks / Trade-offs

- [The dialog reappears every 15 s for a customer who keeps refusing] → Intended (EXP-002); at most one per 15 s.
- [Device owner can only be set on a reset tablet without accounts] → Documented; until then the dialog path stays.
- [`LOCK_TASK_FEATURE_NONE` hides notifications while pinned] → The Wi-Fi approval notification is used after "Salir del modo kiosko", which unpins first.
