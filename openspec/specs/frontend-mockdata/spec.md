# Frontend Mockdata

## Purpose
TBD - Provide an in-memory mock API and UI flow for the frontend without backend connectivity.
## Requirements
### Requirement: Frontend mock API uses in-memory data aligned to backend contract
The frontend SHALL provide in-memory implementations of menu, table, and order APIs aligned with backend contract semantics (numeric `productId`, string order `id`, statuses `PENDING`, `IN_PREPARATION`, `READY`) for development and controlled fallback usage, and SHALL NOT be the default integration mode for production branches.

#### Scenario: Load menu from mock data in development mode
- **WHEN** the UI requests the menu with mock mode explicitly enabled
- **THEN** the system returns a list of products from in-memory mock data without calling external endpoints
- **AND** items include fields needed by redesigned cards (name, description, price, category, and optional image metadata)

#### Scenario: Mock mode is disabled by default in production branch
- **WHEN** the application is started from production-oriented branch configuration
- **THEN** frontend mock mode is not selected by default
- **AND** the UI uses real backend endpoints for menu and order operations

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
The frontend SHALL allow the redesigned client flow (mesa -> menu with tabs -> carrito -> confirmacion -> estado) to function without backend connectivity only when mock mode is explicitly enabled for development or contingency validation.

#### Scenario: Complete client flow offline in explicit mock mode
- **WHEN** a user selects a mesa, adds items with quantities, includes optional notes, and confirms the order while mock mode is enabled
- **THEN** the UI completes the flow and shows confirmation with order ID and status page without calling external endpoints

#### Scenario: Production mode does not silently fallback to offline flow
- **WHEN** the app runs in production mode and backend requests fail
- **THEN** the UI surfaces integration errors according to error-handling UX
- **AND** it does not silently switch to in-memory offline behavior as default production execution

