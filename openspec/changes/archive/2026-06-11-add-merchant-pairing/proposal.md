## Why

The Discovery tablet currently has no real identity: it authenticates to Qhathia by logging in with hard-coded development credentials (`SessionTokenProvider`), which is an explicit interim seam. To operate in the ecosystem the tablet must be **paired** to a real merchant store so it can obtain a long-lived DEVICE token, sync that store's catalog, and forward purchase orders. This change implements the tablet side of the pairing handshake (ClickUp 86e1kbt2d) and lights up the so-far non-functional "Vincular" section already present in the Settings side menu.

## What Changes

- Add a **Vincular (Pair)** section to the existing Settings screen, switchable from the Wi-Fi section via the side menu, with an animated selection shadow on switch.
- **Generate a pairing code** (`POST /api/v1/devices/pairing-token`) only while the merchant is on the pairing settings screen, showing the 5-character code and a **countdown timer** for its 5-minute TTL.
- **Subscribe to pairing status** over a raw WebSocket (`WSS /api/v1/devices/pairing-session/{sessionId}`) and react to `WAITING | APPROVED | REJECTED | EXPIRED`.
- On `APPROVED`, **exchange the session for a DEVICE JWT** (`GET /api/v1/devices/pairing-session/{sessionId}/token`) and persist it. This token replaces the dev-credential login as the source of the app's access token.
- Show **paired-device details** (`GET /api/v1/devices/me`): device name, platform/model, last sync time, and the code used; plus a placeholder for the merchant name where no API supplies it.
- Add an **Unpair** button and a **confirmation dialog**. The actual unpair call is a **TODO** — no backend API exists yet — so the action is wired in the UI but stubbed at the data layer.
- Auto-regenerate / re-request a fresh code when the timer expires while the screen is still open; stop code generation when the merchant leaves the pairing screen.

## Capabilities

### New Capabilities
- `device-pairing`: the full tablet-side pairing lifecycle — code generation with TTL countdown, WebSocket status subscription, DEVICE-token exchange and persistence, paired-device detail display, and the (stubbed) unpair flow.

### Modified Capabilities
- `store-session`: the persisted session SHALL additionally hold the DEVICE access token obtained from pairing, and that token (when present) SHALL be the source of the app's authenticated access token instead of the development-credential login.

## Impact

- **New module code**: `core:network` (pairing API service, WebSocket client, DTOs — adds the `ktor-client-websockets` dependency), `core:domain` (pairing models, repository port, use cases), `core:data` (pairing repository impl, device-session persistence), `features:settings` (pairing screens, ViewModel, MVI contract, side-menu section switching).
- **Modified**: `SessionTokenProvider` (consumes the persisted DEVICE token before falling back to dev login); Settings side menu / navigation (Wi-Fi ↔ Vincular).
- **APIs consumed**: `/api/v1/devices/pairing-token`, `/api/v1/devices/pairing-session/{sessionId}` (WSS), `/api/v1/devices/pairing-session/{sessionId}/token`, `/api/v1/devices/me`.
- **Build**: new version-catalog entry + dependency for Ktor WebSockets; `INTERNET` permission already present.
- **Out of scope / pending**: server-side unpair (TODO), merchant-name source where no API provides it, catalog-sync wiring (separate story).
