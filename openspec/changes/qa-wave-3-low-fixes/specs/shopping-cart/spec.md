## ADDED Requirements

### Requirement: Cell-phone field accepts digits only

The cell-phone fields of the cart and of the product share SHALL accept only digits, spaces and a single leading `+`, up to the length the phone validation accepts. Other characters SHALL be ignored as they are typed or pasted.

#### Scenario: Letters typed

- **WHEN** the consumer types "98a7" in the cell-phone field
- **THEN** the field shows "987"

#### Scenario: Pasted number with prefix

- **WHEN** the consumer pastes "+51 987 654 321"
- **THEN** the field shows "+51 987 654 321"
