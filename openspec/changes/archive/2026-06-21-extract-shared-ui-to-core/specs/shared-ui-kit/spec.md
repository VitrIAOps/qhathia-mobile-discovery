## ADDED Requirements

### Requirement: Shared cross-feature copy lives in `core:ui`

Copy (user-facing strings and content descriptions) that is identical across two or more feature modules SHALL be declared once in `core:ui` (`core/ui/src/main/res/values/strings.xml`) and referenced by features via `com.qhathia.mobile.discovery.core.ui.R`. A feature module MUST NOT declare a `<string>` whose value duplicates one already provided by `core:ui`.

#### Scenario: Network and generic error copy is shared

- **WHEN** a feature needs the network-error title/message, the generic-error title, or the retry label
- **THEN** it references the corresponding `core.ui.R.string` entry
- **AND** no feature `res/values/strings.xml` contains a duplicate of `"Perdí la conexión…"`, `"Parece que la red está inestable. Verifica tu internet e intenta nuevamente."`, `"Algo salió mal"`, or `"Reintentar"`

#### Scenario: Price format and shared actions are shared

- **WHEN** a feature renders a price or a shared action (back, new-search, go-to-cart, voice-search mic, search icon, share) or its content description
- **THEN** it references the shared `core.ui.R.string` entry (e.g. the `S/ %1$.2f` price format)
- **AND** the previously per-feature copies of those strings are removed

#### Scenario: Feature-specific copy stays in the feature

- **WHEN** a string is only similar but semantically feature-specific (e.g. `"¿Cerrar el catálogo?"` vs `"¿Cerrar la búsqueda actual?"`)
- **THEN** it remains declared in the owning feature module and is NOT moved to `core:ui`

### Requirement: Reusable components live in `core:ui` and are stateless

UI building blocks reused across two or more features SHALL be implemented as stateless, previewable composables under `core/ui/src/main/java/.../core/ui/component/`. Each component MUST hoist its state via parameters and lambdas, use the shared theme tokens (`DiscoveryTheme`, `DiscoveryColors`, `Spacing`), accept only generic UI parameters (never a feature or domain type), and provide `@Preview`s for light and dark themes. A feature MUST NOT re-implement a component the kit already provides.

#### Scenario: Duplicated components are extracted

- **WHEN** the kit is in place
- **THEN** `core:ui` provides `CloseIconButton`, `PrimaryActionButton` (filled button with inline loading spinner), `DiscoveryTextField` (labeled single-line field with placeholder + error state), `ConfirmationDialog`, `DiscoveryDivider`, and `InlineErrorText`
- **AND** each ships a light and a dark `@Preview`

#### Scenario: Features consume the shared components

- **WHEN** a feature needs a close "X", a loading-capable primary button, a labeled text field, a close-confirmation dialog, a 1dp divider, or inline error text
- **THEN** it calls the corresponding `core:ui` component
- **AND** the feature's previously private equivalent (e.g. `WifiPasswordDialogContent.SubmitButton`, the cart/wifi close box, the per-feature confirmation `AlertDialog`) is removed

#### Scenario: The kit stays domain-free

- **WHEN** a component would need to interpret a domain type to choose its copy (e.g. mapping `SttErrorCategory` or `ProductSearchPhase` to text)
- **THEN** that mapping stays in the feature module and passes already-resolved strings into the `core:ui` component
- **AND** `core:ui` retains zero dependency on `core:domain`

### Requirement: The refactor preserves observable behavior

Migrating features onto the shared kit SHALL NOT change what any screen renders. After migration each screen MUST display the same text and the same visuals as before, and existing ViewModel unit tests MUST continue to pass without modification.

#### Scenario: No behavior change after migration

- **WHEN** a feature has been migrated to the shared strings and components
- **THEN** the screen renders identical copy and layout to the pre-refactor version
- **AND** the feature's existing unit tests pass unchanged
