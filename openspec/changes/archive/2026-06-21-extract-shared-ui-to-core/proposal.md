## Why

The feature modules (`home`, `catalog`, `product-search`, `product-detail`, `cart`, `settings`) have grown organically and now carry **verbatim-duplicated copy and re-implemented UI building blocks**. The same network-error title appears in four `strings.xml` files; the same price format `S/ %1$.2f` is declared four times; the close "X" button, the loading-capable primary button, the labeled text field, the "¿Cerrar…?" confirmation dialog and the 1dp divider are each hand-rolled in two-to-three features. This duplication makes copy edits error-prone (a wording change must be applied N times), inflates each feature, and lets the same component drift visually between screens. `core:ui` already exists as the shared component library but is under-used. Consolidating now — before more features land — keeps the design system as the single source of truth.

## What Changes

- Introduce a **shared copy catalog** in `core:ui` (`core/ui/src/main/res/values/strings.xml`) holding the strings that are currently duplicated verbatim across two or more feature modules, plus their content descriptions:
  - Network-error title/message, generic-error title, retry label
  - Price format `S/ %1$.2f`
  - Shared action labels & content descriptions: back, new-search, go-to-cart, voice-search mic, search icon, share
  - Close-confirmation copy (`Sí, cerrar` / `Cancelar`) and shared search placeholder / prompt
  - Voice processing copy reused by `home` and `product-search` (`Escuchando`, `Organizando ideas`, slow-response)
- Extract the **duplicated Compose components** into `core:ui/component`, each stateless and previewable:
  - `CloseIconButton` — the bordered top-right "X" (today in `cart` ×2, `settings` wifi dialog)
  - `PrimaryActionButton` — filled button with an inline loading spinner (today in `settings` wifi submit, plus cart/detail action buttons)
  - `DiscoveryTextField` — labeled single-line field with placeholder + error border (today in `cart` and `settings`)
  - `ConfirmationDialog` — `AlertDialog` close-confirmation wrapper (today in `catalog`, `product-detail`, `product-search`)
  - `DiscoveryDivider` — the 1dp full-width divider Box (today in `cart`, `product-detail`, `settings` ×2)
  - `InlineErrorText` — `bodySmall` error-colored text (today in `cart`, `settings`)
- Migrate the feature modules to consume the shared strings and components, then **delete the now-redundant per-feature strings and private composables**.
- Feature-owned, domain-coupled copy resolvers (`home`'s `SttErrorMessages`, `product-search`'s `ProductSearchMessages`) **stay in their feature modules** (so `core:ui` remains domain-free) but are re-pointed at the shared `core.ui.R.string` entries where the text is shared.
- **Non-goal / no behavior change:** every screen renders the same text and the same visuals after the refactor. No feature requirement changes; this is a structural de-duplication.

## Capabilities

### New Capabilities
- `shared-ui-kit`: Defines the requirements for the shared `core:ui` layer — the canonical home for cross-feature copy and reusable Compose components, and the rule that features must not re-declare a string or re-implement a component that the kit already provides.

### Modified Capabilities
<!-- None. Feature-level behavior (rendered text and visuals) is unchanged; this is a structural de-duplication with no requirement deltas. -->

## Impact

- **New code:** `core/ui/src/main/res/values/strings.xml`; new composables under `core/ui/src/main/java/.../core/ui/component/`.
- **Modified code:** the six feature modules' `*.kt` UI files and their `res/values/strings.xml`; `home`'s `SttErrorMessages.kt` and `product-search`'s `ProductSearchMessages.kt` re-pointed at shared strings.
- **Dependencies:** no new Gradle modules and no new libraries — every feature already depends on `core:ui`. `core:ui` gains a `values/strings.xml` (it currently ships only drawables).
- **Tests:** existing ViewModel tests are unaffected (no logic change); the new `core:ui` components ship with `@Preview`s (light + dark) per the Definition of Done.
- **Risk:** low — mechanical migration; the main risk is a missed/renamed string reference, caught at compile time.
