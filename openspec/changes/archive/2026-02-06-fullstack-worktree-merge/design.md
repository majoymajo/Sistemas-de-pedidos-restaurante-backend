## Context

El repositorio tiene front y back en ramas distintas. Para probar integracion real sin mezclar, se propone usar worktrees y ejecutar ambos servicios en paralelo. Esto evita merges tempranos y facilita validacion local.

## Goals / Non-Goals

**Goals:**
- Definir un procedimiento reproducible para levantar front y back con worktrees.
- Documentar comandos de setup, ejecucion y pruebas basicas.
- Alinear el flujo con GitFlow (PR a develop).

**Non-Goals:**
- No cambiar codigo de negocio ni contratos API.
- No definir nueva infraestructura fuera de Docker Compose existente.

## Decisions

- **Usar worktrees en vez de merge directo**: permite validar compatibilidad sin alterar ramas. Alternativa: merge en una rama integracion, se descarta por mayor friccion.
- **Documentar pasos y variables**: asegurar que cualquiera pueda replicar el entorno local.

## Risks / Trade-offs

- [Riesgo] Configuracion local inconsistente entre maquinas -> Mitigacion: comandos exactos y variables explicitadas.
- [Trade-off] Flujo mas largo para pruebas -> Mitigacion: checklist corto y reuse de scripts.
