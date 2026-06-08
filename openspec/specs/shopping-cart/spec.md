# shopping-cart Specification

## Purpose

The shopping-cart capability lets in-store consumers collect products discovered through search and detail into a single, session-scoped basket and review them in a cart dialog. It owns the in-memory basket shared across screens, adding products from search results and the product-detail screen, per-product quantity management bounded by stock, the cart control with its item-count badge, the cart dialog presentation (item list, receipt-styled totals, consumer-data form, and order/share actions), the consumer-data form validation, the render-only "Realizar pedido" and "Compartir por WhatsApp" actions, and the cart session lifecycle (inactivity clearing off the Home screen).

## Requirements

### Requirement: Session-scoped cart basket

The app SHALL maintain a single cart basket as session state that is shared across every screen. The basket SHALL hold the selected products, each with a per-line quantity, and SHALL expose the total item count and the total amount. The basket SHALL be observable so that any screen reflects its current contents and count. The basket SHALL be held in memory for the session (it need not survive process death).

#### Scenario: Basket shared across screens

- **WHEN** a product is added to the cart on one screen and the consumer navigates to another screen
- **THEN** the cart on the other screen reflects the same items and total item count

#### Scenario: Totals computed from line quantities

- **WHEN** the basket contains items with per-unit prices and quantities
- **THEN** each line's subtotal equals price × quantity
- **AND** the total amount equals the sum of all line subtotals
- **AND** the total item count equals the sum of all line quantities

### Requirement: Add products to the cart

The app SHALL let the consumer add a product to the cart from a search result's cart button and from the product-detail "Agregar al carrito" button. Adding from the detail screen SHALL use the quantity selected in the detail's quantity stepper. Adding the same product again SHALL increase that product's existing line quantity rather than creating a duplicate line. There SHALL be no app-imposed limit on the number of distinct products in the cart.

#### Scenario: Add from a search result

- **WHEN** the consumer taps a search result's cart button
- **THEN** that product is added to the cart and the cart's total item count increases

#### Scenario: Add from the product detail with a chosen quantity

- **WHEN** the consumer sets the detail quantity stepper to N and taps "Agregar al carrito"
- **THEN** that product is added to the cart with quantity N

#### Scenario: Adding an existing product merges into one line

- **WHEN** the consumer adds a product that is already in the cart
- **THEN** the product's existing line quantity increases instead of adding a second line

#### Scenario: No limit on distinct products

- **WHEN** the consumer adds many different products
- **THEN** the app does not impose a maximum on the number of distinct products in the cart

### Requirement: Per-product quantity bounded by stock

The per-product quantity SHALL be bounded by the product's inventory availability. The quantity stepper for a product SHALL NOT increase the quantity beyond that product's maximum. Decreasing a cart line's quantity below 1 SHALL remove the line from the cart.

#### Scenario: Increase is blocked at the maximum

- **WHEN** a product's cart-line quantity is at its stock-derived maximum
- **THEN** the stepper's increase control does not raise the quantity further

#### Scenario: Decreasing below one removes the line

- **WHEN** the consumer decreases a cart line's quantity below 1
- **THEN** the line is removed from the cart

### Requirement: Cart control shows the item count

The cart control on the screens that expose it SHALL display the current total item count in the cart, and SHALL update as items are added or removed.

#### Scenario: Count badge reflects the basket

- **WHEN** the cart contains one or more items
- **THEN** the cart control displays the current total item count
- **AND** the displayed count updates when items are added or removed

### Requirement: Cart dialog presentation

Opening the cart SHALL present a dialog following Figma node `758-25521`. The dialog SHALL show the title **"Carrito"** with the total item count as its subtitle, a scrollable list of the cart's products, a totals section styled like a purchase receipt showing subtotals and the total to pay, a consumer-data form, a **"Realizar pedido"** button, a **"Seguir buscando"** button, and a **"Compartir por WhatsApp"** button. Opening or closing the cart SHALL affect only the dialog and SHALL NOT navigate to a separate cart screen.

#### Scenario: Cart opens as a dialog

- **WHEN** the consumer taps the cart control on the product-search or product-detail screen
- **THEN** the app shows the cart dialog over the current screen without navigating to a separate screen

#### Scenario: Dialog header shows title and count

- **WHEN** the cart dialog is shown
- **THEN** its title reads "Carrito" and its subtitle shows the total number of products in the cart

#### Scenario: Totals presented as a receipt

- **WHEN** the cart dialog is shown with items
- **THEN** a receipt-styled totals section shows the line subtotals and the total amount to pay

### Requirement: Cart item interactions

Each product line in the cart dialog SHALL provide a quantity stepper to change that product's quantity, and SHALL provide a way to open that product's detail — either via an explicit "go to detail" control or by tapping the line itself. Opening a product's detail from the cart SHALL dismiss the cart dialog and navigate to that product's detail.

#### Scenario: Stepper changes a line quantity

- **WHEN** the consumer uses a cart line's stepper
- **THEN** that product's quantity changes within its allowed bounds and the totals update

#### Scenario: Tapping a line opens its detail

- **WHEN** the consumer taps a cart line (or its go-to-detail control)
- **THEN** the cart dialog is dismissed and the app navigates to that product's detail

### Requirement: Consumer-data form validation

The cart dialog SHALL provide a consumer-data form requiring a **name** and a **cell-phone number**, both of which are required to generate the order. The name SHALL be required to contain at least one referential name. The cell-phone number SHALL be required to match the general mobile-number format. The **"Realizar pedido"** action SHALL be enabled only when both the name and the cell-phone number are valid.

#### Scenario: Order disabled until data is valid

- **WHEN** the name or the cell-phone number is missing or invalid
- **THEN** the "Realizar pedido" action is not enabled

#### Scenario: Invalid name rejected

- **WHEN** the entered name does not contain at least one referential name
- **THEN** the form marks the name as invalid and the order remains disabled

#### Scenario: Invalid phone rejected

- **WHEN** the entered cell-phone number does not match the general mobile-number format
- **THEN** the form marks the phone as invalid and the order remains disabled

#### Scenario: Valid data enables the order

- **WHEN** a valid name and a valid cell-phone number are entered
- **THEN** the "Realizar pedido" action becomes enabled

### Requirement: Render-only order and share actions

The **"Realizar pedido"** and **"Compartir por WhatsApp"** actions SHALL be render-only because their backends do not exist yet. Tapping "Realizar pedido" SHALL NOT perform a network request to place an order, and tapping "Compartir por WhatsApp" SHALL NOT open WhatsApp or perform a share. Both deferred behaviors SHALL be marked in code as `TODO` to be implemented when their APIs are available.

#### Scenario: Place order performs no network action

- **WHEN** the consumer taps "Realizar pedido" with valid data
- **THEN** the app does not perform an order network request (the action is a TODO placeholder)

#### Scenario: Share performs no action

- **WHEN** the consumer taps "Compartir por WhatsApp"
- **THEN** the app does not open WhatsApp or perform a share (the action is a TODO placeholder)

### Requirement: Closing the cart dialog

Closing the cart dialog via the **"Seguir buscando"** button or the top close ("X") control SHALL dismiss only the dialog and SHALL leave the underlying screen and the cart contents unchanged.

#### Scenario: Seguir buscando dismisses only the dialog

- **WHEN** the consumer taps "Seguir buscando" or the top "X" control
- **THEN** the cart dialog is dismissed
- **AND** the underlying screen and the cart contents are unchanged

### Requirement: Cart session lifecycle

The cart basket SHALL be cleared after 1 minute of inactivity on any screen that is not the Home screen. On the Home screen the inactivity clear SHALL NOT apply. The basket SHALL also be cleared when the consumer places an order (deferred until the order action is implemented).

#### Scenario: Inactivity off Home clears the cart

- **WHEN** the consumer is on a screen other than Home and does not interact for 1 minute
- **THEN** the cart basket is cleared

#### Scenario: Interaction resets the inactivity timer

- **WHEN** the consumer interacts with a non-Home screen before the minute elapses
- **THEN** the 1-minute inactivity countdown restarts

#### Scenario: Home screen does not clear on inactivity

- **WHEN** the consumer is on the Home screen and does not interact for 1 minute
- **THEN** the cart basket is not cleared by the inactivity rule

### Requirement: Theming and accessibility compliance

The cart dialog and the cart controls SHALL support light and dark themes and SHALL meet accessibility requirements. Interactive controls (the cart control with its count, the quantity steppers, the form fields, and the dialog buttons) SHALL expose content descriptions and provide touch targets of at least 48dp.

#### Scenario: Dark mode

- **WHEN** the device is in dark mode
- **THEN** the cart dialog and controls render with the dark theme

#### Scenario: Interactive controls are accessible

- **WHEN** an assistive technology inspects the cart control, the steppers, the form fields, and the dialog buttons
- **THEN** each exposes a content description and a touch target of at least 48dp
