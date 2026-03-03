## Context

El reto pide cerrar la Fase 5 con una gestion de deuda tecnica clara y accionable para trabajo colaborativo. El repo ya tiene insumos utiles (`AUDITORIA.md`, `CALIDAD.md`, evidencias de refactor), pero `DEUDA_TECNICA.md` debe consolidarse como registro operativo con trazabilidad y priorizacion consistente.

Restricciones actuales:
- Trabajo en equipo (3 personas), por lo que el documento debe ser facil de mantener en paralelo.
- Debe servir para planificacion real (sprints), no solo para reporte narrativo.
- Debe conservar enlace con hallazgos de auditoria para evitar drift documental.

## Goals / Non-Goals

**Goals:**
- Definir un estandar minimo para el registro de deuda tecnica (campos, estados y trazabilidad).
- Definir gobierno de deuda: ciclo de revision, criterios de cierre y escalamiento.
- Alinear `AUDITORIA.md` con Fase 5 para que un hallazgo pueda rastrearse hasta un `DT-*`.
- Mantener el resultado conciso y explicativo para lectura ejecutiva y tecnica.

**Non-Goals:**
- No implementar refactors tecnicos de los items DT en este cambio.
- No cambiar APIs ni comportamiento de runtime.
- No reemplazar `AUDITORIA.md`; solo reforzar su trazabilidad con deuda tecnica.

## Decisions

### Decision 1: Registrar deuda en documento unico versionado
- Se mantiene `DEUDA_TECNICA.md` como fuente de verdad del backlog de deuda tecnica.
- Razon: simplifica colaboracion, revision en PR y auditoria historica en git.
- Alternativa descartada: usar tablero externo como fuente principal (pierde trazabilidad directa en repo).

### Decision 2: Estandar de item DT obligatorio
- Cada deuda debe tener campos obligatorios: ID, origen (hallazgo), estado, impacto, esfuerzo, prioridad, owner, fecha objetivo, trigger de pago y enlaces de evidencia.
- Razon: evita entradas ambiguas y permite priorizacion comparable.
- Alternativa descartada: formato libre por item (reduce calidad de decisiones).

### Decision 3: Gobernanza por ciclo de revision fijo
- Se define revision periodica en cada sprint review y reglas de transicion de estado.
- Razon: evita que la deuda quede estancada y fuerza accountability.
- Alternativa descartada: revision ad-hoc solo cuando hay incidentes.

### Decision 4: Trazabilidad bidireccional auditoria <-> deuda
- `AUDITORIA.md` debe referenciar IDs `DT-*` cuando exista deuda abierta asociada.
- `DEUDA_TECNICA.md` debe referenciar `H-*` de origen.
- Razon: facilita seguimiento de causa, mitigacion y pago.
- Alternativa descartada: trazabilidad unidireccional.

## Risks / Trade-offs

- [Riesgo] Documento demasiado largo y dificil de mantener.
  - Mitigacion: exigir formato conciso por item y secciones minimas.

- [Riesgo] Desalineacion entre estado real de implementacion y estado documental.
  - Mitigacion: incluir validacion en PR checklist y revision de sprint.

- [Trade-off] Mayor rigor documental consume tiempo de equipo.
  - Mitigacion: plantilla fija + actualizaciones por lote al cierre de sprint.

- [Trade-off] Priorizar deuda puede retrasar features de negocio.
  - Mitigacion: explicitar triggers y ventanas de pago por prioridad.

## Migration Plan

1. Crear artefactos OpenSpec para estandar y gobierno de deuda tecnica.
2. Actualizar `DEUDA_TECNICA.md` al formato definido (conciso + trazable).
3. Ajustar `AUDITORIA.md` para incluir referencia a IDs `DT-*` cuando aplique.
4. Validar consistencia cruzada (`H-*` <-> `DT-*`) antes de merge.
5. Documentar rutina de mantenimiento para siguientes sprints.

Rollback:
- Si el formato propuesto no es adoptado por el equipo, revertir solo cambios documentales y mantener historial previo en git.

## Open Questions

- Que umbral exacto de esfuerzo/impacto se usara para prioridad alta/media/baja?
- El owner de cada DT sera una persona fija o rol rotativo por sprint?
- Se agregara un reporte automatico (script) para detectar `H-*` sin `DT-*` asociado?
