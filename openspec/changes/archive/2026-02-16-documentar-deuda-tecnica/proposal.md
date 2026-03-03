## Why

La Fase 5 del reto exige gestionar la deuda tecnica como un activo de planificacion, no solo como texto descriptivo. El repositorio ya tiene hallazgos y mitigaciones de auditoria, pero falta consolidarlos en un registro de deuda conciso, trazable y gobernable para toma de decisiones de equipo.

## What Changes

- Estandarizar `DEUDA_TECNICA.md` como registro operativo de Fase 5 (conciso y explicativo).
- Definir campos obligatorios por item de deuda: origen, impacto, esfuerzo, prioridad, dueno, fecha objetivo y trigger de pago.
- Exigir trazabilidad entre cada deuda y los hallazgos de `AUDITORIA.md`/evidencias asociadas.
- Definir una rutina de gobierno (revision periodica, criterios de cierre y criterio de escalamiento).
- Actualizar la especificacion de reporte de auditoria para exigir referencia a deuda tecnica cuando aplique.

## Capabilities

### New Capabilities
- `technical-debt-registry`: Estandar para documentar y mantener un registro de deuda tecnica accionable y priorizado.
- `technical-debt-governance`: Reglas para revision periodica, pago, cierre y seguimiento de deuda tecnica.

### Modified Capabilities
- `audit-report-standard`: Requiere trazabilidad explicita desde hallazgos de auditoria hacia IDs de deuda tecnica cuando exista deuda abierta.

## Impact

- Documentacion afectada: `DEUDA_TECNICA.md`, `AUDITORIA.md`, `CALIDAD.md` (referencias cruzadas), y artefactos OpenSpec de este cambio.
- Proceso afectado: planificacion de sprint y revisiones de calidad/arquitectura.
- Impacto tecnico: sin cambios de runtime ni APIs; cambio de gobierno documental y de trazabilidad.
