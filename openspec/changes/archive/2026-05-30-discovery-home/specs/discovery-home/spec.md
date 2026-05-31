## ADDED Requirements

### Requirement: Home screen entry point

The Discovery app SHALL present a Home screen as the launch destination that gives in-store consumers an inviting, low-friction entry point to the app's functions. The screen SHALL display a search bar, a microphone (voice search) button, a catalog shortcut button, and a settings button.

#### Scenario: Home is the launch screen

- **WHEN** the Discovery app is launched
- **THEN** the Home screen is shown as the start destination
- **AND** the search bar, microphone button, catalog button, and settings button are visible in their idle state

#### Scenario: Entrance animations

- **WHEN** the Home screen first appears
- **THEN** its interactive components animate into view with subtle motion rather than appearing abruptly

### Requirement: Store identity is visible

The Home screen SHALL display the name of the store the device is operating for, read from the store session, so the consumer always knows which store they are interacting with.

#### Scenario: Store name displayed

- **WHEN** the store session exposes a store name
- **THEN** the Home screen displays that store name

#### Scenario: Store name not yet available

- **WHEN** the store session has no store name available
- **THEN** the Home screen displays a neutral placeholder and does not crash or show an empty/blank label

### Requirement: Controls hidden during microphone interaction

The search bar and the catalog and settings buttons SHALL be visible only when the consumer is NOT interacting with the microphone button. While a microphone interaction is active, these controls SHALL be hidden.

#### Scenario: Entering microphone interaction hides controls

- **WHEN** the consumer activates the microphone button from the idle state
- **THEN** the search bar, the catalog button, and the settings button are hidden
- **AND** the microphone listening UI is shown

#### Scenario: Leaving microphone interaction restores controls

- **WHEN** the microphone interaction ends (cancelled, completed, or timed out and dismissed)
- **THEN** the search bar, the catalog button, and the settings button become visible again

### Requirement: Microphone listening window

When the consumer activates the microphone, the app SHALL enter a listening state that lasts at most 30 seconds. If the listening window elapses without captured audio input, the listening state SHALL end. Real speech-to-text transcription is out of scope; the microphone drives UI state only.

#### Scenario: Listening starts on activation

- **WHEN** the consumer activates the microphone button
- **THEN** the app enters the listening state and shows listening feedback

#### Scenario: Listening times out after 30 seconds

- **WHEN** the app has been in the listening state for 30 seconds without captured audio input
- **THEN** the listening window ends and the app stops listening

#### Scenario: Consumer cancels listening

- **WHEN** the consumer dismisses or cancels the microphone interaction before the timeout
- **THEN** the app leaves the listening state and returns to the idle Home state

### Requirement: Retry prompts when no audio is detected

When a listening window ends without detected audio, the assistant SHALL ask the consumer to try again, drawing from a set of multiple distinct retry messages rather than repeating a single fixed message.

#### Scenario: First no-audio timeout shows a retry prompt

- **WHEN** a listening window ends with no detected audio
- **THEN** the app shows a retry prompt inviting the consumer to try speaking again

#### Scenario: Repeated no-audio timeouts vary the prompt

- **WHEN** consecutive listening windows end with no detected audio
- **THEN** the retry prompt shown varies across the available set of retry messages rather than repeating the same message every time

### Requirement: Navigation intents from Home

The Home screen SHALL emit navigation intents toward the catalog, the search results, and the settings destinations when the corresponding controls are used. The destination screens are out of scope for this capability; only the intents are emitted.

#### Scenario: Catalog button emits catalog navigation

- **WHEN** the consumer taps the catalog button
- **THEN** the app emits a navigation intent toward the catalog destination

#### Scenario: Settings button emits settings navigation

- **WHEN** the consumer taps the settings button
- **THEN** the app emits a navigation intent toward the settings destination

#### Scenario: Submitting a search emits results navigation

- **WHEN** the consumer submits a query from the search bar
- **THEN** the app emits a navigation intent toward the search results destination carrying the query

### Requirement: Theming and accessibility compliance

The Home screen SHALL be built from the shared design system, use Material 3 design tokens (no hardcoded colors, type sizes, or dimensions), support light and dark mode, and meet accessibility requirements.

#### Scenario: Dark mode

- **WHEN** the device is in dark mode
- **THEN** the Home screen renders using the design system's dark color scheme

#### Scenario: Interactive controls are accessible

- **WHEN** an assistive technology inspects the microphone, catalog, and settings controls
- **THEN** each interactive control exposes a localized content description and a touch target of at least 48dp
