#!/bin/bash
set -e

echo "╔════════════════════════════════════════════╗"
echo "║  SUITE COMPLETA DE PRUEBAS DE CALIDAD     ║"
echo "╚════════════════════════════════════════════╝"

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Contadores
PASSED=0
FAILED=0

test_suite() {
  local name=$1
  local command=$2
  
  echo -e "\n${YELLOW}[TEST]${NC} $name"
  if eval $command; then
    echo -e "${GREEN}[✅ PASS]${NC} $name"
    ((PASSED++))
  else
    echo -e "${RED}[❌ FAIL]${NC} $name"
    ((FAILED++))
  fi
}

echo -e "\n📦 FASE 1: PRUEBAS UNITARIAS"
echo "════════════════════════════════════"

test_suite "Order Service - Pruebas Unitarias" \
  "cd order-service && mvn -q test"

test_suite "Kitchen Worker - Pruebas Unitarias" \
  "cd kitchen-worker && mvn -q test"

echo -e "\n🔧 FASE 2: INFRAESTRUCTURA"
echo "════════════════════════════════════"

test_suite "Docker Compose - Levantar servicios" \
  "docker-compose up -d && sleep 20"

test_suite "Smoke Test - Frontend" \
  "bash scripts/smoke.sh http://localhost:5173"

test_suite "Smoke Test - Backend" \
  "curl -fsS http://localhost:8080/menu >/dev/null"

echo -e "\n🔌 FASE 3: PRUEBAS DE INTEGRACIÓN"
echo "════════════════════════════════════"

test_suite "API - Obtener Menú" \
  "curl -fsS http://localhost:8080/menu | jq -e 'length > 0'"

test_suite "API - Crear Pedido" \
  "curl -fsS -X POST http://localhost:8080/orders \
    -H 'Content-Type: application/json' \
    -d '{\"tableId\": 5, \"items\": [{\"productId\": 1, \"quantity\": 1}]}' \
    | jq -e 'has(\"id\")'"

test_suite "Base de Datos - Verificar Tablas" \
  "docker exec restaurant-postgres psql -U restaurant_user -d restaurant_db \
    -c '\dt' | grep -q orders"

test_suite "RabbitMQ - Verificar Colas" \
  "docker exec restaurant-rabbitmq rabbitmqctl list_queues | grep -q orders"

echo -e "\n🔄 FASE 4: PRUEBAS FUNCIONALES"
echo "════════════════════════════════════"

# Crear pedido y guardar ID
ORDER_ID=$(curl -s -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"tableId": 7, "items": [{"productId": 1, "quantity": 1}]}' \
  | jq -r '.id')

test_suite "F-03: Pedido creado con UUID" \
  "[[ -n '$ORDER_ID' ]]"

test_suite "F-03: Estado inicial PENDING" \
  "curl -s http://localhost:8080/orders/$ORDER_ID | jq -e '.status == \"PENDING\"'"

sleep 3

test_suite "F-03: Kitchen Worker actualiza a IN_PREPARATION" \
  "curl -s http://localhost:8080/orders/$ORDER_ID | jq -e '.status == \"IN_PREPARATION\"'"

test_suite "F-07: Actualizar estado a READY" \
  "curl -fsS -X PATCH http://localhost:8080/orders/$ORDER_ID/status \
    -H 'Content-Type: application/json' \
    -d '{\"status\": \"READY\"}' | jq -e '.status == \"READY\"'"

echo -e "\n⚡ FASE 5: PRUEBAS NO FUNCIONALES"
echo "════════════════════════════════════"

test_suite "Rendimiento - Carga Ligera" \
  "ab -n 50 -c 5 -q http://localhost:8080/menu 2>&1 | grep -q 'Complete requests'"

test_suite "Resiliencia - Logs con orderId" \
  "docker-compose logs order-service | grep -q 'orderId'"

# Resumen final
echo -e "\n╔════════════════════════════════════════════╗"
echo -e "║          ${GREEN}RESUMEN DE PRUEBAS${NC}                 ║"
echo "╚════════════════════════════════════════════╝"
echo -e "Pruebas ejecutadas: $((PASSED + FAILED))"
echo -e "${GREEN}✅ Pasadas: $PASSED${NC}"
echo -e "${RED}❌ Fallidas: $FAILED${NC}"

if [ $FAILED -eq 0 ]; then
  echo -e "\n${GREEN}🎉 TODAS LAS PRUEBAS PASARON 🎉${NC}"
  exit 0
else
  echo -e "\n${RED}⚠️  ALGUNAS PRUEBAS FALLARON ⚠️${NC}"
  exit 1
fi
