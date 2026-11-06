# QA report — search-structured-filters

**Legend:** ✔ works · ✖ fails (bug present) · ◐ partial · ⏸ blocked / not run.

| | |
|---|---|
| App (before) | `profile` on `490694e` (QA regression 2026-11-03) |
| App (after) | `profile` with this change (branch `fix/search-structured-filters`) |
| Backend | production `http://87.99.156.164` (phase 41) |
| Store | QA store `6b89ea64-d315-4294-9c69-cd412cd8b54a`, never El Martillo |

## Status

- **Handed to the QA team (2026-11-06):** the cases below. Verified with unit tests (`:core:network` 60, `:core:domain` 139, `:core:data` 193, `:features:product-search` 53), `lintDebug` and a `profile` build.

## Cases for the QA team

| Case | Steps | Expected after | Before (QA 03/11) | After | Who |
|---|---|---|---|---|---|
| D:BUS-003 / D:EXP-004 / X:XBU-032 | Search "taladro" → tap the "Bosch" chip | Logcat `KtorClient`: `{"transcribedText":"taladro",…,"filters":{"brands":["Bosch"]}}`; only Bosch products; the panel still lists every brand | ✖ `"taladro marca bosch"`, mixed brands | ⏸ | QA team |
| Tag chip | Tap a non-brand chip (e.g. "Inalámbrico") | `filters.tags` carries the tag **value** (e.g. `inalambrico`), not the label | ✖ | ⏸ | QA team |
| Two chips, no match | "Bosch" + a tag no Bosch product has | Results view with both chips selected and "No hay productos con esos filtros. Quita alguno para ver más."; never "no entendí" | ✖ "no entendí", chips lost | ⏸ | QA team |
| Remove a chip | After the no-match message, untap the tag | New request with only `brands`, products shown | — | ⏸ | QA team |
| Suggestion + chip | "Bosch" + a suggestion | Text = original + suggestion; `filters.brands` = Bosch | — | ⏸ | QA team |
| Alternatives only | Search a product the store does not sell (e.g. "ventilador" in a hardware store) | Headline "No tenemos «ventilador»; te mostramos alternativas." | — | ⏸ | QA team |
| D:EXP-005 (history) | Refine, then "Volver" | Previous result, its chips and its panel restored | ✔ | ⏸ | QA team |

## Unit-test coverage of the delta spec (`product-search`)

| Spec scenario | Test |
|---|---|
| Brand chip filters by brand | `ProductSearchViewModelTest.filterToggled_sendsOriginalTextAndBrands`, `ConversationSearchApiServiceTest.filters_travel_next_to_the_bare_text`, `StructuredSearchFiltersTest.brand option goes to brands as its label` |
| Tag chip sends its value | `tagChip_sendsValueNotLabel`, `StructuredSearchFiltersTest.tag option goes to tags as its value` |
| Suggestion stays in the text | `suggestion_staysInTextWithFilters`, `ComposeSearchQueryUseCaseTest` |
| Clearing every chip returns to the base query | `clearingAllChips_sendsBaseQueryWithoutFilters`, `ConversationSearchApiServiceTest.no_filters_key_without_chips`, `SearchProductsUseCaseTest.filters_are_passed_through_and_empty_ones_dropped` |
| Panel unchanged after a brand chip | `refinement_keepsBasePanel`, `back_restoresPanelAndChips` |
| Two chips with no product / Removing a chip after no match | `twoChips_noResults_keepsChipsAndPanel`, `given_criterion_cleared_then_drops_only_that_criterions_options` |
| Missing product with alternatives | `alternativesOnly_setsMissingCategory`, `ProductMappersTest.given alternatives only then maps the missing category`, `ConversationSearchApiServiceTest.parses_values_and_alternatives_only` |
| Values default to the options | `ProductMappersTest.given filter without values then values default to the options`, `StructuredSearchFiltersTest.values default to the options` |
| Fresh search without results stays unhappy | `freshSearch_noResults_stillUnhappy` |
