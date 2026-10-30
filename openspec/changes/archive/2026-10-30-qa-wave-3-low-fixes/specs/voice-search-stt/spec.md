## REMOVED Requirements

### Requirement: Microphone permission requested at app open
**Reason**: On a fresh install the request appeared over the welcome screen, before the tablet was set up (EXP-004).
**Migration**: Replaced by "Microphone permission requested on Home". Voice capture stays gated on the permission, and every microphone control still asks for it when it is missing.

## ADDED Requirements

### Requirement: Microphone permission requested on Home

The app SHALL request the microphone permission the first time Home is shown in a process when the permission is not granted, and SHALL NOT request it on the onboarding or pairing screens. Voice search SHALL be gated on this permission: no audio is captured without it, and the consumer is told that microphone access is required.

#### Scenario: Fresh install

- **WHEN** the app is opened for the first time and shows the welcome screen
- **THEN** no microphone permission request is shown

#### Scenario: First Home

- **WHEN** Home is shown for the first time and the permission is not granted
- **THEN** the app requests the microphone permission

#### Scenario: Voice search blocked without permission

- **WHEN** the consumer activates voice search and the permission is not granted
- **THEN** the app asks for it, and if it is refused, no audio is captured and the consumer is told that microphone access is required
