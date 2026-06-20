## Context

The product-search vertical already spans `core:network → core:data → core:domain → features:product-search` and drives a phase state machine in `ProductSearchViewModel` (`Searching → Results | SearchingAlternatives → Unhappy | NetworkError`). The `POST /api/v1/conversations/search` response (Fase 9) carries `filters: [{criterion, options[]}]` (≤5 criteria × ≤5 options) and `suggestions: List<String>` (≤5), both `[]` on the unhappy path — but `ConversationSearchResponseDto` models neither, so the data is discarded today.

This change consumes those two fields and adds a refine-by-selection interaction. The implementation slots into the **existing modules**; no new Gradle module is required, which keeps the change aligned with the multi-module dependency graph (`features/* → core:domain ← core:data → core:network`).

Two authoritative docs conflicted on how a selection applies. The ClickUp story says to **re-query** the API with the term concatenated naturally; the API-contract Fase 9 note suggests **client-side** filtering on each product's `brand`/`tags`. Resolved with the product owner: **re-query**, concatenating selected terms onto the **original raw query** (not the head noun, not the running query). This design follows that decision.

## Goals / Non-Goals

**Goals:**
- Carry `filters` and `suggestions` from network DTO → domain `ProductSearchResult` without leaking serialization concerns past `core:data`.
- Render the filters & suggestions section on the happy results screen per Figma (default + selected states); hide it when both are empty.
- Support selecting N filter options and at most one suggestion, with selection driving an API re-query built from the immutable original raw query.
- Reuse the existing search state machine, alternative-search, and unhappy/network handling for re-queries.

**Non-Goals:**
- Client-side filtering on `brand`/`tags` (explicitly not chosen). `brand`/`tags` are not added to the domain `Product` here.
- Multi-turn `sessionId` continuity, head-noun extraction for the concatenation base, or persisting selection across a Nueva búsqueda reset.
- Any backend / API-contract change.

## Decisions

### Re-query on selection, concatenating onto the original raw query
Each select/de-select rebuilds the query as `baseQuery + selected terms` and calls the existing search path. **Rationale:** matches the ClickUp story verbatim and keeps the backend as the single source of truth for which products, filters, and suggestions are relevant after refinement — the returned `filters`/`suggestions` repaint each time. **Base = original raw query** (the query that opened the current results view), held immutably so de-selecting everything reproduces it and selections never compound. *Alternative considered:* client-side filtering (instant, offline) — rejected because filters would only narrow the current page and the story's concatenation examples would not apply; also rejected concatenating onto the running query (compounds terms unpredictably).

### Concatenation lives in a domain use case
A new `ComposeSearchQueryUseCase` (pure Kotlin, `core:domain`) takes the base query, the selected filter terms, and the optional selected suggestion, and returns the natural-language query string. **Rationale:** keeps the joining rule framework-free and unit-testable, mirroring the existing `GeneralizeQueryUseCase`. A filter term renders as `"<criterion> <option>"` lowercased (e.g. *voltaje 12V*); the suggestion appends as its phrase; parts join with single spaces onto the trimmed base.

### Selection state owned by the ViewModel, not by the phase
`ProductSearchPhase.Results` is extended to carry the response's `filters` and `suggestions` (immutable lists) for rendering, while the **current selection** (`Set<FilterOptionRef>` + nullable selected suggestion) and the **base query** live as `ProductSearchViewModel` fields, not in the navigable history entries. **Rationale:** selection is transient interaction state for the active results view; history entries remain snapshots of settled outcomes (consistent with how `source`/`phase` are already historised). The selected state needed for highlighting is derived in `UiState` from the ViewModel selection.

### Domain model shape
`SearchFilter(criterion: String, options: List<String>)` added to `core:domain.model`; suggestions are a plain `List<String>` (no wrapper). `ProductSearchResult` gains `filters: List<SearchFilter>` and `suggestions: List<String>` (default empty for backward-compatible construction). `SearchFilterDto(criterion, options)` added to `core:network`, with `ConversationSearchResponseDto` gaining both fields (defaulted, `ignoreUnknownKeys` already on). `ProductMappers` maps DTO → domain at the data boundary.

### MVI surface
New `ProductSearchUiEvent`s: `FilterToggled(criterion, option)` and `SuggestionSelected(phrase)` (re-tapping the active suggestion de-selects it). `ProductSearchUiState` exposes the criteria/options/suggestions plus the selected set so `ProductSearchContent` can render selected chips. The ViewModel handles each event by mutating the selection, composing the query via `ComposeSearchQueryUseCase`, and calling the existing `performSearch(composed, currentSource)` — which already cancels in-flight jobs and runs the slow-search/alternative/unhappy machinery.

## Risks / Trade-offs

- **A re-query that returns no products drops into the unhappy screen, losing the filter bar.** → Accepted: consistent with the existing empty-result behavior; the consumer can Volver to the previous result via the existing in-memory history.
- **Network cost / latency per tap (one request each).** → Accepted per the product decision; the existing 30s budget, slow-search message, and network-error overlay already cover slow/failed re-queries.
- **Natural-language concatenation may not always read perfectly** (e.g. multiple filters appended to a verbose raw query). → Mitigated by keeping the join rule isolated in `ComposeSearchQueryUseCase` so it can be tuned without touching presentation; backend intent extraction is tolerant of phrasing.
- **Selection vs. refreshed lists after a re-query.** The new response may return different criteria/options. → The ViewModel keeps the user's selection set; options no longer present simply have nothing to highlight. De-selection always remains possible because the base query is retained.

## Migration Plan

Additive only. New DTO fields are defaulted so older responses deserialize unchanged; `ProductSearchResult`'s new fields default to empty, so every existing construction site and test compiles. No rollback steps beyond reverting the change. No data migration.

## Open Questions

- Exact natural-language join for a filter option (`"<criterion> <option>"` vs. just `"<option>"`) — start with `"<criterion> <option>"` lowercased and validate against backend intent quality during apply.
- Whether selected filters should visually persist (greyed) when absent from a refreshed response, or disappear — defaulting to disappear; confirm against Figma during implementation.
