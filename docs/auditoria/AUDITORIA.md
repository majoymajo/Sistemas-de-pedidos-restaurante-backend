# Auditoria tecnica fase 1 (consolidado)

## Metadatos

- Fecha de consolidacion: 2026-02-16
- Snapshot base post-MVP: `51b8f5d`
- Rama de trabajo de consolidacion: `develop`
- Objetivo: mantener un unico reporte canonico de auditoria con trazabilidad hacia calidad y deuda tecnica.

## Alcance de fase 1

Componentes auditados:
- Frontend React (cliente y cocina)
- Order Service (Spring Boot)
- Kitchen Worker (Spring Boot)
- Integracion entre servicios (REST, RabbitMQ, DB)

Criterios evaluados:
- Principios SOLID (SRP, DIP)
- Code smells (acoplamiento, duplicacion, falta de abstraccion)
- Riesgo operativo (seguridad, consistencia de eventos, confiabilidad)

## Formato estandar por hallazgo

Cada hallazgo mantiene:
- Componente afectado
- Evidencia tecnica reproducible
- Principio vulnerado o smell
- Impacto
- Recomendacion
- Estado de remediacion

## Hallazgos consolidados

| ID | Componente | Evidencia tecnica | Principio/smell | Impacto | Recomendacion | Estado |
|---|---|---|---|---|---|---|
| H-ALTA-01 | `order-service` | `order-service/src/main/java/com/restaurant/orderservice/service/OrderService.java` | SRP + God Class + riesgo N+1 | Alto | Separar validacion, mapeo y comandos de publicacion | Pagado |
| H-ALTA-02 | Integracion eventos | `order-service/src/main/java/com/restaurant/orderservice/service/OrderService.java`, `order-service/src/main/java/com/restaurant/orderservice/infrastructure/messaging/RabbitOrderPlacedEventPublisher.java` | Consistencia eventual sin garantias explicitas | Alto | Comando de publicacion con manejo de error y transaccion coherente | Pagado |
| H-ALTA-03 | Arquitectura de datos | `infrastructure/docker/docker-compose.yml`, `kitchen-worker/src/main/resources/application.yml` | Acoplamiento por DB compartida | Alto | Separar bases por servicio (restaurant_db / kitchen_db) | Pagado |
| H-ALTA-04 | Contrato frontend-backend | `src/api/contracts.ts`, `order-service/src/main/java/com/restaurant/orderservice/dto/OrderItemRequest.java` | Inconsistencia de tipos (`productId`) | Alto | Alinear contrato a `number/Long` extremo a extremo | Pagado |
| H-ALTA-05 | Seguridad cocina | `src/pages/kitchen/KitchenLoginPage.tsx`, `src/api/http.ts`, `order-service/src/main/resources/application.yml` | Control de acceso incompleto | Alto | Forzar header/token en endpoints de cocina y alinear PIN/token | Pagado |
| H-ALTA-06 | Arquitectura global | Paquetes de servicio/controlador/infraestructura en backend | Falta de fronteras por capas | Alto | Mantener separacion en capas y puertos/comandos | Pagado parcial (base aplicada) |
| H-MEDIA-01 | `kitchen-worker` | `kitchen-worker/src/main/java/com/restaurant/kitchenworker/listener/OrderEventListener.java`, `kitchen-worker/src/main/java/com/restaurant/kitchenworker/service/OrderProcessingService.java` | DIP debilitado por field injection | Medio | Constructor injection + pruebas ajustadas | Pagado |
| H-MEDIA-02 | Frontend cocina | `src/pages/kitchen/KitchenBoardPage.tsx` | SRP debil en componente UI | Medio | Separar polling, acciones y detalle en unidades claras | Pagado parcial |
| H-MEDIA-03 | Contrato de evento | Publicacion/consumo `order.placed` | Resiliencia de contrato | Medio | Validacion de payload, versionado y control de idempotencia | Pagado parcial |
| H-BAJA-01 | Documentacion operativa | docs de workflow previas | Drift documental | Bajo | Consolidar docs canonicas y limpiar duplicados | Pagado |
| H-BAJA-02 | Calidad no funcional | cobertura/observabilidad/hardening | Riesgo de escalado | Medio | Plan de observabilidad y cobertura minima por sprint | Pendiente |

## Evidencia consolidada de remediacion

Implementaciones verificadas en este corte:
- Catalogo expandido real via backend (`GET /menu` con 16 items, precio, categoria, imagen)
- Frontend en modo real por defecto (`VITE_USE_MOCK=false`) con fallback solo explicito
- Seguridad de cocina alineada por token/header entre frontend y backend
- Polling estable de cocina y acciones de cambio de estado con API real
- Stack docker estable con RabbitMQ, Postgres y servicios Spring activos

Evidencia tecnica de ejecucion:
- `docker compose -f infrastructure/docker/docker-compose.yml up -d --build`
- `GET /menu` responde catalogo expandido
- `POST /orders` crea pedido
- `PATCH /orders/{id}/status` funciona con `X-Kitchen-Token`
- `kitchen-worker` registra consumo de `order.placed` en logs

## Trazabilidad cruzada

- Deuda tecnica consolidada: `docs/quality/DEUDA_TECNICA.md`
- Evidencia de pruebas y release gate: `docs/quality/CALIDAD.md`
- Cambio OpenSpec aplicado: `openspec/changes/main-production-readiness-doc-consolidation/`

## Riesgo residual

Pendientes de siguiente iteracion:
- H-BAJA-02: hardening no funcional (observabilidad, cobertura minima global, controles de abuso)
- Completar separacion de responsabilidades en algunos componentes frontend de cocina

## Cierre de fase 1

La auditoria queda consolidada en un unico archivo canonico y con enlace directo a:
- evidencias de calidad ejecutadas,
- backlog de deuda tecnica,
- artefactos OpenSpec del cambio aplicado.
