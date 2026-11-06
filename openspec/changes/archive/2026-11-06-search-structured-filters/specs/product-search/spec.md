## ADDED Requirements

### Requirement: Filter chips re-query with structured filters

When the consumer selects or de-selects a filter option, the app SHALL issue a new `POST /api/v1/conversations/search` request whose `transcribedText` is the original raw query that started the current results view, with only the selected suggestion (if any) concatenated naturally onto it, and whose `filters` carry the selected options:

- options of the "Marca" criterion SHALL be sent in `filters.brands`, as their labels;
- options of any other criterion SHALL be sent in `filters.tags`, as the criterion's `values` entry matching each option, never as the label.

When no option is selected, the request SHALL carry no `filters`. The app SHALL keep the original raw query as the immutable base, so that de-selecting every chip and the suggestion reproduces the base query.

#### Scenario: Brand chip filters by brand

- **WHEN** the consumer searched "taladro" and taps the "Bosch" chip of the "Marca" criterion
- **THEN** the request carries `transcribedText` "taladro" and `filters.brands` ["Bosch"], with no chip text in `transcribedText`

#### Scenario: Tag chip sends its value

- **WHEN** the consumer taps the "Inalámbrico" option of the "Tipo" criterion, whose matching value is "inalambrico"
- **THEN** the request carries `filters.tags` ["inalambrico"]

#### Scenario: Suggestion stays in the text

- **WHEN** the consumer has the "Bosch" chip selected and selects the suggestion "para uso doméstico"
- **THEN** the request carries `transcribedText` "taladro para uso doméstico" and `filters.brands` ["Bosch"]

#### Scenario: Clearing every chip returns to the base query

- **WHEN** the consumer de-selects every chip and the suggestion
- **THEN** the next request carries the original raw query unchanged and no `filters`

### Requirement: Stable filter panel while refining

The filter criteria, options and suggestions shown SHALL be the ones returned by the search that started the current results view. A refinement SHALL update the products and the selection, but SHALL NOT replace the panel with the refined response's own `filters` or `suggestions`.

#### Scenario: Panel unchanged after a brand chip

- **WHEN** the search returned the brands Bosch, DeWalt and Makita and the consumer taps "Bosch"
- **THEN** the panel still shows Bosch, DeWalt and Makita, with Bosch selected

### Requirement: Refinement without matches keeps the chips

When a refinement with at least one selected chip returns `noResults=true`, the app SHALL stay on the results view with the chips still selected, show no products, and show "No hay productos con esos filtros. Quita alguno para ver más.". It SHALL NOT show the unhappy screen, and SHALL NOT present the response's unfiltered results as matches.

#### Scenario: Two chips with no product

- **WHEN** the consumer selects "Bosch" and "Inalámbrico" and no product has both
- **THEN** the results view shows the no-match message with both chips selected, and the unhappy screen is not shown

#### Scenario: Removing a chip after no match

- **WHEN** after the no-match message the consumer de-selects "Inalámbrico"
- **THEN** the app re-queries with only `filters.brands` ["Bosch"] and shows its products

### Requirement: Alternatives-only notice

When a search response has `alternativesOnly=true` and names the `missingCategory`, the results headline SHALL say that the store does not have that product and that alternatives are shown ("No tenemos «X»; te mostramos alternativas.").

#### Scenario: Missing product with alternatives

- **WHEN** the consumer searches "ventilador" in a store without fans and the response has `alternativesOnly=true` and `missingCategory` "ventilador"
- **THEN** the results headline says "No tenemos «ventilador»; te mostramos alternativas." and the alternatives are listed

## MODIFIED Requirements

### Requirement: Search filters and suggestions surfaced from the response

The app SHALL parse the `filters` (`[{criterion, options[], values[]}]`, where `values[i]` is the value to send for `options[i]` and defaults to the option when absent) and `suggestions` (`List<String>`) fields from the `POST /api/v1/conversations/search` response and carry them into the search result. On the results (has-products) screen, the app SHALL present a **filters & suggestions section** following the Figma design, listing each filter criterion with its options and the suggestion phrases. When the response carries no filters and no suggestions — including the unhappy / no-results path, where the API returns both empty — the section SHALL be hidden.

#### Scenario: Filters and suggestions rendered with results

- **WHEN** a search returns products together with non-empty `filters` and/or `suggestions`
- **THEN** the results screen shows the filters & suggestions section listing each criterion with its options and the suggestion phrases

#### Scenario: Values default to the options

- **WHEN** a criterion arrives without `values`
- **THEN** each option's value is the option label itself

#### Scenario: Section hidden when none are returned

- **WHEN** a search response carries empty `filters` and empty `suggestions`
- **THEN** the app does not show the filters & suggestions section

#### Scenario: Unhappy screen shows no section

- **WHEN** the unhappy / no-results screen is shown
- **THEN** the app does not show the filters & suggestions section

## REMOVED Requirements

### Requirement: Filter or suggestion selection re-queries with concatenated terms

**Reason**: Concatenating filter chips into the free text did not filter (the LLM reinterpreted the sentence) and two chips fell into "no entendí" (F-37). Filters are now structured.

**Migration**: Filter chips follow "Filter chips re-query with structured filters"; a suggestion is still appended to the original raw query as before (same requirement). An empty refinement with chips follows "Refinement without matches keeps the chips".
