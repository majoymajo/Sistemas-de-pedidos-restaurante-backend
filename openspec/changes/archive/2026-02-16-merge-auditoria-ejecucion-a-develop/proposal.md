## Why

Los hallazgos mitigados y refactors de `feature/auditoria-fase-1-ejecucion` deben integrarse en `develop` sin perder cambios recientes de Copilot ni trazabilidad de auditoria. Ademas, la documentacion de auditoria necesita alinearse con el formato de evidencia usado como referencia en el repositorio remoto para mantener consistencia entre equipos.

## What Changes

- Integrar a `develop` los cambios validados en `feature/auditoria-fase-1-ejecucion` usando un flujo controlado (sync remoto, validacion y merge por PR).
- Estandarizar la documentacion de evidencias de auditoria en `docs/auditoria/` con estructura equivalente al ejemplo en `docs/refactor/` (resumen, commits, archivos, evidencia funcional y estado).
- Actualizar `AUDITORIA.md` para reflejar estado de mitigacion y enlaces consistentes a evidencias/documentos de implementacion.
- Definir checklist minimo previo a merge para evitar regresiones documentales o tecnicas (tests backend, estado OpenSpec y diffs limpios).

## Capabilities

### New Capabilities
- `develop-auditoria-integration`: Flujo repetible para llevar mitigaciones de auditoria desde rama de ejecucion a `develop` preservando cambios remotos recientes.
- `auditoria-evidence-template-alignment`: Estandar de formato para documentos de evidencia en `docs/auditoria` alineado con las guias y ejemplos existentes del repositorio.

### Modified Capabilities
- `audit-report-standard`: Ampliar el estandar para exigir trazabilidad de implementacion (commit/branch/PR/evidencia) en hallazgos mitigados.

## Impact

- Flujo Git/PR: `feature/auditoria-fase-1-ejecucion` -> `develop`.
- Documentacion: `AUDITORIA.md`, `docs/auditoria/*.md` y referencias cruzadas con `docs/refactor/*.md`.
- Especificaciones OpenSpec: nuevos specs de integracion/plantilla documental y ajuste de `audit-report-standard`.
- Validacion tecnica: ejecucion de tests backend (`order-service`, `kitchen-worker`) antes de merge.
