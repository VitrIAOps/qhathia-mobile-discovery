# QA report: qa-wave-3-low-fixes

Evidence lives in `openspec/changes/qa-wave-3-low-fixes/qa-evidence/`.

**Legend:** ✔ works · ✖ fails (bug present) · ◐ partial · ⏸ blocked / not run.
**Ran by:** C = Claude (emulator + adb) · U = user-assisted.

## Environment

| Item | Value |
|---|---|
| Baseline build | `main` @ `6c288e6` (wave 2 merged), `profile` variant, 2026-10-30 |
| After build | — |
| Emulators | `Medium_Tablet` (API 26, paired, pinned) · `Tablet_API36` (Android 16, unpaired) |
| Label check | clickable nodes in `uiautomator dump` with `text=""` and `content-desc=""` (`LABELS-*.txt`) |

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
| INI-070 / CAR-080 / DET-080 / VIN-070 | No clickable control without a label | ✖ Unlabelled clickable nodes: Home 4 (gear, mic, "Ir al catálogo", search field), Catalog 40, Cart (1 product) 9, Search 6, Detail 17, Pairing 1 (back arrow) | | `LABELS-before.txt` | C |
| CAR-080 | Cart fields expose their label | ✖ Both cart `EditText`s have `text=""` and no description while empty | | `LABELS-before.txt` (Cart) | C |
| VIN-070 | Pairing code read as one value | ✖ The code is five separate texts ("7" "V" "6" "W" "2") | | `LABELS-before.txt` (Pairing) | C |
| DET-004 | Stepper back to 1 after adding | ✖ Stepper at 3, "Agregar al carrito": stepper still 3; a second tap → cart "6 productos guardados" | | `DET-004-before-stepper-after-add.png`, `DET-004-before-cart-after-two-adds.png` | C |
| DET-062 / DET-063 | Placeholder for missing / broken images | ⏸ No product without a photo or with a broken one found in production results; the code has no `error` / `fallback` on any `AsyncImage` (6 call sites) | | — | C |
| RED-004 | "Reintentar" reloads related products | ⏸ Not reproduced: with Wi-Fi and data turned off right before opening, the detail still loaded (the network dropped too late). The code calls only `load()` on retry | | `RED-004-before-not-reproduced-related-loaded.png` | C |
| CAR-014 | Phone field accepts digits only | ✖ The keyboard is the phone keypad (it offers `N`, `Pause`, `Wait`, `(`, `-`…), and `input text 98a7b` leaves "98a7b" in the field | | `CAR-014-before-1-keyboard.png`, `CAR-014-before-2-letters-accepted.png` | C |
| EXP-005 | History restores the selected chips | ✖ "taladro" → suggestion "Percutor" selected → new search "foco" → "Volver": the result is back with "Percutor" unselected. The chips also report `checked=false` to accessibility while selected | | `EXP-005-before-1-chip-selected.png`, `EXP-005-before-2-after-back.png` | C |
| EXP-003 | Screen stays on while a code is shown | ✖ `Tablet_API36`, code on screen, `screen_off_timeout` 30 s, no touch for 45 s → `mWakefulness=Asleep`, `mScreenState=OFF` | | `EXP-003-before-power.txt` | C |
| EXP-004 | No mic request over the welcome screen | ✖ `pm clear` on `Tablet_API36` → "Allow QhathiaDiscovery to record audio?" over "Te damos la bienvenida a Qhathia" | | `EXP-004-before-permission-over-welcome.png` | C |
| AJU-030 / AJU-032 | "Actividad" and "Código usado" show values | ⏸ Settings needs the staff exit PIN on `Medium_Tablet` | | | U |
| AJU-022 / AJU-040 / AJU-041 / AJU-043 | Wi-Fi permission, "Buscar redes", mobile data, first list | ⏸ Same (Settings) | | | U |
| VIN-003 | Store name on Home | ⏸ Backend: no endpoint returns the store name to the tablet. The app will show it once `/devices/me` sends `storeName` | | | — |

## Notes

- `Medium_Tablet` returns Home after 60 s idle (customer session); runs that switch between emulators restart the flow from Home.
