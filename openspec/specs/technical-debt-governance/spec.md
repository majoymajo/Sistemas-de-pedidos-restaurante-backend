# technical-debt-governance Specification

## Purpose
TBD - created by archiving change documentar-deuda-tecnica. Update Purpose after archive.
## Requirements
### Requirement: Technical debt governance MUST define a periodic review cycle
El equipo SHALL revisar el registro de deuda tecnica en una cadencia fija (minimo una vez por sprint) para actualizar estado, prioridad y plan de pago.

#### Scenario: Sprint review includes debt check
- **WHEN** finaliza un sprint
- **THEN** el equipo actualiza estados `DT-*` y registra decisiones de continuidad, pago o escalamiento

### Requirement: Technical debt governance MUST define status transitions and closure criteria
La gestion SHALL usar estados definidos (por ejemplo: `PENDIENTE`, `EN_PROGRESO`, `PAGADA`, `POSTERGADA`) y reglas claras para transicionar y cerrar una deuda.

#### Scenario: Debt item is marked as paid
- **WHEN** una deuda pasa a estado `PAGADA`
- **THEN** la entrada incluye evidencia de implementacion y fecha de cierre verificable

#### Scenario: Debt item is postponed
- **WHEN** una deuda se mueve a `POSTERGADA`
- **THEN** se documenta justificacion, nuevo trigger y nueva fecha objetivo

### Requirement: Technical debt governance MUST integrate with quality decisions
Las decisiones de deuda tecnica SHALL formar parte de las validaciones de calidad previas a merge de cambios mayores, para evitar crecimiento no controlado de riesgo tecnico.

#### Scenario: Team prepares merge with unresolved high debt
- **WHEN** existe deuda de prioridad alta sin plan de pago vigente
- **THEN** el checklist de calidad exige definir owner, trigger y fecha objetivo antes del cierre

