## ADDED Requirements

### Requirement: History restores the selected filters

When the consumer goes back to a previous result in the search history, the filter chips and suggestion that were selected for that result SHALL be shown as selected again.

#### Scenario: Back to a filtered result

- **WHEN** the consumer selected the "Bosch" chip, then ran a new search, then goes back
- **THEN** the previous result is shown with the "Bosch" chip selected
