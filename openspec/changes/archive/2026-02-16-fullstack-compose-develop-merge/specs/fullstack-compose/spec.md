## ADDED Requirements

### Requirement: Compose stack boots full system with standard ports
The system SHALL provide a Docker Compose stack that starts frontend, order-service, kitchen-worker, postgres and rabbitmq using standardized ports (backend 8080, frontend 5173, postgres 5432, rabbitmq 5672/15672).

#### Scenario: Developer starts the full stack
- **WHEN** a developer runs the documented Docker Compose command
- **THEN** all services start successfully and expose the standardized ports

### Requirement: Frontend runs inside Compose and uses real API
The system SHALL run the frontend inside Docker Compose and configure it to call the backend API through the compose network (no mock mode).

#### Scenario: Frontend requests backend endpoints
- **WHEN** the frontend is running inside the compose stack
- **THEN** API requests are served by the backend container without CORS errors

### Requirement: Single Postgres database for local QA
The system SHALL use a single Postgres instance for order-service and kitchen-worker in the compose stack for QA simplicity.

#### Scenario: Services connect to shared database
- **WHEN** both backend services start in compose
- **THEN** they connect to the same Postgres database using the documented credentials

### Requirement: RabbitMQ verification is defined
The system SHALL provide a verification step to confirm RabbitMQ is working (management UI or queue inspection).

#### Scenario: Verify event flow
- **WHEN** an order is created in the system
- **THEN** a message appears in RabbitMQ (or is consumed) as documented in the verification steps
