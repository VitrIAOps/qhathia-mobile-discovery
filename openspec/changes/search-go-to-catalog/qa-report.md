# QA report: search-go-to-catalog

Evidence lives in `openspec/changes/search-go-to-catalog/qa-evidence/`.

**Legend:** ✔ works · ✖ fails · ⏸ blocked / not run on the emulator.
**Ran by:** C = Claude (emulator + adb).

## Environment

| Item | Value |
|---|---|
| Base | `main` @ `519d8d5` (wave 3 archived) |
| Build | `feat/search-go-to-catalog`, `profile` variant, 2026-10-30 |
| Emulator | `Medium_Tablet` (API 26, 2560×1600, paired to production, pinned) |
| Builds and tests | `testDebugUnitTest` (all modules), `lint`, `assembleDebug`, `assembleProfile`: green. Search 44, Catalog 26, Detail 40 ViewModel tests, 0 failures |
| Figma | Qhatia-DS has no catalog icon; `ic_catalog` is Material `grid_view` (validate with design) |

## Cases

| Case | What is checked | Result | Evidence | By |
|---|---|---|---|---|
| §5.10 | Search top bar shows Volver · Ir al catálogo · Nueva búsqueda · Ir al carrito at 2560×1600 without overlap | ✔ | `01-results-topbar-2-in-cart.png` | C |
| §5.10 | "Ir al catálogo" with 2 products: Catalog opens with no dialog, badge still 2 | ✔ | `02-go-to-catalog-cart-kept.png` | C |
| §5.10 | The no-results screen also shows "Ir al catálogo" | ✔ | `09-unhappy-topbar.png` | C |
| §5.10 / INI-020 | Catalog → Search (no results) → double tap "Ir al catálogo" → one Catalog; "Volver" (empty cart) → Home directly; Back on Home stays | ✔ | `10-volver-after-go-to-catalog-home.png` | C |
| BUS-032 / CAR-040 | Search "Nueva búsqueda" with 2 products: "Se vaciará el carrito (2 productos) y volverás al inicio." | ✔ | `04-search-new-search-dialog-warning.png` | C |
| CAR-040 | "Sí, cerrar" → Home → "Ir al catálogo": badge empty; cart "0 productos guardados", name and phone empty | ✔ (was ✖ ALTO) | `05-car040-after-close-badge-empty.png`, `06-car040-cart-empty.png` | C |
| BUS-032 | Empty cart: plain "Se cerrará la búsqueda y volverás al inicio." | ✔ | `07-search-dialog-empty-cart.png` | C |
| DET-010 | Detail "Nueva búsqueda" with 1 product: "(1 producto)"; confirm empties the cart | ✔ | `08-detail-dialog-1-in-cart.png` | C |
| CAT-024 / CAT-025 | Catalog "Volver" and system Back with products open the warning dialog; "Cancelar" keeps the cart; "Sí, cerrar" goes Home | ✔ | `03-catalog-back-dialog-warning.png` | C |
| BUS-060 / BUS-052 | Search system Back with no history opens the close dialog; Back again closes it | ✔ | (dump) | C |
| A-12 | Catalog network error shows "Reintentar"; retry reloads; a failed retry restarts the 30 s countdown | ⏸ emulator · ✔ unit tests | `CatalogViewModelTest` (`given_network_error_when_retry_*`), previews light/dark | C |
| Labels | New control labelled ("Ir al catálogo"). Unlabelled clickables: Search 1/13 and Catalog 1/40 (the search `EditText`, labelled by its own label node, see wave 3); dialog `TextButton`s carry their text in a child node | ✔ no regression | (dump) | C |

## Notes

- **Network cut not reproducible on this image.** `svc wifi disable` is ignored by the `shell` user on the API 26 production image and `adb root` is not available; Wi-Fi stays connected (same limit as wave 3, RED-004). The retry path is covered by two ViewModel tests and the network-error previews.
- No order or share was sent during this QA.
