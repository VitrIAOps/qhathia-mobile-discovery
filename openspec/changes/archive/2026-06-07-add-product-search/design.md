## Context

The Discovery app already has a working voice-search STT pipeline (`voice-search-stt`) and a Home screen (`discovery-home`). On Home, a successful transcription emits `HomeUiEffect.VoiceTranscribed(transcript)` — currently a no-op — and a typed submit emits `HomeUiEffect.NavigateToResults(query)` with no destination. This change builds the destination: a product-search flow that calls `POST /api/v1/conversations/search` and renders the results as a "catalog viewed from chat", plus the no-results/error "unhappy" screen.

Current state and constraints:
- Multi-module Clean Architecture + MVI. Feature modules may depend on `core:*` but **never** on each other (`features:home` and the new `features:product-search` cannot reference one another).
- `core:network` exists with the Ktor `HttpClient`, `NetworkResult`, `SessionTokenProvider` (interim auth seam that logs in with dev credentials and caches the JWT), and the absolute-URL service pattern (`SttTokenApiService`, `AuthApiService`, `DeepgramClient`).
- `core:ui` already provides `ProcessingMessage` (animated three-dot label), `MicButton`, `VoiceWaveform`, and `DiscoverySearchBar`.
- `core:domain` is pure Kotlin; `core:testing` is a pure-Kotlin JVM module providing fakes/fixtures.
- The search API contract: request `{ transcribedText (≤500, alias 'transcript'), sessionId?, language?='es' }`; response `{ understood, interpretedQuery, results[], noResults, spokenText, displayText, fallbackMessage }`. Each result has `{ id, name, description, price, availability, categoryName, primaryImageUrl, relevanceScore }`. The contract notes a **max of 5** products per response.
- The two Figma frames (results `722-13413`, unhappy `644-26079`) show a left filters/suggestions sidebar that the ClickUp scope explicitly **excludes**.

## Goals / Non-Goals

**Goals:**
- A new `features:product-search` module that owns the results screen, the unhappy screen, the search state machine, and the new-search/back confirmation dialogs.
- One `POST /api/v1/conversations/search` integration in `core:network` + `core:data`, mapping the response to a framework-free `ProductSearchResult` in `core:domain`.
- Search processing states (searching / slow-search >20s / searching-alternatives) reusing `core:ui` `ProcessingMessage`; mic animation retained for voice, hidden during text search.
- Disguised alternative search (keyword generalization → first 3 products) for no-results and search (API) errors only.
- Temporary in-memory search history with Volver back-navigation; Nueva búsqueda confirmation that clears history and returns Home idle.
- A reusable `core:ui` `ProductCard` and horizontal results grid; light + dark previews; unit tests for the use case, repository mapping, and ViewModel state machine.

**Non-Goals:**
- Filters and standalone suggestions sidebar (Figma shows them; scope excludes them).
- Cart screen and cart navigation (cart buttons render but do not navigate).
- Product-detail screen.
- Replacing the interim `SessionTokenProvider` auth seam with real device pairing.
- Multi-turn conversational correlation beyond passing `sessionId` through.

## Decisions

### Decision: New `features:product-search` module (not folded into `features:home`)
The results and unhappy screens are distinct destinations with their own state machine and history. Per the standards, features cannot depend on each other, so anything shared (processing message, mic, search bar, product card) lives in `core:ui`/`core:domain`. Home emits the query as a navigation effect; `:app` wires Home → ProductSearch in the NavHost.
- **Alternative considered:** add the screens to `features:home`. Rejected — it bloats the Home feature and conflates two destinations; the catalog-from-chat is conceptually a separate screen reachable by navigation.

### Decision: Home → ProductSearch handoff carries query + source via the nav route
Extend the Home effects so both a typed submit and a `VoiceTranscribed` transcript navigate to the product-search destination with two arguments: the query string and a `source` flag (`voice` | `text`). The `source` drives whether the mic animation is retained during search. This keeps the contract explicit and avoids a shared singleton between two feature modules.
- **Alternative considered:** a shared in-memory "current query" holder in `core:data`. Rejected — nav arguments are simpler, testable, and avoid hidden coupling. (`sessionId` can be added to the route later for multi-turn.)

### Decision: The bottom-bar microphone captures voice in place (no bounce back to Home)
The chat-area mic is the real animated `MicButton` (compact size), and tapping it runs a full voice search **on the product-search screen** rather than navigating back to Home. `ProductSearchViewModel` reuses the same `core:domain` ports as Home — `AudioRecorder`, `TranscribeVoiceSearchUseCase`, and `VoiceSearchConstants` — to drive a `VoiceListening → VoiceTranscribing → (VoiceSlowResponse)` flow; a successful transcript starts a brand-new in-place search (`source = Voice`). STT failures and no-audio restore the previous screen and do **not** trigger an alternative search (per the spec). No feature-to-feature dependency is introduced because the capture ports live in `core:domain`.
- **Alternative considered:** emit a `NavigateHomeForVoice` effect so the mic flow only lives on Home. Rejected after design review — it forces the consumer to leave the catalog to speak again, breaking the "continue the conversation" UX shown in the Figma. The small duplication of the listening loop is acceptable; a future refactor can extract a shared `VoiceCaptureController` used by both Home and product-search.

### Decision: Search domain shape — `Product` + `ProductSearchResult`, with a single `SearchProductsUseCase`
`core:domain` gets a framework-free `Product` (id, name, description, price, availability, categoryName, imageUrl) and `ProductSearchResult` (products, displayText, noResults, understood, fallbackMessage). `ProductSearchRepository.search(query, language): Result<ProductSearchResult>` is the port; `ProductSearchRepositoryImpl` in `core:data` maps the DTO → domain and reuses `SessionTokenProvider.accessToken()` for the Bearer JWT (same pattern as STT). `SearchProductsUseCase` enforces the ≤500-char cap and orchestrates the call.
- **Alternative considered:** reuse the existing `VoiceSearchResult`. Rejected — that models a transcript, not a product list.

### Decision: Alternative search and keyword generalization live in the domain layer
A `GeneralizeQueryUseCase` (or a pure function in `core:domain`) produces the generalized keyword (strip trailing numeric/spec qualifiers such as "20v", reduce to the head noun, keep it simple and deterministic). The ViewModel, on a no-results/search-error outcome (but **not** an STT error), transitions to the `SearchingAlternatives` state and calls `SearchProductsUseCase` with the generalized query, then keeps the first 3 results. Keeping generalization in the domain layer makes it unit-testable in isolation.
- **Alternative considered:** ask the backend for alternatives. Rejected — no such field in the contract; the task specifies client-side derivation from the keyword and reusing the same search API.

### Decision: Per-request 30s timeout for search; reuse `NetworkResult` error mapping
The shared Ktor client uses long STT-oriented timeouts (≤120s response). The search service applies a per-request timeout of 30s (Ktor `HttpTimeout` request override on the call) to honor the business budget. Network vs. HTTP failures map through the existing `NetworkResult` → an error category so the ViewModel can distinguish a **network error** (keep screen + connection message) from a **search/API error** (unhappy screen + alternative search).
- **Alternative considered:** a second `HttpClient`. Rejected — a per-request timeout override is lighter and keeps one client.

### Decision: Reuse `core:ui` processing/mic/search components; add only `ProductCard` + results grid
`ProcessingMessage` already renders the animated three-dot copy; the new searching/slow-search/searching-alternatives strings plug into it. The mic and `DiscoverySearchBar` are reused for the bottom bar. The only new shared component is `ProductCard` (image via Coil, name, price `S/ {price}`, cart icon button) plus a 2-row horizontal grid (and a 3-up row for alternatives). Strings + the state→copy resolver live in `features:product-search` (where `core:domain` is available), mirroring how `SttErrorMessages` lives in `features:home`.

### Decision: Search history as an in-memory stack in the ViewModel
Each completed search (query + result) is pushed onto a `List` held in the `ProductSearchViewModel` (process-scoped, temporary). Volver pops; an empty stack opens the close-confirmation dialog. Nueva búsqueda (confirmed) clears the stack and emits a navigate-to-Home-idle effect. No persistence — the task says "temporarily" and "cleared on confirmed new search".

## Risks / Trade-offs

- [API caps results at 5 but business says "no limit" (Figma shows 8)] → Build the UI to render whatever the API returns (no client cap); flag the discrepancy to the backend team. Tracked in Open Questions; does not block the client work.
- [Keyword generalization is heuristic and may produce a poor alternative query] → Keep it deterministic and unit-tested; if the generalized search also returns nothing, show the unhappy screen with no alternatives rather than looping. Revisit with real catalog data.
- [Two feature modules in one conversational flow could tempt a direct dependency] → Enforced separation via nav arguments and `core:*` shared code; no `features:home` ↔ `features:product-search` edge.
- [`sessionId` correlation for multi-turn is deferred] → Pass it through where available; the route can carry it later without reshaping the domain.
- [Interim `SessionTokenProvider` (dev-credential login) also gates search] → Acceptable for now; single replace point when device pairing lands, same as STT.

## Open Questions

- Result count: should the client paginate or request more than 5 once the backend lifts the cap? (Pending backend confirmation of the "no limit" rule.)
- Generalization rules: is stripping trailing qualifiers + head-noun reduction sufficient, or should we maintain a small synonym/category map per store rubro?
- Does "Ir al carrito" need to show a disabled/coming-soon affordance, or simply no-op silently until cart navigation lands?
