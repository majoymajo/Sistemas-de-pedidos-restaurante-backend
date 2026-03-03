# production-release-readiness Specification

## Purpose
TBD - created by archiving change main-production-readiness-doc-consolidation. Update Purpose after archive.
## Requirements
### Requirement: Main branch runs in real integration mode by default
The system SHALL configure production-ready execution in `main` so the frontend uses real backend APIs by default and does not depend on in-memory mock data.

#### Scenario: Frontend defaults to real API mode in main
- **WHEN** a developer or evaluator starts the stack from `main` using Docker Compose
- **THEN** frontend configuration resolves with `VITE_USE_MOCK=false` by default
- **AND** frontend requests are sent to the configured backend base URL

### Requirement: Expanded menu is available from backend API
The order-service SHALL expose an expanded menu dataset that includes, at minimum, id, name, description, price, category, active flag, and image URL for each active product required by the redesigned frontend.

#### Scenario: Menu endpoint serves expanded catalog
- **WHEN** a client requests `GET /menu` in production mode
- **THEN** the response includes multiple categories and more than the original minimal 3-item set
- **AND** each returned active product includes `id`, `name`, `description`, `price`, `category`, and optional `imageUrl`

### Requirement: Release validation gate is mandatory before merging to main
The project SHALL define and execute a pre-merge release gate that validates build, Docker Compose startup, and critical API/UI flows before integrating `develop` into `main`.

#### Scenario: Team prepares release merge
- **WHEN** maintainers promote changes from `develop` to `main`
- **THEN** they execute the documented validation gate
- **AND** they record evidence that frontend, order-service, kitchen-worker, rabbitmq, and databases are running
- **AND** they verify critical flows (menu load, order creation, kitchen status transition)

