## ADDED Requirements

### Requirement: Worktree-based full-stack integration flow is documented
The project SHALL provide a documented procedure to run frontend and backend in parallel using git worktrees for local integration testing.

#### Scenario: Developer can start both services from separate worktrees
- **WHEN** a developer follows the documented steps
- **THEN** they can run frontend and backend concurrently without merging branches

### Requirement: Local validation steps are specified
The project SHALL define minimal validation steps to confirm frontend-backend compatibility (API endpoints, basic UI flow).

#### Scenario: Developer verifies basic compatibility
- **WHEN** the services are running locally
- **THEN** the developer can execute the listed requests and confirm expected responses
