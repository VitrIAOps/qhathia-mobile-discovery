## Why

Merchants operating the Qhathia Discovery kiosk currently have no in-app way to manage the device's connectivity — they must leave the app and dig through the Android system settings, which is error-prone on a locked-down, mobile-operated device and breaks the "100% in-app" kiosk experience. They need a guarded, in-app settings area to turn Wi-Fi and mobile data on or off and to join a Wi-Fi network without ever touching the native OS settings.

## What Changes

- Add a **PIN-gated settings entry**: tapping the existing Home settings affordance shows a settings-PIN dialog **as an overlay before navigating into settings** (following the Figma "Configuración protegida": secure badge, four PIN dots, numeric keypad, auto-validate on completion). The unlock PIN is **mocked to `1234`** (the real password API does not exist yet) behind a repository port so it can be swapped for a backend later. A correct PIN opens the Network settings screen directly.
- Add a **Network settings screen** (the settings landing, following the Figma "Red Wifi": a side menu + a main panel; the side menu carries the settings sections) that lets the merchant:
  - Toggle **mobile data** on/off via a switch positioned **above** the Wi-Fi section.
  - Toggle **Wi-Fi** on/off.
  - See a **list of accessible Wi-Fi networks** discovered using standard Android APIs.
  - Enter a **Wi-Fi password** through a dialog (close "X", ~56dp Wi-Fi icon, network name, password `TextField`, submit) when the selected network is secured.
  - **Connect** to a selected Wi-Fi network.
- Enforce the business rule: a network connection can only be selected/changed when the device is **not in airplane mode**.
- All new screens, dialogs, and any processing/loading states follow the existing Material 3 design system in `core:ui`.

## Capabilities

### New Capabilities
- `settings-access`: PIN-gated entry into the in-app settings area — the settings-PIN dialog is shown as an overlay before navigating, and a correct PIN opens the Network settings screen directly. Includes the mocked PIN validation behind a repository port.
- `network-settings`: In-app management of device connectivity — mobile data toggle, Wi-Fi toggle, discovery and listing of accessible Wi-Fi networks (with the connected network marked), the secured-network password dialog, connecting to a network, and the airplane-mode guard.

### Modified Capabilities
<!-- None. The Home settings affordance already exists and currently routes to a placeholder; this change replaces the placeholder destination without altering Home's documented behavior. -->

## Impact

- **New Gradle module:** `features:settings` (Compose + Hilt feature module) holding the PIN gate dialog (hosted as an app-level overlay, like the cart), the network settings screen, ViewModels, and nav graph.
- **`core:domain`:** new domain models (e.g. `WifiNetwork`, `NetworkConnectivityState`), repository ports (`SettingsAccessRepository`, `NetworkSettingsRepository`), and use cases (validate password, observe/toggle Wi-Fi & data, scan networks, connect).
- **`core:data`:** repository implementations backed by Android `WifiManager` / `ConnectivityManager` data sources plus a mocked password data source (default `1234`), with their Hilt DI modules.
- **`:app`:** replace the `SETTINGS_ROUTE` placeholder — Home's settings affordance now opens the PIN gate overlay, and a correct PIN navigates to the Network settings destination; wire the new module.
- **`app/src/main/AndroidManifest.xml`:** add `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, and `ACCESS_FINE_LOCATION` (required to read Wi-Fi scan results); `ACCESS_NETWORK_STATE` already declared. Location is a runtime (dangerous) permission and must be requested at point of use.
- **`gradle/settings.gradle.kts`:** register `:features:settings`.
- **Platform constraint:** programmatic Wi-Fi enable/disable and direct connection are restricted on API 29+; the design documents the standard-API approach and where mocking/abstraction absorbs the gap.
