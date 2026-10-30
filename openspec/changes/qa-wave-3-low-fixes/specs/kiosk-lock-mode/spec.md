## ADDED Requirements

### Requirement: Temporary exit to open Android's mobile-data screen

The temporary kiosk exit SHALL also be started from the "Abrir ajustes de datos" button in Network settings, right before opening Android's mobile-data screen. It SHALL behave exactly as the exit from "Salir del modo kiosko": not persisted, and the app pins again when the merchant goes back to Home or the app restarts.

#### Scenario: Re-lock after changing mobile data

- **WHEN** the merchant opened Android's mobile-data screen from Network settings, came back and went to Home
- **THEN** the app pins its task again
