## Context

Four small, independent fixes in the tablet, each described in `docs/handoff/android-regresion-2026-11-03.md` (§2.1 F-07, F-24, F-63; §2.2 F-43).

## Decisions

- **D1 — F-07.** Remove `requestMicPermissionOnHome`, its `LaunchedEffect`, the launcher and the process flag from `MainActivity`. `rememberMicPermissionGate` (Home, Search, Detail) is the only request path.
- **D2 — F-24 loop.** `RunDeviceHeartbeatUseCase` counts consecutive failures (returned or thrown) and waits `HEARTBEAT_RETRY_DELAYS[failures - 1]` (5, 10, 20, 40 s), else `HEARTBEAT_INTERVAL`. Each wait is `withTimeoutOrNull(wait) { internet goes false → true }`, so regained internet beats at once. `DEVICE_UNLINKED` needs no special case: the session is revoked and `collectLatest` stops the loop.
- **D3 — F-24 "internet".** New `NetworkConnectivityState.hasInternet`, mapped from a new `ConnectivitySnapshot.internetValidated` (`NET_CAPABILITY_INTERNET` and `NET_CAPABILITY_VALIDATED`). The existing `hasInternet` snapshot field (pairing socket reconnect) is unchanged.
- **D4 — F-63.** `uptimeSeconds = SystemClock.elapsedRealtime() / 1000`: device uptime, as the backend `HeartbeatRequest` defines it.
- **D5 — F-43.** `ProductImageGallery` draws only existing extra photos and no column when there are none. This drops the Figma gray-square fill also for one-photo products, which QA reported too. `ProductImage(showMissingLabel)` adds "Sin foto" only when the URL is missing, not on a load failure (DET-063 unchanged).

## Risks / Trade-offs

- [Product may want the Figma gray squares back for products with photos] → One-line change in `ProductImageGallery`.
- [Retries add up to 4 extra requests per outage] → Cheap and idempotent per the backend.
