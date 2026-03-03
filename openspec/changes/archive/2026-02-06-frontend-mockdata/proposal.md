## Why

The frontend currently depends on live backend endpoints and uses order status values that do not match the backend contract. We need an offline, deterministic UI flow for demos and to align the frontend contract before integration.

## What Changes

- Align frontend domain models and status values with the backend contract (PENDING, IN_PREPARATION, READY; UUID id).
- Replace API calls with in-memory mock data for menu, orders, and status transitions.
- Simplify kitchen access for mock mode (direct access, no PIN gate).
- Update frontend docs/config notes to describe mock mode and local usage.

## Capabilities

### New Capabilities
- `frontend-mockdata`: In-memory mock API that drives client and kitchen flows without calling real endpoints, aligned to backend contracts.

### Modified Capabilities
- (none)

## Impact

- Frontend API layer (`src/api/*`) and domain types (`src/domain/*`).
- Client and kitchen pages that depend on status transitions and order identifiers.
- Documentation/config files (e.g., `README.md`, `.env.example`) to reflect mock usage.
