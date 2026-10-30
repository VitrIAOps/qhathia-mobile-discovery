## Why

The merchant can turn "Modo kiosco" on from the staff app (api-contract.md §21) and the tablet already syncs `kioskMode` (`device-remote-settings`), but nothing applies it: a customer at the stand can swipe home, open recents and leave the Discovery app. The tablet must stay on Discovery, full screen, and only the merchant — through Configuración and the exit PIN — can take it out.

The tablets **cannot be provisioned as Device Owner**, and the app **is not a launcher**, so the lock is built on Android screen pinning (`Activity.startLockTask()` without an allow-list).

## What Changes

- **Kiosk lock when `kioskMode = true` and the tablet is paired**: the app pins itself (screen pinning), hides the status and navigation bars (immersive, transient on swipe), keeps the screen on, and swallows Back on Home so the root activity is never finished.
- **The only in-app exit is in Configuración**: the settings side menu gains "Salir del modo kiosko", reached through the existing settings gate — the exit PIN when one is set, **nothing when no PIN is set**. Exiting unpins and shows the system bars so the merchant can leave the app.
- **The exit is temporary**: the lock comes back when the merchant goes back from Configuración to Home, or on the next app start (including `RESTART_APP` and a tablet reboot followed by opening the app).
- **Live remote changes**: `kioskMode` switched to `false` unpins immediately; switched to `true` pins again (unless the merchant is inside Configuración after exiting).
- **Not locked before pairing**: an unpaired tablet (onboarding) is never pinned, even though `kioskMode` defaults to `true`.
- **Documented platform limit**: screen pinning can always be removed with the system gesture (hold Back + Overview). The per-tablet mitigation is to set a screen lock and enable Android's "Ask for PIN before unpinning"; this goes into the tablet setup guide.

## Capabilities

### New Capabilities
- `kiosk-lock-mode`: Applying the remote `kioskMode` — screen pinning, immersive full screen, keep-screen-on, Back handling on Home, live changes, temporary exit and re-lock rules.

### Modified Capabilities
- `settings-access`: Adds the "Salir del modo kiosko" action in the settings area (behind the existing gate). The existing gate requirements do not change.

## Impact

- `core/domain` — `KioskLockState` model, `KioskSessionRepository` port (in-memory "exited from settings" flag), `ObserveKioskLockUseCase` (combines `kioskMode`, pairing state and the exit flag), `ExitKioskModeUseCase`, `ResumeKioskModeUseCase`.
- `core/data` — `KioskSessionRepositoryImpl` (process-scoped, not persisted) + Hilt binding.
- `features/settings` — "Salir del modo kiosko" side-menu item, shown only while the lock is active.
- `app` — `MainViewModel` exposes the lock state; `MainActivity` applies it (`startLockTask`/`stopLockTask`, `WindowInsetsControllerCompat`, `FLAG_KEEP_SCREEN_ON`), Back handler on Home, re-lock when leaving settings.
- Possibly `AppRestarterImpl` — release pinning before relaunching if the platform blocks the relaunch while pinned (see design).
- `docs/` — tablet setup guide: screen lock + "Ask for PIN before unpinning".
- No new permissions, no new dependency, no new Gradle module, no API change.
