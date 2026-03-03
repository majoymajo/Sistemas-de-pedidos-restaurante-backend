## Why

Con los cambios recientes del equipo en `feature/auditoria-fase-1-ejecucion`, aún quedan pendientes tres hallazgos críticos/medios de Fase 1 (H-ALTA-03, H-ALTA-06 y H-MEDIA-03) que afectan desacoplamiento entre servicios, claridad de capas y resiliencia del contrato de eventos. Resolverlos ahora reduce riesgo de regresión y prepara la integración hacia `develop` con una arquitectura más sostenible.

## What Changes

- Desacoplar persistencia compartida entre `order-service` y `kitchen-worker` para eliminar dependencia de tabla `orders` compartida.
- Introducir estructura por capas más explícita en `order-service` (dominio/aplicación/infraestructura) empezando por el flujo de eventos de pedido.
- Fortalecer resiliencia y evolución del contrato de evento `order.placed` (versionado básico, validación y fallback controlado).
- Ajustar configuración Docker/servicios y pruebas para mantener el sistema funcional en esta rama.

## Capabilities

### New Capabilities
- `service-data-decoupling`: Aislar el modelo de datos de `kitchen-worker` respecto al esquema interno de `order-service`.
- `layered-order-architecture`: Definir y aplicar fronteras iniciales de dominio/aplicación/infraestructura en los flujos críticos de pedido.
- `resilient-event-contract`: Establecer contrato de evento más resiliente y evolutivo para integración entre servicios.

### Modified Capabilities
- `audit-phase-1-diagnostic`: Actualizar evidencias de cierre para los hallazgos H-ALTA-03, H-ALTA-06 y H-MEDIA-03 tras la implementación.

## Impact

- Código backend principal: `order-service/` y `kitchen-worker/`.
- Configuración y despliegue local: `docker-compose.yml`, `application.yml` y variables relacionadas.
- Pruebas: unitarias/integración de publicación/consumo de eventos y seguridad de contrato.
- Documentación: actualización de `AUDITORIA.md` con estado de mitigación de hallazgos.
