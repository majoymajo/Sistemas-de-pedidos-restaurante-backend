# Deuda tecnica (consolidado)

## Metadatos

- Fecha de corte: 2026-02-16
- Estado del backlog: activo
- Fuente primaria de hallazgos: `docs/auditoria/AUDITORIA.md`

## Criterio de gestion

Cada item DT incluye:
- ID y hallazgo origen (`H-*`)
- Clasificacion (Fowler)
- Estado
- Impacto y prioridad
- Owner
- Trigger
- Evidencia de pago o plan

## Registro

| ID | Origen | Clasificacion | Estado | Impacto | Prioridad | Owner | Trigger | Evidencia |
|---|---|---|---|---|---|---|---|---|
| DT-001 | H-ALTA-01 | Prudente/Inadvertida | PAGADA | Alto | P1 | Backend | Complejidad excesiva en OrderService | `docs/auditoria/AUDITORIA.md` |
| DT-002 | H-ALTA-04 | Imprudente/Inadvertida | PAGADA | Alto | P1 | Frontend | Contrato `productId` inconsistente | `docs/auditoria/AUDITORIA.md` |
| DT-003 | H-MEDIA-01 | Imprudente/Inadvertida | PAGADA | Medio | P2 | Kitchen Worker | Field injection en clases criticas | `docs/auditoria/AUDITORIA.md` |
| DT-004 | H-ALTA-02 | Prudente/Deliberada | PAGADA | Alto | P1 | Backend + Integracion | Riesgo de inconsistencia por broker | `docs/quality/CALIDAD.md` |
| DT-005 | H-ALTA-03 | Prudente/Deliberada | PAGADA | Alto | P1 | Arquitectura | Acoplamiento por DB compartida | `docs/quality/CALIDAD.md` |
| DT-006 | H-ALTA-05 | Imprudente/Deliberada | PAGADA | Alto | P1 | Seguridad Fullstack | Endpoints de cocina sin control robusto | `docs/quality/CALIDAD.md` |
| DT-007 | H-ALTA-06 | Imprudente/Inadvertida | EN_PROGRESO | Alto | P1 | Arquitectura Backend | Fronteras por capas incompletas | `docs/auditoria/AUDITORIA.md` |
| DT-008 | H-MEDIA-02 | Prudente/Inadvertida | EN_PROGRESO | Medio | P2 | Frontend Cocina | Componente con responsabilidades multiples | `docs/auditoria/AUDITORIA.md` |
| DT-009 | H-MEDIA-03 | Prudente/Deliberada | EN_PROGRESO | Medio | P2 | Integracion Eventos | Endurecer resiliencia e idempotencia | `docs/auditoria/AUDITORIA.md` |
| DT-010 | H-BAJA-01 | Imprudente/Inadvertida | PAGADA | Bajo | P3 | Docs | Drift documental de artefactos markdown | `README.md`, `docs/development/GUIA_RAPIDA.md` |
| DT-011 | H-BAJA-02 | Prudente/Deliberada | PENDIENTE | Medio | P2 | Calidad/Plataforma | Hardening no funcional pre-produccion | `docs/quality/CALIDAD.md` |

## Deuda abierta priorizada

1. DT-011: observabilidad, cobertura minima formal en CI y hardening de abuso.
2. DT-007: profundizar separacion por capas/puertos en backend.
3. DT-008/DT-009: completar desacople de UI cocina y endurecer contrato de eventos.

## Politica de cierre

Para marcar un item como `PAGADA`:
- evidencia tecnica verificable,
- pruebas asociadas en `docs/quality/CALIDAD.md`,
- trazabilidad al hallazgo original en `docs/auditoria/AUDITORIA.md`.
