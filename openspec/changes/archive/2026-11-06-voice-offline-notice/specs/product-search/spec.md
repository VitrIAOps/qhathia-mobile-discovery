## ADDED Requirements

### Requirement: Failed voice search says why

When a voice search started in Search fails, the app SHALL tell the consumer why with a message that outlives the screen: the established network-error title and message when the network is missing or unstable, and "No pude usar el micrófono. Escribe tu búsqueda o inténtalo otra vez." for any other error. The screen SHALL be restored as before (the results kept, or back to the previous screen when the search was opened by voice), and no alternative search SHALL run. When nothing was heard, no message SHALL be shown.

#### Scenario: Microphone without network

- **WHEN** results are shown, the network is lost, and the consumer taps the microphone
- **THEN** within 1 second the app shows "Perdí la conexión…" and the results stay on screen

#### Scenario: Voice search opened from the Catalog without network

- **WHEN** the consumer taps the Catalog microphone without network
- **THEN** the app returns to the Catalog and shows the same network message

#### Scenario: Nothing heard

- **WHEN** the voice capture ends without speech
- **THEN** the previous screen comes back without a message
