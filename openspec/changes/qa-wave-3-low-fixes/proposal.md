## Why

Waves 1 and 2 closed every critical, high and medium case of the 2026-10-25 QA run that the app could fix by itself. What remains are the low cases: controls that screen readers and test tools see without a label, small detail and cart annoyances, a pairing screen that turns off while the code is shown, a microphone prompt over the welcome screen, and a Settings screen with "Pendiente" fields and Wi-Fi controls that do nothing (`docs/handoff/qa-2026-10-25-casos-observados-por-flujo.md`). They are cheap to fix and make the kiosk feel finished.

## What Changes

**Every control has a label** (INI-070, CAR-080, DET-080, VIN-070, EXP-005 vinc.)
- Icon-only buttons (settings gear, close, back, cart buttons on product cards, stepper, mic, search) carry their description on the touchable element itself, not on the icon inside it.
- Text fields expose their label ("Nombres", "Número de celular") to accessibility services.
- The detail thumbnails have a description ("Foto 2 de Taladro…").
- The pairing code is read as one value ("Código de vinculación: 2 Y 5 6 7") instead of five loose letters.

**Product detail** (DET-004, DET-062, DET-063, RED-004)
- After "Agregar al carrito" the stepper goes back to 1.
- A product with no photo, or a photo that fails to load, shows a neutral "no image" placeholder instead of a blank area, in the detail, cards, the cart and the success summary.
- "Reintentar" after a connection error also reloads "Podrías acompañar este producto con…".

**Cart** (CAR-014)
- The cell-phone fields of the cart and of the product share accept digits only (plus a leading `+` and spaces), as the numeric keyboard suggests.

**Search** (EXP-005)
- Going back in the search history restores the filter chips that were selected for that result.

**Onboarding and pairing** (EXP-003, EXP-004)
- The screen stays on while a pairing code is shown.
- **BREAKING (behavior):** the microphone permission is no longer requested over the welcome screen on first launch. It is requested when Home is shown for the first time (after pairing), and still on the first microphone tap if it was refused.

**Settings** (AJU-030, AJU-032, AJU-022, AJU-040, AJU-041, AJU-043)
- "Actividad" shows the last time the tablet was seen (`lastSeenAt`) when no sync time is known.
- "Código usado" is saved when the tablet pairs, so it survives a restart. Tablets paired before this change show "No disponible".
- Wi-Fi: a "Permitir ubicación" action when the permission is missing; "Buscar redes" starts a scan; a scan runs as soon as the permission is granted, so the list is not empty the first time.
- **BREAKING (behavior):** "Datos móviles" becomes a status row ("Activos" / "Inactivos" / "Sin SIM") with an "Abrir ajustes de datos" button. Android does not let a normal app turn mobile data on or off, so the switch always failed. The button does the same temporary kiosk exit as "Salir del modo kiosko" and opens Android's own mobile-data screen (the Internet panel on Android 10+, "Red e Internet" on Android 8–9); the tablet pins again when the merchant goes back to Home.

**Store name, when the backend sends it** (VIN-003, AJU-030 "Comercio")
- The app reads an optional `storeName` from `GET /devices/me` and shows it on Home and in Settings. Today no endpoint returns it to the tablet, so "TIENDA" and "Pendiente" stay until the backend adds it.

**Out of this wave (backend):** VIN-011 (code in lowercase), EXP-001 (500 instead of 404), INI-015 / BUS-017 (numbers in voice search), and the `storeName` field above.

## Capabilities

### New Capabilities
- `accessibility-labels`: every touchable control and text field carries its own label; grouped values (the pairing code) are read as one.

### Modified Capabilities
- `shared-ui-kit`: product images show a placeholder when there is no photo or it fails to load.
- `product-detail`: stepper back to 1 after adding; retry reloads related products.
- `shopping-cart`: the cell-phone field accepts digits only.
- `product-search`: the history restores the selected filter chips.
- `device-pairing`: screen kept on while the code is shown; "Actividad" falls back to `lastSeenAt`; "Código usado" persisted; store name read when available.
- `voice-search-stt`: the microphone permission is requested on Home, not at app open (MODIFIED "Microphone permission requested at app open").
- `network-settings`: mobile data is a status with a button to Android's mobile-data screen (replaces "Toggle mobile data"); location permission action, manual rescan and a scan after the permission is granted.
- `kiosk-lock-mode`: the temporary exit can also be started from the mobile-data button, to open Android's mobile-data screen.
- `discovery-home`: shows the store name when it is known.

## Impact

- `core/ui`: label helpers or patterns for icon buttons, `ProductImage` with placeholder, digit-only phone input in `DiscoveryTextField` (or a phone variant).
- `features/home`, `cart`, `catalog`, `product-search`, `product-detail`, `pairing`, `settings`: labels on touchable controls; detail stepper and retry; search history chips; keep-screen-on in pairing; Wi-Fi actions.
- `app` (`MainActivity`): the mic permission request moves from `onCreate` to the first time Home is shown.
- `core/domain` + `core/data`: persisted pairing code (session DataStore); `PairedDevice` activity fallback; optional `storeName` in `DevicePairingDtos`; `NetworkSettingsRepository.rescan()`.
- No new endpoint, library or Gradle module. `storeName` is an optional field the app tolerates being absent.
