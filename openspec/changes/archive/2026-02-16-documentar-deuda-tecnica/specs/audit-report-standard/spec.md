## MODIFIED Requirements

### Requirement: Audit report links findings to next phases
`AUDITORIA.md` SHALL incluir trazabilidad de hallazgos priorizados hacia decisiones de patrones (Fase 2), tareas de refactorizacion (Fase 3) y deuda tecnica gestionada (Fase 5).

#### Scenario: Team plans phase-2 work
- **WHEN** el equipo selecciona patrones de diseno
- **THEN** puede mapear cada decision a hallazgos concretos del reporte de auditoria

#### Scenario: Team plans technical debt payment
- **WHEN** un hallazgo permanece abierto o parcialmente mitigado
- **THEN** `AUDITORIA.md` referencia el identificador `DT-*` correspondiente en `DEUDA_TECNICA.md`
