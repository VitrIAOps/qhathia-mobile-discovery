## Context

The project is multi-module (`app`, `core:{common,domain,data,network,ui,testing}`, `features:{home,catalog,product-search,product-detail,cart,settings}`). The dependency graph already allows every feature to depend on `core:ui`, `core:domain` and `core:common`, and every feature already does (e.g. `WifiPasswordDialogContent` imports `core.ui.R.drawable.ic_close` as `UiR`). So the seams needed for this refactor exist; they are just under-used.

Two concrete classes of duplication were inventoried in the codebase:

- **Copy duplication** — identical `<string>` values across feature `res/values/strings.xml`:
  - `"Perdí la conexión…"` (network error title) in `catalog`, `home`, `product-detail`, `product-search`.
  - `"Parece que la red está inestable. Verifica tu internet e intenta nuevamente."` (network error message) — same four.
  - `"Algo salió mal"` (generic error title) in `home`, `product-detail`.
  - `"S/ %1$.2f"` (price format) in `cart`, `catalog`, `product-detail`, `product-search`.
  - Action labels / content descriptions: back, new-search, go-to-cart, voice-search mic, search-icon, share, retry — repeated across 2–4 features.
  - Close-confirmation buttons `"Sí, cerrar"` / `"Cancelar"`, search placeholder, voice-processing copy (`"Escuchando"`, `"Organizando ideas"`, slow-response).
- **Component duplication** — the same Compose pattern hand-rolled in multiple features: the bordered close "X" (`CloseIconButton`), the loading-capable filled button (`PrimaryActionButton`), the labeled single-line field (`DiscoveryTextField`), the `"¿Cerrar…?"` `AlertDialog` (`ConfirmationDialog`), the 1dp divider (`DiscoveryDivider`), and the `bodySmall` error text (`InlineErrorText`).

Constraint from the standards: `core:ui` has **no ViewModel, business logic, or domain dependency** — it is an atomic/molecular component library plus theme and tokens. So anything that needs to *interpret a domain type* (e.g. mapping `SttErrorCategory` → copy) must stay in the feature.

## Goals / Non-Goals

**Goals:**
- Make `core:ui` the single source of truth for cross-feature copy and reusable components.
- Remove every verbatim-duplicated string and re-implemented component identified above.
- Keep each extracted component stateless, theme-driven (uses `DiscoveryTheme`/`DiscoveryColors`/`Spacing` tokens), and previewable in light + dark.
- Zero observable change to the rendered UI (same text, same layout, same behavior).

**Non-Goals:**
- No visual redesign, no token/theme changes, no new screens.
- No new Gradle module — `core:ui` already exists and is the correct home.
- Not moving domain-coupled copy resolvers (`SttErrorMessages`, `ProductSearchMessages`) out of their features; only re-pointing the *shared* strings they reference.
- Not de-duplicating strings that merely look similar but are intentionally feature-specific (e.g. `catalog_close_dialog_title` "¿Cerrar el catálogo?" vs `pd_close_dialog_title` "¿Cerrar la búsqueda actual?" stay separate — only the truly identical confirm/dismiss labels are shared).

## Decisions

**Decision 1 — Shared copy lives in `core:ui` string resources, referenced via `core.ui.R`.**
Add `core/ui/src/main/res/values/strings.xml` (the module currently ships only drawables). Features already alias `core.ui.R as UiR`, so a shared string is referenced as `stringResource(UiR.string.discovery_error_network_title)`. Alternative considered: a Kotlin `object` of string constants in `core:common`. Rejected — it bypasses Android resource localization/`stringResource`, and the strings are presentation copy, which belongs with the UI layer, not `common`. Naming: prefix shared keys `discovery_*` (e.g. `discovery_error_network_title`, `discovery_price_format`, `discovery_action_back`) to make their shared origin obvious at the call site and avoid collisions with surviving feature keys.

**Decision 2 — One file per extracted component under `core/ui/component/`, matching the existing convention.**
`core:ui` already organizes components one-per-file (`CartButton.kt`, `DiscoverySearchBar.kt`, `ProductCard.kt`, …). The new components follow suit: `CloseIconButton.kt`, `PrimaryActionButton.kt`, `DiscoveryTextField.kt`, `ConfirmationDialog.kt`, `DiscoveryDivider.kt`, `InlineErrorText.kt`. Each is a pure function of its inputs (state hoisted via parameters + lambdas), uses theme tokens, and ships `@Preview`s for light and dark.

**Decision 3 — Components take generic UI parameters, never feature types.**
`PrimaryActionButton(text, isLoading, enabled, onClick)`, `ConfirmationDialog(title, message, confirmLabel, dismissLabel, onConfirm, onDismiss)`, `DiscoveryTextField(value, onValueChange, label, placeholder, isError, visualTransformation, keyboardType)`. This keeps `core:ui` domain-free (standards rule) and lets every feature pass its own resolved strings/events. The existing `WifiPasswordDialogContent.SubmitButton`/`PasswordField`/close-box become call sites of the shared components.

**Decision 4 — Domain-coupled resolvers stay in features, re-pointed at shared strings.**
`home`'s `SttErrorCategory.toCopy()` and `product-search`'s `ProductSearchPhase.processingTextRes()` continue to live in their feature modules (they see domain types `core:ui` must not). Where the resolved text is now shared (network error title/message, "Escuchando", "Organizando ideas", slow response), the resolver returns `UiR.string.*` instead of the feature-local `R.string.*`. This preserves the documented "keep `core:ui` domain-free" design while still collapsing the underlying copy.

**Decision 5 — Migrate-then-delete, per feature, compiler-guarded.**
For each feature: switch call sites to the shared string/component, then delete the now-unused local `<string>` entries and private composables. The Kotlin/`R` reference is compile-checked, so a missed rename fails the build rather than shipping. Order features independently so the change can be reviewed and landed incrementally.

## Risks / Trade-offs

- **[Over-sharing copy that should stay independent]** → Only migrate strings that are byte-for-byte identical *and* semantically the same concept; leave near-duplicates (catalog vs search close-dialog titles) feature-local. The proposal/spec enumerate exactly which keys move.
- **[A feature still references a deleted local string]** → Caught at compile time (`R.string.x` unresolved). Run `./gradlew :features:<name>:assembleDebug` per migrated feature.
- **[Shared component too rigid for one call site]** → Keep parameters generic and additive (nullable `label`, optional `isLoading`); if one feature needs a genuinely different visual, it keeps its own component rather than overloading the shared one with flags.
- **[Theme regression / dark-mode drift]** → Each extracted component ships light + dark `@Preview`; reviewers compare against the originating screen before deleting the local copy.
- **[Larger-than-expected diff]** → Mechanical but wide (six features). Mitigated by per-feature task grouping so each step is independently buildable and reviewable.

## Migration Plan

1. Land the `core:ui` additions first (strings + components + previews) — purely additive, breaks nothing.
2. Migrate features one at a time (`cart`, `settings`, `product-search`, `product-detail`, `catalog`, `home`): repoint call sites, delete redundant local strings/composables, build the module.
3. Re-point the two domain-coupled resolvers at the shared strings.
4. Full `./gradlew assembleDebug` + existing unit tests as the final gate.

Rollback: revert per-feature commits; `core:ui` additions are inert if unused.

## Open Questions

- None blocking. Key-naming prefix `discovery_*` is assumed; adjust if the team prefers a different convention (cosmetic, resolved during apply).
