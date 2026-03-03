## MODIFIED Requirements

### Requirement: Frontend mock API uses in-memory data aligned to backend contract
The frontend SHALL provide in-memory implementations of menu, table, and order APIs that stay aligned with backend contract semantics (string order `id`, statuses `PENDING`, `IN_PREPARATION`, `READY`) and support UI fields required by the redesign, including table occupancy and order/item notes.

#### Scenario: Load menu from mock data
- **WHEN** the UI requests the menu
- **THEN** the system returns a list of products from in-memory mock data without calling any external endpoint
- **AND** items include fields needed by the redesigned cards (name, description, price, and optional image metadata if available)

#### Scenario: Create order in mock data store
- **WHEN** the UI submits a valid order (tableId and items)
- **THEN** the system creates an order in memory with an `id` and status `PENDING`
- **AND** persists submitted notes in the in-memory order
- **AND** marks the selected table as occupied in mock table state

#### Scenario: Retrieve order status from mock data store
- **WHEN** the UI requests an existing order by id
- **THEN** the system returns the order with current status and notes from memory

### Requirement: Kitchen board updates order status through manual actions
The frontend SHALL allow kitchen users to manually transition orders from `PENDING` to `IN_PREPARATION` and from `IN_PREPARATION` to `READY` using mock data, and SHALL expose updated board state through periodic refresh behavior.

#### Scenario: Transition order to in preparation
- **WHEN** a kitchen user selects an order in status `PENDING` and triggers the action
- **THEN** the system updates the order status to `IN_PREPARATION` in memory

#### Scenario: Transition order to ready
- **WHEN** a kitchen user selects an order in status `IN_PREPARATION` and triggers the action
- **THEN** the system updates the order status to `READY` in memory

#### Scenario: Board reflects mock changes on refresh
- **WHEN** the kitchen board refresh cycle executes after an order create or status change
- **THEN** the UI receives the latest in-memory order list and grouped counts

#### Scenario: No persistence across reloads
- **WHEN** the page is reloaded
- **THEN** the in-memory mock data resets to its initial state

### Requirement: Client flow works without backend connectivity
The frontend SHALL allow the redesigned client flow (mesa -> menu with tabs -> carrito -> confirmacion -> estado) to function without network dependency by using mock data for all API calls.

#### Scenario: Complete client flow offline
- **WHEN** a user selects a mesa, adds items with quantities, includes optional notes, and confirms the order
- **THEN** the UI completes the flow and shows confirmation with order ID and status page without calling any external endpoint

#### Scenario: Occupancy is visible in offline table selection
- **WHEN** the user opens table selection in mock mode
- **THEN** table cards display occupied/available state based on mock in-memory orders
