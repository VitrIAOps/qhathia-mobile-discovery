## ADDED Requirements

### Requirement: Capture stops when its screen is hidden

A voice capture session SHALL stop recording and release the microphone when the screen that started it stops being visible. No audio SHALL be recorded while the app is in the background.

#### Scenario: Background while recording

- **WHEN** a capture session is recording and its screen stops being visible
- **THEN** the recording stops and the microphone is released within the same lifecycle event
