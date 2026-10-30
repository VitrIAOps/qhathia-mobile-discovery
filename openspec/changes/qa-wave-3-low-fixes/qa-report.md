# QA report: qa-wave-3-low-fixes

Evidence lives in `openspec/changes/qa-wave-3-low-fixes/qa-evidence/`.

**Legend:** ✔ works · ✖ fails (bug present) · ◐ partial · ⏸ blocked / not run.
**Ran by:** C = Claude (emulator + adb) · U = user-assisted.

## Environment

| Item | Value |
|---|---|
| Baseline build | `main` @ `6c288e6` (wave 2 merged), `profile` variant, 2026-10-30 |
| After build | `fix/qa-wave-3` (wave 3 implementation), `profile` variant, 2026-10-30 |
| Emulators | `Medium_Tablet` (API 26, paired, pinned) · `Tablet_API36` (Android 16, unpaired) |
| Label check | clickable nodes in `uiautomator dump` with `text=""` and `content-desc=""` (`LABELS-*.txt`). After: an `EditText` also counts as labelled when its own label node carries the description (see Notes) |

## Spike: Android's mobile-data screen (task 1.0)

| Check | Result | Evidence |
|---|---|---|
| Pinned, start `DATA_ROAMING_SETTINGS` (API 26) | Refused: `Attempted Lock Task Mode violation … MobileNetworkSettings`, error 101 | `SPIKE-api26-1-pinned-start-data-settings.png` |
| Pinned, start the Internet panel (API 36) | Refused: `Attempted Lock Task Mode violation … SettingsPanelActivity` | `SPIKE-api36-2-panel-while-pinned.png` |
| Not pinned, Internet panel (API 36) | Opens over the app with the "Mobile data" switch; Back returns to Discovery | `SPIKE-api36-1-internet-panel.png` |
| Not pinned, `DATA_ROAMING_SETTINGS` (API 26) | Opens "Mobile network" **without** a mobile-data switch; Back returns to Discovery | `SPIKE-api26-2-mobile-network-settings.png` |
| Not pinned, data usage (API 26) | The switch is in `Settings$DataUsageSummaryActivity` (internal, no public action); `ACTION_WIRELESS_SETTINGS` opens "Network & Internet", one tap from it. Decision: `ACTION_WIRELESS_SETTINGS` on API 26–28 | `SPIKE-api26-3-data-usage.png` |

## Cases

| Case | What is checked | Before | After | Evidence | By |
|---|---|---|---|---|---|
| INI-070 / CAR-080 / DET-080 / VIN-070 | No clickable control without a label | ✖ Unlabelled clickable nodes: Home 4 (gear, mic, "Ir al catálogo", search field), Catalog 40, Cart (1 product) 9, Search 6, Detail 17, Pairing 1 (back arrow) | ✔ 0 everywhere: Home 0/4, Catalog 0/40, Detail 0/18, Cart 0/10, Search 0/12, Pairing 0/1 (`Tablet_API36`), Settings PIN 0/12, Settings · Red 0/8, Settings · Vincular 0/5. Cards read "<nombre>, <precio>", icons read their action ("Ajustes", "Volver", "Agregar al carrito", "Quitar una unidad"…) | `LABELS-before.txt`, `LABELS-after.txt` | C |
| CAR-080 | Cart fields expose their label | ✖ Both cart `EditText`s have `text=""` and no description while empty | ✔ Empty fields are labelled "Nombres" and "Número de celular" | `LABELS-after.txt` (Cart) | C |
| VIN-070 | Pairing code read as one value | ✖ The code is five separate texts ("7" "V" "6" "W" "2") | ✔ One node "Código de vinculación: K S Q H Z"; no single-letter texts left | `VIN-070-after-code.png`, `LABELS-after.txt` | C |
| DET-080 | Detail photos described | ✖ Thumbnails without a description | ✔ Main photo "Foto 1 de Foco LED 9W luz cálida E27"; thumbnails "Foto N de …" (the product tested has one photo, so the thumbnails are the empty gray slots, decorative) | `LABELS-after.txt` (Detail) | C |
| DET-004 | Stepper back to 1 after adding | ✖ Stepper at 3, "Agregar al carrito": stepper still 3; a second tap → cart "6 productos guardados" | ✔ Stepper at 3 → "Agregar al carrito" → stepper 1 | `DET-004-after-stepper-back-to-1.png` | C |
| DET-062 / DET-063 | Placeholder for missing / broken images | ⏸ No product without a photo or with a broken one found in production results; the code has no `error` / `fallback` on any `AsyncImage` (6 call sites) | ◐ The six call sites use `ProductImage` (placeholder for empty, blank and failed URLs; neutral background while loading). Still no such product in production to show it live; checked in the `ProductImage` previews | — | C |
| RED-004 | "Reintentar" reloads related products | ⏸ Not reproduced: with Wi-Fi and data turned off right before opening, the detail still loaded (the network dropped too late). The code calls only `load()` on retry | ◐ Unit test (failed related + retry → loaded); not reproducible on the emulator for the same reason as before | `ProductDetailViewModelTest` | C |
| CAR-014 | Phone field accepts digits only | ✖ The keyboard is the phone keypad (it offers `N`, `Pause`, `Wait`, `(`, `-`…), and `input text 98a7b` leaves "98a7b" in the field | ✔ `input text 98a7b` leaves "987" | `CAR-014-after-letters-dropped.png` | C |
| EXP-005 | History restores the selected chips | ✖ "taladro" → suggestion "Percutor" selected → new search "foco" → "Volver": the result is back with "Percutor" unselected. The chips also report `checked=false` to accessibility while selected | ✔ Same steps → "Percutor" selected again after "Volver"; chips are checkboxes and report `checked=true` while selected | `EXP-005-after-1-chip-selected.png`, `EXP-005-after-2-after-back.png` | C |
| EXP-003 | Screen stays on while a code is shown | ✖ `Tablet_API36`, code on screen, `screen_off_timeout` 30 s, no touch for 45 s → `mWakefulness=Asleep`, `mScreenState=OFF` | ✔ Same test → `mWakefulness=Awake` | `EXP-003-after-power.txt` | C |
| EXP-004 | No mic request over the welcome screen | ✖ `pm clear` on `Tablet_API36` → "Allow QhathiaDiscovery to record audio?" over "Te damos la bienvenida a Qhathia" | ✔ `pm clear` → the welcome screen without any permission request | `EXP-004-after-no-prompt-over-welcome.png` | C |
| AJU-030 | "Actividad" shows a value | ⏸ Settings needs the staff exit PIN on `Medium_Tablet` | ✔ "Sincronizado hace 4 minutos" (from `lastSeenAt`; `lastSyncAt` is still empty on the server) | `AJU-030-032-after.png` | C (PIN from U) |
| AJU-032 | "Código usado" | ⏸ Same (Settings) | ✔ "No disponible" on `Medium_Tablet`, paired before the code was saved (expected). Saving the code at pairing is covered by unit tests (`DevicePairingRepositoryImplTest`, code survives a new repository instance) | `AJU-030-032-after.png` | C |
| AJU-022 | Location permission can be requested again | ⏸ Same (Settings) | ✔ Location revoked → request on entry → "DENY" → message + "Permitir ubicación" → tap → Android asks again. The "no longer asked" message is covered by a unit test and a preview | `AJU-022-after-2-denied.png`, `AJU-022-after-3-asked-again.png` | C |
| AJU-043 | List not empty the first time | ⏸ Same (Settings) | ✔ "ALLOW" → the list shows "AndroidWifi, Red abierta, Conectado" without leaving the screen; Wi-Fi service records a scan from the app at that moment | `AJU-043-after-list.png` | C |
| AJU-040 | "Buscar redes" scans | ⏸ Same (Settings) | ✔ Tap → `dumpsys wifi` records `CMD_START_SCAN` from the app's uid at the tap time; the list stays filled | — (`dumpsys wifi`) | C |
| AJU-041 | Mobile data | ⏸ Same (Settings) | ✔ Status row "Activos" + "Abrir ajustes de datos" (no switch). Tap → kiosk left (`mLockTaskModeState=NONE`) and Android's "Network & Internet" opens (API 26). Data turned off there (`svc data disable`) → Back → row "Inactivos". Home → "Screen is pinned" → `PINNED` again | `AJU-041-after-0-status-row.png`, `AJU-041-after-1-android-screen.png`, `AJU-041-after-2-back-inactive.png`, `KIO-relock-after-home-pinning-dialog.png` | C |
| VIN-003 | Store name on Home | ⏸ Backend: no endpoint returns the store name to the tablet. The app will show it once `/devices/me` sends `storeName` | ✔ `/devices/me` now sends it: after Settings loaded the device, Home shows "EL MARTILLO — ILUMINACIÓN Y FERRETERÍA" and Settings "El Martillo — Iluminación y Ferretería". It is saved when the paired device is read (Settings), so a newly paired tablet shows "TIENDA" until then | `AJU-030-032-after.png` | C |

## Regression sweep (waves 1 and 2, task 9.4)

| Flow | Result |
|---|---|
| Pinning | ✔ Pinned on Home; the temporary exit (data button) unpins, Home pins again |
| Back = "Volver" / navigation guard | ✔ "Volver" goes back in the search history; the system Back on a result asks "¿Cerrar la búsqueda actual?" and "Sí, cerrar" goes Home |
| Voice stop | ✔ Mic → "Escuchando, toca para detener" → tap → listening stops |
| Cart dismissal | ✔ "Cerrar carrito" closes the cart over Detail |
| Session timeout | ✔ Catalog, 70 s idle → Home, still pinned |

## Notes

- `Medium_Tablet` returns Home after 60 s idle (customer session); runs that switch between emulators restart the flow from Home.
- Compose places the description of a text field in a label node right inside the `EditText` (every Compose text field has an inner scroll node, so the description cannot sit on the field node without dropping its editing semantics). TalkBack reads it as the field's label. The label script accepts that one case; every other clickable must carry text or a description on itself.
- With kiosk mode off the system bars show and the layout moves down by about 48 px: tap positions come from a fresh dump.
