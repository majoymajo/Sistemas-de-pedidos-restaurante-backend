# 📡 Guía de Endpoints y Bases de Datos

## 🗄️ Bases de Datos PostgreSQL

El sistema utiliza **3 bases de datos PostgreSQL separadas** (arquitectura de microservicios):

### 1. restaurant_db (Order Service)
```yaml
Host: localhost
Puerto: 5432
Base de datos: restaurant_db
Usuario: restaurant_user
Contraseña: restaurant_pass
```

**Tablas principales:**
- `orders` - Pedidos realizados
- `menu_items` - Productos del menú
- `tables` - Mesas del restaurante
- `order_items` - Items de cada pedido

### 2. kitchen_db (Kitchen Worker)
```yaml
Host: localhost
Puerto: 5433
Base de datos: kitchen_db
Usuario: kitchen_user
Contraseña: kitchen_pass
```

**Tablas principales:**
- `kitchen_orders` - Pedidos procesados por cocina
- `order_items` - Items de pedidos
- `processing_logs` - Logs de procesamiento

### 3. report_db (Report Service)
```yaml
Host: localhost
Puerto: 5434
Base de datos: report_db
Usuario: report_user
Contraseña: report_pass
```

---

## 🔌 Conectarse a las Bases de Datos

### Opción 1: Línea de comandos (psql)

```bash
# Restaurant DB
psql -h localhost -p 5432 -U restaurant_user -d restaurant_db
# Contraseña: restaurant_pass

# Kitchen DB
psql -h localhost -p 5433 -U kitchen_user -d kitchen_db
# Contraseña: kitchen_pass

# Report DB
psql -h localhost -p 5434 -U report_user -d report_db
# Contraseña: report_pass
```

### Opción 2: DBeaver / DataGrip / PgAdmin

**Restaurant DB:**
- Type: PostgreSQL
- Host: localhost
- Port: 5432
- Database: restaurant_db
- Username: restaurant_user
- Password: restaurant_pass

**Kitchen DB:**
- Type: PostgreSQL
- Host: localhost
- Port: 5433
- Database: kitchen_db
- Username: kitchen_user
- Password: kitchen_pass

**Report DB:**
- Type: PostgreSQL
- Host: localhost
- Port: 5434
- Database: report_db
- Username: report_user
- Password: report_pass

### Opción 3: Docker Exec

```bash
# Conectar directamente al contenedor
docker exec -it restaurant-postgres psql -U restaurant_user -d restaurant_db

docker exec -it kitchen-postgres psql -U kitchen_user -d kitchen_db

docker exec -it report-postgres psql -U report_user -d report_db
```

---

## 🌐 Servicios y URLs

| Servicio | Puerto | URL Base | Swagger/API Docs |
|----------|--------|----------|------------------|
| **Order Service** | 8080 | http://localhost:8080 | http://localhost:8080/swagger-ui.html |
| **Report Service** | 8082 | http://localhost:8082 | http://localhost:8082/swagger-ui.html |
| **Kitchen Worker** | - | (No expone HTTP) | - |
| **Frontend** | 5173 | http://localhost:5173 | - |
| **RabbitMQ Admin** | 15672 | http://localhost:15672 | guest/guest |

⚠️ **Importante:** El Kitchen Worker NO expone endpoints HTTP, solo consume eventos de RabbitMQ.

---

## 📋 Endpoints Disponibles

### 🍽️ Order Service (http://localhost:8080)

#### 1. **Obtener Menú**
```http
GET http://localhost:8080/menu
```

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "name": "Empanadas criollas",
    "description": "Empanadas de carne con salsa casera.",
    "price": 450,
    "category": "entradas",
    "imageUrl": "https://images.unsplash.com/photo-1603360946369-dc9bb6258143",
    "isActive": true
  },
  {
    "id": 5,
    "name": "Bife de chorizo",
    "description": "Corte premium con papas rusticas.",
    "price": 1850,
    "category": "principales",
    "imageUrl": "https://images.unsplash.com/photo-1558030006-450675393462",
    "isActive": true
  }
]
```

---

#### 2. **Crear Pedido**
```http
POST http://localhost:8080/orders
Content-Type: application/json
```

**Request Body:**
```json
{
  "tableId": 5,
  "items": [
    {
      "productId": 1,
      "quantity": 2,
      "note": "Sin cebolla"
    },
    {
      "productId": 3,
      "quantity": 1
    }
  ]
}
```

**Response 201 Created:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "tableId": 5,
  "status": "PENDING",
  "items": [
    {
      "id": 1,
      "productId": 1,
      "quantity": 2,
      "note": "Sin cebolla"
    },
    {
      "id": 2,
      "productId": 3,
      "quantity": 1,
      "note": null
    }
  ],
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

**Validaciones:**
- `tableId`: debe ser entre 1 y 12
- `items`: mínimo 1 item
- `productId`: debe existir y estar activo

---

#### 3. **Obtener Pedido por ID**
```http
GET http://localhost:8080/orders/{id}
```

**Ejemplo:**
```http
GET http://localhost:8080/orders/550e8400-e29b-41d4-a716-446655440000
```

**Response 200 OK:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "tableId": 5,
  "status": "IN_PREPARATION",
  "items": [
    {
      "id": 1,
      "productId": 1,
      "quantity": 2,
      "note": "Sin cebolla"
    }
  ],
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:35:00"
}
```

---

#### 4. **Listar Todos los Pedidos**
```http
GET http://localhost:8080/orders
```

**Response 200 OK:**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "tableId": 5,
    "status": "PENDING",
    "items": [...],
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  },
  {
    "id": "660e8400-e29b-41d4-a716-446655440001",
    "tableId": 3,
    "status": "IN_PREPARATION",
    "items": [...],
    "createdAt": "2024-01-15T10:25:00",
    "updatedAt": "2024-01-15T10:28:00"
  }
]
```

---

#### 5. **Filtrar Pedidos por Estado** (Cocina)
```http
GET http://localhost:8080/orders?status=PENDING,IN_PREPARATION,READY
X-Kitchen-Token: cocina123
```

**Parámetros:**
- `status` (opcional): Lista separada por comas
  - Valores válidos: `PENDING`, `IN_PREPARATION`, `READY`

**Ejemplos:**
```http
# Solo pedidos pendientes
GET http://localhost:8080/orders?status=PENDING
X-Kitchen-Token: cocina123

# Pedidos en preparación y listos
GET http://localhost:8080/orders?status=IN_PREPARATION,READY
X-Kitchen-Token: cocina123
```

⚠️ **Requiere autenticación de cocina** (header `X-Kitchen-Token`)

---

#### 6. **Actualizar Estado de Pedido**
```http
PATCH http://localhost:8080/orders/{id}/status
Content-Type: application/json
X-Kitchen-Token: cocina123
```

**Request Body:**
```json
{
  "status": "READY"
}
```

**Ejemplo completo:**
```http
PATCH http://localhost:8080/orders/550e8400-e29b-41d4-a716-446655440000/status
Content-Type: application/json
X-Kitchen-Token: cocina123

{
  "status": "IN_PREPARATION"
}
```

**Estados válidos:**
- `PENDING` → `IN_PREPARATION`
- `IN_PREPARATION` → `READY`
- También permite retroceder un estado

**Response 200 OK:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "tableId": 5,
  "status": "READY",
  "items": [...],
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:45:00"
}
```

---

#### 7. **Eliminar un Pedido** (Cocina)
```http
DELETE http://localhost:8080/orders/{id}
X-Kitchen-Token: cocina123
```

**Ejemplo:**
```http
DELETE http://localhost:8080/orders/550e8400-e29b-41d4-a716-446655440000
X-Kitchen-Token: cocina123
```

**Response:** 204 No Content

---

#### 8. **Eliminar Todos los Pedidos** (Cocina)
```http
DELETE http://localhost:8080/orders
X-Kitchen-Token: cocina123
```

**Response:** 204 No Content

⚠️ **Cuidado:** Esto borra TODOS los pedidos. Útil para resetear el sistema.

---

### 📊 Report Service (http://localhost:8082)

#### 1. **Obtener Reporte**
```http
GET http://localhost:8082/reports?startDate=2024-01-01&endDate=2024-01-31
```

⚠️ **Nota:** Este endpoint está en desarrollo (retorna `501 Not Implemented`).

---

## 🧪 Colección de Postman

### Configuración de Variables de Entorno

Crea un entorno en Postman con estas variables:

```json
{
  "base_url": "http://localhost:8080",
  "kitchen_token": "cocina123",
  "order_id": ""
}
```

### Ejemplos Paso a Paso

#### Test 1: Flujo Completo de Pedido

1. **Obtener menú disponible**
```http
GET {{base_url}}/menu
```

2. **Crear un pedido nuevo**
```http
POST {{base_url}}/orders
Content-Type: application/json

{
  "tableId": 5,
  "items": [
    {"productId": 1, "quantity": 2}
  ]
}
```

3. **Guardar el `id` del pedido creado** (copiar de la respuesta)

4. **Consultar el pedido**
```http
GET {{base_url}}/orders/{{order_id}}
```

5. **Cocina cambia estado a "En preparación"**
```http
PATCH {{base_url}}/orders/{{order_id}}/status
Content-Type: application/json
X-Kitchen-Token: {{kitchen_token}}

{
  "status": "IN_PREPARATION"
}
```

6. **Cocina marca como listo**
```http
PATCH {{base_url}}/orders/{{order_id}}/status
Content-Type: application/json
X-Kitchen-Token: {{kitchen_token}}

{
  "status": "READY"
}
```

---

## 🔐 Autenticación

### Endpoints Públicos (sin autenticación)
- `GET /menu`
- `POST /orders`
- `GET /orders/{id}`
- `GET /orders` (sin filtros)

### Endpoints Protegidos (requieren token de cocina)
- `GET /orders?status=...` (con filtro)
- `PATCH /orders/{id}/status`
- `DELETE /orders/{id}`
- `DELETE /orders`

**Header requerido:**
```http
X-Kitchen-Token: cocina123
```

⚠️ **Configuración:** El token se define en las variables de entorno:
- Backend: `KITCHEN_AUTH_TOKEN`
- Frontend: `VITE_KITCHEN_PIN`

---

## 🐰 RabbitMQ - Panel de Administración

**URL:** http://localhost:15672

**Credenciales:**
- Usuario: `guest`
- Contraseña: `guest`

### Qué puedes ver:

1. **Exchanges:**
   - `restaurant.exchange` - Exchange principal

2. **Queues:**
   - `order.queue` - Cola de pedidos para kitchen-worker

3. **Mensajes:**
   - Ver eventos `order.placed` en tiempo real
   - Monitoring de mensajes procesados
   - Dead Letter Queue (DLQ) para errores

### Flujo de Eventos:

```
Order Service → restaurant.exchange → order.queue → Kitchen Worker
                     (routing key: order.placed)
```

---

## 📝 Queries Útiles para Bases de Datos

### Restaurant DB (restaurant_db)

```sql
-- Ver todos los pedidos
SELECT * FROM orders ORDER BY created_at DESC;

-- Ver items de un pedido específico
SELECT oi.*, mi.name, mi.price 
FROM order_items oi
JOIN menu_items mi ON oi.product_id = mi.id
WHERE oi.order_id = 'tu-uuid-aqui';

-- Ver menú activo
SELECT * FROM menu_items WHERE is_active = true;

-- Pedidos por mesa
SELECT * FROM orders WHERE table_id = 5 ORDER BY created_at DESC;

-- Contar pedidos por estado
SELECT status, COUNT(*) 
FROM orders 
GROUP BY status;
```

### Kitchen DB (kitchen_db)

```sql
-- Ver pedidos en cocina
SELECT * FROM kitchen_orders ORDER BY created_at DESC;

-- Ver logs de procesamiento
SELECT * FROM processing_logs ORDER BY processed_at DESC LIMIT 10;

-- Pedidos en preparación
SELECT * FROM kitchen_orders WHERE status = 'IN_PREPARATION';
```

---

## 🧰 Troubleshooting

### Error: "Connection refused" al llamar a la API

Verificar que el contenedor esté corriendo:
```bash
docker compose -f infrastructure/docker/docker-compose.yml ps
```

### Error: "Invalid kitchen token"

Verificar que el header sea exactamente:
```http
X-Kitchen-Token: cocina123
```

### No veo datos en kitchen_db

1. Verificar que RabbitMQ esté corriendo
2. Ver logs del kitchen-worker:
```bash
docker logs restaurant-kitchen-worker
```

3. Verificar que el evento se publicó en RabbitMQ (http://localhost:15672)

### Error: "Product not found"

Verificar que el producto existe y está activo:
```sql
SELECT * FROM menu_items WHERE id = 1 AND is_active = true;
```

---

## 📦 Colección Postman Completa (JSON)

Puedes importar esta colección en Postman:

```json
{
  "info": {
    "name": "Restaurant Order System",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "base_url",
      "value": "http://localhost:8080"
    },
    {
      "key": "kitchen_token",
      "value": "cocina123"
    }
  ],
  "item": [
    {
      "name": "Menu",
      "item": [
        {
          "name": "Get Menu",
          "request": {
            "method": "GET",
            "url": "{{base_url}}/menu"
          }
        }
      ]
    },
    {
      "name": "Orders",
      "item": [
        {
          "name": "Create Order",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"tableId\": 5,\n  \"items\": [\n    {\n      \"productId\": 1,\n      \"quantity\": 2\n    }\n  ]\n}"
            },
            "url": "{{base_url}}/orders"
          }
        },
        {
          "name": "Get Order by ID",
          "request": {
            "method": "GET",
            "url": "{{base_url}}/orders/{{order_id}}"
          }
        },
        {
          "name": "List All Orders",
          "request": {
            "method": "GET",
            "url": "{{base_url}}/orders"
          }
        },
        {
          "name": "Filter Orders (Kitchen)",
          "request": {
            "method": "GET",
            "header": [
              {
                "key": "X-Kitchen-Token",
                "value": "{{kitchen_token}}"
              }
            ],
            "url": {
              "raw": "{{base_url}}/orders?status=PENDING,IN_PREPARATION,READY",
              "query": [
                {
                  "key": "status",
                  "value": "PENDING,IN_PREPARATION,READY"
                }
              ]
            }
          }
        },
        {
          "name": "Update Order Status",
          "request": {
            "method": "PATCH",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              },
              {
                "key": "X-Kitchen-Token",
                "value": "{{kitchen_token}}"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"status\": \"IN_PREPARATION\"\n}"
            },
            "url": "{{base_url}}/orders/{{order_id}}/status"
          }
        },
        {
          "name": "Delete Order",
          "request": {
            "method": "DELETE",
            "header": [
              {
                "key": "X-Kitchen-Token",
                "value": "{{kitchen_token}}"
              }
            ],
            "url": "{{base_url}}/orders/{{order_id}}"
          }
        },
        {
          "name": "Delete All Orders",
          "request": {
            "method": "DELETE",
            "header": [
              {
                "key": "X-Kitchen-Token",
                "value": "{{kitchen_token}}"
              }
            ],
            "url": "{{base_url}}/orders"
          }
        }
      ]
    }
  ]
}
```

**Para importar:**
1. Abre Postman
2. Click en "Import"
3. Pega el JSON anterior
4. Click en "Import"

---

## ✅ Checklist de Verificación

Antes de testear, asegúrate de:

- [ ] Docker Desktop está corriendo
- [ ] Contenedores levantados: `docker compose -f infrastructure/docker/docker-compose.yml ps`
- [ ] Order Service responde: `curl http://localhost:8080/menu`
- [ ] RabbitMQ accesible: http://localhost:15672
- [ ] Frontend accesible: http://localhost:5173
- [ ] Variables de entorno configuradas (`.env` existe)

---

¿Necesitas ayuda con algún endpoint específico o con la conexión a alguna base de datos?
