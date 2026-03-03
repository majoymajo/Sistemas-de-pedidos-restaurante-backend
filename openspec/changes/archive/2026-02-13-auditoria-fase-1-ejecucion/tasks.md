## 1. OpenSpec artifacts and technical design

- [x] 1.1 Crear `proposal.md` con alcance de mitigacion para H-ALTA-03, H-ALTA-06 y H-MEDIA-03.
- [x] 1.2 Crear `design.md` con decisiones de desacople de datos, capas y contrato resiliente.
- [x] 1.3 Crear delta specs para capacidades nuevas y modificadas.

## 2. Desacople de persistencia entre microservicios (H-ALTA-03)

- [x] 2.1 Separar infraestructura de base de datos para `kitchen-worker` en `docker-compose.yml`.
- [x] 2.2 Configurar `kitchen-worker` con variables `KITCHEN_DB_*` y migraciones Flyway propias.
- [x] 2.3 Migrar la proyeccion de cocina a tabla propia `kitchen_orders`.

## 3. Fronteras por capas para flujo de eventos (H-ALTA-06)

- [x] 3.1 Introducir puerto de salida de aplicacion para publicacion de eventos en `order-service`.
- [x] 3.2 Introducir adaptador de infraestructura RabbitMQ separado del dominio.
- [x] 3.3 Ajustar comando de publicacion para depender del puerto y no de detalle de transporte.

## 4. Resiliencia de contrato de eventos (H-MEDIA-03)

- [x] 4.1 Versionar contrato `order.placed` con envelope y metadatos de trazabilidad.
- [x] 4.2 Implementar validacion contractual y manejo de version no soportada en `kitchen-worker`.
- [x] 4.3 Enrutar mensajes invalidos a DLQ sin reintentos innecesarios (`AmqpRejectAndDontRequeueException`).

## 5. Pruebas y verificacion

- [x] 5.1 Actualizar y ampliar tests unitarios de producer/consumer para nuevo contrato.
- [x] 5.2 Ejecutar `mvn -pl order-service,kitchen-worker test` con resultado exitoso.
- [x] 5.3 Confirmar estado limpio, commit y push en `feature/auditoria-fase-1-ejecucion`.

## 6. Evidencia y documentacion de auditoria

- [x] 6.1 Registrar evidencia de implementacion para H-ALTA-03 en `docs/auditoria/`.
- [x] 6.2 Registrar evidencia de implementacion para H-ALTA-06 en `docs/auditoria/`.
- [x] 6.3 Registrar evidencia de implementacion para H-MEDIA-03 en `docs/auditoria/`.
- [x] 6.4 Actualizar `AUDITORIA.md` con estado de mitigacion y referencias de evidencia.
