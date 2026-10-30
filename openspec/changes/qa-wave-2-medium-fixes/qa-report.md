# QA report: qa-wave-2-medium-fixes

Evidence lives in `openspec/changes/qa-wave-2-medium-fixes/qa-evidence/` (screenshots, log extracts).

**Legend:** ✔ works · ✖ fails (bug present) · ◐ partial · ⏸ blocked / not run.
**Ran by:** C = Claude (emulator + adb) · U = user-assisted.

## Environment

| Item | Value |
|---|---|
| Baseline build | `main` @ `96798d5` (wave 1 merged), `profile` variant, 2026-10-30 |
| After build | — |
| Emulators | `Medium_Tablet` (API 26, paired, pinned) · `Tablet_API36` (Android 16, unpaired) |
| Backend | production `http://87.99.156.164` |
| Taps | "fast taps" are 2–4 `input tap` commands started together in one `adb shell` (`a & b`), well inside the navigation transition |

## Cases

| Case | What is checked | Before | After | Evidence | By |
|---|---|---|---|---|---|
| INI-020 | Double tap "Ir al catálogo" opens one Catalog | ✖ 2 of 3 tries: after one system Back the screen is still Catalog (two Catalogs stacked) | | `INI-020-before-after-one-back.png` | C |
| DET-021 | Fast taps on a related product open one detail | ✖ 3 of 3 tries (triple tap): after one Back the same product is still shown | | `DET-021-before-dup.png` | C |
| DET-022 | "Volver" ×3 closes only the current detail | ✖ Search → Aplique → related "Interruptor"; triple "Volver" → back on the search results, the Aplique detail was skipped | | `DET-022-before-after-volver-x3.png` | C |
| CAT-024 / CAT-025 | System Back on Catalog with items asks to close | ✖ 1 product in the cart, system Back → Home directly, no confirmation ("Volver" shows it) | | `CAT-024-before-1-catalog-with-item.png`, `CAT-024-before-2-after-system-back.png` | C |
| BUS-052 / BUS-060 | System Back on Search follows "Volver" (history, then confirmation) | ✖ Two searches in a row ("aplique de pared", then "taladro inalambrico" from Search), system Back → Home directly, no history step and no confirmation | | `BUS-060-before-1-second-result.png`, `BUS-060-before-2-after-system-back.png` | C |
| INI-051 / VOZ-003 | "No te escuché…" returns to idle | ✖ Mic on Home with no speech → "No te escuché…" at about 40 s; still on screen 118 s after the tap | | `INI-051-before-1-no-audio.png`, `INI-051-before-2-after-2min.png` | C |
| VOZ-004 | Recording stops when the app leaves the screen | ✖ Home listening, app unpinned, HOME key: the launcher is resumed and AudioFlinger still shows an active record track owned by the app (pid 9027, uid 10088) | | `VOZ-004-before-audio.txt` | C |
| VOZ-008 | Search mic without permission asks or explains | ✖ `RECORD_AUDIO` revoked (startup request denied), Search mic tapped: nothing on screen at 1 s and 7 s, no request, no message | | `VOZ-008-before-1-search-mic-tap.png`, `VOZ-008-before-2-search-mic-7s.png` | C |
| VOZ-010 | Detail mic does something | ✖ Detail mic tapped: nothing happens | | `VOZ-010-before-detail-mic.png` | C |
| CAR-031 | Compartir disabled with an empty form | ✖ Empty cart, empty form: "Realizar pedido" grey, "Compartir" green | | `CAR-031-before-1-empty-cart-empty-form.png` | C |
| CAR-020 | Actions disabled with an empty basket | ✖ Empty cart, valid form: "Realizar pedido" enabled. "Compartir" sent `POST /carts/share-pdf` with no items → `422 VALIDATION_ERROR` | | `CAR-020-before-empty-cart-valid-form.png`, `CAR-020-CAR-033-before-logcat.txt` | C |
| CAR-021 / CAR-024 | More than 20 lines refused before sending | ✖ (wave 1 baseline) 21 distinct products → `422 "items must not exceed 20 entries"` | | wave 1 `qa-evidence/CAR-011-before-422-result.png`, `CAR-011-before-logcat.txt` | C |
| CAR-033 | Share error uses share copy and keeps the cart | ✖ The `422` above shows "Algo salió mal / Hubo un problema generando la orden…" with "Entendido" and "Cancelar orden" (which ends the session). The not-delivered state (Meta `FAILED`) also offers "Cancelar orden" | | `CAR-033-before-share-error-empty-cart.png`, `CAR-044-before-2-reopened-cart.png` | C |
| CAR-043 / CAR-044 | Cart cannot close while sending; no result left behind | ✖ 1 product, valid form, "Compartir" then a tap outside 0.4 s later: the cart closes during the request. Reopening the cart shows the old "No pudimos enviar la cotización" result instead of the summary | | `CAR-043-before-1-closed-while-sending.png`, `CAR-044-before-2-reopened-cart.png` | C |
| VIN-024 | Fast taps on "Generar nuevo código" / "Reintentar" | ◐ `Tablet_API36`, code-generation error offline, network back, 4 fast taps on "Reintentar": **3** `POST /devices/pairing-token` (3 sessions created on the server, 2 of them orphaned), last code shown. The false error QA saw did not show in this run (it depends on the order the answers arrive) | | `VIN-024-before-after-fast-taps.png`, `VIN-024-before-logcat.txt` | C |
| AJU-031 / AJU-050 | Paired tablet offline: connection error, retry never generates | ⏸ Not run yet: Settings on `Medium_Tablet` needs the exit PIN set from staff | | | U |

## Notes

- **Baseline APK:** built from `fix/qa-wave-2` right after the rebase on `main` (`96798d5`), before any wave-2 code change.
