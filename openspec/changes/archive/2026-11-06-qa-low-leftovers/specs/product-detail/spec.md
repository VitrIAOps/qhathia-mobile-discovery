## MODIFIED Requirements

### Requirement: Product detail layout

When a product's detail is loaded, the app SHALL present it following the detail design: a **main product image** accompanied by a **lateral thumbnail** for each further photo (up to three), together with the product **name**, **price**, **availability**, and its **description and characteristics**. Empty thumbnail slots SHALL NOT be drawn. When the product has **no photo**, the main image SHALL show the placeholder with "Sin foto"; a photo that fails to load SHALL show the placeholder without that label.

#### Scenario: Detail rendered with gallery and information

- **WHEN** a product detail with two or more photos is shown
- **THEN** the app shows the main image, one lateral thumbnail per further photo, and the product name, price, availability, and description & characteristics

#### Scenario: One photo

- **WHEN** a product detail has exactly one photo
- **THEN** the main image is shown and no lateral thumbnails are drawn

#### Scenario: No photo

- **WHEN** a product detail has no photo
- **THEN** the main image shows the placeholder with "Sin foto" and no lateral thumbnails are drawn
