# product-search Specification

## Purpose

The product-search capability lets in-store consumers search the store's catalog from the Discovery app — by voice transcript or typed text — and view the matching products as a chat-style results catalog. It owns query submission to the Qhathia search API, the results and "unhappy" (no-results / error) screens, disguised alternative searches, search processing feedback, temporary in-memory search history with back navigation, and resetting the session via a new search.

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

When a search returns products, the app SHALL present a "catalog viewed from chat" results screen following the results design. The screen SHALL show a results headline derived from the API display text (e.g. *Encontré 8 taladros para ti*), the products laid out in **at most 2 rows with horizontal scroll**, a **cart button** on each product, a top bar with **Volver**, **Nueva búsqueda**, and **Ir al carrito** controls, and a bottom search bar with a microphone for starting the next query. There SHALL be **no limit** imposed by the app on the number of products displayed.

#### Scenario: Products rendered as a chat catalog

- **WHEN** a search returns one or more products
- **THEN** the app shows the results screen with the results headline and the products in at most 2 rows that scroll horizontally

#### Scenario: Per-product and top-bar controls present

- **WHEN** the results screen is shown
- **THEN** each product exposes a cart button
- **AND** the top bar exposes Volver, Nueva búsqueda, and Ir al carrito controls
- **AND** a search bar with a microphone is available for the next query

#### Scenario: Cart controls do not navigate

- **WHEN** the consumer taps a product cart button or the Ir al carrito control
- **THEN** the app does not navigate to a cart screen (cart navigation is out of scope)

### Requirement: Navigation to product detail from a result

A product card on the results screen and on the unhappy screen's alternatives SHALL be tappable to open that product's detail. When the consumer taps a product card, the app SHALL navigate to the product-detail screen for that product's id. This card tap is distinct from the per-card cart button, which remains non-navigating.

#### Scenario: Tapping a result opens its detail

- **WHEN** the consumer taps a product card on the results screen
- **THEN** the app navigates to the product-detail screen for that product's id

#### Scenario: Tapping an alternative opens its detail

- **WHEN** the consumer taps one of the alternative product cards on the unhappy screen
- **THEN** the app navigates to the product-detail screen for that product's id

#### Scenario: Cart button still does not navigate

- **WHEN** the consumer taps a product card's cart button
- **THEN** the app does not navigate to a cart screen and does not open the product detail

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

While a search is in progress, the app SHALL show the design-system processing message that matches the current phase, displaying the animated three-dot ("…") indicator: a *searching* message while querying the catalog, a *slow-search* message when a search response has not arrived after **20 seconds**, and a *searching-alternatives* message while an alternative search is running. For a voice search these phases continue the conversation after the already-implemented speech-to-text phases.

#### Scenario: Searching message while querying

- **WHEN** the app is querying the search API for products
- **THEN** it shows the searching processing message with the animated three dots

#### Scenario: Slow-search message after 20 seconds

- **WHEN** a search response has not been received after 20 seconds
- **THEN** the app shows the slow-search processing message with the animated three dots

#### Scenario: Searching-alternatives message

- **WHEN** the app is running an alternative search after a no-results or search-error outcome
- **THEN** it shows the searching-alternatives processing message with the animated three dots

### Requirement: No-results and search-error unhappy screen

When a search returns no results or fails with a search (API) error, the app SHALL present the "unhappy" screen following its design: an apology message (*Lo siento, no pude encontrar lo que estás buscando. De pronto te interesaría…*) together with **3 alternative products**, plus the same top bar and bottom search bar as the results screen. If the unhappy screen remains without consumer activity for **30 seconds**, the app SHALL return to the Home idle screen.

#### Scenario: No results shows alternatives

- **WHEN** a search returns no products
- **THEN** the app shows the unhappy screen with the apology message and up to 3 alternative products

#### Scenario: Search error shows alternatives

- **WHEN** the search request fails with a search (API) error
- **THEN** the app shows the unhappy screen with the apology message and up to 3 alternative products

#### Scenario: Unhappy screen auto-returns to idle

- **WHEN** the unhappy screen has been shown with no consumer activity for 30 seconds
- **THEN** the app returns to the Home idle screen

#### Scenario: Network error keeps the established message

- **WHEN** the search fails due to lost or unstable network connectivity
- **THEN** the app keeps the screen and shows the established network-error title and message (*Perdí la conexión… / Parece que la red está inestable…*)

### Requirement: Disguised alternative search

The 3 alternatives on the unhappy screen SHALL be obtained from an **alternative search** derived from the original query's keyword by generalizing it (e.g. *Taladro 20v* → *Taladros*). This alternative search SHALL reuse the same search API and take only the **first 3 products** of its result. The alternative search SHALL be run disguised inside the search process (surfaced as the *searching-alternatives* state), not as a visibly separate step. The alternative search SHALL apply only to no-results and search (API) errors, and SHALL NOT apply to speech-to-text errors.

#### Scenario: Alternatives derived from the keyword

- **WHEN** an alternative search is triggered for the query *Taladro 20v*
- **THEN** the app issues a generalized query (e.g. *Taladros*) to the same search API
- **AND** displays the first 3 products of that result as alternatives

#### Scenario: Alternative search is disguised

- **WHEN** the alternative search is running
- **THEN** it is presented as part of the ongoing search process (the searching-alternatives state) rather than as a separate user-visible search

#### Scenario: No alternative search for STT errors

- **WHEN** a voice search fails with a speech-to-text error
- **THEN** the app does not run an alternative search

### Requirement: Temporary search history with back navigation

The app SHALL store the history of search results temporarily (in memory) so the consumer can go back to a previous result. The **Volver** control SHALL return to the immediately previous search result when one exists. When no previous result exists, **Volver** SHALL open the new-search confirmation dialog.

#### Scenario: Back returns to the previous result

- **WHEN** the consumer has performed more than one search and taps Volver
- **THEN** the app shows the immediately previous search result

#### Scenario: Back with no previous result asks to close

- **WHEN** the consumer taps Volver and there is no previous search result
- **THEN** the app opens the confirmation dialog to close the current search

### Requirement: New search resets the session

The **Nueva búsqueda** control SHALL open a confirmation dialog to close the current search. On confirmation, the app SHALL clear the temporary search history and return to the Home idle screen.

#### Scenario: New search asks for confirmation

- **WHEN** the consumer taps Nueva búsqueda
- **THEN** the app opens a confirmation dialog to close the current search and return to Home

#### Scenario: Confirming new search clears history

- **WHEN** the consumer confirms the new-search dialog
- **THEN** the app clears the temporary search history
- **AND** returns to the Home idle screen

#### Scenario: Cancelling new search keeps the current results

- **WHEN** the consumer dismisses the new-search dialog
- **THEN** the app keeps the current search results and history unchanged

### Requirement: Theming and accessibility compliance

The product-search screens SHALL be built from the shared design system, use Material 3 design tokens (no hardcoded colors, type sizes, or dimensions), support light and dark mode, and meet accessibility requirements.

#### Scenario: Dark mode

- **WHEN** the device is in dark mode
- **THEN** the product-search screens render using the design system's dark color scheme

#### Scenario: Interactive controls are accessible

- **WHEN** an assistive technology inspects the top-bar controls, the product cart buttons, and the microphone
- **THEN** each interactive control exposes a localized content description and a touch target of at least 48dp
