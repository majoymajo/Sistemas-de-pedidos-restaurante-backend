# TEST_CASES_AI.md

**Matriz de Pruebas Generadas por Gema B**  
**Fecha:** Marzo 4, 2026  
**Alcance:** HDU-01 a HDU-08 del Sistema de Pedidos de Restaurante  
**Enfoque:** BDD Quality Engineering, Serenity BDD, Equivalence Partitioning, Boundary Value Analysis

---

## Matriz de Pruebas – HDU-01: Respuesta 201 Created con Header Location

### Resumen Analítico

| Concepto | Análisis |
|----------|----------|
| **Reglas de Negocio** | Toda creación exitosa de orden debe devolver estado 201 Created; el header Location debe contener la URI absoluta del recurso creado (`/orders/{id}`); la orden inicia siempre en estado PENDING |
| **Validaciones Técnicas** | tableId ∈ [1, 12]; items.length ≥ 1; todos los productId deben existir en DB con is_active=true; el UUID de la orden debe ser único |
| **Códigos HTTP Esperados** | 201 Created (flujo feliz); 400 Bad Request (validación fallida); 422 Unprocessable Entity (negocio) |
| **Reglas de Seguridad** | No aplica autenticación en creación de orden (MVP scope); el evento debe publicarse correctamente a RabbitMQ |
| **Reglas de Máquina de Estados** | Nueva orden → PENDING (inicial); será consumida por kitchen-worker y pasará a IN_PREPARATION/READY |

### Casos de Prueba

```gherkin
Feature: HDU-01 - Creación de Órdenes con Respuesta 201 Created

Background:
  Given que el servicio "order-service" está disponible en "http://localhost:8080"
  And que la base de datos "restaurant_db" contiene al menos 5 productos activos
  And que RabbitMQ está configurado con el exchange "orders.events"

# ============================================================================
# FLUJO FELIZ - Casos Positivos
# ============================================================================

Scenario: CT-01-001-POSITIVE | Crear orden válida con 1 item
  Given que tengo los siguientes datos válidos:
    | Campo      | Valor                      |
    | tableId    | 5                          |
    | productId  | 1                          |
    | quantity   | 2                          |
    | note       | Sin cebolla                |
  When envío un POST a "/orders" con el payload:
    """
    {
      "tableId": 5,
      "items": [
        {
          "productId": 1,
          "quantity": 2,
          "note": "Sin cebolla"
        }
      ]
    }
    """
  Then el código de estado debe ser 201 Created
  And el header "Location" debe estar presente y contener "/orders/"
  And el cuerpo de la respuesta debe contener:
    | Campo      | Valor        |
    | status     | PENDING      |
    | tableId    | 5            |
  And el evento "order.placed" debe ser publicado en RabbitMQ con eventVersion=1
  And la orden debe existir en "restaurant_db.orders" con uuid único

Scenario: CT-01-002-POSITIVE | Crear orden válida con múltiples items
  Given que tengo una lista de 3 productos activos:
    | productId | quantity | note     |
    | 1         | 2        | Sin sal  |
    | 3         | 1        |          |
    | 5         | 3        | Punto    |
  When envío un POST a "/orders" con 3 items y tableId=7
  Then el código de estado debe ser 201 Created
  And el header "Location" apunta a un UUID válido (formato v4)
  And la respuesta contiene exactamente 3 items en el array "items"
  And cada item tiene su productId, quantity y note correctos
  And el evento RabbitMQ incluye itemCount=3

Scenario: CT-01-003-POSITIVE | Crear orden en mesa al límite superior (tableId=12)
  Given que tableId=12 es la mesa máxima permitida
  When envío un POST a "/orders" con tableId=12 e items válido
  Then el código de estado debe ser 201 Created
  And el evento se publica correctamente
  And la orden persiste con tableId=12 en la DB

Scenario: CT-01-004-POSITIVE | Crear orden en mesa al límite inferior (tableId=1)
  Given que tableId=1 es la mesa mínima permitida
  When envío un POST a "/orders" con tableId=1 e items válido
  Then el código de estado debe ser 201 Created
  And la orden persiste correctamente

Scenario: CT-01-005-POSITIVE | Orden con nota en item
  Given que un item tiene nota="Sin lácteos"
  When creo una orden con ese item
  Then el cuerpo respuesta incluye la nota exactamente igual
  And el RabbitMQ event contiene la nota en el item

# ============================================================================
# CASOS NEGATIVOS - Validación de Entrada
# ============================================================================

Scenario: CT-01-006-NEGATIVE | tableId fuera de rango superior (13)
  Given que tableId=13 excede el máximo permitido (12)
  When envío POST a "/orders" con tableId=13
  Then el código de estado debe ser 400 Bad Request o 422 Unprocessable Entity
  And el cuerpo de error contiene un mensaje descriptivo sobre el rango válido
  And NO se publica evento en RabbitMQ
  And la orden NO persiste en la base de datos

Scenario: CT-01-007-NEGATIVE | tableId fuera de rango inferior (0)
  Given que tableId=0 es menor que el mínimo (1)
  When envío POST con tableId=0
  Then el código debe ser 400 o 422
  And la orden no se crea

Scenario: CT-01-008-NEGATIVE | tableId negativo (-5)
  Given que tableId=-5 es inválido
  When envío POST con tableId=-5
  Then el código debe ser 400 o 422

Scenario: CT-01-009-NEGATIVE | Sin items en la orden
  Given que items es un arreglo vacío []
  When envío POST a "/orders" con tableId=5 e items=[]
  Then el código de estado debe ser 400 Bad Request o 422 Unprocessable Entity
  And el error menciona que se requiere al menos 1 item
  And NO se publica evento
  And la orden NO persiste

Scenario: CT-01-010-NEGATIVE | productId que no existe
  Given que productId=999 no existe en "menu_items"
  When envío POST con tableId=5 e items=[{productId: 999, quantity: 1}]
  Then el código de estado debe ser 400 Bad Request o 422 Unprocessable Entity
  And el error especifica que el producto no existe
  And la orden NO persiste

Scenario: CT-01-011-NEGATIVE | productId inactivo (is_active=false)
  Given que el producto con id=2 existe pero is_active=false
  When intento crear una orden con productId=2
  Then el código debe ser 400 o 422 (regla de negocio)
  And el error menciona que el producto no está disponible
  And la orden NO persiste

Scenario: CT-01-012-NEGATIVE | quantity=0
  Given que quantity debe ser ≥ 1
  When envío POST con quantity=0
  Then el código debe ser 400 o 422
  And el error menciona que la cantidad debe ser positiva

Scenario: CT-01-013-NEGATIVE | quantity negativa (-3)
  Given que quantity negativa es inválida
  When envío POST con quantity=-3
  Then el código debe ser 400 o 422

Scenario: CT-01-014-NEGATIVE | tableId como string en lugar de número
  Given que tableId="mesa5" (texto en lugar de número)
  When envío POST con ese JSON malformado
  Then el código de estado debe ser 400 Bad Request
  And el error debe indicar un problema de tipo de dato

Scenario: CT-01-015-NEGATIVE | JSON malformado (llave sin cerrar)
  Given que envío un JSON con formato inválido: {"tableId": 5, "items"
  When procesa la petición
  Then el código debe ser 400 Bad Request
  And el error describe el problema de parseo JSON

# ============================================================================
# CASOS LÍMITE - Boundary Value Analysis
# ============================================================================

Scenario: CT-01-016-BOUNDARY | Orden con máxima cantidad en un item (largeNumber)
  Given que quantity puede ser un número muy grande (ej: 999999)
  When creo una orden con quantity=999999
  Then el código debe ser 201 Created
  And la orden persiste con esa cantidad exacta
  # Nota: Sin validación de límite superior explícita en reglas de negocio

Scenario: CT-01-017-BOUNDARY | Orden con múltiples items del mismo producto
  Given que items=[{productId: 1, qty: 2}, {productId: 1, qty: 3}]
  When creo la orden
  Then el código debe ser 201 Created
  And ambos items se persisten (no consolidados)
  And el evento RabbitMQ incluye ambos items por separado

Scenario: CT-01-018-BOUNDARY | Nota vacía en item
  Given que nota es una cadena vacía ""
  When creo una orden con esa nota
  Then el código es 201 Created
  And la nota se guarda como null o ""

Scenario: CT-01-019-BOUNDARY | Nota con caracteres especiales
  Given que nota="¡Sin sal, pimentón! & <>" (con especiales)
  When creo la orden
  Then el código es 201 Created
  And la nota se persiste exactamente igual (escapada si es necesario)

Scenario: CT-01-020-BOUNDARY | Header Location con UUID v4 válido
  Given que creé una orden exitosamente
  When examino el header "Location"
  Then debe tener formato: "http://localhost:8080/orders/{UUID-v4}"
  And el UUID debe ser válido según RFC 4122

# ============================================================================
# CASOS DE EVENTO/ASINCRONÍA
# ============================================================================

Scenario: CT-01-021-EVENT | Evento order.placed con eventVersion=1
  Given que creo una orden válida
  When se publica el evento en RabbitMQ
  Then el evento tiene estructura:
    """
    {
      "eventVersion": 1,
      "eventType": "order.placed",
      "orderId": "...",
      "tableId": 5,
      "items": [...],
      "timestamp": "..."
    }
    """
  And eventVersion es exactamente 1 (no 2, no "1", no null)

Scenario: CT-01-022-EVENT | Idempotencia: Orden duplicada retorna 201 con diferente UUID
  Given que enviado el mismo payload dos veces
  When la segunda petición es procesada
  Then ambas retornan 201 Created
  And los UUID generados son DIFERENTES
  And hay DOS órdenes en la base de datos
  # Nota: El sistema no implementa idempotencia de cliente (token de idempotencia)

Scenario: CT-01-023-EVENT | Evento publicado antes de que GET devuelva 201
  Given que creo una orden exitosamente
  When consultó GET /orders/{id} inmediatamente
  Then el evento ya debe existir en el topic de RabbitMQ (o está en vías de estarlo)
  And la orden existe en restaurant_db

# ============================================================================
# CASOS DE CONTRATOS REST
# ============================================================================

Scenario: CT-01-024-CONTRACT | Header Content-Type en respuesta 201
  Given que creo una orden
  When obtengo respuesta 201
  Then el header "Content-Type" es "application/json"
  And la respuesta es un JSON válido

Scenario: CT-01-025-CONTRACT | Cuerpo de respuesta 201 contiene createdAt y updatedAt
  Given que creo una orden exitosamente
  When analizo la respuesta
  Then el cuerpo incluye:
    - "id" (uuid)
    - "tableId" (number)
    - "status" ("PENDING")
    - "items" (array)
    - "createdAt" (ISO 8601 timestamp)
    - "updatedAt" (ISO 8601 timestamp)

# ============================================================================
# ACCESO A RECURSO CREADO (Verificación del endpoint GET)
# ============================================================================

Scenario: CT-01-026-ACCESS | GET a recurso creado retorna 200 OK sin autenticación
  Given que creé una orden y obtuve su Location header
  When envío GET a "http://localhost:8080/orders/{id}" sin token
  Then el código de estado es 200 OK
  And el cuerpo contiene toda la información de la orden

Scenario: CT-01-027-ACCESS | GET a recurso creado con X-Kitchen-Token retorna 200 OK
  Given que creé una orden exitosamente
  When envío GET a "/orders/{id}" CON header "X-Kitchen-Token: cocina123"
  Then el código es 200 OK
  And la respuesta incluye la orden completa con su estado PENDING

Scenario: CT-01-028-ACCESS | Orden recién creada tiene estado PENDING en GET
  Given que creé una orden
  When consultó el recurso via GET
  Then el campo "status" es exactamente "PENDING"
  And no puede haber sido modificado por otro consumer

```

---

## Matriz de Pruebas – HDU-02: Respuesta 200 OK con Lista Vacía

### Resumen Analítico

| Concepto | Análisis |
|----------|----------|
| **Reglas de Negocio** | Cualquier endpoint de lectura (menú, reportes, órdenes) debe retornar 200 OK incluso sin registros; la respuesta debe ser un array JSON vacío `[]`, nunca `null` |
| **Validaciones Técnicas** | Los DTOs deben inicializar listas con `new ArrayList<>()` para que Jackson serialice `[]`; la paginación (si aplica) debe funcionar con offset/limit sobre lista vacía |
| **Códigos HTTP Esperados** | 200 OK (lista vacía o con datos); 401 Unauthorized (falta autenticación si aplica) |
| **Reglas de Seguridad** | El endpoint /menu NO requiere autenticación; GET /reports puede requerir X-Kitchen-Token según la revisión (MVP scope) |
| **Máquina de Estados** | N/A (endpoints de lectura) |

### Casos de Prueba

```gherkin
Feature: HDU-02 - Respuesta 200 OK con Lista Vacía

Background:
  Given que el servicio "order-service" está disponible en "http://localhost:8080"
  And que el servicio "report-service" está disponible en "http://localhost:8082"

# ============================================================================
# MENÚ VACÍO
# ============================================================================

Scenario: CT-02-001-POSITIVE | GET /menu retorna 200 OK con array vacío cuando no hay productos
  Given que la base de datos "restaurant_db.menu_items" está vacía
    OR todos los productos tienen "is_active" = false
  When envío un GET a "/menu" sin autenticación
  Then el código de estado es 200 OK
  And el Content-Type es "application/json"
  And el cuerpo es exactamente: []
  And NO es null

Scenario: CT-02-002-POSITIVE | GET /menu retorna 200 OK con productos cuando existen
  Given que existen 3 productos activos en la DB
  When envío un GET a "/menu"
  Then el código de estado es 200 OK
  And el cuerpo es un array con exactamente 3 elementos
  And cada elemento contiene: id, name, description, price, category, imageUrl, isActive

Scenario: CT-02-003-BOUNDARY | GET /menu filtra SOLO productos con is_active=true
  Given que existen 5 productos en total:
    - 3 con is_active=true
    - 2 con is_active=false
  When envío GET a "/menu"
  Then el código es 200 OK
  And el array contiene exactamente 3 productos (solo los activos)

# ============================================================================
# REPORTES VACÍOS
# ============================================================================

Scenario: CT-02-004-POSITIVE | GET /reports con rango sin órdenes READY retorna 200 OK con array vacío
  Given que en "report_db" NO existen órdenes con estado READY en el rango 2026-01-01 a 2026-01-31
  When envío GET a "/reports?startDate=2026-01-01&endDate=2026-01-31"
  Then el código de estado es 200 OK
  And el cuerpo contiene:
    """
    {
      "totalRevenue": 0.0,
      "orders": [],
      "productSummary": []
    }
    """
  And el array "orders" es vacío []

Scenario: CT-02-005-POSITIVE | GET /reports retorna totalRevenue=0 cuando no hay ventas
  Given que no hay órdenes READY en el período
  When consulto /reports con parámetros válidos
  Then el código es 200 OK
  And totalRevenue es exactamente 0 o 0.0
  And productSummary es un array vacío []

Scenario: CT-02-006-POSITIVE | GET /reports con rango futuro sin datos
  Given que solicito reportes para 2099-01-01 a 2099-12-31 (futuro)
  When envío GET a "/reports?startDate=2099-01-01&endDate=2099-12-31"
  Then el código es 200 OK
  And todos los conteos y sumatorias son 0

# ============================================================================
# ÓRDENES VACÍAS
# ============================================================================

Scenario: CT-02-007-POSITIVE | GET /orders retorna 200 OK con array vacío cuando no hay órdenes
  Given que la tabla "restaurant_db.orders" está vacía
    OR todas las órdenes han sido eliminadas (deleted=true)
  When envío GET a "/orders" sin parámetros
  Then el código de estado es 200 OK
  And el cuerpo es exactamente: []

Scenario: CT-02-008-POSITIVE | GET /orders?status=PENDING retorna 200 OK con array vacío
  Given que NO hay órdenes con estado PENDING
  When envío GET a "/orders?status=PENDING"
  Then el código es 200 OK
  And el array "orders" es vacío []
  And NO incluye órdenes en otros estados

# ============================================================================
# SEGURIDAD: Acceso sin Autenticación
# ============================================================================

Scenario: CT-02-009-NEGATIVE | GET /menu sin autenticación retorna 200 OK (público)
  Given que GET /menu es un endpoint público (sin requerir X-Kitchen-Token)
  When envío GET a "/menu" SIN el header X-Kitchen-Token
  Then el código es 200 OK
  And la respuesta contiene el menú normalmente

Scenario: CT-02-010-NEGATIVE | GET /reports sin X-Kitchen-Token retorna 401 Unauthorized
  Given que GET /reports requiere autenticación (X-Kitchen-Token válido)
  When envío GET a "/reports?startDate=...&endDate=..." SIN el token
  Then el código de estado es 401 Unauthorized
  And el cuerpo NO es una lista vacía, sino un error JSON estandarizado
  And el mensaje de error menciona que se requiere autenticación

Scenario: CT-02-011-NEGATIVE | GET /reports con X-Kitchen-Token inválido retorna 401
  Given que X-Kitchen-Token es "invalid_token_xyz"
  When envío GET a "/reports" con ese token inválido
  Then el código es 401 Unauthorized

Scenario: CT-02-012-NEGATIVE | GET /orders sin X-Kitchen-Token pero con status=PENDING
  Given que GET /orders?status=PENDING requiere X-Kitchen-Token (acceso desde cocina)
  When envío GET SIN el token
  Then el código es 401 Unauthorized
  And NO se retorna lista (vacía o no)

# ============================================================================
# CONTRATOS: Estructura de Respuesta
# ============================================================================

Scenario: CT-02-013-CONTRACT | Respuesta de /menu es un array de objetos MenuItem
  Given que solicito GET /menu
  When obtengo respuesta 200
  Then el Content-Type es "application/json"
  And cada elemento del array tiene exactamente estos campos:
    - id (number)
    - name (string)
    - description (string)
    - price (number)
    - category (string)
    - imageUrl (string)
    - isActive (boolean)

Scenario: CT-02-014-CONTRACT | Respuesta de /reports es un objeto ReportResponseDTO con estructura fija
  Given que solicito GET /reports con parámetros válidos
  When obtengo respuesta 200
  Then el cuerpo tiene exactamente esta estructura:
    """
    {
      "totalRevenue": number,
      "orders": array,
      "productSummary": array
    }
    """

Scenario: CT-02-015-BOUNDARY | Lista con 1 elemento retorna array con 1 elemento (no vacío)
  Given que existe exactamente 1 producto activo
  When envío GET a "/menu"
  Then el código es 200 OK
  And el array tiene exactamente 1 elemento
  And el elemento es un objeto válido MenuItem

```

---

## Matriz de Pruebas – HDU-03: Validación de Estructura y Tipo de Datos con 400

### Resumen Analítico

| Concepto | Análisis |
|----------|----------|
| **Reglas de Negocio** | La validación de formato debe ser separada de validación de negocio; un JSON malformado es un error de protocolo (400), no de lógica |
| **Validaciones Técnicas** | Jackson debe fallar en deserialización de tipos incorrectos; validadores de Spring deben capturar restricciones de dominio (@NotNull, @Min, @Max) |
| **Códigos HTTP Esperados** | 400 Bad Request (validación de entrada fallida); 201 Created (payload válido) |
| **Reglas de Seguridad** | Los mensajes de error no deben exponer rutas internas de archivos ni información técnica de Spring |
| **Máquina de Estados** | N/A |

### Casos de Prueba

```gherkin
Feature: HDU-03 - Validación de Estructura y Tipo de Datos

Background:
  Given que el servicio "order-service" está disponible

# ============================================================================
# JSON MALFORMADO
# ============================================================================

Scenario: CT-03-001-NEGATIVE | JSON con llave sin cerrar
  Given que envío un payload con JSON inválido: {"tableId": 5, "items"
  When procesa POST a "/orders"
  Then el código de estado es 400 Bad Request
  And el error contiene un mensaje sobre formato JSON
  And el mensaje es legible (no exposición de traces internos)

Scenario: CT-03-002-NEGATIVE | JSON con comilla sin cerrar en string
  Given que envío: {"tableId": 5, "items": [{"productId": "unclosed}]}
  When procesa POST a "/orders"
  Then el código es 400 Bad Request
  And el mensaje de error describe el problema de parseo

Scenario: CT-03-003-NEGATIVE | JSON con caracteres no escapados
  Given que envío: {"tableId": 5, "items": [{"note": "Sin "cebolla"}]}
  When procesa POST a "/orders"
  Then el código es 400 Bad Request

Scenario: CT-03-004-NEGATIVE | Cuerpo vacío en POST
  Given que envío POST a "/orders" con cuerpo vacío ("" o sin body)
  When procesa la solicitud
  Then el código es 400 Bad Request
  And el error menciona que el cuerpo es requerido

# ============================================================================
# TIPO DE DATO INCORRECTO
# ============================================================================

Scenario: CT-03-005-NEGATIVE | tableId como string en lugar de number
  Given que tableId="mesa5" (string en lugar de número)
  When envío POST a "/orders" con ese payload:
    """
    {
      "tableId": "mesa5",
      "items": [...]
    }
    """
  Then el código de estado es 400 Bad Request
  And el error menciona que tableId debe ser un número entero
  And NO se intenta coercer el string a número

Scenario: CT-03-006-NEGATIVE | tableId como objeto en lugar de number
  Given que tableId={} (objeto vacío)
  When envío POST
  Then el código es 400 Bad Request

Scenario: CT-03-007-NEGATIVE | items como string en lugar de array
  Given que items="producto1,producto2" (string en lugar de array)
  When envío POST
  Then el código es 400 Bad Request
  And el error menciona que items debe ser un array

Scenario: CT-03-008-NEGATIVE | productId como string en lugar de number
  Given que productId="uno" (string)
  When envío POST con items=[{"productId": "uno", "quantity": 1}]
  Then el código es 400 Bad Request
  And el error menciona el tipo incorrecto de productId

Scenario: CT-03-009-NEGATIVE | quantity como string en lugar de number
  Given que quantity="dos"
  When envío POST
  Then el código es 400 Bad Request

Scenario: CT-03-010-NEGATIVE | quantity como null
  Given que quantity es null en items
  When envío POST
  Then el código es 400 Bad Request o 422 Unprocessable Entity
  And el error menciona que quantity es requerida

# ============================================================================
# CAMPOS FALTANTES
# ============================================================================

Scenario: CT-03-011-NEGATIVE | Falta tableId en payload
  Given que envío: {"items": [{"productId": 1, "quantity": 1}]}
  When procesa POST a "/orders"
  Then el código es 400 Bad Request
  And el error menciona que tableId es requerido

Scenario: CT-03-012-NEGATIVE | Falta items en payload
  Given que envío: {"tableId": 5}
  When procesa POST a "/orders"
  Then el código es 400 Bad Request
  And el error menciona que items es requerido

Scenario: CT-03-013-NEGATIVE | Falta productId en item
  Given que envío: {"tableId": 5, "items": [{"quantity": 2}]}
  When procesa POST
  Then el código es 400 Bad Request
  And el error menciona que productId es requerido en items

Scenario: CT-03-014-NEGATIVE | Falta quantity en item
  Given que envío: {"tableId": 5, "items": [{"productId": 1}]}
  When procesa POST
  Then el código es 400 Bad Request o 422
  And el error menciona que quantity es requerida

# ============================================================================
# TIPOS COMPLEJOS
# ============================================================================

Scenario: CT-03-015-NEGATIVE | note como número en lugar de string
  Given que note=123 (número)
  When envío POST
  Then Jackson convierte a string o rechaza con 400
  # Comportamiento depende de configuración de Jackson

Scenario: CT-03-016-NEGATIVE | Payload con campos adicionales desconocidos
  Given que envío: {"tableId": 5, "items": [...], "extraField": "ignorar"}
  When procesa POST
  Then el código es 201 Created
  And el campo extraField es ignorado (no causa error 400)
  # Jackson ignora por defecto los campos desconocidos

# ============================================================================
# FLUJO FELIZ: JSON Válido
# ============================================================================

Scenario: CT-03-017-POSITIVE | JSON correctamente formado con tipos válidos
  Given que envío:
    """
    {
      "tableId": 5,
      "items": [
        {"productId": 1, "quantity": 2, "note": "Sin sal"}
      ]
    }
    """
  When procesa POST a "/orders"
  Then el código de estado es 201 Created
  And se retorna la orden creada con UUID

Scenario: CT-03-018-POSITIVE | JSON válido con tipos correctos y nota null
  Given que envío: {"tableId": 5, "items": [{"productId": 1, "quantity": 1, "note": null}]}
  When procesa POST
  Then el código es 201 Created

Scenario: CT-03-019-POSITIVE | JSON con tipos válidos y múltiples items
  Given que envío una orden con 5 items, cada uno con productId, quantity válidos
  When procesa POST
  Then el código es 201 Created

# ============================================================================
# CASOS DE ERROR 400 vs 422
# ============================================================================

Scenario: CT-03-020-DISTINCTION | Distinguir 400 (formato) de 422 (negocio)
  Given que se espera:
    - 400 Bad Request: JSON malformado, tipos incorrectos, estructura inválida
    - 422 Unprocessable Entity: JSON válido pero valores violan reglas de negocio
  When envío POST a "/orders"
  Then se retorna 400 si el problema es de parsing/tipo
  And se retorna 422 si la entidad es semánticamente inválida (ej: tableId=13)

Scenario: CT-03-021-BOUNDARY | Array items vacío: ¿400 o 422?
  Given que items=[] (array vacío, JSON válido)
  When envío POST
  Then el código es 422 Unprocessable Entity (regla de negocio: al menos 1 item)
  And NO es 400 (pues el JSON es válido sintácticamente)

```

---

## Matriz de Pruebas – HDU-04: Desactivación Lógica de Productos con Auditoría

### Resumen Analítico

| Concepto | Análisis |
|----------|----------|
| **Reglas de Negocio** | La eliminación de productos es lógica (soft delete): campo is_active se pone en false; campos de auditoría se actualizan (updated_at, updated_by); no se pierde información histórica |
| **Validaciones Técnicas** | El producto debe existir antes de desactivar; si ya está inactivo, la operación es idempotente (retorna 200 OK); campos de auditoría en "products" o "menu_items" |
| **Códigos HTTP Esperados** | 200 OK (éxito, tanto si estaba activo como inactivo); 404 Not Found (producto no existe); 401/403 (falta autenticación/autorización) |
| **Reglas de Seguridad** | Solo administrador puede desactivar productos (requiere autenticación); se debe loguear quién y cuándo |
| **Máquina de Estados** | is_active: true → false (irreversible en MVP) |

### Casos de Prueba

```gherkin
Feature: HDU-04 - Desactivación Lógica de Productos

Background:
  Given que el servicio "order-service" está disponible
  And que la DB "restaurant_db" contiene la tabla "menu_items" con columnas:
    - id, name, is_active, price, updated_at, updated_by, ...

# ============================================================================
# FLUJO FELIZ: Producto Activo Desactivado
# ============================================================================

Scenario: CT-04-001-POSITIVE | Desactivar producto activo retorna 200 OK con metadatos de auditoría
  Given que existe un producto con id=1, name="Empanadas", is_active=true
  And que me autentico como administrador
  When envío DELETE a "/products/1"
  Then el código de estado es 200 OK
  And el cuerpo contiene:
    """
    {
      "id": 1,
      "name": "Empanadas",
      "isActive": false,
      "auditMetadata": {
        "updatedAt": "2026-03-04T10:30:00Z",
        "updatedBy": "admin_user",
        "actionType": "DEACTIVATION"
      }
    }
    """
  And en la DB, menu_items.is_active = false para id=1
  And menu_items.updated_at se actualizó al timestamp actual
  And menu_items.updated_by contiene el usuario administrador

Scenario: CT-04-002-POSITIVE | Desactivar producto retorna createdAt original en auditoría
  Given que un producto fue creado hace 10 días
  When desactivo el producto
  Then el cuerpo incluye createdAt sin cambios
  And updated_at es el timestamp actual

Scenario: CT-04-003-POSITIVE | Menú GET /menu no incluye productos desactivados
  Given que desactivo un producto
  When hago GET a "/menu"
  Then la respuesta NO incluye ese producto
  And solo contiene productos con is_active=true

# ============================================================================
# IDEMPOTENCIA: Producto ya desactivado
# ============================================================================

Scenario: CT-04-004-POSITIVE | Desactivar producto ya inactivo retorna 200 OK
  Given que existe un producto con id=2, is_active=false (ya desactivado)
  When envío DELETE a "/products/2"
  Then el código de estado es 200 OK
  And el cuerpo contiene isActive=false
  And el mensaje o campo "status" indica que ya estaba desactivado
  And updated_at se actualiza (registrando el intento de desactivación)

Scenario: CT-04-005-POSITIVE | Múltiples DELETE al mismo producto: todos retornan 200 OK
  Given que desactivo un producto y repito la operación 3 veces
  When envío DELETE al mismo /products/{id}
  Then todas las respuestas son 200 OK
  And la DB solo registra un soft delete (is_active=false)
  And updated_at se actualiza cada vez (registrando cada intento)

# ============================================================================
# PRODUCTO NO ENCONTRADO
# ============================================================================

Scenario: CT-04-006-NEGATIVE | Desactivar producto inexistente retorna 404
  Given que envío DELETE a "/products/999" (no existe)
  When procesa la solicitud
  Then el código de estado es 404 Not Found
  And el cuerpo contiene un error JSON estandarizado
  And el mensaje especifica que el producto no fue encontrado

Scenario: CT-04-007-NEGATIVE | DELETE a /products/-1 retorna 404
  Given que intento desactivar producto con id negativo
  When envío DELETE a "/products/-1"
  Then el código es 404 Not Found

Scenario: CT-04-008-NEGATIVE | DELETE a /products/0 retorna 404
  Given que intento con id=0
  When envío DELETE
  Then el código es 404 Not Found

# ============================================================================
# SEGURIDAD: Autenticación y Autorización
# ============================================================================

Scenario: CT-04-009-NEGATIVE | Desactivar producto sin autenticación retorna 401
  Given que intento DELETE sin ningún header de autenticación
  When envío DELETE a "/products/1"
  Then el código de estado es 401 Unauthorized
  And el error menciona que se requiere autenticación
  And el producto NO se desactiva en la DB

Scenario: CT-04-010-NEGATIVE | Desactivar producto con token inválido retorna 401
  Given que envío un header de autenticación inválido
  When procesa DELETE
  Then el código es 401 Unauthorized

Scenario: CT-04-011-NEGATIVE | Desactivar producto sin permisos de administrador retorna 403
  Given que me autentico como "usuario_mesero" (no administrador)
  When envío DELETE a "/products/1"
  Then el código de estado es 403 Forbidden
  And el error menciona permisos insuficientes
  And el producto se mantiene activo

# ============================================================================
# CONTRATOS REST
# ============================================================================

Scenario: CT-04-012-CONTRACT | Respuesta 200 OK incluye toda la información del producto
  Given que desactivo un producto exitosamente
  When obtengo respuesta 200
  Then el cuerpo contiene:
    - id (number)
    - name (string)
    - price (number)
    - category (string)
    - isActive (boolean, ahora false)
    - auditMetadata (object con updatedAt, updatedBy, actionType)

Scenario: CT-04-013-CONTRACT | Respuesta 404 retorna JSON estandarizado de error
  Given que intento desactivar producto inexistente
  When obtengo respuesta 404
  Then el cuerpo es:
    """
    {
      "timestamp": "2026-03-04T10:30:00Z",
      "status": 404,
      "error": "Not Found",
      "message": "Product with id 999 not found",
      "path": "/products/999"
    }
    """

# ============================================================================
# CASOS DE CONFLICTO
# ============================================================================

Scenario: CT-04-014-EDGE | Desactivar producto que tiene órdenes activas
  Given que un producto tiene órdenes en estado PENDING o IN_PREPARATION
  When desactivo el producto
  Then el código es 200 OK
  And el producto se desactiva
  And las órdenes existentes NO se modifican (no es cascada)
  # Nota: Las órdenes ya creadas contienen el producto por ID, no referencia activa

Scenario: CT-04-015-EDGE | Desactivar todos los productos (listar después debe ser vacío)
  Given que desactivo los 5 productos disponibles
  When hago GET a "/menu"
  Then retorna 200 OK con array vacío []

# ============================================================================
# INTEGRACIÓN: Desactivar y crear orden con ese producto
# ============================================================================

Scenario: CT-04-016-INTEGRATION | Crear orden con producto desactivado falla
  Given que desactivo producto con id=1
  When intento crear una orden que incluye productId=1
  Then el código es 400 Bad Request o 422 Unprocessable Entity
  And el error menciona que el producto no está disponible

```

---

## Matriz de Pruebas – HDU-05: Respuestas de Error JSON Estandarizadas (Desarrollador)

### Resumen Analítico

| Concepto | Análisis |
|----------|----------|
| **Reglas de Negocio** | Toda respuesta de error (4xx, 5xx) debe ser un JSON con estructura consistente; nunca HTML o texto plano |
| **Validaciones Técnicas** | Estructura de error: `{timestamp, status, error, path, message}`; todos los servicios deben cumplir el contrato |
| **Códigos HTTP Esperados** | 400 (validación), 401 (auth), 404 (recurso no existe), 409 (conflicto), 422 (negocio), 500 (error interno) |
| **Reglas de Seguridad** | Los errores pueden incluir información técnica legible; NO expondrán detalles internos sensibles (stack traces, SQL) |
| **Máquina de Estados** | N/A |

### Casos de Prueba

```gherkin
Feature: HDU-05 - Respuestas de Error JSON Estandarizadas (Desarrollador)

Background:
  Given que tanto "order-service" como "report-service" están disponibles
  And que todas las respuestas de error tienen estructura JSON estandarizada

# ============================================================================
# VALIDACIÓN DE REGLAS DE NEGOCIO (400 Bad Request)
# ============================================================================

Scenario: CT-05-001-NEGATIVE | Validación fallida: tableId fuera de rango
  Given que intento crear una orden con tableId=13
  When envío POST a "/orders"
  Then el código de estado es 400 Bad Request o 422 Unprocessable Entity
  And el cuerpo es un JSON con estructura:
    """
    {
      "timestamp": "2026-03-04T10:30:00Z",
      "status": 400,
      "error": "Bad Request",
      "path": "/orders",
      "message": "tableId must be between 1 and 12"
    }
    """
  And NO contiene: stack traces, nombres de clases Java, SQL

Scenario: CT-05-002-NEGATIVE | Validación fallida: sin items
  Given que intento crear una orden con items=[]
  When envío POST a "/orders"
  Then el código es 400 o 422
  And el mensaje es legible: "Order must contain at least one item"

Scenario: CT-05-003-NEGATIVE | Validación fallida: producto inexistente
  Given que intento con productId=999
  When envío POST
  Then el código es 400 o 422
  And el mensaje: "Product with id 999 does not exist or is inactive"

Scenario: CT-05-004-NEGATIVE | JSON malformado
  Given que envío JSON con llave sin cerrar
  When procesa POST
  Then el código es 400 Bad Request
  And el mensaje describe el error de parseo JSON (sin traces)

# ============================================================================
# ACCESO SIN AUTORIZACIÓN (401 Unauthorized)
# ============================================================================

Scenario: CT-05-005-NEGATIVE | GET /orders sin X-Kitchen-Token
  Given que intento GET a "/orders?status=PENDING" sin el token
  When procesa la solicitud
  Then el código de estado es 401 Unauthorized
  And el cuerpo contiene:
    """
    {
      "timestamp": "2026-03-04T10:30:00Z",
      "status": 401,
      "error": "Unauthorized",
      "path": "/orders",
      "message": "X-Kitchen-Token header is missing or invalid"
    }
    """

Scenario: CT-05-006-NEGATIVE | GET /reports con token inválido
  Given que envío X-Kitchen-Token: "invalid_token"
  When procesa GET a "/reports?startDate=...&endDate=..."
  Then el código es 401 Unauthorized
  And el mensaje especifica que el token es inválido

# ============================================================================
# RECURSO NO ENCONTRADO (404 Not Found)
# ============================================================================

Scenario: CT-05-007-NEGATIVE | GET /orders/{uuid} con UUID inexistente
  Given que intento GET a "/orders/550e8400-e29b-41d4-a716-999999999999"
  When procesa la solicitud
  Then el código de estado es 404 Not Found
  And el cuerpo contiene:
    """
    {
      "timestamp": "2026-03-04T10:30:00Z",
      "status": 404,
      "error": "Not Found",
      "path": "/orders/550e8400-e29b-41d4-a716-999999999999",
      "message": "Order not found"
    }
    """

Scenario: CT-05-008-NEGATIVE | GET /products/999
  Given que el producto no existe
  When envío GET
  Then el código es 404 Not Found
  And el mensaje: "Product not found"

# ============================================================================
# ERROR INESPERADO DEL SERVIDOR (500 Internal Server Error)
# ============================================================================

Scenario: CT-05-009-NEGATIVE | report-service pierde conexión a DB
  Given que la base de datos report_db no es accesible
  When intento GET a "/reports"
  Then el código de estado es 500 Internal Server Error
  And el cuerpo es un JSON estandarizado:
    """
    {
      "timestamp": "2026-03-04T10:30:00Z",
      "status": 500,
      "error": "Internal Server Error",
      "path": "/reports",
      "message": "An unexpected error occurred"
    }
    """
  And NO contiene: stack trace de excepción, nombres de clases, detalles de conexión JDBC

Scenario: CT-05-010-NEGATIVE | order-service lanza excepción no controlada
  Given que ocurre una NullPointerException en la lógica
  When intento crear una orden
  Then el código es 500 Internal Server Error
  And el mensaje es genérico (sin exposición interna)

# ============================================================================
# ESTRUCTURA CONSISTENTE EN TODOS LOS SERVICIOS
# ============================================================================

Scenario: CT-05-011-CONTRACT | order-service y report-service devuelven estructura idéntica
  Given que provoco un error en order-service (ej: 404)
  And que provoco un error equivalente en report-service (ej: 404)
  When examino ambas respuestas
  Then ambas tienen la estructura:
    - timestamp (ISO 8601)
    - status (number)
    - error (string, código HTTP)
    - path (string, ruta solicitada)
    - message (string, descripción)
  And NO hay campos adicionales inconsistentes

Scenario: CT-05-012-CONTRACT | Content-Type es "application/json" en todos los errores
  Given que provoco cualquier error (400, 401, 404, 500)
  When examino el header Content-Type
  Then es "application/json; charset=UTF-8"
  And NO es "text/html" ni "text/plain"

# ============================================================================
# ERRORES CON CONTEXTO ADICIONAL
# ============================================================================

Scenario: CT-05-013-VARIATION | Validación con múltiples errores
  Given que envío un payload con varios errores:
    - tableId=25 (fuera de rango)
    - items=[] (vacío)
  When procesa POST
  Then el código es 400 o 422
  And el mensaje menciona al menos el primer error detectado
  # Nota: El sistema puede retornar los errores en un array o como mensaje único

Scenario: CT-05-014-VARIATION | Error con detalles adicionales opcionales
  Given que ocurre un error de validación
  When examino la respuesta
  Then puede contener un campo adicional "details" con array de errores por campo:
    """
    {
      ...
      "details": [
        {"field": "tableId", "message": "must be between 1 and 12"},
        {"field": "items", "message": "must not be empty"}
      ]
    }
    """

# ============================================================================
# CASOS DE TRANSICIÓN DE ESTADO (Conflicto)
# ============================================================================

Scenario: CT-05-015-NEGATIVE | Transición inválida de estado (409 Conflict)
  Given que intento cambiar una orden READY a IN_PREPARATION (hacia atrás)
  When envío PATCH a "/orders/{id}/status" con status="IN_PREPARATION"
  Then el código de estado es 409 Conflict
  Y el cuerpo contiene:
    """
    {
      "timestamp": "2026-03-04T10:30:00Z",
      "status": 409,
      "error": "Conflict",
      "path": "/orders/...",
      "message": "Invalid state transition from READY to IN_PREPARATION"
    }
    """

```

---

## Matriz de Pruebas – HDU-06: Respuestas de Error JSON Estandarizadas (Seguridad)

### Resumen Analítico

| Concepto | Análisis |
|----------|----------|
| **Reglas de Negocio** | Las respuestas de error al cliente son genéricas y legibles; los detalles técnicos sensibles se registran solo en logs internos |
| **Validaciones Técnicas** | Jackson debe serializar errores sin exponer traces; interceptadores deben limpiar mensajes antes de enviar al cliente |
| **Códigos HTTP Esperados** | 400, 401, 404, 409, 422, 500 (mismos que HDU-05, pero con énfasis en seguridad) |
| **Reglas de Seguridad** | NUNCA exponer: stack traces, nombres de clases Java, rutas de archivos, queries SQL, detalles de conexión DB, versiones de frameworks |
| **Máquina de Estados** | N/A |

### Casos de Prueba

```gherkin
Feature: HDU-06 - Respuestas de Error JSON Estandarizadas (Seguridad)

Background:
  Given que el sistema ejecuta en modo production
  And que los logs internos capturan trazas completas, pero NO se envían al cliente

# ============================================================================
# VALIDACIÓN DE NEGOCIO SIN EXPOSICIÓN TÉCNICA
# ============================================================================

Scenario: CT-06-001-SECURITY | Validación de tableId no expone restricciones de DB
  Given que envío tableId=25 (fuera del rango 1-12)
  When procesa POST a "/orders"
  Then el código es 400 Bad Request o 422 Unprocessable Entity
  And el mensaje es: "Table ID must be between 1 and 12"
  And NO contiene:
    - "CheckConstraint violated"
    - "@Min(1)"
    - "@Max(12)"
    - "Hibernate validation"
    - "javax.validation"

Scenario: CT-06-002-SECURITY | Error de tipo de dato sin mencionar clases Java
  Given que envío tableId="texto" (tipo incorrecto)
  When procesa POST
  Then el código es 400 Bad Request
  And el mensaje es: "tableId must be a number"
  And NO contiene:
    - "NumberFormatException"
    - "Jackson"
    - "com.fasterxml.jackson"

Scenario: CT-06-003-SECURITY | Campo faltante sin exposición de Bean Validation
  Given que envío un JSON sin el campo "tableId"
  When procesa POST
  Then el código es 400 Bad Request
  And el mensaje es: "tableId is required"
  And NO contiene:
    - "@NotNull"
    - "ConstraintViolation"
    - "javax.validation"

# ============================================================================
# RECURSO NO ENCONTRADO SIN PISTAS TÉCNICAS
# ============================================================================

Scenario: CT-06-004-SECURITY | GET /orders/{uuid} inexistente sin SQL/ORM expuesto
  Given que intento GET a "/orders/550e8400-e29b-41d4-a716-999999999999"
  When obtengo respuesta 404
  Then el mensaje es: "Order not found"
  And NO contiene:
    - "SELECT * FROM orders WHERE id = ..."
    - "EntityNotFoundException"
    - "Hibernate"
    - "JPA"
    - Nombre de tabla de DB ("orders", "restaurant_db")

Scenario: CT-06-005-SECURITY | GET /reports con parámetros inválidos sin Hibernate
  Given que envío startDate con formato incorrecto
  When procesa GET
  Then el código es 400 Bad Request
  And el mensaje es: "startDate must be in format YYYY-MM-DD"
  Y NO contiene:
    - "DateTimeParseException"
    - "java.time.format"
    - Stack trace

# ============================================================================
# ERROR INTERNO SIN EXPOSICIÓN DE STACK TRACE
# ============================================================================

Scenario: CT-06-006-SECURITY | Error 500 sin stack trace ni detalles técnicos
  Given que ocurre una excepción no controlada en report-service
  When intento GET a "/reports"
  Then el código es 500 Internal Server Error
  And el mensaje es genérico: "An unexpected error occurred"
  Y NO contiene:
    - java.lang.NullPointerException
    - org.springframework.web
    - at org.springframework.data
    - Nombre de archivo Java
    - Número de línea

Scenario: CT-06-007-SECURITY | Error de conexión DB sin detalles JDBC
  Given que la base de datos está caída
  When intento GET a "/reports"
  Then el código es 500 Internal Server Error
  And el mensaje NO contiene:
    - "org.postgresql.util"
    - "Connection refused"
    - Nombre del servidor DB
    - Puerto 5433, 5434
    - Credenciales (usuario, password)

Scenario: CT-06-008-SECURITY | Error de timeout DB sin exposición
  Given que la query a DB toma > 30 segundos
  When procesa GET
  Then el código es 500 o 504 Gateway Timeout
  And el mensaje es genérico
  Y NO expone:
    - "QueryTimeoutException"
    - "Timeout after 30 seconds"
    - Nombre de query SQL

# ============================================================================
# CONFLICTO DE ESTADO SIN DETALLES DE MÁQUINA DE ESTADOS
# ============================================================================

Scenario: CT-06-009-SECURITY | Transición inválida sin exposición de enum
  Given que intento transicionar READY → PENDING
  When envío PATCH a "/orders/{id}/status"
  Then el código es 409 Conflict
  And el mensaje es: "Invalid state transition"
  Y NO contiene:
    - "enum OrderStatus"
    - "READY"
    - "PENDING"
    - "StateTransitionValidator"

# ============================================================================
# AUTORIZACIÓN FALLIDA SIN DETALLES DEL MECANISMO
# ============================================================================

Scenario: CT-06-010-SECURITY | Token inválido sin exposición de estructura
  Given que envío X-Kitchen-Token: "invalid_xyz"
  When procesa GET a "/orders?status=PENDING"
  Then el código es 401 Unauthorized
  Y el mensaje es: "Authentication failed"
  Y NO contiene:
    - "Token validation failed at position 3"
    - "HmacSHA256"
    - Detalles del algoritmo de verificación
    - Nombre de variable interna ("secretKey")

Scenario: CT-06-011-SECURITY | Token ausente sin pistas de autoridad
  Given que omito X-Kitchen-Token
  When procesa GET
  Then el código es 401 Unauthorized
  Y el mensaje es: "Missing authentication header"
  Y NO sugiere dónde obtener el token

# ============================================================================
# LOGGING INTERNO SIN IMPACTO EN CLIENTE
# ============================================================================

Scenario: CT-06-012-INTERNAL | Logs internos capturan detalles completos
  Given que ocurre un error 500 en order-service
  When examino el archivo de logs internos (Docker/CloudWatch)
  Then el log contiene:
    - Stack trace completo
    - Mensaje de excepción original
    - Valores de parámetros
    - Nombre de métodos implicados
  Y la respuesta al cliente solo contiene: "An unexpected error occurred"

Scenario: CT-06-013-AUDIT | Errores de seguridad se registran con detalles para auditoría
  Given que intento un ataque con payload malformado (SQL injection simulation)
  When procesa POST
  Then el cliente recibe: 400 Bad Request (genérico)
  Y el log interno registra:
    - IP de origen
    - Timestamp
    - Payload completo (para análisis)
    - Clasificación de riesgo

# ============================================================================
# CONSISTENCIA EN TODOS LOS SERVICIOS
# ============================================================================

Scenario: CT-06-014-CONSISTENCY | order-service y report-service ocultan detalles igual
  Given que provoco un error 500 en ambos servicios
  When examino las respuestas
  Then ambas tienen mensaje genérico idéntico
  Y ninguna expone detalles técnicos
  Y ambas registran en logs internos
  Y ambas incluyen "timestamp" y "path" para auditoría

```

---

## Matriz de Pruebas – HDU-07: Validación de Máquina de Estados en Transiciones

### Resumen Analítico

| Concepto | Análisis |
|----------|----------|
| **Reglas de Negocio** | Estados válidos: PENDING, IN_PREPARATION, READY; transiciones permitidas: PENDING→IN_PREPARATION, IN_PREPARATION→READY; cualquier otra es inválida |
| **Validaciones Técnicas** | PATCH /orders/{id}/status valida transición antes de persistir; StateTransitionValidator o enum con método canTransitionTo() |
| **Códigos HTTP Esperados** | 200 OK (transición válida); 409 Conflict (transición inválida); 401 (falta token); 404 (orden no existe) |
| **Reglas de Seguridad** | Solo kitchen-worker (con X-Kitchen-Token válido) puede cambiar estado |
| **Máquina de Estados** | PENDING ← inicio; PENDING → IN_PREPARATION → READY → fin (no reversible en MVP) |

### Casos de Prueba

```gherkin
Feature: HDU-07 - Validación de Máquina de Estados

Background:
  Given que el servicio "order-service" está disponible
  And que RabbitMQ está configurado para publicar eventos de cambio de estado
  And que la cocina se autentica con X-Kitchen-Token: "cocina123"

# ============================================================================
# TRANSICIONES VÁLIDAS
# ============================================================================

Scenario: CT-07-001-POSITIVE | Transición válida: PENDING → IN_PREPARATION
  Given que existe una orden con status=PENDING
  When envío PATCH a "/orders/{id}/status" con payload:
    """
    {
      "status": "IN_PREPARATION"
    }
    """
  And incluyo header X-Kitchen-Token: "cocina123"
  Then el código de estado es 200 OK
  And el cuerpo retorna:
    """
    {
      "id": "...",
      "status": "IN_PREPARATION",
      "updatedAt": "2026-03-04T10:35:00Z"
    }
    """
  And en la DB, la orden tiene status="IN_PREPARATION"
  And se publica un evento "order.status.changed" en RabbitMQ

Scenario: CT-07-002-POSITIVE | Transición válida: IN_PREPARATION → READY
  Given que existe una orden con status=IN_PREPARATION
  When envío PATCH con status="READY"
  Then el código es 200 OK
  And el status se actualiza a READY
  And el evento se publica

Scenario: CT-07-003-POSITIVE | Transición parcial: PENDING → IN_PREPARATION (primera vez)
  Given que una orden nueva está en PENDING
  When cambio a IN_PREPARATION
  Then la transición es exitosa
  And el timestamp "updatedAt" se incrementa

Scenario: CT-07-004-POSITIVE | Orden en READY permanece en READY (idempotencia)
  Given que una orden está en estado READY
  When envío PATCH con status="READY" (sin cambio)
  Then el código es 200 OK (sin error)
  And el estado sigue siendo READY
  And updatedAt se actualiza (registrando el intento)

# ============================================================================
# TRANSICIONES INVÁLIDAS: Salto de Estados
# ============================================================================

Scenario: CT-07-005-NEGATIVE | Salto inválido: PENDING → READY (omitir IN_PREPARATION)
  Given que una orden está en PENDING
  When intento PATCH con status="READY"
  Then el código de estado es 409 Conflict
  And el cuerpo contiene:
    """
    {
      "status": 409,
      "error": "Conflict",
      "message": "Invalid state transition from PENDING to READY",
      "currentStatus": "PENDING",
      "requestedStatus": "READY"
    }
    """
  And la orden mantiene su status=PENDING original

Scenario: CT-07-006-NEGATIVE | Transición hacia atrás: IN_PREPARATION → PENDING
  Given que una orden está en IN_PREPARATION
  When intento PATCH con status="PENDING"
  Then el código es 409 Conflict
  And el mensaje indica transición inválida
  And la orden permanece en IN_PREPARATION

Scenario: CT-07-007-NEGATIVE | Transición hacia atrás: READY → IN_PREPARATION
  Given que una orden está en READY (estado terminal en MVP)
  When intento PATCH con status="IN_PREPARATION"
  Then el código es 409 Conflict
  And la orden se mantiene en READY

Scenario: CT-07-008-NEGATIVE | Transición hacia atrás: READY → PENDING
  Given que una orden está en READY
  When intento PATCH con status="PENDING"
  Then el código es 409 Conflict

# ============================================================================
# ESTADO INVÁLIDO (Valores no permitidos)
# ============================================================================

Scenario: CT-07-009-NEGATIVE | Estado inexistente: COMPLETED
  Given que intento enviar status="COMPLETED" (no existe)
  When envío PATCH
  Then el código es 400 Bad Request o 422 Unprocessable Entity
  Y el error menciona estados válidos: PENDING, IN_PREPARATION, READY

Scenario: CT-07-010-NEGATIVE | Estado inválido: "ready" (lowercase en lugar de READY)
  Given que envío status="ready" (minúsculas)
  When procesa PATCH
  Then el código es 400 o 422
  # Comportamiento depende de configuración de Jackson (case-sensitive)

Scenario: CT-07-011-NEGATIVE | Estado como null
  Given que envío payload: {"status": null}
  When procesa PATCH
  Then el código es 400 Bad Request

Scenario: CT-07-012-NEGATIVE | Status como número en lugar de string
  Given que envío: {"status": 2}
  When procesa PATCH
  Then el código es 400 Bad Request

# ============================================================================
# ORDEN NO ENCONTRADA
# ============================================================================

Scenario: CT-07-013-NEGATIVE | Transición en orden inexistente
  Given que intento PATCH a "/orders/550e8400-e29b-41d4-a716-999999999999"
  When procesa la solicitud
  Then el código de estado es 404 Not Found
  Y el mensaje: "Order not found"

Scenario: CT-07-014-NEGATIVE | UUID inválido en URL
  Given que envío PATCH a "/orders/invalid-uuid"
  When procesa
  Then el código es 400 Bad Request o 404 Not Found

# ============================================================================
# SEGURIDAD: Autenticación y Autorización
# ============================================================================

Scenario: CT-07-015-NEGATIVE | Transición sin X-Kitchen-Token
  Given que intento PATCH sin el header X-Kitchen-Token
  When procesa la solicitud
  Then el código de estado es 401 Unauthorized
  Y el error menciona que se requiere autenticación
  Y la orden NO se modifica

Scenario: CT-07-016-NEGATIVE | Transición con X-Kitchen-Token inválido
  Given que envío X-Kitchen-Token: "wrong_token"
  When procesa PATCH
  Then el código es 401 Unauthorized

Scenario: CT-07-017-NEGATIVE | Transición sin header X-Kitchen-Token (ausente)
  Given que OMITO completamente el header X-Kitchen-Token
  When envío PATCH a "/orders/{id}/status"
  Then el código es 401 Unauthorized

# ============================================================================
# FLUJO COMPLETO: 3 Transiciones
# ============================================================================

Scenario: CT-07-018-INTEGRATION | Flujo completo PENDING → IN_PREPARATION → READY
  Given que creo una orden (status=PENDING)
  When cambio a IN_PREPARATION:
    Then el código es 200 OK
    And status="IN_PREPARATION"
  When cambio a READY:
    Then el código es 200 OK
    And status="READY"
  When intento cambiar a PENDING:
    Then el código es 409 Conflict (transición inválida)
    And status sigue siendo READY

# ============================================================================
# EVENTOS DE CAMBIO DE ESTADO
# ============================================================================

Scenario: CT-07-019-EVENT | Evento publicado en cada transición válida
  Given que cambio estado de una orden
  When la transición es válida
  Then se publica un evento "order.status.changed" en RabbitMQ con:
    - orderId
    - previousStatus
    - newStatus
    - timestamp

Scenario: CT-07-020-EVENT | No hay evento si transición falla
  Given que intento una transición inválida (409)
  When el cambio es rechazado
  Then NO se publica evento en RabbitMQ
  And la orden permanece sin cambios en la DB

# ============================================================================
# CONCURRENCIA
# ============================================================================

Scenario: CT-07-021-EDGE | Dos PATCH concurrentes a la misma orden
  Given que intento actualizar el status de la misma orden desde dos clientes simultáneamente
  When ambos envían PATCH
  Then uno de los dos es exitoso (200 OK)
  And el otro podría fallar (409 Conflict, si la transición no es válida desde el nuevo estado)
  And el estado final es consistente en la DB
  # Nota: Requiere optimistic locking con versionamiento

```

---

## Matriz de Pruebas – HDU-08: Respuesta 422 Unprocessable Entity para Errores de Validación

### Resumen Analítico

| Concepto | Análisis |
|----------|----------|
| **Reglas de Negocio** | 422 se retorna cuando el JSON es válido pero los valores violan reglas de negocio (tableId fuera de rango, transición inválida, producto inactivo) |
| **Validaciones Técnicas** | 400 = error de sintaxis/tipo de dato; 422 = semántica/dominio violada; ambos estados pueden ocurrir en el mismo endpoint |
| **Códigos HTTP Esperados** | 400 Bad Request (JSON inválido, tipo incorrecto); 422 Unprocessable Entity (negocio); 201 Created (éxito) |
| **Reglas de Seguridad** | Los mensajes 422 pueden ser más específicos que 400, pues transmiten información de negocio legítima |
| **Máquina de Estados** | Transición inválida → 422 (no 400, pues el JSON es válido) |

### Casos de Prueba

```gherkin
Feature: HDU-08 - Respuesta 422 Unprocessable Entity para Validación de Negocio

Background:
  Given que el servicio "order-service" está disponible
  And que la distinción entre 400 y 422 es clara y consistente

# ============================================================================
# VALIDACIÓN DE MESA (tableId)
# ============================================================================

Scenario: CT-08-001-NEGATIVE | tableId=13 (fuera de rango): 422, no 400
  Given que envío un payload JSON válido pero tableId=13:
    """
    {
      "tableId": 13,
      "items": [
        {"productId": 1, "quantity": 1}
      ]
    }
    """
  When procesa POST a "/orders"
  Then el código de estado es 422 Unprocessable Entity
  And el cuerpo contiene:
    """
    {
      "status": 422,
      "error": "Unprocessable Entity",
      "message": "tableId must be between 1 and 12",
      "invalidField": "tableId",
      "receivedValue": 13
    }
    """
  And NO es 400 (el JSON es válido sintácticamente)

Scenario: CT-08-002-NEGATIVE | tableId=0 (por debajo de rango): 422
  Given que tableId=0
  When procesa POST
  Then el código es 422 Unprocessable Entity
  And el mensaje especifica el rango válido

Scenario: CT-08-003-CONTRAST | tableId="string" (tipo incorrecto): 400, no 422
  Given que envío tableId="mesa5" (tipo incorrecto)
  When procesa POST
  Then el código de estado es 400 Bad Request
  Y NO es 422 (el error es de tipo, no de lógica)

Scenario: CT-08-004-CONTRAST | tableId ausente en JSON: 400, no 422
  Given que omito tableId del payload
  When procesa POST
  Then el código es 400 Bad Request

# ============================================================================
# VALIDACIÓN DE ITEMS
# ============================================================================

Scenario: CT-08-005-NEGATIVE | items=[] (vacío): 422, no 400
  Given que envío un JSON válido con items=[]:
    """
    {
      "tableId": 5,
      "items": []
    }
    """
  When procesa POST
  Then el código es 422 Unprocessable Entity
  Y el mensaje: "Order must contain at least one item"

Scenario: CT-08-006-CONTRAST | items como string en lugar de array: 400
  Given que items="producto1,producto2" (tipo incorrecto)
  When procesa POST
  Then el código es 400 Bad Request

# ============================================================================
# VALIDACIÓN DE PRODUCTOS
# ============================================================================

Scenario: CT-08-007-NEGATIVE | productId inexistente: 422
  Given que productId=999 no existe en la DB:
    """
    {
      "tableId": 5,
      "items": [
        {"productId": 999, "quantity": 1}
      ]
    }
    """
  When procesa POST
  Then el código es 422 Unprocessable Entity
  Y el mensaje: "Product with id 999 does not exist"

Scenario: CT-08-008-NEGATIVE | productId inactivo (is_active=false): 422
  Given que productId=2 existe pero is_active=false
  When procesa POST con productId=2
  Then el código es 422 Unprocessable Entity
  Y el mensaje: "Product is not available"

Scenario: CT-08-009-CONTRAST | productId como string: 400
  Given que productId="uno" (tipo incorrecto)
  When procesa POST
  Then el código es 400 Bad Request

Scenario: CT-08-010-NEGATIVE | quantity=0 (cantidad inválida): 422
  Given que quantity=0:
    """
    {
      "tableId": 5,
      "items": [
        {"productId": 1, "quantity": 0}
      ]
    }
    """
  When procesa POST
  Then el código es 422 Unprocessable Entity
  Y el mensaje: "quantity must be greater than 0"

Scenario: CT-08-011-CONTRAST | quantity como string: 400
  Given que quantity="dos" (tipo incorrecto)
  When procesa POST
  Then el código es 400 Bad Request

# ============================================================================
# VALIDACIÓN DE TRANSICIONES DE ESTADO
# ============================================================================

Scenario: CT-08-012-NEGATIVE | Transición inválida: 422, no 400
  Given que una orden está en PENDING:
    ```
    {
      "status": "READY"
    }
    ```
  When envío PATCH a "/orders/{id}/status" con payload válido pero transición inválida
  Then el código es 422 Unprocessable Entity
  Y el mensaje: "Invalid state transition from PENDING to READY"

Scenario: CT-08-013-NEGATIVE | Estado inválido (valor no existente): 400, no 422
  Given que status="COMPLETED" (no existe):
    ```
    {
      "status": "COMPLETED"
    }
    ```
  When procesa PATCH
  Then el código es 400 Bad Request o 422 Unprocessable Entity
  # Ambos son razonables: 400 si es error de enum, 422 si es validación de negocio

# ============================================================================
# DIFERENCIACIÓN CLARA ENTRE 400 Y 422
# ============================================================================

Scenario: CT-08-014-RULE | Matriz de Decisión: 400 vs 422
  Given que debo decidir entre 400 y 422:
    | Escenario | Código | Razón |
    | JSON mal formado | 400 | Error sintáctico |
    | Tipo de dato incorrecto | 400 | Error de tipo |
    | Campo faltante requerido | 400 | Error de estructura |
    | tableId válido pero fuera de rango | 422 | Violación de regla de negocio |
    | Producto inexistente | 422 | Violación de referencia de negocio |
    | Cantidad 0 | 422 | Violación de lógica de negocio |
    | Transición inválida | 422 | Violación de máquina de estados |
  Then cada endpoint respeta esta matriz consistentemente

Scenario: CT-08-015-CONSISTENCY | order-service y report-service usan 422 uniformemente
  Given que valido endpoints en ambos servicios
  When encuentro un escenario idéntico en ambos
  Then ambos retornan 422 (no mezcla con 400 por el mismo error)

# ============================================================================
# MENSAJE DE ERROR DESCRIPTIVO EN 422
# ============================================================================

Scenario: CT-08-016-UX | Mensaje de 422 es descriptivo para el cliente
  Given que recibo 422 Unprocessable Entity
  When leo el mensaje
  Then es claro qué validación falló:
    - "tableId must be between 1 and 12" (vs. "Invalid input")
    - "Product with id 999 does not exist" (vs. "Validation error")
    - "quantity must be greater than 0" (vs. "Bad value")

Scenario: CT-08-017-OPTIONAL | 422 puede incluir detalles de validación
  Given que recibo 422
  When examino la respuesta
  Then puede contener un campo "details":
    """
    {
      "status": 422,
      "message": "Validation failed",
      "details": {
        "tableId": "must be between 1 and 12",
        "items[0].productId": "does not exist"
      }
    }
    """

# ============================================================================
# CASOS INTEGRADOS: 400 vs 422 en Flujos Reales
# ============================================================================

Scenario: CT-08-018-INTEGRATION | Crear orden con múltiples validaciones fallidas
  Given que envío:
    """
    {
      "tableId": "mesa5",
      "items": []
    }
    """
  When procesa POST
  Then el primer error detectado es tableId (400, tipo incorrecto)
  Y el cliente debe corregir eso antes de ver otros errores
  # Nota: El sistema puede retornar todos los errores a la vez, o parar en el primero

Scenario: CT-08-019-INTEGRATION | Crear orden con JSON válido pero negocio inválido
  Given que envío:
    """
    {
      "tableId": 25,
      "items": [
        {"productId": 999, "quantity": -1}
      ]
    }
    """
  When procesa POST
  Then el código es 422 Unprocessable Entity
  Y el mensaje especifica qué falló en la validación de negocio
  Y NO es 400 (pues el JSON es válido)

# ============================================================================
# ERRORES HTTP vs DOMAIN ERRORS
# ============================================================================

Scenario: CT-08-020-PHILOSOPHY | 422 es para errores de dominio, no de protocolo
  Given que defino:
    - Protocolo HTTP: JSON, formato, tipo de datos
    - Dominio de negocio: tableId 1-12, items ≥ 1, productos activos
  When valido una solicitud:
    - Error de protocolo → 400 Bad Request
    - Error de dominio → 422 Unprocessable Entity
  Then la respuesta al cliente es clara y utilizable

```

---

## Resumen General de Técnicas Aplicadas

| Técnica | HDUs | Descripción |
|---------|------|-------------|
| **Equivalence Partitioning** | 01, 02, 03, 04, 07, 08 | División de dominios en clases de equivalencia (ej: tableId ∈ [1,12] vs. fuera de rango) |
| **Boundary Value Analysis** | 01, 02, 04, 07, 08 | Pruebas en límites (tableId=1, tableId=12, tableId=0, tableId=13) |
| **State Transition Testing** | 07 | Matriz de transiciones válidas e inválidas de máquina de estados |
| **Error Guessing** | 03, 05, 06 | Identificación de escenarios de error probables (JSON malformado, tipos incorrectos) |
| **Decision Table Testing** | 08 | Matriz de decisión 400 vs 422 |
| **Contrast Testing** | 03, 08 | Contraste entre comportamientos similares (400 vs 422, éxito vs. fallo) |
| **Integration Testing** | 01, 04, 07, 08 | Pruebas del flujo completo (creación → actualización → lectura) |
| **Security Testing** | 04, 05, 06, 07 | Validación de autorización, exposición de información, auditoría |
| **Contract Testing** | 01, 02, 04, 05 | Validación de estructura de respuesta JSON |
| **Idempotence Testing** | 01, 04, 07 | Validación de operaciones repetidas |

---

## Notas de Implementación

### Próximas Fases

1. **Serenity BDD:** Convertir estos escenarios Gherkin a especificaciones ejecutables con Serenity/JBehave
2. **Test Automation:** Implementar runners de Cucumber/BDD en Maven con `maven-failsafe-plugin`
3. **PACT (Contract Testing):** Para validar contratos entre servicios (order-service ↔ kitchen-worker)
4. **Mutant Testing:** Verificar que las validaciones sean realmente efectivas

### Restricciones de Alcance

- **MVP:** No incluye autenticación de cliente (solo kitchen-worker); no hay token de idempotencia
- **Soft Delete:** Las órdenes pueden ser marcadas como deletedas, pero los reportes pueden incluirlas según configuración
- **Sin Versionamiento de Eventos:** El sistema espera eventVersion=1 estrictamente; cualquier otra versión va a DLQ

---

**Generado por:** Gema B (Generador de Casos de Prueba QA)  
**Formato:** BDD con Gherkin  
**Compatibilidad:** Serenity BDD, Cucumber, JBehave  
**Licencia:** Proyecto Sistemas-de-pedidos-restaurante
