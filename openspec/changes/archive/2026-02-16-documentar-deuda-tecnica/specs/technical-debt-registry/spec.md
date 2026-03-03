## ADDED Requirements

### Requirement: Technical debt registry MUST use a standard entry structure
`DEUDA_TECNICA.md` SHALL registrar cada deuda tecnica con un identificador unico (`DT-*`) y campos minimos obligatorios: origen (`H-*`), clasificacion, estado, impacto, esfuerzo, prioridad, owner, fecha objetivo, trigger de pago y referencias de evidencia.

#### Scenario: Team creates a new debt entry
- **WHEN** un integrante agrega una nueva deuda tecnica al registro
- **THEN** la entrada incluye todos los campos obligatorios definidos por el estandar

#### Scenario: Team reviews registry completeness
- **WHEN** se revisa el documento antes de merge a `develop`
- **THEN** no existen entradas `DT-*` sin origen `H-*` o sin owner y fecha objetivo

### Requirement: Technical debt registry MUST preserve traceability to audit findings
Cada entrada `DT-*` SHALL enlazar al hallazgo de auditoria que la origina y a la evidencia tecnica relevante para verificar contexto y avance.

#### Scenario: Reviewer inspects a debt item
- **WHEN** un revisor abre una entrada `DT-*`
- **THEN** puede navegar al hallazgo `H-*` en `AUDITORIA.md` y a su evidencia asociada

### Requirement: Technical debt registry MUST provide prioritization and payment roadmap
El registro SHALL incluir una vista de priorizacion (impacto vs esfuerzo) y un plan de pago por ventanas de tiempo para apoyar planificacion de sprint.

#### Scenario: Team plans next sprint
- **WHEN** el equipo selecciona deuda tecnica para pagar
- **THEN** puede usar prioridad, esfuerzo y fecha objetivo para decidir una secuencia de ejecucion
