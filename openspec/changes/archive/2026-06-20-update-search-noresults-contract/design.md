## Context

The product-search vertical (`core:network → core:data → core:domain → features:product-search`) already deserializes `noResults`, `understood`, and `fallbackMessage` from `POST /api/v1/conversations/search`, but `ProductSearchViewModel` predates the Fase 8 contract: it decides happy vs. unhappy from `result.products.isEmpty()` and, on an empty/erroring result, runs a **second** request through `GeneralizeQueryUseCase` (e.g. *Taladro 20v* → *Taladros*) to fabricate 3 alternatives — surfaced as the `SearchingAlternatives` phase.

Under the new contract the backend itself returns, on a no-match query, `noResults=true` **with up to 3 store products already in `results`**, plus `suggestedFromCategory: String?` to personalize the copy. The client-side fallback is now redundant and produces different products than the backend curates. This change aligns the app to consume the contract and retires the disguised alternative search.

All work lands in existing modules; no new Gradle module is required (the dependency graph `features/* → core:domain ← core:data → core:network` is unchanged).

## Goals / Non-Goals

**Goals:**
- Decide the search outcome from the response's `noResults` flag, not from an empty product list.
- Render the unhappy screen from the up-to-3 products the response already carries — one request per search.
- Carry `suggestedFromCategory` network → domain → presentation and use it to personalize the unhappy "Te puede interesar [de X]:" copy.
- Remove the now-dead disguised alternative search (`SearchingAlternatives` phase, its message, `GeneralizeQueryUseCase`).

**Non-Goals:**
- Adding `brand`/`tags` to the domain `Product` or client-side sidebar filtering (separate concern, already resolved via re-query).
- Multi-turn `sessionId` continuity or changing the 30s response budget, slow-search threshold, idle timeout, or network-error handling.
- Any backend / API-contract change.

## Decisions

### Outcome keyed off `noResults`, in a single request
`performSearch` settles on `result.noResults`: `false → settleResults(...)`, `true → settleUnhappy(result.products, result.suggestedFromCategory)`. No second request is issued. **Rationale:** the contract makes the backend the single source of truth for both the outcome and the suggestion set; the flag means "the ranking found no match," distinct from "the list is empty." *Alternative considered:* keep inferring from `products.isEmpty()` — rejected because `results` is now non-empty on the unhappy path, so emptiness no longer maps to the unhappy outcome.

### Retire the disguised alternative search
`GeneralizeQueryUseCase`, the `SearchingAlternatives` phase, `startAlternativeSearch`, and the `ps_searching_alternatives` string are removed. The state machine collapses to `Searching → (SlowSearch) → Results | Unhappy`, with `NetworkError` for connectivity. **Rationale:** its sole purpose was to fabricate 3 alternatives, which the backend now supplies. *Alternative considered:* keep it as a fallback when the response carries `noResults=true` but an empty `results` — rejected: an empty unhappy list is valid per the contract (a store with <3 AVAILABLE products), and a second generalized request would re-introduce the divergence this change removes.

### Search-API error vs. no-results are different paths
A genuine search-API failure (non-200, malformed body) has no usable suggestions, so it settles the unhappy screen with an **empty** suggestion list (the apology header still shows); a connectivity failure keeps the existing `NetworkError` overlay. **Rationale:** only `noResults=true` responses carry curated suggestions; errors cannot. This preserves today's error/Network split while dropping the generalized retry.

### Unhappy copy: a single apology with a category-personalized invite line
`suggestedFromCategory` is carried through `ConversationSearchResponseDto` → `ProductSearchResult` → `ProductSearchPhase.Unhappy`. The unhappy body shows a single apology block: a constant first line (`ps_unhappy_message`, "Lo siento, no pude encontrar lo que estás buscando.") followed by an **invite line** that swaps on the category:
- non-null `suggestedFromCategory` → "Relacionado a '*<categoría>*' te podría interesar…" using the value **verbatim** as the API sends it (lowercase, singular, e.g. *lámpara*);
- null → the generic "De pronto te interesaría…".

There is no subtitle and no separate "Te puede interesar" lead-in above the products. The API `fallbackMessage` is **not** surfaced (it duplicated the apology and added little), so `ProductSearchPhase.Unhappy` does not carry it; `ProductSearchResult.fallbackMessage` remains in the domain but is unused by this screen.

**Rationale:** one controlled, kiosk-consistent apology reads cleaner than apology + subtitle + lead-in; folding the personalization into the apology's invite line removes redundancy. Using the category verbatim keeps the copy deterministic and avoids locale-specific pluralization rules.

### Mapper keeps the emptiness safety
`ConversationSearchResponseDto.toDomain()` keeps `noResults = noResults || products.isEmpty()` so a malformed `noResults=false` with empty `results` still routes to unhappy rather than an empty results grid. The unhappy branch uses whatever `products` the response carried (0–3).

## Risks / Trade-offs

- **An unhappy response with 0 suggestions (store has <3 AVAILABLE) shows an apology with no products.** → Accepted: valid per the contract; the apology copy and bottom search bar still let the consumer continue.
- **Removing `GeneralizeQueryUseCase` deletes a tested use case and its wiring.** → Mitigated: its only caller was the alternative search being removed; tests for it are deleted with it, and Hilt provision (if any) is dropped.
- **`SearchingAlternatives` removal touches the phase enum and processing-message mapping.** → Mitigated: it is one `data object` and one string; the `when` over phases becomes exhaustive again after removal.

## Migration Plan

Additive at the DTO/domain layer (`suggestedFromCategory` defaulted null → older responses deserialize unchanged) and subtractive in presentation/domain (remove the alternative-search path). No data migration. Rollback = revert the change; no persisted state depends on it.

## Open Questions

_Resolved with the product owner:_
- **Unhappy copy** — a single apology with a category-personalized invite line; no subtitle and no separate "Te puede interesar" lead-in. The API `fallbackMessage` is not shown.
- **Invite line** — non-null category → "Relacionado a '*lámpara*' te podría interesar…" (category verbatim); null → generic "De pronto te interesaría…".
