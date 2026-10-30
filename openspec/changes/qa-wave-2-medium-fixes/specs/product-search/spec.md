## ADDED Requirements

### Requirement: Search microphone asks for permission

Before starting voice capture on the search screen, the app SHALL check the microphone permission and, when it is missing, request it. When the permission is refused, the app SHALL show the same message Home shows ("Necesitamos acceso al micrófono para la búsqueda por voz") and SHALL NOT start capture.

#### Scenario: Permission missing

- **WHEN** the consumer taps the search microphone and the microphone permission is not granted
- **THEN** the system permission request is shown

#### Scenario: Permission refused

- **WHEN** the consumer refuses the microphone permission from the search screen
- **THEN** the message "Necesitamos acceso al micrófono para la búsqueda por voz" is shown and no capture starts

#### Scenario: Permission granted

- **WHEN** the permission is granted
- **THEN** the search screen starts listening

### Requirement: Leaving search stops voice capture

When the search screen stops being visible, any voice capture in progress SHALL be cancelled and the screen SHALL show the results it showed before the microphone was used.

#### Scenario: Background while listening on search

- **WHEN** the search screen is listening and the app goes to the background
- **THEN** the capture is cancelled and the previous results are shown when the app comes back
