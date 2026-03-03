## Context

La rama `feature/auditoria-fase-1-ejecucion` ya contiene mejoras para seguridad (CoR) y publicacion de eventos (Command), pero persisten tres hallazgos abiertos:

- H-ALTA-03: `order-service` y `kitchen-worker` comparten base/tablas (`restaurant_db.orders`).
- H-ALTA-06: las fronteras dominio/aplicacion/infraestructura no son explicitas en flujos criticos.
- H-MEDIA-03: el contrato de eventos no tiene versionado/validacion robusta ante evolucion.

El objetivo es cerrar estos hallazgos sin romper el flujo actual `POST /orders` -> `order.placed` -> `kitchen-worker`.

Restricciones:
- Mantener compatibilidad con Docker Compose local.
- No introducir dependencias operativas complejas (Kafka, schema registry, etc.).
- Preservar cambios ya integrados por el equipo en esta rama.

## Goals / Non-Goals

**Goals:**
- Desacoplar persistencia entre servicios (patron `database-per-service`).
- Hacer explicitas capas minimas en el flujo de eventos usando `Ports and Adapters`.
- Definir contrato de evento versionado (`v1`) con validacion/fallback en consumo.
- Mantener pruebas unitarias y arranque de contexto en ambos modulos.

**Non-Goals:**
- Rediseñar completamente todos los paquetes del backend.
- Implementar outbox transaccional completo o CDC en esta iteracion.
- Introducir broker adicional o cambiar RabbitMQ.

## Decisions

### 1) Persistencia aislada para `kitchen-worker`
- Decision: mover `kitchen-worker` a DB propia (`kitchen_db`) y tabla propia (`kitchen_orders`).
- Rationale: elimina acoplamiento fisico directo con el modelo interno de `order-service`.
- Alternativas consideradas:
  - Mantener misma DB pero distinto schema: mejora parcial, pero sigue dependencia operativa compartida.
  - Mantener igual y solo documentar: no cierra H-ALTA-03.

### 2) Capas explicitas para integracion de eventos
- Decision: introducir frontera por puerto de salida en `order-service`:
  - Dominio: evento de dominio de pedido.
  - Aplicacion: comando que publica via puerto.
  - Infraestructura: adaptador RabbitMQ que serializa contrato.
- Rationale: reduce mezcla de reglas de negocio con detalles de transporte.
- Alternativas consideradas:
  - Dejar `OrderService` publicando directo: mas simple pero mantiene H-ALTA-06.
  - Refactor total hexagonal de todo el servicio: muy costoso para esta fase.

### 3) Contrato de evento resiliente y evolutivo
- Decision: usar envelope con metadata (`eventId`, `eventType`, `eventVersion`, `occurredAt`) + payload (`orderId`, `tableId`, `items`, `createdAt`), tolerando campos extra y validando requeridos.
- Rationale: mejora trazabilidad, versionado y tolerancia a evolucion del mensaje.
- Alternativas consideradas:
  - Mantener payload plano: bajo costo, pero fragil ante cambios de contrato.
  - Integrar schema registry: robusto, pero fuera de alcance tecnico actual.

## Risks / Trade-offs

- [Mayor complejidad inicial en paquetes] -> Mitigacion: aplicar solo en flujo `order.placed` como vertical slice.
- [Cambios de infraestructura local (segunda DB)] -> Mitigacion: defaults claros en `docker-compose.yml` y `application.yml`.
- [Mensajes invalidos/version no soportada] -> Mitigacion: rechazo controlado y envio a DLQ para analisis.
- [Posible drift entre contrato productor/consumidor] -> Mitigacion: compartir estructura `v1` explicita y tests de unidad.

## Migration Plan

1. Actualizar configuraciones de DB para `kitchen-worker` (compose + application).
2. Crear migracion propia de `kitchen-worker` para `kitchen_orders`.
3. Introducir clases de dominio/aplicacion/infra para publicacion de evento en `order-service`.
4. Introducir contrato versionado y validador en `kitchen-worker`.
5. Ajustar tests unitarios/contexto.
6. Verificar con `mvn test` por modulo y arranque en compose.

Rollback:
- Revertir commit de esta iteracion; servicios vuelven al contrato y DB previos.

## Open Questions

- En Fase 3, ¿se implementara outbox para garantizar atomicidad DB + evento?
- ¿Se estandarizara un catalogo de versiones/event-types para todos los eventos futuros?
- ¿Se requiere exponer metricas de DLQ para monitoreo continuo?
