# GitHub Copilot Instructions — Restaurant Ordering System

## 1. Project Context (MANDATORY)
This repository contains a full-stack restaurant ordering system,
received under a **legacy handover (Brownfield)** context.

⚠️ Critical rules:
- DO NOT assume greenfield
- DO NOT break existing functionality
- DO NOT silently change external contracts (REST, AMQP events, DB schemas)

The system is **event-driven**.
Backend services **MUST NOT call each other via REST**.
The only allowed integration between `order-service` and `kitchen-worker`
is **RabbitMQ**.

---

## 2. High-Level Architecture (DO NOT VIOLATE)

### Backend
- Java microservices with **fully separated databases**
- `order-service`
  - REST API
  - Publishes `order.placed` events (version 1 strictly)
  - Persists to `restaurant_db`
- `kitchen-worker`
  - AMQP consumer
  - Projects orders into `kitchen_db`
  - MUST NOT expose HTTP endpoints

### Communication Rules
- REST → frontend ↔ order-service only
- AMQP → order-service → RabbitMQ → kitchen-worker
- ❌ Forbidden: direct REST calls between backend services

---

## 3. Critical Business Rules (MUST BE PRESERVED)

### Validation Rules
- `tableId` must be a positive integer between 1 and 12
- An order must contain **at least one item**
- All `productId` values must exist and be active (`is_active = true`)

### Order States
- New orders ALWAYS start in `PENDING`
- Valid states: `PENDING`, `IN_PREPARATION`, `READY`
- The backend currently does NOT enforce transition guards
- The frontend enforces transitions via `orderStatus.ts`

### Events
- Event: `order.placed`
  - `eventVersion` MUST be exactly `1`
  - Any other value → send directly to DLQ (no retries)
- The kitchen-worker MUST be **idempotent**
  - If an order does not exist, it must be created (upsert behavior)

---

## 4. Security — Mitigation-Oriented Instructions (REFACTORING ALLOWED)

Security weaknesses have been identified.
**Refactoring and module rewrites ARE ALLOWED and ENCOURAGED** if they improve security,
as long as external behavior remains compatible.

Copilot MUST follow these instructions:

### Kitchen Authentication
- Replace hardcoded or frontend-exposed secrets with:
  - Environment-based configuration
  - Or backend-managed secrets
- NEVER embed secrets in Vite `VITE_*` variables

### Token Handling
- Introduce token rotation capability
- Avoid static shared secrets
- Ensure tokens are validated server-side ONLY

### Authorization
- Centralize kitchen authorization logic
- Consider replacing the current Chain of Responsibility
  with a single, testable authorization module if clarity improves

### Destructive Operations
- Protect destructive endpoints (e.g. DELETE /orders) with:
  - Secondary confirmation mechanisms
  - Soft-delete strategies
  - Audit logging (who, when, what)

### Backend Enforcement
- Move critical validation and state transition rules
  from frontend-only logic into the backend
- Frontend validation is NOT sufficient for security

Copilot SHOULD:
- Propose secure refactors
- Introduce defensive programming
- Add tests validating security rules

Copilot MUST NOT:
- Leave secrets exposed
- Trust the frontend for authorization or state validation
- Silence or ignore security-related failures

---

## 5. Backend Code Style — Java / Spring Boot

- Java 17
- Spring Boot 3.2.x
- Lombok allowed (avoid excessive magic)
- Prefer:
  - Small, explicit methods
  - Clear naming
  - Immutability where possible

### Layering (STRICT)
- controller → HTTP only
- application → use cases
- domain → pure business logic
- infrastructure → technical implementations
- repository → persistence access

❌ Forbidden:
- Business logic inside controllers
- Direct repository access from controllers
- AMQP concerns leaking into domain logic

---

## 6. Frontend Code Style — React + TypeScript

- React 18 with strict TypeScript
- Vite as bundler
- TailwindCSS for styling
- TanStack Query for server state

### Frontend Architecture
- `pages/` → views
- `components/` → reusable UI
- `domain/` → frontend business rules
- `api/` → HTTP contracts
- `store/` → global state (Context API)

❌ Forbidden:
- Business rules inside UI components
- Hardcoding state transitions outside `orderStatus.ts`
- Duplicating contracts already defined in `contracts.ts`

---

## 7. Testing Strategy (MANDATORY)

### Approach
- **Test Driven Development (TDD) is mandatory**
- Tests MUST be written before production code
- Commit history must reflect RED → GREEN → REFACTOR

### Backend
- JUnit 5
- Mockito
- H2 for persistence tests
- spring-rabbit-test for AMQP
- jqwik for property-based testing when applicable

### Frontend
- Focus on domain logic and state transitions
- Avoid brittle UI-only snapshot tests

Copilot MUST:
- Propose tests first
- Design test cases using:
  - Equivalence Partitioning
  - Boundary Value Analysis
  - Decision Tables for complex logic

---

## 8. Allowed Use of Copilot

Copilot MAY:
- Generate boilerplate code
- Propose incremental refactors
- Write tests and documentation
- Explain legacy code

Copilot MUST NOT:
- Rewrite the entire system without intent
- Change public contracts silently
- Introduce new dependencies without justification
- Assume behavior not present in the repository

---

## 9. Handling Uncertainty

When information is missing:
- State assumptions explicitly
- Mark open questions for `HANDOVER_REPORT.md`
- NEVER invent system behavior

---

## 10. Final Objective

The goal is NOT:
❌ to modernize everything  
❌ to maximize changes  
❌ to “fix” the system blindly  

The goal IS:
✅ to understand the system  
✅ to evolve it safely  
✅ to improve quality and security  
✅ to document decisions clearly  

Copilot must act as a **senior engineer in a legacy handover scenario**.
