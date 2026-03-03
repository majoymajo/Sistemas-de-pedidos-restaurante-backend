## Why

El proyecto tiene un MVP funcional, pero para evolucionarlo con seguridad se necesita una línea base de calidad técnica y un diagnóstico explícito de deuda de diseño. La Fase 1 del reto exige documentar antipatrones y su impacto para orientar las fases de arquitectura y refactorización.

## What Changes

- Definir el proceso de auditoría técnica inicial del sistema con foco en violaciones de SOLID y code smells.
- Estandarizar el contenido esperado de `AUDITORIA.md` para que cada hallazgo tenga evidencia, principio vulnerado e impacto.
- Establecer criterios mínimos de cobertura de análisis para backend, frontend y capas compartidas.

## Capabilities

### New Capabilities
- `audit-phase-1-diagnostic`: Capacidad para ejecutar una auditoría estructurada del estado post-MVP y registrar hallazgos priorizados.
- `audit-report-standard`: Capacidad para producir un `AUDITORIA.md` consistente, trazable y útil para fases posteriores.

### Modified Capabilities
- Ninguna.

## Impact

- Documentación afectada: `AUDITORIA.md`.
- Artefactos OpenSpec: `openspec/changes/auditoria-fase-1-diagnostico/*`.
- Equipos involucrados: desarrollo backend, frontend y coordinación técnica de auditoría.
