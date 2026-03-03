# 🔍 Análisis de Valores Hardcodeados - Sistema de Pedidos

## ❌ Problemas Encontrados

### 1. **order-service/src/main/resources/application.yml**

#### Valores Hardcodeados:
```yaml
server:
  port: 8080  # ❌ HARDCODED

rabbitmq:
  exchange:
    name: order.exchange  # ❌ HARDCODED
  routing-key:
    order-placed: order.placed  # ❌ HARDCODED

security:
  kitchen:
    token-value: ${KITCHEN_AUTH_TOKEN:1234}  # ❌ Fallback incorrecto (debería ser cocina123)
```

**Impacto:**
- No se puede cambiar el puerto sin modificar el código
- Los nombres de exchange/routing-key no son configurables
- El token de fallback (`1234`) no coincide con el .env (`cocina123`)

---

### 2. **kitchen-worker/src/main/resources/application.yml**

#### Valores Hardcodeados:
```yaml
server:
  port: 8081  # ❌ HARDCODED

rabbitmq:
  exchange:
    name: order.exchange  # ❌ HARDCODED
  queue:
    name: order.placed.queue  # ❌ HARDCODED
  routing-key:
    order-placed: order.placed  # ❌ HARDCODED
  dlq:
    name: order.placed.dlq  # ❌ HARDCODED
    exchange: order.dlx  # ❌ HARDCODED
```

**Impacto:**
- No se puede cambiar configuración de RabbitMQ sin modificar el código
- Dificulta tener múltiples entornos (dev, staging, prod)

---

### 3. **kitchen-worker/src/main/java/com/restaurant/kitchenworker/config/RabbitMQConfig.java**

#### Valores Hardcodeados (Líneas 78 y 125):
```java
.withArgument("x-dead-letter-routing-key", "order.placed.failed")  // ❌ HARDCODED línea 78

.with("order.placed.failed");  // ❌ HARDCODED línea 125
```

**Impacto:**
- El routing key de DLQ no es configurable

---

### 4. **report-service/src/main/resources/application.yml**

#### Valores Hardcodeados:
```yaml
server:
  port: 8082  # ❌ HARDCODED

rabbitmq:
  exchange:
    name: order.exchange  # ❌ HARDCODED
  queue:
    name: order.placed.report.queue  # ❌ HARDCODED
  routing-key:
    order-placed: order.placed  # ❌ HARDCODED
  dlq:
    name: order.placed.report.dlq  # ❌ HARDCODED
    exchange: order.report.dlx  # ❌ HARDCODED
```

---

### 5. **order-service/src/main/java/com/restaurant/orderservice/config/WebConfig.java**

#### CORS Hardcodeado (Línea 29):
```java
.allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")  // ❌ HARDCODED
```

**Impacto:**
- Los orígenes permitidos para desarrollo están hardcodeados
- No se puede cambiar el puerto del frontend sin modificar el código

---

### 6. **.env.example Incompleto**

#### Variables Faltantes:
```bash
# Faltan:
- SERVER_PORT (order-service)
- KITCHEN_WORKER_PORT
- REPORT_SERVICE_PORT
- RABBITMQ_EXCHANGE_NAME
- RABBITMQ_ROUTING_KEY_ORDER_PLACED
- RABBITMQ_DLQ_ROUTING_KEY
- RABBITMQ_DLQ_NAME
- RABBITMQ_DLX_NAME
- CORS_ALLOWED_ORIGINS (para desarrollo)
- FRONTEND_PORT (para CORS)
```

---

## ✅ Solución Propuesta

### Cambios a Aplicar:

1. **Actualizar application.yml de cada servicio** - Usar variables de entorno
2. **Actualizar RabbitMQConfig.java** - Externalizar routing keys de DLQ
3. **Actualizar WebConfig.java** - Externalizar orígenes CORS de desarrollo
4. **Actualizar .env.example** - Agregar todas las variables faltantes
5. **Actualizar docker-compose.yml** - Pasar las nuevas variables

---

## 📝 Variables de Entorno Recomendadas

### .env.example Completo:

```bash
# ========================================
# FRONTEND
# ========================================
VITE_USE_MOCK=false
VITE_ALLOW_MOCK_FALLBACK=false
VITE_API_BASE_URL=http://localhost:8080
VITE_KITCHEN_TOKEN_HEADER=X-Kitchen-Token
VITE_KITCHEN_PIN=cocina123

# ========================================
# ORDER SERVICE
# ========================================
SERVER_PORT=8080
DB_URL=jdbc:postgresql://postgres:5432/restaurant_db
DB_USER=restaurant_user
DB_PASS=restaurant_pass
KITCHEN_TOKEN_HEADER=X-Kitchen-Token
KITCHEN_AUTH_TOKEN=cocina123

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173
CORS_ALLOWED_ORIGIN_PATTERNS=

# ========================================
# KITCHEN WORKER
# ========================================
KITCHEN_WORKER_PORT=8081
KITCHEN_DB_URL=jdbc:postgresql://kitchen-postgres:5432/kitchen_db
KITCHEN_DB_USER=kitchen_user
KITCHEN_DB_PASS=kitchen_pass

# ========================================
# REPORT SERVICE
# ========================================
REPORT_SERVICE_PORT=8082
REPORT_DB_URL=jdbc:postgresql://report-postgres:5432/report_db
REPORT_DB_USER=report_user
REPORT_DB_PASS=report_pass

# ========================================
# RABBITMQ CONFIGURATION
# ========================================
RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASS=guest

# Exchange and Routing Keys
RABBITMQ_EXCHANGE_NAME=order.exchange
RABBITMQ_ROUTING_KEY_ORDER_PLACED=order.placed
RABBITMQ_DLQ_ROUTING_KEY=order.placed.failed

# Kitchen Worker Queues
RABBITMQ_KITCHEN_QUEUE_NAME=order.placed.queue
RABBITMQ_KITCHEN_DLQ_NAME=order.placed.dlq
RABBITMQ_KITCHEN_DLX_NAME=order.dlx

# Report Service Queues
RABBITMQ_REPORT_QUEUE_NAME=order.placed.report.queue
RABBITMQ_REPORT_DLQ_NAME=order.placed.report.dlq
RABBITMQ_REPORT_DLX_NAME=order.report.dlx

# ========================================
# POSTGRES DATABASES
# ========================================
POSTGRES_DB=restaurant_db
POSTGRES_USER=restaurant_user
POSTGRES_PASSWORD=restaurant_pass

KITCHEN_POSTGRES_DB=kitchen_db
KITCHEN_POSTGRES_USER=kitchen_user
KITCHEN_POSTGRES_PASSWORD=kitchen_pass

REPORT_POSTGRES_DB=report_db
REPORT_POSTGRES_USER=report_user
REPORT_POSTGRES_PASSWORD=report_pass
```

---

## 🎯 Beneficios de la Corrección

1. **Flexibilidad**: Cambiar configuración sin tocar el código
2. **Múltiples Entornos**: Fácil configuración para dev/staging/prod
3. **Seguridad**: Tokens y credenciales en variables de entorno
4. **Consistencia**: Todos los servicios usan el mismo patrón
5. **Documentación**: .env.example documenta todas las variables necesarias
6. **DevOps Friendly**: Compatible con CI/CD y orquestadores (Kubernetes, Docker Swarm)

---

## 🚨 Prioridad de Correcciones

### 🔴 Alta Prioridad (Seguridad):
- ✅ KITCHEN_AUTH_TOKEN fallback incorrecto
- ✅ CORS origins hardcodeados

### 🟡 Media Prioridad (Flexibilidad):
- ✅ Server ports hardcodeados
- ✅ RabbitMQ exchange/routing keys hardcodeados

### 🟢 Baja Prioridad (Mejora):
- ✅ DLQ routing keys hardcodeados en código Java

---

## 📋 Checklist de Implementación

- [ ] Actualizar .env.example con todas las variables
- [ ] Actualizar application.yml de order-service
- [ ] Actualizar application.yml de kitchen-worker
- [ ] Actualizar application.yml de report-service
- [ ] Actualizar RabbitMQConfig.java (kitchen-worker)
- [ ] Actualizar RabbitMQConfig.java (report-service)
- [ ] Actualizar WebConfig.java (order-service)
- [ ] Actualizar docker-compose.yml para pasar las nuevas variables
- [ ] Actualizar .env local
- [ ] Probar todos los servicios con las nuevas variables
- [ ] Actualizar documentación (README.md, GUIA_RAPIDA.md)

---

**Fecha de Análisis:** 19 de Febrero, 2026  
**Analizado por:** Copilot Orchestrator
