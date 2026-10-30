## Why

The full QA run of 2026-10-25 (`docs/handoff/qa-2026-10-25-casos-observados-por-flujo.md`) found that the kiosk tells customers things that are not true (a quote "sent" that WhatsApp never delivered, an "order generated" that does not exist), hands one customer's cart and phone number to the next one, and can drop out of kiosk mode or never finish pairing. Every case in this wave can be fixed in the app alone, so it does not wait on the backend. Backend follow-ups (B-01, B-02, B-14, nginx WebSocket timeout, Meta registration, cleartext HTTP) are tracked separately.

## What Changes

**Honest share outcome** (DET-032, CAR-007, CAR-032, CAR-008, DET-034, CAR-011)
- A share counts as delivered only when the server answers `status = SENT`. `FAILED` and `STUB` are shown as "not delivered", never as success.
- "Compartir por WhatsApp" gets its own success copy (a quote was sent), separate from the order copy ("¡Pedido generado con éxito!").
- Retry only on connectivity, timeout and 5xx. Never retry a 4xx or a `FAILED` answer.

**Customer session hand-off** (CAR-040, CAR-041, CAR-042)
- **BREAKING (behavior):** when a customer's session times out, the app ends the whole session: it clears the basket and the name/cell-phone form, closes any open dialog and returns to Home. Today only the basket is cleared and the screen stays.
- The timeout also applies on Home while the basket is not empty.
- Touches and typing inside dialogs (cart, share, PIN) count as activity, so a customer typing their data is never interrupted.

**Only orderable products reach the order** (DET-013, CAR-023, CAR-025, BUS-042 on the app side)
- Add-to-cart and share are allowed only for products that are active and `AVAILABLE` (the same rule the server enforces).
- When the server rejects an order or share because a product is no longer available, the app rechecks the cart lines, marks the ones that are unavailable and lets the customer remove them, instead of looping on a generic error.

**Robust pairing** (VIN-031, VIN-032, VIN-030)
- The pairing WebSocket sends a client ping every 20 s so the proxy does not close it while idle. If it closes or fails before a final status, the app reconnects with backoff, and right away when the network comes back. The server sends the current status on every connect (contract), so an approval sent while the tablet was offline is still received.
- The one-shot token exchange is never polled. It is called once after `APPROVED`, or once as a fallback when the server rejects the handshake.
- Turning the screen off and on, or leaving and coming back while the code is valid, keeps the current code instead of generating a new one.

**Landscape only** (business rule; also closes VIN-030 and the portrait cases VIN-052, CAT-027, DET-024, CAR-048)
- The app is always shown in landscape and does not rotate on any screen, including onboarding and pairing. This includes Android 16 tablets, which ignore orientation locks for apps that target SDK 36 unless the app opts out.

**Kiosk stays locked** (KIO-041, EXP-002, EXP-007, EXP-001, EXP-006, EXP-010)
- `RESTART_APP` relaunches the app without leaving the tablet on the Android desktop. The command is acknowledged only after the app is running again.
- Pinning is requested again when the tablet comes back to Home and is not pinned (the customer can still refuse the system dialog, because Device Owner is not available).
- System bars stay hidden while dialogs, the keyboard or focus changes are on screen.

**Others**
- The Catalog microphone opens voice capture in Search instead of sending an empty search (CAT-030, VOZ-009).
- After 5 wrong attempts in a row, the settings PIN locks for 24 hours. After that the merchant gets 5 new attempts. The lock survives an app restart, and a new PIN sent from the staff app lifts it (AJU-011).
- Network logs redact customer name, cell-phone, tokens and the `Authorization` header in every build that logs (RED-060, app side).

## Capabilities

### New Capabilities
- `customer-session`: Ends one customer's session on inactivity (basket, consumer form, open dialogs, return to Home) and defines what counts as activity, including activity inside dialogs.
- `screen-orientation`: The app is fixed to landscape on every screen and on every supported Android version.

### Modified Capabilities
- `cart-whatsapp-share`: Delivered only on `SENT`; share-specific success copy; no retry of 4xx or `FAILED`; auto-share after an order follows the same delivery rule.
- `cart-order-submission`: A server rejection for unavailable products flags the affected lines instead of showing a generic error loop.
- `shopping-cart`: The inactivity rule moves to `customer-session`; only active `AVAILABLE` products can be added.
- `product-detail`: An inactive or non-`AVAILABLE` product cannot be added or shared, and the screen says so.
- `product-search`: A voice hand-off with no query starts listening instead of searching.
- `device-pairing`: WebSocket keep-alive and reconnection, a single exchange (never polled), and keeping the code when the screen is recreated.
- `settings-access`: 24-hour lock after 5 wrong PIN attempts.
- `kiosk-lock-mode`: Re-request pinning on Home and keep the bars hidden over dialogs. (Delta on the unarchived `kiosk-lock-mode` change.)
- `device-remote-commands`: A restart keeps the tablet on Discovery, and the ack is sent after the relaunch. (Delta on the unarchived `device-remote-commands` change.)
- `app-build-config`: Network logs in the `profile` build are redacted.

## Impact

- `core/domain`: `ProductDetail.active`, a share outcome model (`Delivered` / `NotDelivered` / `Rejected`), a shared `ShareQuoteUseCase` used by cart and detail, `IsOrderableUseCase`, a customer-session port and use cases, a PIN-attempt port, and pending restart ack.
- `core/data`: share repository reads `status` and classifies HTTP codes; order repository classifies 400/404; pairing repository with socket reconnection and a single-exchange guard; DataStore entries for PIN lockout and pending restart ack; in-memory customer-session signal.
- `core/network`: log redaction in `KtorClientFactory`; WebSocket `pingInterval`; parse of the API error `code`; `ProductDetailDto.active` mapping.
- `app/AndroidManifest.xml`: `screenOrientation="landscape"` on every activity, plus the Android 16 opt-out property for large screens.
- `docs/android/tablet-kiosk-setup.md`: lock rotation on the device as a second layer.
- `core/ui`: user-activity reporter (CompositionLocal) and immersive handling for dialog windows.
- `features/cart`, `features/product-detail`, `features/product-search`, `features/catalog`, `features/pairing`, `features/settings`, `app` (`MainActivity`, `MainViewModel`, restart trampoline activity in the manifest).
- No new Gradle module and no new library. No API contract change: only fields and endpoints that already exist are used (`status` of share-pdf, `active` of product detail, the pairing WebSocket "current status on connect").
- Optional backend hardening, not required by this change: an idempotent token exchange for a short window. (The nginx timeout is not involved: the 60 s close comes from the app's own socket timeout, confirmed on production.)
- Archive order: this change must be archived after `kiosk-lock-mode` and `device-remote-commands`.
