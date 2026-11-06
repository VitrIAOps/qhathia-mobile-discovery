## MODIFIED Requirements

### Requirement: Welcome screen

The onboarding flow SHALL start with a Welcome screen that displays a bienvenida title, the Qhathia application name, a short supporting message, and a "Continuar" button. Activating "Continuar" SHALL navigate to the initial configuration screen.

When the store session has a pending unlink notice, the Welcome screen SHALL also show, above the title, the notice "Esta tablet se desvinculó de la tienda. Para volver a usarla, vincúlala de nuevo desde la app del comercio.", announced to accessibility services. Activating "Continuar" SHALL dismiss the notice before navigating.

The screen SHALL preserve the existing Material 3 design system and provide a `@Preview` in light and dark themes, with and without the notice.

#### Scenario: Welcome content is shown

- **WHEN** the Welcome screen is displayed
- **THEN** it shows the bienvenida title, the Qhathia app name, the supporting message, and a "Continuar" button

#### Scenario: Continue navigates to initial configuration

- **WHEN** the merchant taps "Continuar"
- **THEN** the app navigates to the initial configuration screen

#### Scenario: Unlink notice is shown after a revocation

- **WHEN** the Welcome screen is displayed and the unlink notice is pending
- **THEN** it shows the unlink notice above the title, together with the rest of the Welcome content

#### Scenario: No notice on a normal first run

- **WHEN** the Welcome screen is displayed and no unlink notice is pending
- **THEN** no unlink notice is shown

#### Scenario: Continue dismisses the notice

- **WHEN** the unlink notice is shown and the merchant taps "Continuar"
- **THEN** the notice is dismissed and the app navigates to the initial configuration screen; returning to Welcome later shows no notice
