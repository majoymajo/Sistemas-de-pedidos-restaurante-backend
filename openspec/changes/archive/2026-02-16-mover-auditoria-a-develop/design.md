## Context

Actualmente `AUDITORIA.md` existe en `main` por el commit `e1f09b0`, mientras que `origin/develop` no contiene ese archivo. El equipo requiere que la auditoría se centralice en `develop` para iterar con menor riesgo sobre producción.

## Goals / Non-Goals

**Goals:**
- Trasladar `AUDITORIA.md` a `develop` preservando autoría e historial del contenido.
- Limpiar `main` para que no mantenga ese archivo.
- Dejar evidencia verificable de que la operación fue correcta.

**Non-Goals:**
- Modificar el contenido de `AUDITORIA.md`.
- Cambiar estrategia general de branching del repositorio.
- Reescribir historial (no usar force-push ni reset destructivo).

## Decisions

1. Usar `git cherry-pick e1f09b0` en `develop` para portar exactamente el commit del archivo.
   - Alternativa considerada: copiar archivo manualmente en `develop`.
   - Razón para descartar alternativa: se pierde vínculo directo con el commit original y se duplica trabajo.

2. Usar `git revert e1f09b0` en `main` para anular el cambio sin reescribir historial.
   - Alternativa considerada: `git reset --hard` en `main`.
   - Razón para descartar alternativa: es destructivo y rompe trazabilidad compartida.

3. Validar resultado con inspección explícita de árbol por rama (`git ls-tree`).
   - Alternativa considerada: validar sólo con `git status` local.
   - Razón para descartar alternativa: `git status` no confirma estado entre ramas remotas.

## Risks / Trade-offs

- [Riesgo] Conflicto al hacer `cherry-pick` o `revert` si hubo cambios simultáneos en ramas -> Mitigación: ejecutar en ramas actualizadas y validar estado antes/después.
- [Riesgo] El commit `e1f09b0` podría contener más cambios además de `AUDITORIA.md` -> Mitigación: confirmar `git show --name-status e1f09b0` antes de aplicar.
- [Trade-off] Se agregan commits adicionales de corrección en historial -> Mitigación: usar mensajes claros que expliquen el motivo.
