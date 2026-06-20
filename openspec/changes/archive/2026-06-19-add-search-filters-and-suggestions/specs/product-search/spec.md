## ADDED Requirements

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

When the consumer selects or de-selects a filter option or a suggestion, the app SHALL issue a new `POST /api/v1/conversations/search` request whose query is built by concatenating the currently selected terms **naturally onto the original raw query** that started the current results view (e.g. raw query *busco un taladro* with suggestion *para uso doméstico* → *busco un taladro para uso doméstico*). The app SHALL retain that original raw query as the immutable concatenation base, so that de-selecting all terms reproduces the base query. The re-query SHALL refresh both the products and the filters/suggestions lists from the new response, and SHALL reuse the existing search processing, alternative-search, and unhappy/network-error handling.

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

- **WHEN** a filter/suggestion re-query returns no products
- **THEN** the app follows the established alternative-search and unhappy-screen handling

## MODIFIED Requirements

### Requirement: Results catalog viewed from chat

When a search returns products, the app SHALL present a "catalog viewed from chat" results screen following the results design. The screen SHALL show a results headline derived from the API display text (e.g. *Encontré 8 taladros para ti*), the products laid out in **at most 2 rows with horizontal scroll**, a **cart button** on each product, a top bar with **Volver**, **Nueva búsqueda**, and an **Ir al carrito** control that shows the current cart item count, and a bottom search bar with a microphone for starting the next query. When the response carries filters and/or suggestions, the screen SHALL also show the **filters & suggestions section** for refining the search. There SHALL be **no limit** imposed by the app on the number of products displayed.

#### Scenario: Products rendered as a chat catalog

- **WHEN** a search returns one or more products
- **THEN** the app shows the results screen with the results headline and the products in at most 2 rows that scroll horizontally

#### Scenario: Per-product and top-bar controls present

- **WHEN** the results screen is shown
- **THEN** each product exposes a cart button
- **AND** the top bar exposes Volver, Nueva búsqueda, and Ir al carrito controls
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
