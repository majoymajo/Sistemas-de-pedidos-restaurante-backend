## Why

El proyecto ya tiene un frontend redisenado funcional, pero la ejecucion actual depende de modo mock para mostrar el catalogo completo y no representa un estado de produccion en `main`. Ademas, la documentacion de auditoria, calidad y deuda tecnica esta dispersa en multiples archivos, lo que dificulta la trazabilidad del reto y la mantenibilidad del repositorio.

## What Changes

- Entregar un release estable en `main` con flujo real end-to-end (frontend + order-service + kitchen-worker + rabbitmq + postgres) ejecutable por Docker Compose.
- Eliminar dependencia de mocks en entorno de produccion: el frontend en `main` debe consumir APIs reales por defecto.
- Expandir el catalogo de menu del backend para igualar la experiencia de frontend (items, precio, categoria e imagen), evitando menus reducidos al cambiar a modo real.
- Unificar criterios de configuracion y validacion operativa para cocina/ordenes (token de cocina, endpoints y smoke tests de integracion).
- Consolidar documentacion Markdown por dominio:
  - mantener separados los entregables clave del reto (auditoria, deuda tecnica, calidad),
  - fusionar extras redundantes dentro de un unico documento por dominio.
- Redisenar `README.md` como punto de entrada unico del repo, incorporando diagramas Mermaid, arquitectura, flujo de ejecucion y rutas de evidencia.
- Limpiar archivos obsoletos/duplicados y actualizar referencias internas para evitar links rotos.
- **BREAKING**: el comportamiento por defecto de frontend en `main` deja de usar mock data (`VITE_USE_MOCK=false`), por lo que requiere backend disponible.

## Capabilities

### New Capabilities
- `production-release-readiness`: define criterios de release a `main` con stack real, smoke tests y validacion de endpoints operativos.
- `docs-consolidation-governance`: define estructura documental objetivo, reglas de consolidacion y trazabilidad entre auditoria, deuda tecnica y calidad.
- `readme-architecture-experience`: establece `README.md` como entrada canonica con Mermaid, quickstart y mapa de documentos.

### Modified Capabilities
- `frontend-mockdata`: el mock se mantiene solo como soporte de desarrollo/pruebas locales y no como comportamiento por defecto en produccion.
- `frontend-redesign`: la experiencia visual redisenada debe operar con catalogo real del backend en `main`, no solo con datos en memoria.
- `audit-report-standard`: se amplia para exigir vinculos explicitos entre hallazgos, deuda tecnica y evidencias de calidad/pruebas.
- `audit-phase-1-diagnostic`: se extiende para cerrar fase con evidencia ejecutada y pendiente residual documentada para siguientes fases.

## Impact

- Frontend: `src/api/*`, configuracion de entorno, manejo de fallback mock/API real y validaciones de flujo cliente/cocina.
- Backend (`order-service`): exposicion de catalogo expandido y consistencia de datos para menu/ordenes en ejecucion real.
- Infraestructura: `infrastructure/docker/docker-compose.yml` y variables de entorno para ejecucion productiva reproducible.
- Documentacion: `README.md`, `docs/auditoria/*`, `docs/quality/*`, `docs/development/*` y depuracion de Markdown redundante.
- Flujo de entrega: merge controlado `develop -> main` con evidencia de verificacion antes de push final.
