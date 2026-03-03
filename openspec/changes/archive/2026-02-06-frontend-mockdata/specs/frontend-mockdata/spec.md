## ADDED Requirements

### Requirement: Frontend mock API uses in-memory data aligned to backend contract
The frontend SHALL provide in-memory implementations of menu and order APIs that use the backend contract for identifiers and statuses (UUID `id`, statuses `PENDING`, `IN_PREPARATION`, `READY`).

#### Scenario: Load menu from mock data
- **WHEN** the UI requests the menu
- **THEN** the system returns a list of products from in-memory mock data without calling any external endpoint

#### Scenario: Create order in mock data store
- **WHEN** the UI submits a valid order (tableId and items)
- **THEN** the system creates an order in memory with a UUID `id` and status `PENDING`

#### Scenario: Retrieve order status from mock data store
- **WHEN** the UI requests an existing order by id
- **THEN** the system returns the order with the current status from memory

### Requirement: Kitchen board updates order status through manual actions
The frontend SHALL allow kitchen users to manually transition orders from `PENDING` to `IN_PREPARATION` and from `IN_PREPARATION` to `READY` using mock data without persistence across reloads.

#### Scenario: Transition order to in preparation
- **WHEN** a kitchen user selects an order in status `PENDING` and triggers the action
- **THEN** the system updates the order status to `IN_PREPARATION` in memory

#### Scenario: Transition order to ready
- **WHEN** a kitchen user selects an order in status `IN_PREPARATION` and triggers the action
- **THEN** the system updates the order status to `READY` in memory

#### Scenario: No persistence across reloads
- **WHEN** the page is reloaded
- **THEN** the in-memory mock data resets to its initial state

### Requirement: Client flow works without backend connectivity
The frontend SHALL allow the client flow (mesa ? menú ? carrito ? confirmación ? estado) to function without any network dependency by using mock data for all API calls.

#### Scenario: Complete client flow offline
- **WHEN** a user selects a mesa, adds items to the cart, and confirms the order
- **THEN** the UI completes the flow and shows a confirmation and status page without calling any external endpoint
