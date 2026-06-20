## ADDED Requirements

### Requirement: Search outcome decided by the response's noResults flag

The app SHALL decide the search outcome from the response's `noResults` flag of `POST /api/v1/conversations/search`, in a **single request** per search. When `noResults` is `false`, the app SHALL present the results screen with the returned products. When `noResults` is `true`, the app SHALL present the unhappy screen using the products the **same response** returned in `results` (up to 3). The app SHALL NOT issue a second search to obtain unhappy-path suggestions.

#### Scenario: Results when noResults is false

- **WHEN** a search response has `noResults=false`
- **THEN** the app presents the results screen with the returned products

#### Scenario: Unhappy from the same response when noResults is true

- **WHEN** a search response has `noResults=true` with up to 3 products in `results`
- **THEN** the app presents the unhappy screen showing those products
- **AND** does not issue a second search request

#### Scenario: Empty suggestions on the unhappy path

- **WHEN** a search response has `noResults=true` with an empty `results` list
- **THEN** the app presents the unhappy screen with its apology copy and no suggested products

## MODIFIED Requirements

### Requirement: No-results and search-error unhappy screen

When a search response carries `noResults=true`, or the search fails with a search (API) error, the app SHALL present the "unhappy" screen following its design, with the same top bar and bottom search bar as the results screen. The screen SHALL show an apology (*Lo siento, no pude encontrar lo que estás buscando.*) followed by an **invite line** personalized from `suggestedFromCategory`: when it is a non-null category the invite line SHALL read "Relacionado a '&lt;categoría&gt;' te podría interesar…" using the value verbatim, otherwise the generic "De pronto te interesaría…". The screen SHALL show up to **3 suggested products**: on a `noResults=true` response the suggested products SHALL be the ones the response returned in `results`; on a search (API) error there SHALL be no suggested products. If the unhappy screen remains without consumer activity for **30 seconds**, the app SHALL return to the Home idle screen.

#### Scenario: No-match query shows the response's suggestions

- **WHEN** a search response has `noResults=true` with up to 3 products
- **THEN** the app shows the unhappy screen with the apology message and those products as suggestions

#### Scenario: Personalized invite line from suggestedFromCategory

- **WHEN** an unhappy response has a non-null `suggestedFromCategory`
- **THEN** the apology's invite line reads "Relacionado a '&lt;categoría&gt;' te podría interesar…"
- **AND** when `suggestedFromCategory` is null the invite line reads the generic "De pronto te interesaría…"

#### Scenario: Search error shows no suggestions

- **WHEN** the search request fails with a search (API) error
- **THEN** the app shows the unhappy screen with the apology message and no suggested products

#### Scenario: Unhappy screen auto-returns to idle

- **WHEN** the unhappy screen has been shown with no consumer activity for 30 seconds
- **THEN** the app returns to the Home idle screen

#### Scenario: Network error keeps the established message

- **WHEN** the search fails due to lost or unstable network connectivity
- **THEN** the app keeps the screen and shows the established network-error title and message (*Perdí la conexión… / Parece que la red está inestable…*)

### Requirement: Search processing feedback

While a search is in progress, the app SHALL show the design-system processing message that matches the current phase, displaying the animated three-dot ("…") indicator: a *searching* message while querying the catalog, and a *slow-search* message when a search response has not arrived after **20 seconds**. For a voice search these phases continue the conversation after the already-implemented speech-to-text phases.

#### Scenario: Searching message while querying

- **WHEN** the app is querying the search API for products
- **THEN** it shows the searching processing message with the animated three dots

#### Scenario: Slow-search message after 20 seconds

- **WHEN** a search response has not been received after 20 seconds
- **THEN** the app shows the slow-search processing message with the animated three dots

## REMOVED Requirements

### Requirement: Disguised alternative search

**Reason**: The backend now returns up to 3 store suggestions directly in the `POST /api/v1/conversations/search` response when `noResults=true`, so the client no longer fabricates alternatives by generalizing the query and issuing a second request.

**Migration**: The unhappy screen renders the products from the same response's `results` (see "Search outcome decided by the response's noResults flag" and the updated "No-results and search-error unhappy screen"). The generalized second search, its `searching-alternatives` processing state, and the keyword-generalization use case are removed.
