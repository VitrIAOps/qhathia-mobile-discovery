## MODIFIED Requirements

### Requirement: Customer session ends on inactivity

The app SHALL end the current customer's session after a period without user activity when the app is on any screen other than Home, or on Home while the cart basket is not empty. The period SHALL be 180 seconds while the cart dialog is open (cart, consumer form, or an order or share result), and 60 seconds otherwise.

Ending the session SHALL:
- clear the cart basket;
- clear the consumer name and cell-phone entered in the cart and in the product share dialog;
- reset any share or order submission state;
- hide the soft keyboard and clear the input focus;
- close any open cart, share or settings-gate dialog;
- return to the Home screen.

#### Scenario: Inactivity off Home ends the session

- **WHEN** the consumer is on a screen other than Home, the cart dialog is closed, and there is no user activity for 60 seconds
- **THEN** the cart basket is cleared
- **AND** the consumer name and cell-phone fields are cleared
- **AND** any open dialog is closed and the app shows the Home screen

#### Scenario: Open cart gets the longer budget

- **WHEN** the cart dialog is open and there is no user activity for 60 seconds
- **THEN** nothing is cleared and the cart stays open
- **AND** after 180 seconds without activity the session ends as above

#### Scenario: Typing restarts the cart budget

- **WHEN** the customer types one letter in the cart form every 30 seconds
- **THEN** the session does not end

#### Scenario: No keyboard left over Home

- **WHEN** the session ends on inactivity while the keyboard is open in the cart form
- **THEN** Home is shown without the keyboard

#### Scenario: Inactivity on Home with items ends the session

- **WHEN** the app is on Home, the basket has at least one item, the cart dialog is closed, and there is no user activity for 60 seconds
- **THEN** the basket and the consumer data are cleared

#### Scenario: Home with an empty basket is not timed out

- **WHEN** the app is on Home with an empty basket and there is no user activity for 60 seconds
- **THEN** nothing is cleared and Home stays as it is

#### Scenario: Next customer starts clean

- **WHEN** a session ended on inactivity and a new customer opens the cart
- **THEN** the cart is empty and the name and cell-phone fields are empty
