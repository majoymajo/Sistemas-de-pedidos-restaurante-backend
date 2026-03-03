## Context

La Fase 1 del reto técnico solicita un diagnóstico del estado actual del código (snapshot post-MVP), buscando antipatrones de diseño y deuda técnica antes de cualquier refactorización. Actualmente `AUDITORIA.md` existe como marcador inicial y aún no contiene hallazgos estructurados.

## Goals / Non-Goals

**Goals:**
- Definir un método repetible para auditar el proyecto completo con apoyo de IA.
- Crear un estándar mínimo de reporte en `AUDITORIA.md` que conecte hallazgo -> principio vulnerado -> impacto.
- Priorizar hallazgos para alimentar directamente Fase 2 (patrones) y Fase 3 (refactor).

**Non-Goals:**
- Implementar refactorizaciones en esta fase.
- Cambiar arquitectura productiva durante el diagnóstico.
- Cerrar deuda histórica fuera del alcance de la auditoría inicial.

## Decisions

1. Usar una auditoría por dominios (backend, frontend, integración/eventos, documentación operativa) para reducir puntos ciegos.
   - Alternativa: revisión global no estructurada.
   - Motivo de descarte: dificulta trazabilidad y comparación entre equipos.

2. Registrar cada hallazgo con una plantilla fija: componente, evidencia, principio/olor de código, impacto y severidad.
   - Alternativa: notas libres por integrante.
   - Motivo de descarte: genera inconsistencia y dificulta priorización.

3. Tratar `AUDITORIA.md` como artefacto vivo de Fase 1, con secciones por prioridad y recomendaciones accionables.
   - Alternativa: reporte único al final del sprint.
   - Motivo de descarte: retrasa retroalimentación y la preparación de Fase 2.

## Risks / Trade-offs

- [Riesgo] Hallazgos subjetivos o no reproducibles -> Mitigación: exigir evidencia técnica concreta (archivo, función, flujo) por hallazgo.
- [Riesgo] Sobrecarga de observaciones de bajo impacto -> Mitigación: clasificar severidad (Alta/Media/Baja) y priorizar por riesgo de escalabilidad.
- [Trade-off] Mayor tiempo de análisis antes de codificar -> Mitigación: límite de alcance por fase y checklist claro para cerrar diagnóstico.

## Migration Plan

1. Confirmar snapshot base y ramas de trabajo para auditoría.
2. Ejecutar revisión por dominios con plantilla común.
3. Consolidar resultados en `AUDITORIA.md` y validar coherencia cruzada.
4. Dejar insumos listos para selección de patrones en Fase 2.

## Open Questions

- ¿Se exige cobertura del 100% de módulos o una muestra representativa por riesgo?
- ¿Qué nivel de detalle mínimo espera el docente para cada evidencia técnica?
- ¿Se incluirán métricas automáticas (lint, complejidad) en esta fase o solo análisis cualitativo?
