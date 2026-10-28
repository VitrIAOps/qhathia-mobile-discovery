# QA report: qa-wave-1-critical-fixes

Evidence lives in `openspec/changes/qa-wave-1-critical-fixes/qa-evidence/` (screenshots, uiautomator dumps, log extracts). Logs are stored with tokens masked.

**Legend:** ✔ works · ✖ fails (bug present) · ◐ partial · ⏸ blocked / not run · — not applicable.
**Ran by:** C = Claude (emulator + adb) · U = user-assisted (staff app / OWNER token).

## Environment

| Item | Value |
|---|---|
| Baseline build | `main` @ `f8622dd`, `profile` variant |
| Emulators | `Medium_Tablet` (API 26, 2560×1600, 320 dpi) · `Tablet_API36` (Android 16, same screen) |
| Backend | production `http://87.99.156.164` |
| Tablet state | paired (DEVICE token present), `kioskMode` from remote settings |

## Production checks

| Check | Result | Evidence |
|---|---|---|
| 1.2 Pairing WebSocket without ping closes at about 61 s | ✔ reproduced, **cause is the app**: the app's Ktor config (`socketTimeoutMillis = 60_000`) ends the idle socket at 60.2 s with a normal completion. A plain client without ping stayed open 150 s, so nginx is not involved. | `qa-evidence/1.2-pairing-websocket-probe.txt` |
| 1.2 Ping every 20 s keeps it open past 90 s | ✔ the app config plus `pingIntervalMillis = 20_000` was still open at the 100 s cap; pongs every 20 s | same |
| 1.2 Current status sent on (re)connect | ✔ `WAITING` received within 0.2 s on the first connect and on a reconnect to the same session | same |
| 1.1 Restart from a separate process keeps the task pinned | ✔ API 26: 3/3 and API 36: 3/3 (new pid, still `PINNED`, Discovery on top, no pin dialog). Conditions: the trampoline must be **opaque** (a translucent one crashes on API 26 when it fixes the orientation), and the shell must not apply the INACTIVE seed (it unpinned on start). | `qa-evidence/KIO-041-spike-trampoline-fixed-logcat.txt`, `…-fixed.png`, `…-api36-logcat.txt`, `…-api36.png` |
| 1.3 Android 16 AVD | ✔ `Tablet_API36` (Android 16, x86_64, 2560×1600, sw800dp) boots in about 67 s. It is **not paired** (onboarding), which is useful for pairing tests. | — |

## Cases

| Case | What is checked | Before | After | Evidence | By |
|---|---|---|---|---|---|
| CAT-030 / VOZ-009 | Catalog mic starts listening; no empty search (422) | ✖ No listening state; `POST /conversations/search` → `422 transcribedText must not be blank`; unhappy screen "no pude encontrar…" | | `CAT-030-before-*.png`, `CAT-030-before-logcat.txt` | C |
| CAR-008 | Cart "Compartir" success title is not "Pedido generado" | ✖ Cart "Compartir" (no order created) shows **"¡Pedido generado con éxito!"** + "Acércate al vendedor para recibir tu pedido…" | | `CAR-008-CAR-032-before-share-result.png` | C |
| CAR-007 / CAR-032 / DET-032 | `FAILED` share never shown as sent | ✖ Server answered `201 {"status":"FAILED", … "(#133010) Account not registered"}`. **Cart:** success screen + "Revisa tu celular…". **Detail:** "¡Cotización enviada!" + "Revisa tu celular para ver la cotización." Nothing reached the test phone | | `CAR-008-CAR-032-before-share-result.png`, `CAR-032-before-logcat.txt`, `DET-032-before-share-result.png`, `DET-032-before-logcat.txt` | C |
| DET-034 / CAR-011 | 4xx share sent once, not 3 times | ✖ Cart with 21 distinct products → `422 VALIDATION_ERROR "items must not exceed 20 entries"` sent **3 times**, then the generic "Hubo un problema generando la orden" (CAR-033 copy, wave 2). No `LOW_STOCK` product was found in production search results, so DET-034 itself was not exercised; it is the same `shareWithRetry` path | | `CAR-011-before-422-result.png`, `CAR-011-before-logcat.txt` | C |
| CAR-040 / CAR-041 | Next customer starts with an empty basket and form | ✖ After 60 s off Home the basket is cleared, but the screen **stays on Catalog** (does not return Home) and the next customer's cart shows the previous name `ClienteAnteriorQA` and cell-phone `911111111` | | `CAR-041-before-1-*.png`, `CAR-040-before-1-after-timeout.png`, `CAR-041-before-2-next-customer-cart.png` | C |
| CAR-042 | Typing in the cart dialog keeps the session | ✖ Typing one character every 15 s in the cart dialog: the basket goes from "1 producto guardado" to "0 productos guardados" at about 60 s | | `CAR-042-before-typing-75s.png` | C |
| DET-013 | Inactive product: add and share disabled | | | | C/U |
| CAR-023 / CAR-025 | Unavailable lines flagged after 400, no error loop | | | | C/U |
| BUS-042 | Stale inactive result cannot be added (app side) | | | | C/U |
| VIN-030 | Pairing code kept across screen off/on and re-entry | ✖ On `Tablet_API36` (unpaired): the screen off/on kept `JRRHT`, but rotating to portrait → `KXK2P`, back to landscape → `C9H9F`, leaving and re-entering → `SPJ2Q`. Each one is a new code, and the old ones stay approvable (VIN-022) | | `VIN-030-before-codes.txt`, `VIN-030-before-1-initial.png`, `VIN-030-before-3-portrait.png` | C |
| Landscape | No rotation on any screen, API 26 and API 36 | ✖ API 26: `user_rotation 1` → app in portrait (1600×2560), catalog tabs and cards cut. API 36: portrait too (`sw800dp … port`, `ROTATION_90`). API 36 also logs `Ignoring manifest-declared fixed orientation … since target sdk 36`, which confirms the D12 opt-out is needed | | `LANDSCAPE-before-api26-portrait.png`, `LANDSCAPE-before-api36-portrait.png` | C |
| VIN-031 | Approval after 70 s pairs the tablet | | | | U |
| VIN-032 | Approval during a 20 s network cut pairs the tablet | | | | U |
| KIO-041 / EXP-008 | `RESTART_APP` keeps Discovery pinned; ack after relaunch | ✖ Same restart code triggered while pinned: `Attempt to violate Lock Task Mode`, process dies, lock lost, Android launcher shown | | `KIO-041-before-logcat.txt`, `KIO-041-before-current-restart-pinned.png` | C |
| EXP-002 / EXP-007 | Refused pinning asked again on Home (throttled) | ✖ "NO THANKS" (or any touch outside the dialog) leaves the app unpinned; Catalog → Home does not ask again (`mLockTaskModeState=NONE`) | | `EXP-002-before-1-pin-dialog.png`, `EXP-007-before-home-not-repinned.png` | C |
| EXP-001 / EXP-006 / EXP-010 | Bars stay hidden over dialogs and keyboard | ✖ With kiosk active, opening the cart dialog shows the status bar and the navigation bar (`isReadyForDisplay` false → true); the navigation bar covers the bottom search bar | | `EXP-001-before-cart-dialog.png` | C |
| AJU-011 | 5 wrong PINs lock for 24 h; survives restart; remote PIN lifts it | ✖ Exit PIN set from staff (`settingsVersion` 21). 10 wrong PINs in a row: only "PIN incorrecto", keypad still enabled; the correct PIN then opens settings. No attempt limit | | `AJU-011-before-after-10-wrong.png` | C |
| RED-060 | Name, phone, tokens and PIN hash masked in logcat | ✖ The `profile` build logs in plain text: the STT `token` (`/stt/token`), **`customerName` and `customerPhone`** (share request body), and **`exitPinHash`** (`/devices/me/settings`). A 4-digit PIN hash is recoverable by trying all 10,000 values, so this leaks the exit PIN. `LogLevel.BODY` does not print headers, so `Authorization` is not logged | | `RED-060-before-logcat.txt` (values masked by QA) | C |

## Notes

- **Baseline APK:** `main` @ `f8622dd`, `profile`, installed over the existing debug build so the pairing was kept. After the 1.1 spike, the clean `main` APK was rebuilt and reinstalled.
- **Kiosk state on the API 26 emulator:** `kioskMode` is on (the side menu shows "Salir del modo kiosko"), and no exit PIN is set.
- **Extra finding (1.1):** the shell seeds `kioskLock` with `INACTIVE`, so every cold start while pinned calls `stopLockTask()` and then asks to pin again. This is added to D8 / task 6.0.
- **Extra finding (1.2):** the API error body uses `errorCode` (for example `"errorCode":"VALIDATION_ERROR"`), not `code`. The error parsing in tasks 4.4 and 5.2 must read `errorCode`.
- **Seen in passing (wave 2):** CAR-031: the cart "Compartir" button looks enabled with an empty form.
