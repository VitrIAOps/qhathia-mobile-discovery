## Why

Wave 1 (`qa-wave-1-critical-fixes`) fixed the critical and high cases of the 2026-10-25 QA run. The medium cases that remain make the kiosk feel broken in daily use: a double tap opens the same screen twice, the system Back and "Volver" do different things, "No te escuché…" never goes away, the microphone keeps recording after the customer leaves, dialogs close while a request is still running, and the share error offers "Cancelar orden", which empties the cart. Every case in this wave can be fixed in the app alone (`docs/handoff/qa-2026-10-25-casos-observados-por-flujo.md`).

## What Changes

**One tap, one screen** (INI-020, DET-021, DET-022)
- A navigation action runs only from the screen that is currently shown. A second tap during the transition (double tap on "Ir al catálogo", on a related product, or "Volver" pressed three times) does nothing, so screens are never stacked twice and Back never pops past the screen the customer meant to leave.

**Back does what "Volver" does** (CAT-024, CAT-025, BUS-052, BUS-060)
- On Catalog, Search and Detail, the system Back runs the same action as the on-screen "Volver": the close confirmation when the cart has items, the previous result in Search history, and so on.

**Voice** (INI-051, VOZ-003, VOZ-004, VOZ-008, VOZ-010)
- "No te escuché…" returns to the idle Home after the same 30 s as the other voice errors.
- Leaving the screen or the app (screen hidden) stops any recording or transcription in progress, on Home and on Search.
- The Search microphone asks for the microphone permission like Home does, and says why when it is refused.
- The Detail microphone opens voice search, like the Catalog microphone does since wave 1.

**Cart and share dialogs** (CAR-033, CAR-043, CAR-044, CAR-020, CAR-021, CAR-024, CAR-031)
- A share error uses share copy and offers only "Entendido", which goes back to the cart with everything kept. "Cancelar orden" stays only on the order error.
- While an order or share request is running, the cart and the product share dialog cannot be closed (outside tap, Back or the close button). Closing a result screen does what its main button does, so no result is left hanging for the next time the dialog opens.
- "Realizar pedido" and "Compartir" are enabled only when the basket has products, the customer data is valid and no line is flagged. Compartir no longer looks enabled with an empty form.
- The basket holds at most 20 distinct products (the server limit). Adding a 21st product is refused with a message; more units of a product already in the basket are still allowed.

**Pairing and settings** (VIN-024, AJU-031, AJU-050)
- "Generar nuevo código" is ignored while a code request is running, so fast taps no longer end in the false "No se pudo generar…" error.
- On a paired tablet, a failure loading the device details in "Vincular" shows a connection error, and "Reintentar" loads the details again. It never generates a pairing code on a paired tablet.

**Out of this wave**
- BUS-003 / EXP-004 (filter chips that do not filter): the chips refine the text the search AI reads, so the fix is on the backend.
- DET-012 (related product charged at full price): backend B-02.
- VIN-022 (abandoned codes stay approvable): needs a backend way to cancel a pairing session.
- Low cases (accessibility labels, placeholders, stepper, Wi-Fi details, "TIENDA" title): wave 3.

## Capabilities

### New Capabilities
- `app-navigation`: Navigation runs once per tap from the visible screen, and the system Back runs each screen's "Volver" action.

### Modified Capabilities
- `discovery-home`: the no-speech retry state returns to idle after 30 s; leaving Home stops voice capture.
- `voice-search-stt`: voice capture stops when its screen is hidden.
- `product-search`: the Search microphone asks for the microphone permission; leaving Search stops voice capture; system Back follows "Volver".
- `product-catalog`: system Back follows "Volver" (close confirmation with items in the cart).
- `product-detail`: the Detail microphone opens voice search; the share dialog cannot be closed while sending.
- `shopping-cart`: at most 20 distinct products in the basket; actions need a non-empty basket.
- `cart-order-submission`: the cart cannot be closed while a request runs; closing a result screen runs its main action.
- `cart-whatsapp-share`: share error copy and actions ("Entendido" only, cart kept); Compartir needs valid customer data.
- `device-pairing`: a single code request at a time; on a paired tablet a loading failure shows a connection error and retries the load.

## Impact

- `app` (`MainActivity` NavHost): navigation and pop calls go through a guard that checks the source entry is resumed.
- `features/catalog`, `features/product-search`, `features/product-detail`: `BackHandler` sending the screen's Back event; Detail `ActivateMic` hand-off; Search microphone permission request; Search and Home stop voice capture on `ON_STOP`.
- `features/home`: `NoAudioRetry` idle timer; `ScreenStopped` cancels capture.
- `features/cart`: share error state and copy, dialog dismissal rules, action enablement, 20-line message.
- `features/product-detail`: share dialog dismissal rules.
- `core/domain`: `CartConstants.MAX_DISTINCT_LINES = 20`; `AddToCartUseCase` returns whether the product was added or refused by the line limit.
- `features/pairing`: generation guard; separate error for loading the paired device, with its own retry.
- `core/ui`: a shared microphone-permission helper reused by Home and Search, if it simplifies both.
- No API, DTO or dependency changes.
