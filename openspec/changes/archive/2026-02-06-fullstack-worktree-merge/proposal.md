## Why

Necesitamos una forma controlada y reversible de probar la integracion full-stack (frontend + backend) sin mezclar ramas ni perder trazabilidad. Los worktrees permiten ejecutar ambas ramas en paralelo y validar compatibilidad local antes de integrar de forma permanente.

## What Changes

- Definir el flujo de trabajo con worktrees para ejecutar frontend y backend en paralelo.
- Documentar comandos de setup y validacion para probar la integracion local.
- Establecer convenciones para ramas base/compare durante la prueba.

## Capabilities

### New Capabilities
- `fullstack-worktree`: Flujo local con worktrees para ejecutar front y back simultaneamente y validar integracion.

### Modified Capabilities
- (none)

## Impact

- Documentacion de trabajo (OpenSpec artifacts).
- Flujo local de desarrollo para pruebas de integracion.
