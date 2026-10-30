## ADDED Requirements

### Requirement: Product images have a placeholder

Every product image (cards, detail gallery and thumbnails, cart lines, success summary) SHALL show a neutral "no image" placeholder when the product has no photo or the photo fails to load, and a neutral background while it loads. A product image area SHALL never be left blank.

#### Scenario: Product without photo

- **WHEN** a product has no image URL
- **THEN** its image area shows the "no image" placeholder

#### Scenario: Broken photo

- **WHEN** a product image URL fails to load
- **THEN** its image area shows the "no image" placeholder
