# frontend-redesign Specification

## Purpose
TBD - created by archiving change frontend-redesign-from-figma. Update Purpose after archive.
## Requirements
### Requirement: Table selection shows occupancy with explicit visual states
The frontend SHALL display table occupancy state in the table selection screen, using clear visual indicators for `occupied` and `available` tables.

#### Scenario: Occupancy is visible before selecting a table
- **WHEN** the user opens the table selection screen
- **THEN** each table card shows its current occupancy state
- **AND** occupied tables are rendered with a red visual state
- **AND** available tables are rendered with a green visual state

#### Scenario: User selects an available table
- **WHEN** the user selects a table marked as available
- **THEN** the table is stored as the current table for the client flow
- **AND** the user can continue to the menu screen

### Requirement: Menu experience uses category tabs and quantity controls
The frontend SHALL provide category tabs in the menu and allow users to choose item quantities before confirming the order, while consuming the menu dataset from backend APIs by default in production mode.

#### Scenario: User filters menu by category tabs with real backend data
- **WHEN** the user changes the selected category tab
- **THEN** the menu list updates to show items for that category based on the active backend response
- **AND** the currently selected tab is visually highlighted

#### Scenario: User adjusts quantity for a selected item
- **WHEN** the user increases or decreases quantity for an item
- **THEN** the cart state reflects the new quantity
- **AND** the order summary totals are recalculated

### Requirement: Cart and confirmation preserve notes and order ID
The frontend SHALL support additional notes in the order flow and display the created order ID in confirmation.

#### Scenario: User adds notes before confirming
- **WHEN** the user enters notes on one or more cart items or on the order
- **THEN** the notes are included in the create order request payload

#### Scenario: Confirmation displays created order identifier
- **WHEN** the order is successfully created
- **THEN** the confirmation screen displays the returned order ID
- **AND** the user can navigate to order status tracking using that ID

### Requirement: Client can track order status from dedicated screen
The frontend SHALL provide an order status screen where users can consult the current status of an order by ID.

#### Scenario: Status is queried by order ID
- **WHEN** the user enters or navigates with a valid order ID
- **THEN** the frontend requests order details from the API
- **AND** displays the mapped status label for the current order state

#### Scenario: Status view refreshes periodically
- **WHEN** the user remains on the order status screen
- **THEN** the frontend refreshes order status at a fixed interval
- **AND** updates the visible status when backend state changes

### Requirement: Kitchen access and board support real-time operations
The frontend SHALL provide a kitchen login gate by PIN and a kitchen board that refreshes frequently, shows order summaries with product names and quantities, and supports status transitions against real API endpoints in production mode.

#### Scenario: Kitchen board reflects incoming and updated orders from API
- **WHEN** new orders arrive or existing orders change status in backend
- **THEN** the kitchen board refresh cycle updates visible columns and counters
- **AND** each order card shows summary of table, item names, quantities, and current status

#### Scenario: Kitchen user changes order status through API
- **WHEN** a kitchen user executes a valid status transition
- **THEN** the frontend sends the status update request to the backend API
- **AND** the board updates to place the order in the corresponding status column

