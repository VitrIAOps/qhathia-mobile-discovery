## ADDED Requirements

### Requirement: One pairing code request at a time

While a pairing code request is in flight, further requests to generate a code (repeated taps on "Generar nuevo código") SHALL be ignored. Leaving the pairing section SHALL cancel a request in flight.

#### Scenario: Fast taps on "Generar nuevo código"

- **WHEN** the merchant taps "Generar nuevo código" several times in quick succession
- **THEN** a single code request is sent and a single code is shown, with no error

### Requirement: Paired-device loading failure

On a paired tablet, when the paired-device details cannot be loaded, the pairing section SHALL show a connection error ("No pudimos cargar los datos del dispositivo. Revisa la conexión e inténtalo de nuevo.") and its retry action SHALL load the paired-device details again. A paired tablet SHALL never request a pairing code from that section.

#### Scenario: Offline on a paired tablet

- **WHEN** the tablet is paired, has no network and the merchant opens "Vincular"
- **THEN** the connection error is shown, not the code-generation error

#### Scenario: Retry on a paired tablet

- **WHEN** the merchant taps "Reintentar" on that error
- **THEN** the app requests the paired-device details again and does not request a pairing code
