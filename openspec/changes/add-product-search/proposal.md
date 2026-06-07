## Why

The voice-search STT pipeline already turns a consumer's spoken words into text, but that transcript currently goes nowhere — `HomeUiEffect.VoiceTranscribed` is observed as a no-op and the typed-search path (`NavigateToResults`) has no destination. In-store consumers can speak or type a query but never see the products the store offers. This change closes the loop: it takes the query (voice or text) and returns a browsable catalog of matching products, so the consumer can actually discover what the store sells — the core promise of the Discovery experience.

## What Changes

- Introduce a **product-search capability**: send the query to the Qhathia backend (`POST /api/v1/conversations/search`) and present the matching products as a "catalog viewed from chat" screen, following the results Figma. The query is capped at **500 characters**.
- Add the **results screen** (catalog-from-chat): a results-count headline (e.g. *Encontré 8 taladros para ti*, from the API `displayText`), products laid out in **2 rows with horizontal scroll**, a per-card **cart button**, a top bar with **Volver** / **Nueva búsqueda** / **Ir al carrito**, and a bottom **search bar + microphone** for the next query. The microphone animation is **kept** on this screen for voice-initiated searches.
- Add the **search processing states** with the existing 3-dot animation, surfaced through new design-system copy: *Revisando catálogo…* (searching), *Buscando minuciosamente…* (search response slow, >20s), *Buscando sugerencias…* (searching alternatives). For **text** searches the microphone animation is not shown during the search; for **voice** searches it is retained alongside the already-implemented STT phases.
- Add the **no-results / search-error "unhappy" screen** following its Figma: a *Lo siento, no pude encontrar lo que estás buscando. De pronto te interesaría…* message plus **3 alternative products**. The alternatives come from an **alternative search** — derived from the original keyword (e.g. *Taladro 20v* → *Taladros*) — run **disguised inside the search process** (under the *Buscando sugerencias…* state), taking the first 3 products of that result. Left idle for **30 seconds**, the unhappy screen auto-returns to Home idle.
- Add **search history with back navigation**: each search result is pushed onto a temporary, in-memory history so **Volver** can return to the previous result. When no previous result exists, **Volver** opens the new-search confirmation dialog.
- Add **Nueva búsqueda**: opens a confirmation dialog; on confirm it **clears the history** and returns to Home idle.
- Preserve the **network-error** behavior already established for STT: keep the screen with the *Perdí la conexión… / Parece que la red está inestable…* message.
- Enforce the **technical budgets**: ≤30s search request timeout; the slow-search message appears after 20s.
- **Out of scope:** product filters, standalone product suggestions, and cart navigation (the cart buttons render but do not navigate); product-detail screen.

## Capabilities

### New Capabilities
- `product-search`: The end-to-end product search — query submission (voice or text, ≤500 chars) to `POST /api/v1/conversations/search`; the catalog-from-chat results screen (2-row horizontal scroll, results headline, cart buttons, top bar, follow-up search bar + mic); the search processing states (searching / slow-search / searching-alternatives) reusing the 3-dot animation; the no-results & search-error unhappy screen with the disguised alternative search (keyword generalization → first 3 products); temporary search history with Volver back-navigation; Nueva búsqueda confirmation + history reset; the 30s idle auto-return from the unhappy screen and the ≤30s search timeout / 20s slow-search budgets.

### Modified Capabilities
- `discovery-home`: A successful voice transcription and a submitted text query now **drive the product search** instead of being a no-op / undestined navigation. The Home screen hands off the query (with its source — voice or text) to the product-search flow; for a voice search the microphone/processing context carries over so the search processing messages continue the same conversation.

## Impact

- **New module:** `features:product-search` (results & unhappy screens, `ProductSearchViewModel`, UiState/UiEvent/UiEffect, search state machine, navigation graph, processing/error copy + resolver). Feature modules cannot depend on each other, so shared pieces live in `core:*`.
- **Modules touched:** `core:network` (search API service + request/response DTOs for `/api/v1/conversations/search`, with a per-request 30s timeout), `core:domain` (`Product` domain model, `ProductSearchResult`, `SearchProductsUseCase`, alternative-keyword generalization, `ProductSearchRepository` port, search constants), `core:data` (`ProductSearchRepositoryImpl` + DTO→domain mappers + Hilt wiring, reusing `SessionTokenProvider` for the Bearer JWT), `core:ui` (a `ProductCard` component + horizontal results grid; reuses the existing `ProcessingMessage`, `VoiceWaveform`/mic, `DiscoverySearchBar`), `core:testing` (fakes/fixtures for the search repository and products), `features:home` (route `VoiceTranscribed` + text submit into product search), `:app` (register the new feature in the NavHost and wire the Home→ProductSearch handoff).
- **Build / tooling:** `settings.gradle.kts` registers `:features:product-search`; no new third-party dependencies expected (Ktor + serialization already present).
- **API:** consumes `POST /api/v1/conversations/search` (Bearer JWT) — request `{ transcribedText, sessionId?, language? }`, response `{ understood, results[], noResults, displayText, fallbackMessage, … }`. Note: the contract currently returns **max 5** products while the business rule states "no limit"; flagged as a risk to reconcile with the backend (see design.md).
- **Manifest:** no new permissions — `INTERNET` / `ACCESS_NETWORK_STATE` are already declared for the STT pipeline.
- **Out of scope / deferred:** filters & suggestions sidebar shown in the Figma, cart screen and cart navigation, product detail; multi-turn `sessionId` correlation beyond passing it through.
