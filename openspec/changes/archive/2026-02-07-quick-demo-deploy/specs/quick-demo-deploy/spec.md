## ADDED Requirements

### Requirement: Demo stack can be exposed with temporary public URLs
The system SHALL allow the demo to be published using temporary public URLs for the frontend and backend without requiring paid infrastructure.

#### Scenario: Backend URL is published
- **WHEN** the operator runs a Quick Tunnel for `http://localhost:8080`
- **THEN** a temporary public URL is generated for backend access

#### Scenario: Frontend URL is published
- **WHEN** the operator runs a Quick Tunnel for `http://localhost:5173`
- **THEN** a temporary public URL is generated for frontend access

### Requirement: Frontend targets the public backend URL during demo
The frontend SHALL be configurable to consume the backend public URL during the demo deployment.

#### Scenario: Backend base URL configured
- **WHEN** the operator sets `VITE_API_BASE_URL` to the backend public URL and rebuilds the frontend
- **THEN** the demo UI uses the public backend for all requests

### Requirement: Demo flow validates menu, order creation, and kitchen board
The demo SHALL allow a user to view the menu, create an order, and observe the order in the kitchen board.

#### Scenario: End-to-end demo flow
- **WHEN** a user opens the public frontend URL
- **THEN** they can view the menu, place an order, and see it in the kitchen board view
