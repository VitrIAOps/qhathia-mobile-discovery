## MODIFIED Requirements

### Requirement: Microphone permission requested on Home

The app SHALL request the microphone permission when the consumer first taps a microphone control (Home, Search or Product detail) and the permission is not granted. It SHALL NOT request it on arriving at Home, nor on the onboarding or pairing screens. Voice search SHALL be gated on this permission: no audio is captured without it, and the consumer is told that microphone access is required.

#### Scenario: Fresh install

- **WHEN** the app is opened for the first time and shows the welcome screen
- **THEN** no microphone permission request is shown

#### Scenario: First Home

- **WHEN** Home is shown for the first time after pairing and the permission is not granted
- **THEN** no microphone permission request is shown until the consumer taps the microphone

#### Scenario: First microphone tap

- **WHEN** the consumer taps the microphone and the permission is not granted
- **THEN** the app requests the microphone permission, and on ALLOW starts listening

#### Scenario: Voice search blocked without permission

- **WHEN** the consumer activates voice search and the permission is not granted
- **THEN** the app asks for it, and if it is refused, no audio is captured and the consumer is told that microphone access is required
