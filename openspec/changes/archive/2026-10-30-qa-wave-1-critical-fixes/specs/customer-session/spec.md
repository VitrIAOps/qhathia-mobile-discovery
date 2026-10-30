## ADDED Requirements

### Requirement: Customer session ends on inactivity

The app SHALL end the current customer's session after 60 seconds without user activity when the app is on any screen other than Home, or on Home while the cart basket is not empty. Ending the session SHALL clear the cart basket, clear the consumer name and cell-phone entered in the cart and in the product share dialog, reset any share or order submission state, close any open cart, share or settings-gate dialog, and return to the Home screen.

#### Scenario: Inactivity off Home ends the session

- **WHEN** the consumer is on a screen other than Home and there is no user activity for 60 seconds
- **THEN** the cart basket is cleared
- **AND** the consumer name and cell-phone fields are cleared
- **AND** any open dialog is closed and the app shows the Home screen

#### Scenario: Inactivity on Home with items ends the session

- **WHEN** the app is on Home, the basket has at least one item, and there is no user activity for 60 seconds
- **THEN** the basket and the consumer data are cleared

#### Scenario: Home with an empty basket is not timed out

- **WHEN** the app is on Home with an empty basket and there is no user activity for 60 seconds
- **THEN** nothing is cleared and Home stays as it is

#### Scenario: Next customer starts clean

- **WHEN** a session ended on inactivity and a new customer opens the cart
- **THEN** the cart is empty and the name and cell-phone fields are empty

### Requirement: Session ends the same way on every path

Accepting an order or share success, cancelling from the order or share error state, and the inactivity timeout SHALL all end the customer session through the same operation, so the basket, the consumer data and the submission state are cleared together.

#### Scenario: Accept after success clears the consumer data

- **WHEN** the consumer taps "Aceptar" on a success screen and later reopens the cart
- **THEN** the basket and the name and cell-phone fields are empty

### Requirement: Activity inside dialogs and the keyboard counts

Any touch in the main window or inside an app dialog, and any text change in an app text field, SHALL count as user activity and SHALL restart the 60-second inactivity countdown.

#### Scenario: Typing in the cart dialog keeps the session

- **WHEN** the consumer is typing their name or cell-phone in the cart dialog for more than 60 seconds in total, with no pause of 60 seconds
- **THEN** the session is not ended and the basket is kept

#### Scenario: Touching inside a dialog keeps the session

- **WHEN** the consumer taps controls inside the cart dialog (steppers, remove, buttons)
- **THEN** each tap restarts the inactivity countdown
