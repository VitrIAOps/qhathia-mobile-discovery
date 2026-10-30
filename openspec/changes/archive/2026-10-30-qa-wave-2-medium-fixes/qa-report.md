# QA report: qa-wave-2-medium-fixes

Evidence lives in `openspec/changes/qa-wave-2-medium-fixes/qa-evidence/` (screenshots, log extracts).

**Legend:** ✔ works · ✖ fails (bug present) · ◐ partial · ⏸ blocked / not run.
**Ran by:** C = Claude (emulator + adb) · U = user-assisted.

## Environment

| Item | Value |
|---|---|
| Baseline build | `main` @ `96798d5` (wave 1 merged), `profile` variant, 2026-10-30 |
| After build | `fix/qa-wave-2` @ `d546573`, `profile` variant, 2026-10-30 |
| Emulators | `Medium_Tablet` (API 26, paired, pinned) · `Tablet_API36` (Android 16, unpaired) |
| Backend | production `http://87.99.156.164` |
| Taps | "fast taps" are 2–4 `input tap` commands started together in one `adb shell` (`a & b`), well inside the navigation transition |

## Cases

| Case | What is checked | Before | After | Evidence | By |
|---|---|---|---|---|---|
| INI-020 | Double tap "Ir al catálogo" opens one Catalog | ✖ 2 of 3 tries: after one system Back the screen is still Catalog (two Catalogs stacked) | ✔ 5 of 5 tries (triple tap on "Ir al catálogo" at its real position, then one system Back): back on Home every time. A first run tapped 48 px too low, which on Catalog lands on "Ir al carrito" (that Back closed the cart); not an app issue | `INI-020-before-after-one-back.png` → `INI-020-after-home-after-one-back.png` | C |
| DET-021 | Fast taps on a related product open one detail | ✖ 3 of 3 tries (triple tap): after one Back the same product is still shown | ✔ 3 of 3 (triple tap on the related "Sensor de movimiento"): one Back returns to the Aplique detail | `DET-021-before-dup.png` → `DET-021-after-back-to-first.png` | C |
| DET-022 | "Volver" ×3 closes only the current detail | ✖ Search → Aplique → related "Interruptor"; triple "Volver" → back on the search results, the Aplique detail was skipped | ✔ 2 of 2: Aplique → related "Sensor" → triple "Volver" → back on the Aplique detail (only one screen popped) | `DET-022-before-after-volver-x3.png` → `DET-022-after-volver-x3.png` | C |
| CAT-024 / CAT-025 | System Back on Catalog with items asks to close | ✖ 1 product in the cart, system Back → Home directly, no confirmation ("Volver" shows it) | ✔ 1 product in the cart, system Back on Catalog → "¿Cerrar el catálogo?"; Back again → dialog dismissed, still on Catalog | `CAT-024-before-1-catalog-with-item.png`, `CAT-024-before-2-after-system-back.png` → `CAT-024-after-close-confirmation.png` | C |
| BUS-052 / BUS-060 | System Back on Search follows "Volver" (history, then confirmation) | ✖ Two searches in a row ("aplique de pared", then "taladro inalambrico" from Search), system Back → Home directly, no history step and no confirmation | ✔ Two searches ("aplique de pared exterior" 8 results, "taladro inalambrico" 1): system Back → the 8 results; Back again → "¿Cerrar la búsqueda actual?"; Back on the dialog → dismissed, still on Search; "Sí, cerrar" → Home | `BUS-060-before-1-second-result.png`, `BUS-060-before-2-after-system-back.png` → `BUS-060-after-1-second-result.png`, `BUS-060-after-2-back-to-first-result.png`, `BUS-052-after-3-close-confirmation.png` | C |
| INI-051 / VOZ-003 | "No te escuché…" returns to idle | ✖ Mic on Home with no speech → "No te escuché…" at about 40 s; still on screen 118 s after the tap | ✔ Mic on Home with no speech: "No te escuché…" at 32 s, back to the idle Home 31 s later | `INI-051-before-1-no-audio.png`, `INI-051-before-2-after-2min.png` → `INI-051-after-1-no-audio.png`, `INI-051-after-2-back-to-idle.png` | C |
| VOZ-004 | Recording stops when the app leaves the screen | ✖ Home listening, app unpinned, HOME key: the launcher is resumed and AudioFlinger still shows an active record track owned by the app (pid 9027, uid 10088) | ✔ Home listening + HOME key (unpinned): launcher resumed, no input thread and no audio-policy session. Search listening (opened by voice from Detail) + HOME: no active record track; back in the app, Search leaves to the Detail it came from (nothing to restore) | `VOZ-004-before-audio.txt` → `VOZ-004-after-home-audio.txt`, `VOZ-004-after-search-audio.txt`, `VOZ-004-after-search-returns-to-detail.png` | C |
| VOZ-008 | Search mic without permission asks or explains | ✖ `RECORD_AUDIO` revoked (startup request denied), Search mic tapped: nothing on screen at 1 s and 7 s, no request, no message | ✔ `RECORD_AUDIO` revoked: the Home mic shows the system request. Search mic after a refusal: "Necesitamos acceso al micrófono para la búsqueda por voz", no capture. Android did not show its dialog again on Search because it kept the earlier refusal; the request path is the same composable as Home | `VOZ-008-before-1-search-mic-tap.png`, `VOZ-008-before-2-search-mic-7s.png` → `VOZ-008-after-search-mic-denied-message.png` | C |
| VOZ-010 | Detail mic does something | ✖ Detail mic tapped: nothing happens | ✔ Detail mic (Amoladora) → Search opens with "Escuchando..." | `VOZ-010-before-detail-mic.png` → `VOZ-010-after-detail-mic-opens-search-listening.png` | C |
| CAR-031 | Compartir disabled with an empty form | ✖ Empty cart, empty form: "Realizar pedido" grey, "Compartir" green | ✔ 1 product, empty form: "Realizar pedido" and "Compartir" both grey | `CAR-031-before-1-empty-cart-empty-form.png` → `CAR-031-after-1-item-empty-form.png` | C |
| CAR-020 | Actions disabled with an empty basket | ✖ Empty cart, valid form: "Realizar pedido" enabled. "Compartir" sent `POST /carts/share-pdf` with no items → `422 VALIDATION_ERROR` | ✔ Valid form, line removed (0 products): both actions grey; nothing can be sent | `CAR-020-before-empty-cart-valid-form.png`, `CAR-020-CAR-033-before-logcat.txt` → `CAR-020-after-empty-cart-valid-form.png` | C |
| CAR-021 / CAR-024 | More than 20 lines refused before sending | ✖ (wave 1 baseline) 21 distinct products → `422 "items must not exceed 20 entries"` | ✔ 20 distinct products (27 units) from Catalog, a detail's related products and two searches; the 21st ("Amoladora angular" from Search) → "Tu carrito ya tiene 20 productos distintos. Realiza tu pedido o quita alguno para agregar más.", badge stays 27. Quantity cap (CAR-024) covered by unit tests | wave 1 `qa-evidence/CAR-011-before-422-result.png`, `CAR-011-before-logcat.txt` → `CAR-021-after-21st-product-refused.png` | C |
| CAR-033 | Share error uses share copy and keeps the cart | ✖ The `422` above shows "Algo salió mal / Hubo un problema generando la orden…" with "Entendido" and "Cancelar orden" (which ends the session). The not-delivered state (Meta `FAILED`) also offers "Cancelar orden" | ✔ Meta `FAILED`: "No pudimos enviar la cotización" with a single "Entendido"; no "Cancelar orden". The share error state (`ShareError`, "Hubo un problema enviando la cotización…") cannot be reached against production now that an empty or 21-line basket cannot be sent; covered by unit tests and previews | `CAR-033-before-share-error-empty-cart.png`, `CAR-044-before-2-reopened-cart.png` → `CAR-033-after-not-delivered-only-entendido.png` | C |
| CAR-043 / CAR-044 | Cart cannot close while sending; no result left behind | ✖ 1 product, valid form, "Compartir" then a tap outside 0.4 s later: the cart closes during the request. Reopening the cart shows the old "No pudimos enviar la cotización" result instead of the summary | ✔ "Compartir" then a tap outside 0.4 s later: the dialog stays and shows the result (a tap after the result would have gone to the summary, see next). Tap outside on the result → cart summary ("1 producto guardado"); closed and reopened → summary, no old result. One request | `CAR-043-before-1-closed-while-sending.png`, `CAR-044-before-2-reopened-cart.png` → `CAR-043-after-1-outside-tap-while-sending.png`, `CAR-044-after-1-outside-tap-on-result.png`, `CAR-044-after-2-reopened-cart.png` | C |
| VIN-024 | Fast taps on "Generar nuevo código" / "Reintentar" | ◐ `Tablet_API36`, code-generation error offline, network back, 4 fast taps on "Reintentar": **3** `POST /devices/pairing-token` (3 sessions created on the server, 2 of them orphaned), last code shown. The false error QA saw did not show in this run (it depends on the order the answers arrive) | ✔ `Tablet_API36`, same steps (error offline, network back, 4 fast taps on "Reintentar"): **1** `POST /devices/pairing-token`, code shown, no error | `VIN-024-before-after-fast-taps.png`, `VIN-024-before-logcat.txt` → `VIN-024-after-fast-taps.png`, `VIN-024-after-logcat.txt` | C |
| AJU-031 / AJU-050 | Paired tablet offline: connection error, retry never generates | ⏸ Not run yet: Settings on `Medium_Tablet` needs the exit PIN set from staff | | | U |

## Regression sweep (9.6)

| Flow | Result | By |
|---|---|---|
| Customer session: 1 product on Catalog, idle | ✔ back on Home after 62 s, cart "0 productos guardados", still `PINNED` (`REGRESSION-session-timeout-cart-empty.png`) | C |
| Share outcome with Meta `FAILED` | ✔ not-delivered state, one request (CAR-033 above) | C |
| Pinning after relaunch | ✔ "Pin app?" on relaunch, GOT IT → `PINNED`; Back on Home does nothing | C |
| Cart dialog with kiosk active | ✔ bars hidden (cart screenshots above) | C |
| Flagged lines after `409` (wave 1) | ⏸ needs a product set out of stock by the user; code path unchanged, unit tests green | — |

## Notes

- **Baseline APK:** built from `fix/qa-wave-2` right after the rebase on `main` (`96798d5`), before any wave-2 code change.
