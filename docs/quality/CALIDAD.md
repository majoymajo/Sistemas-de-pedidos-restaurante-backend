# Calidad y validacion (consolidado)

## Metadatos

- Fecha de corte: 2026-02-16
- Rama: `develop`
- Objetivo: dejar evidencia de calidad para promotion a `main` en modo API real.

## Estrategia de calidad

Capas verificadas:
- Build frontend
- Tests backend (`order-service`)
- Tests worker (`kitchen-worker`)
- Stack completo con Docker Compose
- Smoke tests de endpoints criticos y flujo cliente/cocina

## Validaciones ejecutadas

### 1) Build frontend

Comando:
- `npm run build`

Resultado:
- Exitoso (sin errores de compilacion)

### 2) Tests de backend

Comando:
- `mvn -pl order-service test`

Resultado:
- `BUILD SUCCESS`
- `Tests run: 61, Failures: 0, Errors: 0, Skipped: 0`

### 3) Tests de kitchen-worker

Comando:
- `mvn -pl kitchen-worker test`

Resultado:
- `BUILD SUCCESS`
- `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`

### 4) Stack docker en modo real

Comando:
- `docker compose -f infrastructure/docker/docker-compose.yml up -d --build`

Servicios verificados en `Up`:
- `restaurant-frontend`
- `restaurant-order-service`
- `restaurant-kitchen-worker`
- `restaurant-postgres` (healthy)
- `kitchen-postgres` (healthy)
- `restaurant-rabbitmq` (healthy)

### 5) Smoke tests de API y flujo de cocina

Comandos (resumen):
- `GET /menu`
- `POST /orders`
- `GET /orders/{id}`
- `GET /orders?status=PENDING,IN_PREPARATION,READY` con `X-Kitchen-Token`
- `PATCH /orders/{id}/status` con `X-Kitchen-Token`

Resultado observado:
- `MENU_COUNT=16`
- `ORDER_STATUS_BEFORE=PENDING`
- `ORDER_STATUS_AFTER=IN_PREPARATION`
- `LIST_CONTAINS_ORDER=1`

### 6) Evidencia de mensajeria (RabbitMQ -> kitchen-worker)

En logs de `kitchen-worker` se verifico:
- Recepcion de `order.placed`
- Procesamiento correcto de evento
- Persistencia/actualizacion en `kitchen_db`

## Gate de release (pre-main)

Checklist de salida:
- [x] Frontend nuevo funcional en modo real (sin mock por defecto)
- [x] Backend con catalogo expandido (`/menu`)
- [x] Seguridad cocina por header/token
- [x] Flujo pedido -> evento -> cocina verificado
- [x] Documentacion consolidada por dominio
- [x] OpenSpec actualizado con avance de tareas

## Riesgo residual

Pendientes no bloqueantes para siguientes iteraciones:
- Observabilidad avanzada (dashboards y alertas)
- Cobertura global minima formal por modulo en CI
- Hardening adicional (rate limit, politicas de abuso)

## Trazabilidad

- Auditoria: `docs/auditoria/AUDITORIA.md`
- Deuda tecnica: `docs/quality/DEUDA_TECNICA.md`
- OpenSpec: `openspec/changes/main-production-readiness-doc-consolidation/`
