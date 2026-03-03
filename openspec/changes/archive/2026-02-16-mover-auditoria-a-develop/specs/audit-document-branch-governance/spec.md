## ADDED Requirements

### Requirement: Audit document lives on develop
El repositorio SHALL mantener `AUDITORIA.md` en la rama `develop` como rama de trabajo para la auditoría colaborativa.

#### Scenario: Develop contains audit file
- **WHEN** se inspecciona el árbol de `develop`
- **THEN** `AUDITORIA.md` está presente

### Requirement: Main remains free of audit working document
La rama `main` SHALL quedar sin `AUDITORIA.md` después de aplicar la corrección.

#### Scenario: Main does not contain audit file
- **WHEN** se inspecciona el árbol de `main`
- **THEN** `AUDITORIA.md` no está presente

### Requirement: Change is traceable through non-destructive git operations
La corrección SHALL preservar trazabilidad usando operaciones no destructivas (`cherry-pick` y `revert`).

#### Scenario: History records move and revert
- **WHEN** se revisa el historial de `main` y `develop`
- **THEN** existen commits explícitos para el traslado y la reversión sin reescritura forzada
