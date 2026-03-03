## Why

El archivo `AUDITORIA.md` fue creado en `main` pero el flujo de trabajo del equipo define que este tipo de documentación debe nacer y mantenerse en `develop`. Corregirlo ahora evita divergencias de historial y confusión durante la auditoría colaborativa con IA.

## What Changes

- Mover el contenido de `AUDITORIA.md` desde `main` hacia `develop` mediante `cherry-pick` del commit que lo creó.
- Revertir en `main` el commit que añadió `AUDITORIA.md` para dejar esa rama en el estado previo a ese archivo.
- Documentar criterios de validación para confirmar que `AUDITORIA.md` queda sólo en `develop`.

## Capabilities

### New Capabilities
- `audit-document-branch-governance`: Define el comportamiento esperado para ubicación y trazabilidad del documento de auditoría entre ramas de trabajo.

### Modified Capabilities
- Ninguna.

## Impact

- Ramas Git: `main` y `develop`.
- Historial Git: un `cherry-pick` en `develop` y un `revert` en `main`.
- Archivo afectado: `AUDITORIA.md`.
