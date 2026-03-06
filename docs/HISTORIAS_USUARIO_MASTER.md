# Historias de Usuario — Master — Sistema de Pedidos de Restaurante

> **Documento consolidado** — Combina `HDU_refinadas.md` (rama `develop`) y `USER_STORIES_REFINEMENT.md` (rama `feature/test-cases`).  
> Generadas mediante análisis INVEST con Gema A (Google Gemini). Fase de Diseño Inteligente — BDD Quality Engineering.  
> Incluye secciones de **Revisión humana** en HDU-07 y HDU-08.

---

## HDU-01 — Respuesta 201 Created con Header Location en Creación de Órdenes

**Como** Desarrollador del Frontend del Restaurante,  
**Quiero** que el endpoint de creación de órdenes responda con un estado `201 Created` y el encabezado `Location` con la URI del recurso,  
**Para** que la aplicación pueda redirigir o consultar el estado de la nueva orden de manera estandarizada y eficiente.

### Criterios de Aceptación

**Escenario 1 — Creación Exitosa (Happy Flow)**
```gherkin
Dado que el cliente envía una petición POST a /orders con un tableId entre 1 y 12 y al menos un producto activo
Cuando la API procesa la solicitud exitosamente
Entonces la respuesta debe tener el código 201 Created
Y el header Location debe ser la URL del recurso (ej: http://localhost:8080/orders/{uuid})
Y el evento order.placed debe publicarse en RabbitMQ
```

**Escenario 2 — Datos Inválidos (Error Flow)**
```gherkin
Dado que el cliente envía una petición POST a /orders con un tableId fuera de rango (ej: 13)
Cuando la API valida las reglas de negocio
Entonces la respuesta debe tener el código 400 Bad Request
Y no debe incluir un encabezado Location
```

**Escenario 3 — Verificación de Acceso (Alternative)**
```gherkin
Dado que una orden ha sido creada y se recibió el header Location
Cuando el cliente realiza un GET a la URL proporcionada usando X-Kitchen-Token
Entonces la API debe retornar el detalle con estado PENDING
Y debe incluir todos los ítems correspondientes
```

### Notas Adicionales
- **Sub-historias sugeridas:** Estandarización de respuestas de error siguiendo RFC 7807 · Implementación de HATEOAS para navegación dinámica.
- **Dependencias:** Requiere persistencia en `restaurant_db` para generación de UUID previa al header.
- **Riesgos:** Posible ruptura con versiones previas del Frontend. Riesgo de latencia bajo Reverse Proxy.

---

## HDU-02 — Respuesta 200 OK con Lista Vacía en Endpoints sin Registros

**Como** Desarrollador Frontend del Sistema de Pedidos,  
**Quiero** que los endpoints de consulta de órdenes, menú y reportes retornen un código de estado `200 OK` con una lista vacía cuando no existan registros,  
**Para** asegurar la consistencia del contrato de la API y evitar fallos en el renderizado de la interfaz por datos inexistentes.

### Criterios de Aceptación

**CA-1 — Menú sin productos**
```gherkin
Dado que no existen productos marcados como activos en la base de datos
Cuando el cliente realiza una petición GET al endpoint /menu
Entonces el sistema debe responder con un código 200 OK
Y el cuerpo debe ser exactamente []
```

**CA-2 — Reportes sin ventas**
```gherkin
Dado que no se han procesado órdenes READY en el rango de fechas
Cuando el administrador consulta GET /reports?startDate=...&endDate=...
Entonces el sistema responde con 200 OK
Y la lista de resultados en la respuesta debe estar vacía
```

**CA-3 — Acceso no autorizado (Flujo de Error)**
```gherkin
Dado que el endpoint requiere X-Kitchen-Token
Cuando se realiza la petición GET sin el encabezado correspondiente
Entonces el sistema debe retornar 401 Unauthorized
Y no debe retornar ningún arreglo ni dato
```

### Notas Adicionales
- **Sub-historias:** Estandarización en `order-service` · Estandarización en `report-service`.
- **Dependencias:** Los DTOs deben inicializar listas con `new ArrayList<>()` para que Jackson serialice `[]` y no `null`.
- **Riesgos:** Posible confusión en frontend entre vacío y error de carga. Evitar ocultar errores 400 retornando un vacío erróneo.

---

## HDU-03 — Validación de Estructura y Tipo de Datos con Respuesta 400

**Como** Desarrollador del Frontend del Sistema de Pedidos,  
**Quiero** que la API del servicio de órdenes y reportes valide la estructura y el tipo de los datos recibidos,  
**Para** recibir una respuesta `400 Bad Request` que detalle el error de entrada y me permita informar al usuario correctamente.

### Criterios de Aceptación

**Escenario 1 — JSON malformado**
```gherkin
Dado que el order-service está activo y disponible
Cuando el cliente realiza un POST /orders enviando un cuerpo con formato JSON inválido
Entonces el sistema debe responder con un código HTTP 400 Bad Request
Y el cuerpo de la respuesta debe contener un mensaje descriptivo del error de formato
```

**Escenario 2 — Tipo de dato incorrecto**
```gherkin
Dado que el endpoint POST /orders espera que tableId sea un número entero
Cuando el cliente envía una cadena de texto (ej. "mesa_uno")
Entonces el sistema debe responder con un código HTTP 400 Bad Request
Y detallar que tableId no corresponde con el tipo esperado
```

**Escenario 3 — Petición válida (Flujo feliz)**
```gherkin
Dado que el cliente envía una petición con JSON bien formado y tipos correctos
Cuando se procesa la petición
Entonces el sistema debe retornar un código HTTP 201 Created
Y el evento order.placed debe ser publicado en RabbitMQ
```

### Notas Adicionales
- **Sub-historias:** Implementación de estándar RFC 7807 · Validación de formatos ISO 8601 en `report-service`.
- **Dependencias:** Los DTOs deben estar correctamente anotados para la validación de Jackson.
- **Riesgos:** Impacto en Frontend si no está preparado para el nuevo formato. Saturación de Logs por peticiones inválidas.

---

## HDU-04 — Desactivación Lógica de Productos con Auditoría

**Como** Administrador del Sistema,  
**Quiero** desactivar productos del menú mediante una eliminación lógica que retorne una respuesta exitosa con detalles de auditoría,  
**Para** garantizar que el producto deje de estar disponible sin afectar reportes históricos y tener constancia de los cambios.

### Criterios de Aceptación

**Escenario 1 — Flujo Feliz**
```gherkin
Dado que existe un producto con ID 123 y estado isActive igual a true
Y estoy autenticado como Administrador
Cuando envío una solicitud DELETE al endpoint /products/123
Entonces el sistema debe responder con un código 200 OK
Y el cuerpo debe contener el estado desactivado
Y debe incluir metadatos de auditoría: timestamp, user_id y action_type (DEACTIVATION)
Y en la base de datos el campo isActive debe ser false
```

**Escenario 2 — Producto ya desactivado (Edge Case)**
```gherkin
Dado que existe un producto con ID 456 y estado isActive igual a false
Cuando envío una solicitud DELETE al endpoint /products/456
Entonces el sistema debe responder con un código 200 OK
Y el mensaje debe indicar que el recurso ya se encontraba en estado desactivado
Y debe incluir los metadatos de auditoría actualizados
```

**Escenario 3 — Producto no encontrado (Error)**
```gherkin
Dado que no existe ningún producto con el ID 999
Cuando envío una solicitud DELETE al endpoint /products/999
Entonces el sistema debe responder con un código 404 Not Found
Y el cuerpo debe especificar que el producto no fue encontrado
```

### Notas Adicionales
- **Sub-historias:** JPA Auditing para capturar automáticamente `updated_at` · Filtrado explícito de productos activos en `GET /menu`.
- **Dependencias:** Campos de auditoría (`updated_at`, `updated_by`) en `restaurant_db`.
- **Riesgos:** Confusión con expectativa de `204 No Content` en DELETE estándar. Posible evento asincrónico necesario en `report-service`.

---

## HDU-05 — Respuestas de Error JSON Estandarizadas (Desarrollador / Consumidor API)

**Como** Desarrollador del Frontend o consumidor de la API,  
**Quiero** que los microservicios `order-service` y `report-service` devuelvan un objeto JSON estandarizado ante cualquier falla,  
**Para** que la aplicación cliente pueda gestionar las excepciones de manera predecible y uniforme.

> **Estructura de error esperada:** `timestamp` · `status` · `error` · `path` · `message`

### Criterios de Aceptación

**CA-01 — Validación de reglas de negocio**
```gherkin
Dado que un cliente intenta crear una orden enviando un tableId fuera de rango (1-12)
Cuando realiza una solicitud POST a /orders
Entonces el sistema debe responder con 400 Bad Request
Y el body debe ser un JSON con los campos: timestamp, status, error, path, message
```

**CA-02 — Acceso sin autorización**
```gherkin
Dado que un usuario intenta consultar órdenes mediante GET /orders
Cuando no incluye el header X-Kitchen-Token: cocina123
Entonces el sistema debe responder con 401 Unauthorized
Y la estructura JSON de error debe ser estandarizada
```

**CA-03 — Error inesperado del servidor**
```gherkin
Dado que ocurre una falla interna en el microservicio report-service
Cuando un administrador intenta acceder a GET /reports
Entonces el sistema responde 500 Internal Server Error
Y los detalles técnicos sensibles deben estar ocultos en la respuesta
```

### Notas Adicionales
- **Sub-historias:** Librería común de manejo global de excepciones en Spring Boot 3.2 · Actualizar interceptor de errores en React.
- **Dependencias:** Se sugiere adoptar RFC 7807 (Problem Details for HTTP APIs).
- **Riesgos:** Cambio en estructura de error puede romper integraciones actuales. Requiere plan de retrocompatibilidad.

---

## HDU-06 — Respuestas de Error JSON Estandarizadas (Seguridad — Sin Datos Técnicos)

**Como** Responsable de Seguridad del Sistema,  
**Quiero** que todas las respuestas de error emitidas por las APIs del ecosistema sigan un formato JSON estandarizado y omitan detalles técnicos internos,  
**Para** prevenir la exposición de vulnerabilidades y garantizar una comunicación profesional y segura con los consumidores del servicio.

### Criterios de Aceptación

**Escenario 1 — Validación de negocio**
```gherkin
Dado que el cliente intenta realizar un pedido con un tableId inválido (ejemplo: 25)
Cuando se envía la solicitud POST al endpoint /orders
Entonces el sistema debe retornar un código de estado 400 Bad Request
Y el cuerpo debe incluir un mensaje legible sin mencionar clases de Java o restricciones de base de datos
```

**Escenario 2 — Recurso no encontrado**
```gherkin
Dado que un usuario de Cocina intenta acceder a una orden con un UUID inexistente
Cuando se envía la solicitud GET con el header X-Kitchen-Token válido
Entonces el sistema debe retornar un código de estado 404 Not Found
Y la respuesta debe ser un JSON limpio, sin rutas de archivos ni logs de Hibernate
```

**Escenario 3 — Error inesperado de servidor**
```gherkin
Dado que el microservicio report-service pierde conexión con su base de datos
Cuando el administrador intenta generar un reporte mediante GET /reports
Entonces el sistema debe retornar un código de estado 500 Internal Server Error
Y el mensaje debe ser genérico, sin stack traces ni detalles de conexión JDBC
```

### Notas Adicionales
- **Sub-historias:** Librería común de manejo de excepciones para los tres microservicios · Actualizar interceptores de error en Frontend (React).
- **Riesgos:** Mientras se oculta la información al usuario, los logs internos (Docker/CloudWatch) deben capturar el error completo para debugging técnico.

---

## HDU-07 — Validación de Máquina de Estados en Transiciones de Órdenes

**Como** Sistema de Cocina,  
**Quiero** que la API valide que los cambios de estado de un pedido sigan el flujo permitido (`PENDING → IN_PREPARATION → READY`),  
**Para** que el sistema me notifique explícitamente mediante un error de conflicto cuando se rompan las reglas.

### Criterios de Aceptación

**Escenario 1 — Transición válida (Flujo Feliz)**
```gherkin
Dado que existe un pedido con estado PENDING
Y se proporciona un token de cocina válido
Cuando se envía una solicitud PATCH a /orders/{id}/status con IN_PREPARATION
Entonces el sistema debe actualizar el estado del pedido exitosamente
Y debe retornar un código HTTP 200 OK
```

**Escenario 2 — Salto de estado prohibido**
```gherkin
Dado que existe un pedido con estado PENDING
Y se proporciona un token de cocina válido
Cuando se envía una solicitud PATCH con READY (saltando IN_PREPARATION)
Entonces el sistema no debe actualizar el estado
Y debe retornar un código HTTP 409 Conflict
```

**Escenario 3 — Transición de estado hacia atrás**
```gherkin
Dado que existe un pedido con estado IN_PREPARATION
Y se proporciona un token de cocina válido
Cuando se envía una solicitud PATCH con PENDING
Entonces el sistema debe denegar el cambio
Y debe retornar 409 Conflict
```

### Notas Adicionales
- **Sub-historias:** Implementación del `GlobalExceptionHandler` · Lógica de validación de máquina de estados en el dominio.
- **Riesgos:** Inconsistencia si el frontend no maneja el código 409 correctamente, mostrando un mensaje genérico al usuario.

### Revisión humana
> ⚠️ **Conflicto de código HTTP con HDU-08:** Esta historia retorna `409 Conflict` ante una transición de estado inválida, mientras que HDU-08 retorna `422 Unprocessable Entity` para el mismo tipo de error (violación de la máquina de estados). Tener dos códigos HTTP diferentes para el mismo escenario genera inconsistencia en el backend y obliga al frontend a manejar dos caminos de error distintos, aumentando la complejidad innecesariamente y el riesgo de bugs. **Se recomienda unificar en un único código HTTP** — preferiblemente `409 Conflict` para conflictos de estado de negocio, ya que semánticamente describe mejor una colisión con el estado actual del recurso.
>
> 💡 **Mejora de redacción:** El rol "Sistema de Cocina" como actor es poco convencional en formato de historia de usuario. Sería más preciso usar "Operador de Cocina" o "Personal de Cocina" para hacer la historia más legible y alineada con el estándar INVEST.

---

## HDU-08 — Respuesta 422 Unprocessable Entity para Errores de Validación de Negocio

**Como** Desarrollador del Frontend,  
**Quiero** que los microservicios retornen el código HTTP `422 Unprocessable Entity` ante fallos de validación de negocio,  
**Para** presentar al usuario final mensajes de error específicos y diferenciar errores de lógica de errores técnicos de comunicación.

### Criterios de Aceptación

**Escenario 1 — Creación de orden con mesa inválida**
```gherkin
Dado que el cliente está intentando realizar un pedido
Cuando envía una petición POST a /orders con un tableId mayor a 12
Entonces el sistema debe responder con un código de estado HTTP 422 Unprocessable Entity
Y el cuerpo de la respuesta debe contener un mensaje indicando que el número de mesa es inválido
```

**Escenario 2 — Transición de estado inválida**
```gherkin
Dado que existe una orden con estado READY en el sistema
Cuando la cocina intenta actualizar el estado a IN_PREPARATION mediante una petición PATCH
Entonces el servicio debe rechazar el cambio con un código HTTP 422 Unprocessable Entity
Y la orden debe mantener su estado READY original
```

**Escenario 3 — JSON malformado (distinción 400 vs 422)**
```gherkin
Dado que el microservicio order-service está operativo
Cuando se envía un POST con un JSON que tiene una llave sin cerrar
Entonces el sistema debe responder con un código HTTP 400 Bad Request
Y esto confirma la distinción entre error de formato (400) y error de negocio (422)
```

### Notas Adicionales
- **Sub-historias:** Estandarización del objeto `ErrorResponse` · Validaciones personalizadas en Spring para `tableId` (1-12).
- **Dependencias:** Requiere interceptores Axios/Fetch configurados en el frontend para el código 422.
- **Riesgos:** Inconsistencia entre servicios si `order-service` y `report-service` no aplican el mismo criterio. No confundir errores Auth (401/403) con lógica de negocio.

### Revisión humana
> ⚠️ **Conflicto de código HTTP con HDU-07:** El Escenario 2 de esta historia retorna `422 Unprocessable Entity` para una transición de estado inválida (`READY → IN_PREPARATION`), mientras que HDU-07 retorna `409 Conflict` para el mismo tipo de fallo. Usar dos códigos HTTP distintos para la misma clase de error (violación de regla de negocio en la máquina de estados) genera ambigüedad en el contrato de la API y complejidad innecesaria en el cliente. **Se recomienda consolidar en un único código** para todos los errores de transición de estado — `409 Conflict` es la elección más semánticamente correcta para este contexto.
>
> 💡 **Mejora de alcance:** El Escenario 1 (mesa inválida) corresponde a una validación de entrada de datos, no a un error de lógica de negocio avanzada. Podría encajar mejor en HDU-03 (validación 400) o unificarse en un único estándar de error de negocio con HDU-05. Tener dos HDUs solapados en validaciones puede confundir al equipo sobre cuál implementar primero.
