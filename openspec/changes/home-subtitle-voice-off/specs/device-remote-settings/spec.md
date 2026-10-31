## MODIFIED Requirements

### Requirement: Voice assistant setting

When the stored `voiceAssistantEnabled` is `false`, the app SHALL hide every microphone control (Home, Catalog, Product search, Product detail) so voice search cannot be started, and the Home idle subtitle SHALL invite the customer to type (*"Escribe lo que buscas, por nombre o detalles"*) instead of pointing at the microphone. When it is `true` or no settings are stored, microphone controls and the Home subtitle SHALL be shown as today. A change SHALL apply without restarting the app.

#### Scenario: Voice assistant turned off

- **WHEN** the stored settings have `voiceAssistantEnabled = false`
- **THEN** no microphone button is shown on Home, Catalog, Product search or Product detail, and text search keeps working

#### Scenario: Home subtitle invites typing while voice is off

- **WHEN** the stored settings have `voiceAssistantEnabled = false` and Home is idle
- **THEN** the subtitle reads "Escribe lo que buscas, por nombre o detalles" and does not mention pressing the microphone

#### Scenario: Voice assistant turned back on

- **WHEN** a later sync stores `voiceAssistantEnabled = true`
- **THEN** the microphone buttons reappear without restarting the app
- **AND** the Home subtitle is again "Presiona aquí y describe lo que buscas, por nombre o detalles"
