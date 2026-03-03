## Context

El repositorio usa OpenSpec para documentar y ejecutar cambios con enfoque Spec-Driven, pero AI_WORKFLOW.md no lo explica de forma concreta. Necesitamos una seccion clara que describa el protocolo, los artefactos y el uso especifico en este proyecto.

## Goals / Non-Goals

**Goals:**
- Explicar el protocolo Spec-Driven con pasos claros.
- Documentar como usamos OpenSpec en este repo (crear change, artefactos, apply, verify, archive).
- Mantener la guia breve, accionable y alineada al flujo real del equipo.

**Non-Goals:**
- No introducir nuevas herramientas ni procesos fuera de OpenSpec.
- No duplicar documentacion tecnica del backend/frontend.

## Decisions

- **Actualizar AI_WORKFLOW.md como fuente unica del flujo**: concentrar el protocolo y comandos en un solo archivo para reducir confusion. Alternativa: documentarlo en README, se descarta para mantener README orientado al producto.
- **Ejemplos concretos con comandos OpenSpec**: incluir comandos reales usados en este repo para reducir ambiguedad. Alternativa: descripcion conceptual sin comandos, se descarta por baja accionabilidad.

## Risks / Trade-offs

- [Riesgo] El documento quede desactualizado ? Mitigacion: actualizarlo cuando se creen nuevos cambios o se modifique el flujo.
- [Trade-off] Documentacion mas larga ? Mitigacion: usar secciones cortas y listas directas.
