#!/bin/bash
set -e

echo "=== SMOKE TEST COMPLETO ==="

# 1. Frontend
echo -n "Frontend (5173): "
curl -fsS http://localhost:5173 >/dev/null && echo "✅" || echo "❌"

# 2. Order Service
echo -n "Order Service (8080): "
curl -fsS http://localhost:8080/menu >/dev/null && echo "✅" || echo "❌"

# 3. Swagger UI
echo -n "Swagger UI: "
curl -fsS http://localhost:8080/swagger-ui.html >/dev/null && echo "✅" || echo "❌"

# 4. RabbitMQ Management
echo -n "RabbitMQ Management (15672): "
curl -fsS -u guest:guest http://localhost:15672/api/overview >/dev/null && echo "✅" || echo "❌"

# 5. PostgreSQL
echo -n "PostgreSQL (5432): "
docker exec restaurant-postgres pg_isready -q && echo "✅" || echo "❌"

# 6. Crear pedido de prueba
echo -n "Crear pedido: "
ORDER_ID=$(curl -fsS -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"tableId": 1, "items": [{"productId": 1, "quantity": 1}]}' \
  | jq -r '.id')
[[ -n "$ORDER_ID" ]] && echo "✅ ($ORDER_ID)" || echo "❌"

# 7. Verificar procesamiento
echo -n "Procesamiento asíncrono: "
sleep 3
STATUS=$(curl -fsS http://localhost:8080/orders/$ORDER_ID | jq -r '.status')
[[ "$STATUS" == "IN_PREPARATION" ]] && echo "✅" || echo "⚠️  (estado: $STATUS)"

echo "=== SMOKE TEST COMPLETADO ==="
