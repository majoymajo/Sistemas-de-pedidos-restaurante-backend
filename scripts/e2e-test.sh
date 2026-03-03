#!/bin/bash
set -e

echo "=== PRUEBA E2E COMPLETA ==="

# 1. Obtener menú
echo -e "\n1. Obteniendo menú..."
curl -s http://localhost:8080/menu | jq '.'

# 2. Crear pedido
echo -e "\n2. Creando pedido..."
ORDER_ID=$(curl -s -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "tableId": 7,
    "items": [
      {"productId": 1, "quantity": 2, "note": "Sin cebolla"},
      {"productId": 2, "quantity": 1}
    ]
  }' | jq -r '.id')
echo "Order ID: $ORDER_ID"

# 3. Verificar estado inicial
echo -e "\n3. Verificando estado inicial..."
sleep 1
STATUS=$(curl -s http://localhost:8080/orders/$ORDER_ID | jq -r '.status')
echo "Estado: $STATUS"
[[ "$STATUS" == "PENDING" ]] && echo "✅ Estado inicial correcto"

# 4. Esperar procesamiento por Kitchen Worker
echo -e "\n4. Esperando procesamiento asíncrono..."
sleep 3

# 5. Verificar estado actualizado
echo -e "\n5. Verificando estado actualizado..."
STATUS=$(curl -s http://localhost:8080/orders/$ORDER_ID | jq -r '.status')
echo "Estado: $STATUS"
[[ "$STATUS" == "IN_PREPARATION" ]] && echo "✅ Procesado por Kitchen Worker"

# 6. Cocina marca como listo
echo -e "\n6. Actualizando a READY..."
curl -s -X PATCH http://localhost:8080/orders/$ORDER_ID/status \
  -H "Content-Type: application/json" \
  -d '{"status": "READY"}' | jq '.'

# 7. Verificar estado final
echo -e "\n7. Verificando estado final..."
STATUS=$(curl -s http://localhost:8080/orders/$ORDER_ID | jq -r '.status')
echo "Estado: $STATUS"
[[ "$STATUS" == "READY" ]] && echo "✅ Pedido listo"

echo -e "\n=== PRUEBA E2E COMPLETADA ✅ ==="
