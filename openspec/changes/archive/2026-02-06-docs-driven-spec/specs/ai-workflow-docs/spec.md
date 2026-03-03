## ADDED Requirements

### Requirement: AI_WORKFLOW.md documents Spec-Driven protocol and OpenSpec usage
The system SHALL provide a clear, project-specific description of the Spec-Driven protocol and how OpenSpec is used in this repository, including the artifact flow and required commands.

#### Scenario: Reader can follow the Spec-Driven flow
- **WHEN** a contributor reads AI_WORKFLOW.md
- **THEN** they can identify the sequence of artifacts (proposal, specs, design, tasks) and the commands to create and apply a change

### Requirement: AI_WORKFLOW.md includes local conventions for this project
The system SHALL document local conventions for this repository, including naming of changes, branch usage, and when to use apply/verify/archive steps.

#### Scenario: Reader sees repo-specific conventions
- **WHEN** a contributor reads the workflow section
- **THEN** they can see the conventions used in this repo (change naming, branch targets, and when to run verify/archive)
