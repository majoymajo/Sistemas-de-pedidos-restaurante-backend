## MODIFIED Requirements

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

