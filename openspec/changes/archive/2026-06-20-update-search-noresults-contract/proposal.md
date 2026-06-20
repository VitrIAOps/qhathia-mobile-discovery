## Why

The `POST /api/v1/conversations/search` contract changed (Fase 8): on a no-match query the backend now returns `noResults=true` **with up to 3 store products already in `results`** (plus a nullable `suggestedFromCategory` to personalize the copy). The app still ignores `noResults` and fabricates the 3 alternatives itself — running a second "disguised alternative search" by generalizing the query. That client-side fallback is now redundant, costs an extra request, and produces different products than the backend intends to show.

## What Changes

- Drive the happy/unhappy decision from the response's **`noResults` flag** instead of inferring it from an empty product list.
- On the unhappy path, render the **up-to-3 suggested products the backend already returned in `results`** — no second request.
- **BREAKING (internal):** retire the client-side disguised alternative search — remove the `SearchingAlternatives` phase, its processing message, and `GeneralizeQueryUseCase`. A search now settles in a single request: `Searching → Results | Unhappy`.
- Model the new **`suggestedFromCategory`** field through network → domain and use it to personalize the unhappy copy ("Te puede interesar de *lámparas*:" vs the generic "Te puede interesar:").
- A genuine search-API error (non-200 / malformed body) settles the unhappy screen with **no** suggestions (the apology copy still shows); connectivity failures keep the existing network-error handling.

## Capabilities

### New Capabilities
<!-- none — this updates an existing capability -->

### Modified Capabilities
- `product-search`: the no-results/unhappy behavior now consumes server-supplied suggestions keyed off `noResults` (instead of a client-side disguised alternative search), and personalizes the unhappy copy from `suggestedFromCategory`. The "disguised alternative search" requirement is removed and the "search processing feedback" requirement drops the searching-alternatives state.

## Impact

- **`core:network`** — `ConversationSearchResponseDto` gains `suggestedFromCategory: String? = null`.
- **`core:domain`** — `ProductSearchResult` gains `suggestedFromCategory: String?`; `GeneralizeQueryUseCase` is removed.
- **`core:data`** — `ProductMappers` maps `suggestedFromCategory`; the `noResults = noResults || products.isEmpty()` safety remains but the unhappy branch keeps the returned products.
- **`features:product-search`** — `ProductSearchViewModel` branches on `noResults` and stops issuing the second generalized search; `ProductSearchPhase.SearchingAlternatives` is removed and `ProductSearchPhase.Unhappy` carries `suggestedFromCategory`; `ProductSearchContent`'s unhappy body shows a single apology whose invite line is personalized from the category ("Relacionado a '<X>' te podría interesar…" / generic); the searching-alternatives string is removed and the invite strings are added.
- **API**: no contract change — consumes the existing `noResults` / `results` / `suggestedFromCategory` from `POST /api/v1/conversations/search`.
- No new Gradle module is required; all affected modules already exist.
