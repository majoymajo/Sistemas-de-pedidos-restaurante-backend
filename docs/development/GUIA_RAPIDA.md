# Guia rapida

## 1) Levantar todo en modo real (recomendado)

```bash
cp .env.example .env
docker compose -f infrastructure/docker/docker-compose.yml up -d --build
```

Verificar estado:

```bash
docker compose -f infrastructure/docker/docker-compose.yml ps
```

## 2) URLs utiles

- Frontend cliente/cocina: `http://localhost:5173`
- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`
- RabbitMQ: `http://localhost:15672` (`guest/guest`)

## 3) Smoke test minimo

```bash
curl http://localhost:8080/menu

curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"tableId":5,"items":[{"productId":1,"quantity":2}]}'
```

## 4) Cocina (PIN/token)

Por defecto:
- Header: `X-Kitchen-Token`
- Token/PIN: `cocina123`

Consulta de pedidos de cocina:

```bash
curl "http://localhost:8080/orders?status=PENDING,IN_PREPARATION,READY" \
  -H "X-Kitchen-Token: cocina123"
```

## 5) Variables clave

Frontend:
- `VITE_USE_MOCK=false`
- `VITE_ALLOW_MOCK_FALLBACK=false`
- `VITE_API_BASE_URL=http://localhost:8080`

Backend:
- `KITCHEN_TOKEN_HEADER=X-Kitchen-Token`
- `KITCHEN_AUTH_TOKEN=cocina123`

## 6) Comandos de mantenimiento

```bash
# logs
docker compose -f infrastructure/docker/docker-compose.yml logs -f

# reiniciar stack
docker compose -f infrastructure/docker/docker-compose.yml down
docker compose -f infrastructure/docker/docker-compose.yml up -d --build

# limpiar volumenes
docker compose -f infrastructure/docker/docker-compose.yml down -v
```

## 7) Flujo OpenSpec (resumen)

```bash
openspec list --json
openspec status --change <change-name> --json
openspec instructions apply --change <change-name> --json
```

## 8) Troubleshooting rapido

- Frontend sin datos: validar `VITE_USE_MOCK=false` y `VITE_API_BASE_URL`.
- Cocina sin permisos: validar token/header entre frontend y backend.
- No llegan eventos: revisar `rabbitmq` y logs de `kitchen-worker`.
- Menu reducido: verificar migracion `V5__expand_menu_catalog.sql` en `order-service`.
