# product-search Specification

## Purpose

The product-search capability lets in-store consumers search the store's catalog from the Discovery app — by voice transcript or typed text — and view the matching products as a chat-style results catalog. It owns query submission to the Qhathia search API, the results and "unhappy" (no-results / error) screens, search processing feedback, temporary in-memory search history with back navigation, and resetting the session via a new search.
## Requirements
### Requirement: Query submission to the product search API

The app SHALL submit a consumer search query to the Qhathia backend via `POST /api/v1/conversations/search`, authenticated with the session Bearer JWT, and present the returned products. The query MAY originate from voice (a transcript) or from typed text. The query SHALL be capped at **500 characters**; input beyond that limit SHALL NOT be accepted. The app SHALL wait at most **30 seconds** for the search response.

#### Scenario: Voice transcript drives a search

- **WHEN** a voice search produces a transcript
- **THEN** the app submits that transcript to `POST /api/v1/conversations/search` with the session Bearer JWT
- **AND** presents the returned products on the results screen

#### Scenario: Typed text drives a search

- **WHEN** the consumer submits a typed query
- **THEN** the app submits that text to `POST /api/v1/conversations/search`
- **AND** presents the returned products on the results screen

#### Scenario: Query length is capped

- **WHEN** the consumer enters a query
- **THEN** the app limits the query to at most 500 characters

#### Scenario: Search response budget exceeded

- **WHEN** a search response is not received within 30 seconds
- **THEN** the search is treated as failed and the search-error path is taken

### Requirement: Results catalog viewed from chat

When a search returns products, the app SHALL present a "catalog viewed from chat" results screen following the results design. The screen SHALL show a results headline derived from the API display text (e.g. *Encontré 8 taladros para ti*), the products laid out in **at most 2 rows with horizontal scroll**, a **cart button** on each product, a top bar with **Volver**, **Ir al catálogo**, **Nueva búsqueda**, and an **Ir al carrito** control that shows the current cart item count, and a bottom search bar with a microphone for starting the next query. When the response carries filters and/or suggestions, the screen SHALL also show the **filters & suggestions section** for refining the search. There SHALL be **no limit** imposed by the app on the number of products displayed.

#### Scenario: Products rendered as a chat catalog

- **WHEN** a search returns one or more products
- **THEN** the app shows the results screen with the results headline and the products in at most 2 rows that scroll horizontally

#### Scenario: Per-product and top-bar controls present

- **WHEN** the results screen is shown
- **THEN** each product exposes a cart button
- **AND** the top bar exposes Volver, Ir al catálogo, Nueva búsqueda, and Ir al carrito controls
- **AND** a search bar with a microphone is available for the next query

#### Scenario: Filters & suggestions section present when returned

- **WHEN** the results screen is shown and the response carried filters and/or suggestions
- **THEN** the app shows the filters & suggestions section alongside the results

#### Scenario: Per-product cart button adds to the cart

- **WHEN** the consumer taps a product's cart button
- **THEN** the product is added to the cart and the Ir al carrito control's item count increases

#### Scenario: Ir al carrito opens the cart dialog

- **WHEN** the consumer taps the Ir al carrito control
- **THEN** the app opens the cart dialog over the results screen without navigating to a separate cart screen

### Requirement: Search filters and suggestions surfaced from the response

The app SHALL parse the `filters` (`[{criterion, options[]}]`) and `suggestions` (`List<String>`) fields from the `POST /api/v1/conversations/search` response and carry them into the search result. On the results (has-products) screen, the app SHALL present a **filters & suggestions section** following the Figma design, listing each filter criterion with its options and the suggestion phrases. When the response carries no filters and no suggestions — including the unhappy / no-results path, where the API returns both empty — the section SHALL be hidden.

#### Scenario: Filters and suggestions rendered with results

- **WHEN** a search returns products together with non-empty `filters` and/or `suggestions`
- **THEN** the results screen shows the filters & suggestions section listing each criterion with its options and the suggestion phrases

#### Scenario: Section hidden when none are returned

- **WHEN** a search response carries empty `filters` and empty `suggestions`
- **THEN** the app does not show the filters & suggestions section

#### Scenario: Unhappy screen shows no section

- **WHEN** the unhappy / no-results screen is shown
- **THEN** the app does not show the filters & suggestions section

### Requirement: Filter and suggestion selection rules

The app SHALL let the consumer select filter options and suggestions on the results screen, and SHALL render a selected option/suggestion in its selected visual state per the Figma design. The app SHALL allow **any number (N) of filter options** to be selected at once, across one or more criteria. The app SHALL allow **at most one suggestion** to be selected at a time; selecting a different suggestion SHALL replace the previously selected suggestion.

#### Scenario: Multiple filters selected at once

- **WHEN** the consumer selects two or more filter options
- **THEN** all selected options are shown in their selected state at the same time

#### Scenario: Selecting a filter is reversible

- **WHEN** the consumer taps an already-selected filter option
- **THEN** that option is de-selected

#### Scenario: Only one suggestion at a time

- **WHEN** a suggestion is selected and the consumer selects a different suggestion
- **THEN** the previously selected suggestion is replaced by the newly selected one

#### Scenario: Selected item shows its selected state

- **WHEN** a filter option or suggestion is selected
- **THEN** it is rendered in the selected visual state per the design

### Requirement: Filter or suggestion selection re-queries with concatenated terms

When the consumer selects or de-selects a filter option or a suggestion, the app SHALL issue a new `POST /api/v1/conversations/search` request whose query is built by concatenating the currently selected terms **naturally onto the original raw query** that started the current results view (e.g. raw query *busco un taladro* with suggestion *para uso doméstico* → *busco un taladro para uso doméstico*). The app SHALL retain that original raw query as the immutable concatenation base, so that de-selecting all terms reproduces the base query. The re-query SHALL refresh both the products and the filters/suggestions lists from the new response, and SHALL reuse the existing search processing and unhappy/network-error handling.

#### Scenario: Selecting a suggestion re-queries with it appended

- **WHEN** the consumer selects a suggestion
- **THEN** the app submits a new search whose query is the original raw query with the suggestion concatenated naturally
- **AND** presents the products and filters/suggestions from the new response

#### Scenario: Selecting filters re-queries with them appended

- **WHEN** the consumer selects one or more filter options
- **THEN** the app submits a new search whose query is the original raw query with the selected filter terms concatenated naturally

#### Scenario: De-selecting all terms returns to the base query

- **WHEN** the consumer de-selects every previously selected filter and suggestion
- **THEN** the next re-query uses the original raw query unchanged

#### Scenario: An empty re-query result follows the unhappy path

- **WHEN** a filter/suggestion re-query returns `noResults=true`
- **THEN** the app follows the established unhappy-screen handling

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

### Requirement: Navigation to product detail from a result

A product card on the results screen and on the unhappy screen's alternatives SHALL be tappable to open that product's detail. When the consumer taps a product card, the app SHALL navigate to the product-detail screen for that product's id. This card tap is distinct from the per-card cart button, which adds the product to the cart rather than opening the detail.

#### Scenario: Tapping a result opens its detail

- **WHEN** the consumer taps a product card on the results screen
- **THEN** the app navigates to the product-detail screen for that product's id

#### Scenario: Tapping an alternative opens its detail

- **WHEN** the consumer taps one of the alternative product cards on the unhappy screen
- **THEN** the app navigates to the product-detail screen for that product's id

#### Scenario: Cart button adds without opening the detail

- **WHEN** the consumer taps a product card's cart button
- **THEN** the product is added to the cart and the app does not open the product detail

### Requirement: Microphone animation retained for voice searches

The microphone animation SHALL be kept on the results screen and during the search processing of a **voice-initiated** search. For a **text-initiated** search, the microphone animation SHALL NOT be shown while the search is in progress.

#### Scenario: Voice search keeps the mic animation

- **WHEN** a voice-initiated search is processing or its results are shown
- **THEN** the microphone animation is visible

#### Scenario: Text search hides the mic animation while searching

- **WHEN** a text-initiated search is in progress
- **THEN** the microphone animation is not shown during the search

#### Scenario: Bottom microphone captures voice in place

- **WHEN** the consumer taps the bottom microphone on the results or unhappy screen
- **THEN** the app captures and transcribes the consumer's voice on the same screen (without navigating away)
- **AND** a successful transcript starts a new in-place search
- **AND** a speech-to-text failure or no detected audio restores the previous screen without running an alternative search

### Requirement: Search processing feedback

While a search is in progress, the app SHALL show the design-system processing message that matches the current phase, displaying the animated three-dot ("…") indicator: a *searching* message while querying the catalog, and a *slow-search* message when a search response has not arrived after **20 seconds**. For a voice search these phases continue the conversation after the already-implemented speech-to-text phases.

#### Scenario: Searching message while querying

- **WHEN** the app is querying the search API for products
- **THEN** it shows the searching processing message with the animated three dots

#### Scenario: Slow-search message after 20 seconds

- **WHEN** a search response has not been received after 20 seconds
- **THEN** the app shows the slow-search processing message with the animated three dots

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

### Requirement: Temporary search history with back navigation

The app SHALL store the history of search results temporarily (in memory) so the consumer can go back to a previous result. The **Volver** control SHALL return to the immediately previous search result when one exists. When no previous result exists, **Volver** SHALL open the new-search confirmation dialog.

#### Scenario: Back returns to the previous result

- **WHEN** the consumer has performed more than one search and taps Volver
- **THEN** the app shows the immediately previous search result

#### Scenario: Back with no previous result asks to close

- **WHEN** the consumer taps Volver and there is no previous search result
- **THEN** the app opens the confirmation dialog to close the current search

### Requirement: New search resets the session

The **Nueva búsqueda** control SHALL open a confirmation dialog to close the current search. When the cart contains products, the dialog message SHALL warn that the cart will be emptied and SHALL state how many products it contains; when the cart is empty, the dialog SHALL keep the plain close message. On confirmation, the app SHALL end the customer session (clearing the cart and the consumer data), clear the temporary search history and return to the Home idle screen. The same dialog, opened by **Volver** or the system Back when there is no previous result, SHALL behave the same way.

#### Scenario: New search asks for confirmation

- **WHEN** the consumer taps Nueva búsqueda
- **THEN** the app opens a confirmation dialog to close the current search and return to Home

#### Scenario: Dialog warns that the cart will be emptied

- **WHEN** the cart contains 2 products and the consumer opens the close dialog
- **THEN** the dialog message says the cart (2 productos) will be emptied and the consumer will return to the start

#### Scenario: Dialog with an empty cart keeps the plain message

- **WHEN** the cart is empty and the consumer opens the close dialog
- **THEN** the dialog message says the search will be closed and the consumer will return to the start, with no cart warning

#### Scenario: Confirming new search ends the session

- **WHEN** the consumer confirms the close dialog
- **THEN** the cart and the consumer name and cell-phone are cleared
- **AND** the app clears the temporary search history
- **AND** returns to the Home idle screen

#### Scenario: Cancelling new search keeps the current results

- **WHEN** the consumer dismisses the new-search dialog
- **THEN** the app keeps the current search results, history and cart unchanged

### Requirement: Theming and accessibility compliance

The product-search screens SHALL be built from the shared design system, use Material 3 design tokens (no hardcoded colors, type sizes, or dimensions), support light and dark mode, and meet accessibility requirements.

#### Scenario: Dark mode

- **WHEN** the device is in dark mode
- **THEN** the product-search screens render using the design system's dark color scheme

#### Scenario: Interactive controls are accessible

- **WHEN** an assistive technology inspects the top-bar controls, the product cart buttons, and the microphone
- **THEN** each interactive control exposes a localized content description and a touch target of at least 48dp

### Requirement: Search microphone asks for permission

Before starting voice capture on the search screen, the app SHALL check the microphone permission and, when it is missing, request it. When the permission is refused, the app SHALL show the same message Home shows ("Necesitamos acceso al micrófono para la búsqueda por voz") and SHALL NOT start capture.

#### Scenario: Permission missing

- **WHEN** the consumer taps the search microphone and the microphone permission is not granted
- **THEN** the system permission request is shown

#### Scenario: Permission refused

- **WHEN** the consumer refuses the microphone permission from the search screen
- **THEN** the message "Necesitamos acceso al micrófono para la búsqueda por voz" is shown and no capture starts

#### Scenario: Permission granted

- **WHEN** the permission is granted
- **THEN** the search screen starts listening

### Requirement: Leaving search stops voice capture

When the search screen stops being visible, any voice capture in progress SHALL be cancelled and the screen SHALL show the results it showed before the microphone was used.

#### Scenario: Background while listening on search

- **WHEN** the search screen is listening and the app goes to the background
- **THEN** the capture is cancelled and the previous results are shown when the app comes back

### Requirement: Voice hand-off without a query starts listening

When the product-search screen is opened with source Voice and an empty query (for example from the Catalog microphone), it SHALL start in-place voice capture immediately and SHALL NOT send a search request with an empty query.

#### Scenario: Catalog microphone opens voice capture

- **WHEN** the consumer taps the microphone on the Catalog and the search screen opens with an empty query
- **THEN** the search screen shows the listening state and captures the consumer's voice
- **AND** no search request is sent until a transcript is available

#### Scenario: Voice hand-off with a query still searches

- **WHEN** the search screen is opened with source Voice and a non-empty query
- **THEN** it submits that query to the search API as before

### Requirement: History restores the selected filters

When the consumer goes back to a previous result in the search history, the filter chips and suggestion that were selected for that result SHALL be shown as selected again.

#### Scenario: Back to a filtered result

- **WHEN** the consumer selected the "Bosch" chip, then ran a new search, then goes back
- **THEN** the previous result is shown with the "Bosch" chip selected

### Requirement: Ir al catálogo opens the catalog

The product-search top bar SHALL show an **Ir al catálogo** control in every search phase (results, no results, error). Tapping it SHALL open the Catalog screen directly, without a confirmation dialog, keeping the cart and the consumer data. After it, the screen stack SHALL be Home followed by the Catalog, so "Volver" from the Catalog follows the Catalog rules.

#### Scenario: Ir al catálogo from results keeps the cart

- **WHEN** the cart contains 2 products and the consumer taps Ir al catálogo on the results screen
- **THEN** the app shows the Catalog with no dialog
- **AND** the cart control still shows 2

#### Scenario: Ir al catálogo from the no-results screen

- **WHEN** a search returned no products and the consumer taps Ir al catálogo
- **THEN** the app shows the Catalog

#### Scenario: Catalog opened from search sits on top of Home

- **WHEN** the consumer went Home → Catalog → Search and taps Ir al catálogo
- **THEN** the app shows a single Catalog screen above Home
- **AND** "Volver" on that Catalog with an empty cart returns to Home

#### Scenario: Double tap opens one catalog

- **WHEN** the consumer taps Ir al catálogo twice quickly
- **THEN** only one Catalog screen is opened

