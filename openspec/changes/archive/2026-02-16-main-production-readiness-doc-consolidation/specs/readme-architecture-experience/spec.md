## ADDED Requirements

### Requirement: README is the single entry point for repository onboarding
The repository SHALL provide a structured `README.md` that acts as the canonical starting point for setup, architecture understanding, and evidence navigation.

#### Scenario: New reviewer opens repository
- **WHEN** a reviewer reads `README.md`
- **THEN** they find quickstart steps, runtime modes, and links to audit/quality/debt documents
- **AND** they can run the project without opening additional guides first

### Requirement: README includes architecture and flow diagrams in Mermaid
The `README.md` SHALL include Mermaid diagrams representing system architecture and primary user flow to improve clarity for evaluators and maintainers.

#### Scenario: Reviewer inspects architecture section
- **WHEN** a reviewer reaches the architecture section in `README.md`
- **THEN** they see at least one Mermaid diagram for service/component interactions
- **AND** they see at least one Mermaid diagram for client-to-kitchen operational flow

### Requirement: README documents production vs development execution modes
The `README.md` SHALL explicitly document how mock mode differs from real integration mode and define expected defaults for production branches.

#### Scenario: Developer validates environment behavior
- **WHEN** a developer configures environment variables using README guidance
- **THEN** they can distinguish development mock mode from production real-API mode
- **AND** they can confirm which mode is expected in `main`

