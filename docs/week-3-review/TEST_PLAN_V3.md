# TEST_PLAN_V3.md — Plan de Pruebas del Sistema de Pedidos de Restaurante

**Versión:** 3.0  
**Fecha:** 25 de febrero de 2026  
**Fase:** Re-Arquitectura y API REST (DEV)  
**Documentos de referencia:**
- `docs/week-3-review/ARCHITECTURE.md` — Visión arquitectónica y contrato REST
- `docs/week-3-review/REST_API_AUDIT.md` — Auditoría de endpoints
- `docs/TEST_PLAN.md` — Plan de pruebas v1 (módulo de reportes)

---

## Índice

1. [Estrategia de calidad](#1-estrategia-de-calidad)
2. [Pruebas de integración — Endpoints REST](#2-pruebas-de-integración--endpoints-rest)
3. [Casos de prueba en Gherkin](#3-casos-de-prueba-en-gherkin)
4. [Matriz de pruebas](#4-matriz-de-pruebas)
5. [Gestión de riesgos](#5-gestión-de-riesgos)

---

## 1. Estrategia de calidad

### 1.1 Objetivo del plan

Establecer la estrategia integral de verificación y validación del sistema de pedidos de restaurante en el contexto de la Fase de Re-Arquitectura y API REST. Este plan evoluciona la versión anterior (centrada exclusivamente en `report-service`) hacia una cobertura completa de los tres servicios (`order-service`, `kitchen-worker`, `report-service`), con énfasis particular en:

- La conformidad de los endpoints REST con el contrato formalizado en `ARCHITECTURE.md`.
- La corrección semántica de los códigos HTTP según la auditoría técnica.
- La consistencia del manejo de errores y la estructura `ErrorResponse` unificada.
- La integridad de las operaciones de soft delete y sus metadatos de auditoría.
- La protección contra filtración de información interna en respuestas de error.

### 1.2 Alcance

#### Incluido

| Componente | Servicios | Cobertura |
|---|---|---|
| API REST — `order-service` | `POST /orders`, `GET /orders`, `GET /orders/{id}`, `PATCH /orders/{id}/status`, `DELETE /orders/{id}`, `DELETE /orders`, `GET /menu` | Contrato HTTP, validación, seguridad, soft delete |
| API REST — `report-service` | `GET /reports` | Contrato HTTP, manejo de errores, estructura `ErrorResponse` |
| Mensajería AMQP | `order-service → kitchen-worker`, `order-service → report-service` | Publicación de `order.placed`, consumo idempotente, DLQ |
| Seguridad | Interceptor de cocina (`X-Kitchen-Token`) | Diferenciación `401`/`403`, no filtración de datos internos |
| Máquina de estados | `PENDING → IN_PREPARATION → READY` | Transiciones válidas e inválidas |
| Soft delete | Entidad `Order` (`deleted`, `deleted_at`) | Consistencia, auditoría, exclusión en queries |
| Validación de dominio | `tableId` (1–12), productos activos, items no vacíos | Reglas de negocio en capas de servicio y DTO |

#### Excluido

| Componente | Justificación |
|---|---|
| Pruebas de rendimiento y carga | Fuera del alcance de esta fase; planificado como actividad posterior |
| Pruebas de UI End-to-End (Cypress/Playwright) | El frontend React se valida con pruebas de componente y hooks; E2E requiere infraestructura completa |
| Despliegue en ambientes productivos | Esta fase opera exclusivamente en el entorno de desarrollo local con Docker Compose |
| Refactorización a Clean Architecture | Solo se valida la corrección del contrato actual; la restructuración interna de paquetes es objeto de fases posteriores (Strangler Fig Fases 1–4) |
| Optimistic locking / ETag | Decisión arquitectónica pendiente (ADR-004 / ADR-006) |

### 1.3 Niveles de prueba

#### Nivel 1 — Pruebas unitarias

| Aspecto | Detalle |
|---|---|
| **Objetivo** | Validar lógica de dominio, validadores, mappers y servicios en aislamiento completo |
| **Alcance** | `OrderValidator`, `OrderStatus.validateTransition()`, `OrderMapper`, `OrderEventBuilder`, `DateRangeFilter`, `ReportAggregationService`, `OrderPlacedEventValidator` |
| **Dependencias** | Sin framework de aplicación. Dobles de prueba vía Mockito para puertos de salida |
| **Criterio de éxito** | Cobertura ≥ 80% en clases de dominio y servicio; 100% de transiciones de estado cubiertas |
| **Herramientas** | JUnit 5, Mockito, AssertJ, jqwik (property-based) |

**Evolución desde v1:** El plan anterior cubría unitarias solo en `report-service` (DateRangeFilter, ReportAggregationService). Esta versión extiende la cobertura a `order-service` (validación de órdenes, transiciones de estado, construcción de eventos) y `kitchen-worker` (validación de contratos de eventos).

#### Nivel 2 — Pruebas de integración

| Aspecto | Detalle |
|---|---|
| **Objetivo** | Verificar la interacción entre capas (controlador → servicio → repositorio) y la conformidad del contrato HTTP |
| **Alcance** | Todos los endpoints REST de `order-service` y `report-service`; consumo de eventos en `kitchen-worker` |
| **Dependencias** | Spring Test Context, MockMvc, H2 in-memory, `spring-rabbit-test` |
| **Criterio de éxito** | Cada código HTTP documentado en `ARCHITECTURE.md §2` tiene al menos un test que lo valida |
| **Herramientas** | Spring Boot Test, MockMvc, `@DataJpaTest`, TestContainers (planificado) |

**Evolución desde v1:** El plan anterior validaba integración solo en `ReportControllerIntegrationTest` y `OrderReportRepositoryIntegrationTest`. Esta versión incorpora pruebas de contrato HTTP para los 7 endpoints de `order-service`, incluyendo validación de headers `Location`, estructura `ErrorResponse` y soft delete.

#### Nivel 3 — Pruebas manuales

| Aspecto | Detalle |
|---|---|
| **Objetivo** | Verificar escenarios exploratorios, flujos end-to-end con el stack Docker completo y validaciones de seguridad |
| **Alcance** | Flujo completo: crear orden → verificar evento → ver en cocina → cambiar estado → verificar reporte |
| **Herramientas** | cURL, Postman/Insomnia, RabbitMQ Management UI (`localhost:15672`), Swagger UI (`localhost:8080/swagger-ui.html`) |
| **Criterio de éxito** | Checklist de smoke test aprobado sin defectos bloqueantes |

#### Nivel 4 — Pruebas automatizadas de contrato

| Aspecto | Detalle |
|---|---|
| **Objetivo** | Garantizar que los esquemas de eventos AMQP son compatibles entre productor y consumidores |
| **Alcance** | Esquema de `OrderPlacedEvent` entre `order-service` (productor) y `kitchen-worker` / `report-service` (consumidores) |
| **Estado** | Planificado (ADR-005 pendiente). Actualmente cubierto parcialmente por `OrderPlacedEventValidator` en `kitchen-worker` |
| **Herramientas** | Spring Cloud Contract o Pact (por evaluar) |

### 1.4 Herramientas utilizadas

| Herramienta | Propósito | Servicio(s) |
|---|---|---|
| **JUnit 5** | Framework de pruebas unitarias e integración | Todos |
| **Mockito** | Dobles de prueba (mocks, stubs, spies) | `order-service`, `report-service` |
| **AssertJ** | Aserciones fluidas y legibles | Todos |
| **jqwik** | Property-based testing para invariantes matemáticas | `report-service` |
| **MockMvc** | Simulación de requests HTTP sin servidor real | `order-service`, `report-service` |
| **H2 Database** | Base de datos in-memory para tests de integración | Todos |
| **spring-rabbit-test** | Simulación de mensajería AMQP en tests | `order-service`, `kitchen-worker` |
| **Vitest** | Framework de pruebas frontend (hooks, componentes) | Frontend React |
| **React Testing Library** | Testing de componentes React | Frontend React |
| **Docker Compose** | Stack completo para pruebas manuales y smoke tests | Infraestructura |
| **cURL / Postman** | Ejecución manual de requests HTTP | Pruebas manuales |

### 1.5 Criterios de entrada

| Criterio | Verificación |
|---|---|
| Documento `ARCHITECTURE.md` revisado y aprobado por el equipo | El contrato REST es la fuente de verdad para las aserciones |
| Historias de usuario (`HDU.md`) completas con criterios de aceptación Gherkin | Cada historia tiene al menos un escenario feliz y un escenario de error |
| Ambiente de desarrollo funcional | Docker Compose levanta los 3 servicios + PostgreSQL × 3 + RabbitMQ sin errores |
| Código compilable sin errores | `mvn compile` exitoso en los 3 módulos |
| Base de datos migrada | Flyway ejecuta todas las migraciones sin conflictos |
| Suite de pruebas anterior (v1) ejecutándose sin fallos | Los 61 tests de `order-service` y 13 de `kitchen-worker` pasan |

### 1.6 Criterios de salida

| Criterio | Umbral |
|---|---|
| Todos los tests automatizados pasan | 100% green |
| Cobertura de línea en clases de servicio y dominio | ≥ 80% |
| Cada código HTTP del contrato tiene test de integración | 100% de los códigos documentados en §2 de ARCHITECTURE.md |
| Estructura `ErrorResponse` validada en todos los endpoints de error | Campos `timestamp`, `status`, `error`, `message`, `path` presentes |
| Ningún test expone información interna en respuestas de error `500` | 0 filtraciones detectadas |
| Smoke test manual aprobado | Flujo completo POST → evento → cocina → PATCH → reporte verificado |
| Defectos críticos y altos resueltos | 0 defectos abiertos de severidad crítica o alta |
| Historias de usuario HDU-01 a HDU-08 cubiertas | Cada historia tiene al menos un test automatizado asociado |

---

## 2. Pruebas de integración — Endpoints REST

Esta sección define las pruebas de integración que validan la conformidad de cada endpoint con el contrato REST formalizado en `ARCHITECTURE.md §2`. Cada prueba verifica códigos HTTP, headers, estructura de respuesta y comportamiento de error.

### 2.1 POST /orders — Crear orden

| ID | Escenario | Request | Código esperado | Validación |
|---|---|---|---|---|
| INT-POST-01 | Creación exitosa con payload válido | `{"tableId":5,"items":[{"productId":1,"quantity":2}]}` | `201 Created` | Body contiene `id`, `status: "PENDING"`, `items[].productName`; header `Location` presente con patrón `/orders/{uuid}` |
| INT-POST-02 | `tableId` menor a 1 | `{"tableId":0,"items":[{"productId":1,"quantity":1}]}` | `400 Bad Request` | Body sigue estructura `ErrorResponse`; campo `message` menciona tableId |
| INT-POST-03 | `tableId` mayor a 12 | `{"tableId":13,"items":[{"productId":1,"quantity":1}]}` | `400 Bad Request` | Body sigue estructura `ErrorResponse` |
| INT-POST-04 | Items vacíos | `{"tableId":5,"items":[]}` | `400 Bad Request` | Body sigue estructura `ErrorResponse` |
| INT-POST-05 | Producto inexistente | `{"tableId":5,"items":[{"productId":99999,"quantity":1}]}` | `404 Not Found` | Body sigue estructura `ErrorResponse` |
| INT-POST-06 | Producto inactivo | `{"tableId":5,"items":[{"productId":<inactivo>,"quantity":1}]}` | `422 Unprocessable Entity` | Body sigue estructura `ErrorResponse`; mensaje distingue "inactivo" de "no encontrado" |
| INT-POST-07 | JSON malformado | `{invalid json}` | `400 Bad Request` | Body sigue estructura `ErrorResponse` |
| INT-POST-08 | `tableId` nulo | `{"items":[{"productId":1,"quantity":1}]}` | `400 Bad Request` | Validación de Bean Validation (`@NotNull`) |
| INT-POST-09 | Header `Location` válido | Payload válido | `201 Created` | `Location` contiene UUID válido; `GET` al URL del header retorna la orden creada |

**Aserciones transversales para todos los `201`:**
- Header `Content-Type: application/json` presente.
- Header `Location` coincide con el patrón `/orders/[0-9a-f-]{36}`.
- Campo `status` siempre es `"PENDING"`.
- Campo `createdAt` es un timestamp ISO 8601 válido.

### 2.2 GET /orders — Listar órdenes

| ID | Escenario | Request | Código esperado | Validación |
|---|---|---|---|---|
| INT-LIST-01 | Listar sin filtro (órdenes existen) | `GET /orders` | `200 OK` | Body es `Array<OrderResponse>` con al menos un elemento |
| INT-LIST-02 | Listar sin resultados | `GET /orders` (BD vacía) | `200 OK` | Body es `[]` (arreglo vacío), **no** `204` |
| INT-LIST-03 | Filtrar por estado válido | `GET /orders?status=PENDING` | `200 OK` | Todas las órdenes en respuesta tienen `status: "PENDING"` |
| INT-LIST-04 | Filtrar por múltiples estados | `GET /orders?status=PENDING,IN_PREPARATION` | `200 OK` | Órdenes solo con status `PENDING` o `IN_PREPARATION` |
| INT-LIST-05 | Filtrar por estado inválido | `GET /orders?status=INVALID_STATUS` | `400 Bad Request` | Body sigue `ErrorResponse`; **no** retorna `500` |
| INT-LIST-06 | Órdenes eliminadas excluidas | Soft-delete previo + `GET /orders` | `200 OK` | Las órdenes eliminadas lógicamente no aparecen en la respuesta |

### 2.3 GET /orders/{id} — Obtener orden por ID

| ID | Escenario | Request | Código esperado | Validación |
|---|---|---|---|---|
| INT-GET-01 | Orden existente | `GET /orders/{uuid-válido}` | `200 OK` | Body contiene `id`, `tableId`, `status`, `items`, `createdAt`, `updatedAt` |
| INT-GET-02 | UUID inexistente | `GET /orders/{uuid-válido-pero-inexistente}` | `404 Not Found` | Body sigue `ErrorResponse` |
| INT-GET-03 | Formato de UUID inválido | `GET /orders/abc` | `400 Bad Request` | Body sigue `ErrorResponse`; **no** retorna `500`. Handler de `MethodArgumentTypeMismatchException` activo |
| INT-GET-04 | Orden eliminada (soft delete) | `GET /orders/{id-de-orden-eliminada}` | `404 Not Found` | Orden existe en BD con `deleted=true` pero no es accesible vía API |

### 2.4 PATCH /orders/{id}/status — Actualizar estado

| ID | Escenario | Request | Código esperado | Validación |
|---|---|---|---|---|
| INT-PATCH-01 | Transición válida PENDING → IN_PREPARATION | Body: `{"status":"IN_PREPARATION"}` + `X-Kitchen-Token` válido | `200 OK` | Body contiene orden con `status: "IN_PREPARATION"`; `updatedAt` actualizado |
| INT-PATCH-02 | Transición válida IN_PREPARATION → READY | Body: `{"status":"READY"}` + token válido | `200 OK` | Body contiene orden con `status: "READY"` |
| INT-PATCH-03 | Transición inválida PENDING → READY | Body: `{"status":"READY"}` sobre orden PENDING | `409 Conflict` | Body sigue `ErrorResponse`; **no** retorna `400` |
| INT-PATCH-04 | Transición inválida READY → IN_PREPARATION | Body: `{"status":"IN_PREPARATION"}` sobre orden READY | `409 Conflict` | Body sigue `ErrorResponse`; estado terminal respetado |
| INT-PATCH-05 | Transición inválida retroceso a PENDING | Body: `{"status":"PENDING"}` | `409 Conflict` | No se permite el retroceso |
| INT-PATCH-06 | Sin header `X-Kitchen-Token` | Body válido, sin header de autenticación | `401 Unauthorized` | Body sigue `ErrorResponse` |
| INT-PATCH-07 | Token inválido | Header `X-Kitchen-Token: token-incorrecto` | `403 Forbidden` | Body sigue `ErrorResponse`; diferenciación respecto a `401` |
| INT-PATCH-08 | UUID inválido en path | `PATCH /orders/not-a-uuid/status` | `400 Bad Request` | Handler de `MethodArgumentTypeMismatchException` |
| INT-PATCH-09 | Orden inexistente | UUID válido pero sin orden | `404 Not Found` | Body sigue `ErrorResponse` |
| INT-PATCH-10 | Body sin campo `status` | `{}` | `400 Bad Request` | Validación de `@NotNull` |

### 2.5 DELETE /orders/{id} — Soft delete individual

| ID | Escenario | Request | Código esperado | Validación |
|---|---|---|---|---|
| INT-DEL-01 | Eliminación exitosa | `DELETE /orders/{id}` + token válido | `200 OK` | Body contiene `deletedId` y `deletedAt`; `GET /orders/{id}` posterior retorna `404` |
| INT-DEL-02 | Orden ya eliminada | `DELETE /orders/{id-ya-eliminado}` + token | `404 Not Found` | Idempotencia: orden ya eliminada no es re-eliminable |
| INT-DEL-03 | Orden inexistente | `DELETE /orders/{uuid-inexistente}` + token | `404 Not Found` | Body sigue `ErrorResponse` |
| INT-DEL-04 | Sin token de cocina | `DELETE /orders/{id}` sin header | `401 Unauthorized` | Body sigue `ErrorResponse` |
| INT-DEL-05 | Token inválido | `DELETE /orders/{id}` con token erróneo | `403 Forbidden` | Body sigue `ErrorResponse` |
| INT-DEL-06 | UUID inválido | `DELETE /orders/not-uuid` + token | `400 Bad Request` | Handler de tipo mismatch |
| INT-DEL-07 | Verificación de metadatos de auditoría | Eliminar orden + inspeccionar respuesta | `200 OK` | `deletedAt` es timestamp ISO 8601 válido; campo presente y no nulo |

### 2.6 DELETE /orders — Soft delete masivo

| ID | Escenario | Request | Código esperado | Validación |
|---|---|---|---|---|
| INT-DELALL-01 | Eliminación masiva con confirmación | `DELETE /orders` + token + `X-Confirm-Destructive: true` | `200 OK` | Body contiene `deletedCount` > 0 y `deletedAt` |
| INT-DELALL-02 | Sin header de confirmación | `DELETE /orders` + token, sin `X-Confirm-Destructive` | `400 Bad Request` | Body sigue `ErrorResponse`; mensaje exige header de confirmación |
| INT-DELALL-03 | Confirmación en `false` | `DELETE /orders` + token + `X-Confirm-Destructive: false` | `400 Bad Request` | Body sigue `ErrorResponse` |
| INT-DELALL-04 | Sin token de cocina | `DELETE /orders` + confirmación, sin token | `401 Unauthorized` | Body sigue `ErrorResponse` |
| INT-DELALL-05 | Verificación post-eliminación | Eliminar todo + `GET /orders` | `200 OK` | `GET /orders` retorna `[]` |
| INT-DELALL-06 | Sin órdenes activas | `DELETE /orders` + token + confirmación (BD sin órdenes activas) | `200 OK` | `deletedCount: 0`; operación idempotente |

### 2.7 GET /menu — Productos activos

| ID | Escenario | Request | Código esperado | Validación |
|---|---|---|---|---|
| INT-MENU-01 | Menú con productos activos | `GET /menu` | `200 OK` | Body es `Array<ProductResponse>`; todos los elementos tienen `isActive: true` |
| INT-MENU-02 | Estructura del producto | `GET /menu` | `200 OK` | Cada producto contiene `id`, `name`, `description`, `price`, `category`, `imageUrl`, `isActive` |
| INT-MENU-03 | Productos inactivos excluidos | Desactivar producto + `GET /menu` | `200 OK` | Producto con `isActive: false` no aparece en la respuesta |

### 2.8 GET /reports — Reporte de ventas (report-service)

| ID | Escenario | Request | Código esperado | Validación |
|---|---|---|---|---|
| INT-RPT-01 | Reporte con datos válidos | `GET /reports?startDate=2026-02-01&endDate=2026-02-25` | `200 OK` | Body contiene `totalReadyOrders`, `totalRevenue`, `productBreakdown[]` |
| INT-RPT-02 | Formato de fecha inválido | `GET /reports?startDate=invalid&endDate=2026-02-25` | `400 Bad Request` | Body sigue `ErrorResponse` con `message` descriptivo; **no** body vacío |
| INT-RPT-03 | Rango invertido (start > end) | `GET /reports?startDate=2026-03-01&endDate=2026-02-01` | `422 Unprocessable Entity` | Body sigue `ErrorResponse`; código **no** es `400` (rango semánticamente inválido) |
| INT-RPT-04 | Parámetros faltantes | `GET /reports` (sin query params) | `400 Bad Request` | Body sigue `ErrorResponse` |
| INT-RPT-05 | Sin órdenes en rango | Rango válido sin órdenes READY | `200 OK` | `totalReadyOrders: 0`, `totalRevenue: 0`, `productBreakdown: []` |
| INT-RPT-06 | Solo órdenes READY incluidas | Órdenes PENDING + READY en rango | `200 OK` | Solo las READY contribuyen a métricas |

### 2.9 Validaciones transversales — Estructura ErrorResponse

Las siguientes aserciones se aplican a **todo** endpoint que retorne un código de error (4xx o 5xx):

| ID | Validación | Aserción |
|---|---|---|
| INT-ERR-01 | Estructura completa | Body contiene los campos `timestamp`, `status`, `error`, `message` |
| INT-ERR-02 | Campo `status` coincide con código HTTP | `response.body.status == response.statusCode` |
| INT-ERR-03 | Campo `error` es nombre estándar | Valor coincide con `HttpStatus.getReasonPhrase()` (e.g., `"Bad Request"`, `"Not Found"`) |
| INT-ERR-04 | `Content-Type` es JSON | Header `Content-Type: application/json` presente, **nunca** `text/html` |
| INT-ERR-05 | Sin filtración de datos internos en `500` | Campo `message` no contiene: stack traces, nombres de clase Java (`com.restaurant.*`), queries SQL, rutas del sistema de archivos |
| INT-ERR-06 | `timestamp` es ISO 8601 válido | Parseable como `LocalDateTime` sin excepción |

### 2.10 Validaciones transversales — Seguridad

| ID | Validación | Método |
|---|---|---|
| INT-SEC-01 | Error `500` genérico no expone `ex.getMessage()` | Provocar excepción inesperada; verificar que el campo `message` es genérico |
| INT-SEC-02 | Excepciones no controladas en `report-service` retornan JSON | Provocar error no capturado; verificar `Content-Type: application/json` (no HTML) |
| INT-SEC-03 | Token inválido retorna `403`, no `401` | Enviar token incorrecto; verificar código `403` |
| INT-SEC-04 | Token ausente retorna `401`, no `403` | Omitir header; verificar código `401` |

---

## 3. Casos de prueba en Gherkin

### 3.1 HDU-01 — Creación con 201 y Location

> **Como** consumidor de la API  
> **Quiero** que las operaciones de creación respondan con `201 Created` y el header `Location`  
> **Para** poder acceder al recurso recién creado de forma estándar.

**Principios INVEST:** Independiente (no depende de otra HDU), Negociable (el formato del header es convención REST), Valiosa (habilita descubrimiento estándar del recurso), Estimable (cambio localizado en `OrderController`), Small (un solo endpoint afectado), Testable (verificable por presencia del header).

```gherkin
Feature: Creación de recursos con semántica HTTP correcta

  Background:
    Given que el catálogo de productos contiene al menos un producto activo con id 1
    And que la mesa 5 se encuentra disponible

  Scenario: Crear una orden exitosamente retorna 201 con Location
    Given que el payload de la orden es válido:
      | tableId | 5 |
      | items   | [{"productId": 1, "quantity": 2}] |
    When se envía una solicitud POST a "/orders"
    Then la respuesta debe tener código 201
    And debe incluir el header "Location" con el patrón "/orders/{uuid}"
    And el cuerpo debe contener el campo "id" que coincide con el UUID del header Location
    And el campo "status" debe ser "PENDING"
    And el header "Content-Type" debe ser "application/json"

  Scenario: La URL del header Location es accesible
    Given que se creó una orden exitosamente vía POST a "/orders"
    And que la respuesta contenía el header "Location" con valor "/orders/{id}"
    When se envía una solicitud GET a la URL del header "Location"
    Then la respuesta debe tener código 200
    And el cuerpo debe contener la misma orden creada previamente

  Scenario: Creación fallida no incluye header Location
    Given que el payload contiene un tableId inválido de 0
    When se envía una solicitud POST a "/orders"
    Then la respuesta debe tener código 400
    And no debe incluir el header "Location"
    And el cuerpo debe seguir la estructura ErrorResponse
```

---

### 3.2 HDU-02 — Listas vacías con 200 OK

> **Como** consumidor de la API  
> **Quiero** recibir `200 OK` con un arreglo vacío cuando no existan resultados  
> **Para** evitar ambigüedad en la interpretación del frontend.

**Principios INVEST:** Independiente (aplicable a cualquier endpoint de listado), Valiosa (elimina ambigüedad 200 vs 204), Estimable (convención ya parcialmente implementada), Testable (verificable por tipo de respuesta).

```gherkin
Feature: Consulta de colecciones sin resultados

  Scenario: Obtener lista vacía de órdenes retorna 200 con arreglo vacío
    Given que no existen órdenes activas en el sistema
    When se envía una solicitud GET a "/orders"
    Then la respuesta debe tener código 200
    And el cuerpo debe ser un arreglo JSON vacío "[]"
    And no debe retornar código 204

  Scenario: Filtrar por estado sin coincidencias retorna 200 con arreglo vacío
    Given que existen órdenes solo con estado "PENDING"
    When se envía una solicitud GET a "/orders?status=READY"
    Then la respuesta debe tener código 200
    And el cuerpo debe ser un arreglo JSON vacío "[]"

  Scenario: Menú sin productos activos retorna 200 con arreglo vacío
    Given que todos los productos están marcados como inactivos
    When se envía una solicitud GET a "/menu"
    Then la respuesta debe tener código 200
    And el cuerpo debe ser un arreglo JSON vacío "[]"
```

---

### 3.3 HDU-03 — Errores de tipo retornan 400

> **Como** consumidor de la API  
> **Quiero** que los errores de formato o tipo incorrecto retornen `400 Bad Request`  
> **Para** diferenciar errores del cliente de errores internos del servidor.

**Principios INVEST:** Independiente, Valiosa (elimina el `500` espurio que confunde al frontend), Estimable (requiere handler de `MethodArgumentTypeMismatchException`), Small (un handler global), Testable (provocable con inputs malformados).

```gherkin
Feature: Manejo de errores de tipo de argumento

  Scenario: Enviar UUID con formato inválido en GET retorna 400
    Given que el endpoint "/orders/{id}" requiere un identificador UUID
    When se envía una solicitud GET a "/orders/abc"
    Then la respuesta debe tener código 400
    And el cuerpo debe seguir la estructura ErrorResponse
    And el campo "message" debe indicar formato inválido
    And no debe retornar código 500

  Scenario: Enviar UUID inválido en PATCH retorna 400
    Given que el endpoint "/orders/{id}/status" requiere un identificador UUID
    When se envía una solicitud PATCH a "/orders/not-a-uuid/status" con body:
      | status | IN_PREPARATION |
    Then la respuesta debe tener código 400
    And el cuerpo debe seguir la estructura ErrorResponse

  Scenario: Enviar enum inválido en filtro de estado retorna 400
    When se envía una solicitud GET a "/orders?status=INVALID_STATUS"
    Then la respuesta debe tener código 400
    And el cuerpo debe seguir la estructura ErrorResponse
    And no debe retornar código 500

  Scenario: Enviar UUID inválido en DELETE retorna 400
    Given que el header "X-Kitchen-Token" contiene un token válido
    When se envía una solicitud DELETE a "/orders/xyz"
    Then la respuesta debe tener código 400
    And el cuerpo debe seguir la estructura ErrorResponse
```

---

### 3.4 HDU-04 — Soft delete con respuesta consistente

> **Como** consumidor de la API  
> **Quiero** que las eliminaciones lógicas respondan con `200 OK` e incluyan metadatos de auditoría  
> **Para** confirmar que el recurso fue desactivado correctamente.

**Principios INVEST:** Independiente, Valiosa (proporciona trazabilidad de eliminación), Estimable (cambio en `OrderController` y servicio), Testable (verificable por presencia de campos `deletedAt` y `deletedBy`).

```gherkin
Feature: Eliminación lógica de recursos

  Background:
    Given que existe una orden activa con id "{orderId}"
    And que el header "X-Kitchen-Token" contiene un token válido

  Scenario: Eliminar lógicamente una orden existente retorna metadatos
    When se envía una solicitud DELETE a "/orders/{orderId}"
    Then la respuesta debe tener código 200
    And el cuerpo debe incluir el campo "deletedAt" con un timestamp ISO 8601 válido
    And el cuerpo debe incluir el campo "deletedId" con el valor "{orderId}"

  Scenario: Orden eliminada no aparece en listados posteriores
    When se envía una solicitud DELETE a "/orders/{orderId}"
    And luego se envía una solicitud GET a "/orders"
    Then la orden con id "{orderId}" no debe estar presente en la lista

  Scenario: Consultar orden eliminada retorna 404
    When se envía una solicitud DELETE a "/orders/{orderId}"
    And luego se envía una solicitud GET a "/orders/{orderId}"
    Then la respuesta debe tener código 404

  Scenario: Eliminar orden ya eliminada retorna 404
    Given que la orden con id "{orderId}" ya fue eliminada previamente
    When se envía una solicitud DELETE a "/orders/{orderId}"
    Then la respuesta debe tener código 404
    And el cuerpo debe seguir la estructura ErrorResponse

  Scenario: Eliminación masiva retorna conteo y requiere confirmación
    Given que existen 5 órdenes activas en el sistema
    And que el header "X-Confirm-Destructive" tiene valor "true"
    When se envía una solicitud DELETE a "/orders"
    Then la respuesta debe tener código 200
    And el campo "deletedCount" debe ser 5
    And el campo "deletedAt" debe ser un timestamp ISO 8601 válido
```

---

### 3.5 HDU-05 — Estructura de error unificada

> **Como** consumidor de la API  
> **Quiero** que todos los errores sigan una estructura estándar  
> **Para** manejar respuestas de error de manera consistente.

**Principios INVEST:** Independiente, Valiosa (permite parsing genérico de errores en el frontend), Estimable (requiere estandarizar `ErrorResponse` en `report-service`), Small (estructura ya existe en `order-service`, falta replicar), Testable (verificable con inspección de schema).

```gherkin
Feature: Estructura unificada de errores

  Scenario: Error de validación en order-service sigue ErrorResponse
    Given que se envía un payload inválido a "POST /orders"
    When el servicio responde con error
    Then la respuesta debe seguir la estructura ErrorResponse
    And debe incluir el campo "timestamp" con formato ISO 8601
    And debe incluir el campo "status" como número entero
    And debe incluir el campo "error" como cadena de texto
    And debe incluir el campo "message" como cadena de texto descriptiva

  Scenario: Error en report-service sigue la misma estructura
    Given que se envía un formato de fecha inválido a "GET /reports"
    When el servicio responde con error
    Then la respuesta debe seguir la estructura ErrorResponse con los mismos campos
    And el "Content-Type" debe ser "application/json"
    And no debe retornar una página HTML de error

  Scenario: Campo "status" coincide con el código HTTP real
    Given que se provoca un error 404 en "GET /orders/{id-inexistente}"
    When se inspecciona la respuesta
    Then el campo "status" en el body debe ser 404
    And el código HTTP de la respuesta debe ser 404

  Scenario: Error 500 usa estructura ErrorResponse sin HTML
    Given que se produce una excepción no controlada en el servicio
    When el sistema retorna un error 500
    Then el "Content-Type" debe ser "application/json"
    And el body debe contener la estructura ErrorResponse
    And no debe retornar contenido HTML
```

---

### 3.6 HDU-06 — Seguridad en mensajes de error

> **Como** responsable del sistema  
> **Quiero** que los mensajes de error no expongan información sensible  
> **Para** proteger detalles internos de implementación.

**Principios INVEST:** Independiente, Valiosa (previene vectores de reconocimiento de infraestructura), Estimable (cambio en el handler genérico de `500`), Small (una modificación puntual), Testable (verificable por inspección de contenido del campo `message`).

```gherkin
Feature: Protección de información sensible en respuestas de error

  Scenario: Error 500 no contiene stack trace
    Given que se produce una excepción no controlada en "order-service"
    When el sistema retorna un error con código 500
    Then el campo "message" no debe contener "at com.restaurant"
    And el campo "message" no debe contener "java.lang."
    And el campo "message" no debe contener ".java:"

  Scenario: Error 500 no contiene nombres de clases internas
    Given que se produce una excepción de base de datos
    When el sistema retorna un error con código 500
    Then el campo "message" no debe contener "PSQLException"
    And el campo "message" no debe contener "HibernateException"
    And el campo "message" no debe contener "JdbcSQLException"

  Scenario: Error 500 no contiene rutas del sistema de archivos
    Given que se produce una excepción inesperada
    When el sistema retorna un error con código 500
    Then el campo "message" no debe contener "C:\\"
    And el campo "message" no debe contener "/home/"
    And el campo "message" no debe contener "/var/"
    And el mensaje debe ser genérico como "An unexpected error occurred"

  Scenario: Error en report-service no expone detalles internos
    Given que se produce una excepción inesperada en "report-service"
    When el sistema retorna un error
    Then el campo "message" debe ser un texto genérico
    And no debe contener información de la base de datos
    And el "Content-Type" debe ser "application/json"
```

---

### 3.7 HDU-07 — Conflictos de estado retornan 409

> **Como** consumidor de la API  
> **Quiero** que las transiciones de estado inválidas retornen `409 Conflict`  
> **Para** distinguir conflictos de negocio de errores sintácticos.

**Principios INVEST:** Independiente, Valiosa (el frontend puede distinguir conflictos de validación), Estimable (cambio en `GlobalExceptionHandler` para `InvalidStatusTransitionException`), Small (un handler), Testable (provocable con transiciones ilegales).

```gherkin
Feature: Manejo de conflictos de estado

  Background:
    Given que el header "X-Kitchen-Token" contiene un token válido

  Scenario: Transición PENDING → READY retorna 409
    Given que una orden se encuentra en estado "PENDING"
    When se intenta cambiar su estado a "READY" vía PATCH
    Then la respuesta debe tener código 409
    And el cuerpo debe seguir la estructura ErrorResponse
    And el campo "error" debe ser "Conflict"

  Scenario: Transición READY → IN_PREPARATION retorna 409
    Given que una orden se encuentra en estado "READY"
    When se intenta cambiar su estado a "IN_PREPARATION" vía PATCH
    Then la respuesta debe tener código 409
    And el cuerpo debe seguir la estructura ErrorResponse

  Scenario: Transición READY → PENDING retorna 409
    Given que una orden se encuentra en estado "READY"
    When se intenta cambiar su estado a "PENDING" vía PATCH
    Then la respuesta debe tener código 409

  Scenario: Transición IN_PREPARATION → PENDING retorna 409
    Given que una orden se encuentra en estado "IN_PREPARATION"
    When se intenta cambiar su estado a "PENDING" vía PATCH
    Then la respuesta debe tener código 409

  Scenario: Transición válida no retorna 409
    Given que una orden se encuentra en estado "PENDING"
    When se intenta cambiar su estado a "IN_PREPARATION" vía PATCH
    Then la respuesta debe tener código 200
    And el campo "status" debe ser "IN_PREPARATION"
```

---

### 3.8 HDU-08 — Reglas de negocio retornan 422

> **Como** consumidor de la API  
> **Quiero** que las violaciones de reglas de negocio retornen `422 Unprocessable Entity`  
> **Para** diferenciar errores lógicos de errores de formato.

**Principios INVEST:** Independiente, Valiosa (distinción semántica entre 400 y 422), Estimable (requiere nuevo handler o modificación de existente), Testable (provocable con datos semánticamente inválidos).

```gherkin
Feature: Validaciones de reglas de negocio

  Scenario: Crear orden con producto inactivo retorna 422
    Given que el producto con id 99 existe pero está marcado como inactivo
    When se envía una solicitud POST a "/orders" con body:
      | tableId | 5                                    |
      | items   | [{"productId": 99, "quantity": 1}]   |
    Then la respuesta debe tener código 422
    And el cuerpo debe seguir la estructura ErrorResponse
    And el campo "message" debe indicar que el producto está inactivo
    And no debe retornar código 404

  Scenario: Solicitar reporte con rango de fechas inválido retorna 422
    Given que la fecha de inicio es "2026-03-15"
    And que la fecha de fin es "2026-03-01"
    When se envía una solicitud GET a "/reports?startDate=2026-03-15&endDate=2026-03-01"
    Then la respuesta debe tener código 422
    And el cuerpo debe seguir la estructura ErrorResponse
    And el campo "message" debe indicar que el rango de fechas es inválido

  Scenario: Distinguir 400 de 422 — formato vs. semántica
    Given que se envía una fecha con formato incorrecto "not-a-date"
    When se envía una solicitud GET a "/reports?startDate=not-a-date&endDate=2026-03-01"
    Then la respuesta debe tener código 400
    And no debe retornar código 422
```

---

### 3.9 Casos adicionales — Flujo completo de orden

```gherkin
Feature: Flujo completo de ciclo de vida de una orden

  Scenario: Ciclo de vida completo PENDING → IN_PREPARATION → READY
    Given que existe un producto activo con id 1
    When se crea una orden para la mesa 5 con el producto 1
    Then la orden debe tener estado "PENDING"
    When el personal de cocina cambia el estado a "IN_PREPARATION"
    Then la orden debe tener estado "IN_PREPARATION"
    When el personal de cocina cambia el estado a "READY"
    Then la orden debe tener estado "READY"
    And no debe ser posible cambiar el estado nuevamente

  Scenario: Soft delete preserva integridad del reporte
    Given que se creó una orden que alcanzó estado "READY"
    And que el report-service recibió el evento correspondiente
    When se elimina lógicamente la orden en order-service
    Then la orden debe desaparecer de "GET /orders"
    But el reporte generado en report-service debe seguir incluyendo la orden
```

---

## 4. Matriz de pruebas

### 4.1 Pruebas de order-service

| ID | Historia | Escenario | Tipo | Prioridad | Estado | Riesgo asociado |
|---|---|---|---|---|---|---|
| INT-POST-01 | HDU-01 | POST /orders — creación exitosa con Location | Integración | Alta | Pendiente | RSK-T01: Ausencia de header Location |
| INT-POST-02 | HDU-08 | POST /orders — tableId < 1 retorna 400 | Integración | Alta | Pendiente | RSK-T05: Validación incompleta |
| INT-POST-03 | HDU-08 | POST /orders — tableId > 12 retorna 400 | Integración | Alta | Pendiente | RSK-T05: Validación incompleta |
| INT-POST-04 | HDU-05 | POST /orders — items vacíos retorna 400 | Integración | Media | Pendiente | RSK-T05: Validación incompleta |
| INT-POST-05 | HDU-05 | POST /orders — producto inexistente retorna 404 | Integración | Media | Pendiente | RSK-T02: Códigos HTTP incorrectos |
| INT-POST-06 | HDU-08 | POST /orders — producto inactivo retorna 422 | Integración | Alta | Pendiente | RSK-T02: Códigos HTTP incorrectos |
| INT-POST-07 | HDU-05 | POST /orders — JSON malformado retorna 400 | Integración | Media | Pendiente | RSK-T02: Códigos HTTP incorrectos |
| INT-POST-08 | HDU-05 | POST /orders — tableId nulo retorna 400 | Integración | Media | Pendiente | RSK-T05: Validación incompleta |
| INT-POST-09 | HDU-01 | POST /orders — GET al Location retorna la orden | Integración | Alta | Pendiente | RSK-T01: Ausencia de header Location |
| INT-LIST-01 | HDU-02 | GET /orders — listar con resultados | Integración | Media | Pendiente | — |
| INT-LIST-02 | HDU-02 | GET /orders — lista vacía retorna 200 con [] | Integración | Alta | Pendiente | RSK-T02: Códigos HTTP incorrectos |
| INT-LIST-03 | — | GET /orders — filtrar por estado válido | Integración | Media | Pendiente | — |
| INT-LIST-04 | — | GET /orders — filtrar por múltiples estados | Integración | Media | Pendiente | — |
| INT-LIST-05 | HDU-03 | GET /orders — estado inválido retorna 400 (no 500) | Integración | Alta | Pendiente | RSK-T02: Códigos HTTP incorrectos |
| INT-LIST-06 | HDU-04 | GET /orders — órdenes eliminadas excluidas | Integración | Alta | Pendiente | RSK-T04: Fallas en soft delete |
| INT-GET-01 | — | GET /orders/{id} — orden existente | Integración | Media | Pendiente | — |
| INT-GET-02 | — | GET /orders/{id} — UUID inexistente retorna 404 | Integración | Media | Pendiente | — |
| INT-GET-03 | HDU-03 | GET /orders/{id} — UUID inválido retorna 400 (no 500) | Integración | Crítica | Pendiente | RSK-T02: Códigos HTTP incorrectos |
| INT-GET-04 | HDU-04 | GET /orders/{id} — orden eliminada retorna 404 | Integración | Alta | Pendiente | RSK-T04: Fallas en soft delete |
| INT-PATCH-01 | HDU-07 | PATCH — PENDING → IN_PREPARATION (válida) | Integración | Alta | Pendiente | — |
| INT-PATCH-02 | HDU-07 | PATCH — IN_PREPARATION → READY (válida) | Integración | Alta | Pendiente | — |
| INT-PATCH-03 | HDU-07 | PATCH — PENDING → READY retorna 409 | Integración | Crítica | Pendiente | RSK-T02: Códigos HTTP incorrectos |
| INT-PATCH-04 | HDU-07 | PATCH — READY → IN_PREPARATION retorna 409 | Integración | Alta | Pendiente | RSK-T02: Códigos HTTP incorrectos |
| INT-PATCH-05 | HDU-07 | PATCH — retroceso a PENDING retorna 409 | Integración | Alta | Pendiente | RSK-T02: Códigos HTTP incorrectos |
| INT-PATCH-06 | HDU-06 | PATCH — sin token retorna 401 | Integración | Alta | Pendiente | RSK-T03: Exposición de datos sensibles |
| INT-PATCH-07 | HDU-06 | PATCH — token inválido retorna 403 | Integración | Alta | Pendiente | RSK-T03: Exposición de datos sensibles |
| INT-PATCH-08 | HDU-03 | PATCH — UUID inválido retorna 400 | Integración | Alta | Pendiente | RSK-T02: Códigos HTTP incorrectos |
| INT-PATCH-09 | — | PATCH — orden inexistente retorna 404 | Integración | Media | Pendiente | — |
| INT-PATCH-10 | HDU-05 | PATCH — body sin status retorna 400 | Integración | Media | Pendiente | RSK-T05: Validación incompleta |
| INT-DEL-01 | HDU-04 | DELETE /orders/{id} — eliminación exitosa con metadatos | Integración | Crítica | Pendiente | RSK-T04: Fallas en soft delete |
| INT-DEL-02 | HDU-04 | DELETE /orders/{id} — orden ya eliminada retorna 404 | Integración | Alta | Pendiente | RSK-T04: Fallas en soft delete |
| INT-DEL-03 | — | DELETE /orders/{id} — orden inexistente retorna 404 | Integración | Media | Pendiente | — |
| INT-DEL-04 | HDU-06 | DELETE — sin token retorna 401 | Integración | Alta | Pendiente | RSK-T03: Exposición de datos |
| INT-DEL-05 | HDU-06 | DELETE — token inválido retorna 403 | Integración | Alta | Pendiente | RSK-T03: Exposición de datos |
| INT-DEL-06 | HDU-03 | DELETE — UUID inválido retorna 400 | Integración | Media | Pendiente | RSK-T02: Códigos HTTP incorrectos |
| INT-DEL-07 | HDU-04 | DELETE /orders/{id} — verificar metadatos auditoría | Integración | Alta | Pendiente | RSK-T04: Fallas en soft delete |
| INT-DELALL-01 | HDU-04 | DELETE /orders — masivo con confirmación | Integración | Crítica | Pendiente | RSK-T04: Fallas en soft delete |
| INT-DELALL-02 | HDU-04 | DELETE /orders — sin confirmación retorna 400 | Integración | Alta | Pendiente | RSK-T04: Fallas en soft delete |
| INT-DELALL-03 | HDU-04 | DELETE /orders — confirmación false retorna 400 | Integración | Alta | Pendiente | RSK-T04: Fallas en soft delete |
| INT-DELALL-04 | HDU-06 | DELETE /orders — sin token retorna 401 | Integración | Alta | Pendiente | RSK-T03: Exposición de datos |
| INT-DELALL-05 | HDU-04 | DELETE /orders — post-eliminación lista vacía | Integración | Alta | Pendiente | RSK-T04: Fallas en soft delete |
| INT-DELALL-06 | — | DELETE /orders — sin órdenes activas retorna 200 con count 0 | Integración | Media | Pendiente | — |

### 4.2 Pruebas de GET /menu

| ID | Historia | Escenario | Tipo | Prioridad | Estado | Riesgo asociado |
|---|---|---|---|---|---|---|
| INT-MENU-01 | HDU-02 | GET /menu — productos activos | Integración | Media | Pendiente | — |
| INT-MENU-02 | — | GET /menu — estructura del producto completa | Integración | Media | Pendiente | RSK-T06: Inconsistencias REST |
| INT-MENU-03 | — | GET /menu — inactivos excluidos | Integración | Media | Pendiente | — |

### 4.3 Pruebas de report-service

| ID | Historia | Escenario | Tipo | Prioridad | Estado | Riesgo asociado |
|---|---|---|---|---|---|---|
| INT-RPT-01 | — | GET /reports — reporte con datos válidos | Integración | Alta | Pendiente | — |
| INT-RPT-02 | HDU-05 | GET /reports — fecha inválida retorna 400 con ErrorResponse | Integración | Crítica | Pendiente | RSK-T06: Inconsistencias REST |
| INT-RPT-03 | HDU-08 | GET /reports — rango invertido retorna 422 | Integración | Alta | Pendiente | RSK-T02: Códigos HTTP incorrectos |
| INT-RPT-04 | HDU-05 | GET /reports — parámetros faltantes retorna 400 | Integración | Alta | Pendiente | RSK-T06: Inconsistencias REST |
| INT-RPT-05 | HDU-02 | GET /reports — sin órdenes retorna métricas en cero | Integración | Media | Pendiente | — |
| INT-RPT-06 | — | GET /reports — solo órdenes READY incluidas | Integración | Alta | Pendiente | — |

### 4.4 Pruebas transversales

| ID | Historia | Escenario | Tipo | Prioridad | Estado | Riesgo asociado |
|---|---|---|---|---|---|---|
| INT-ERR-01 | HDU-05 | ErrorResponse — estructura completa en todos los errores | Integración | Crítica | Pendiente | RSK-T06: Inconsistencias REST |
| INT-ERR-02 | HDU-05 | ErrorResponse — status coincide con código HTTP | Integración | Alta | Pendiente | RSK-T06: Inconsistencias REST |
| INT-ERR-03 | HDU-05 | ErrorResponse — campo error es nombre estándar | Integración | Media | Pendiente | RSK-T06: Inconsistencias REST |
| INT-ERR-04 | HDU-05 | ErrorResponse — Content-Type es JSON, no HTML | Integración | Crítica | Pendiente | RSK-T06: Inconsistencias REST |
| INT-ERR-05 | HDU-06 | Seguridad — 500 no expone detalles internos | Integración | Crítica | Pendiente | RSK-T03: Exposición de datos sensibles |
| INT-ERR-06 | HDU-05 | ErrorResponse — timestamp ISO 8601 válido | Integración | Media | Pendiente | RSK-T06: Inconsistencias REST |
| INT-SEC-01 | HDU-06 | Seguridad — error 500 genérico seguro | Integración | Crítica | Pendiente | RSK-T03: Exposición de datos sensibles |
| INT-SEC-02 | HDU-06 | Seguridad — report-service retorna JSON en errores | Integración | Crítica | Pendiente | RSK-T03: Exposición de datos sensibles |
| INT-SEC-03 | HDU-06 | Seguridad — token inválido retorna 403 | Integración | Alta | Pendiente | RSK-T03: Exposición de datos sensibles |
| INT-SEC-04 | HDU-06 | Seguridad — token ausente retorna 401 | Integración | Alta | Pendiente | RSK-T03: Exposición de datos sensibles |

### 4.5 Pruebas unitarias de dominio

| ID | Historia | Escenario | Tipo | Prioridad | Estado | Riesgo asociado |
|---|---|---|---|---|---|---|
| UNIT-DOM-01 | HDU-07 | OrderStatus — transición PENDING → IN_PREPARATION válida | Unitaria | Alta | Pendiente | — |
| UNIT-DOM-02 | HDU-07 | OrderStatus — transición IN_PREPARATION → READY válida | Unitaria | Alta | Pendiente | — |
| UNIT-DOM-03 | HDU-07 | OrderStatus — transición PENDING → READY inválida | Unitaria | Alta | Pendiente | RSK-T02: Códigos HTTP |
| UNIT-DOM-04 | HDU-07 | OrderStatus — transición READY → * inválida (terminal) | Unitaria | Alta | Pendiente | RSK-T02: Códigos HTTP |
| UNIT-DOM-05 | HDU-08 | OrderValidator — tableId = 0 rechazado | Unitaria | Alta | Pendiente | RSK-T05: Validación incompleta |
| UNIT-DOM-06 | HDU-08 | OrderValidator — tableId = 13 rechazado | Unitaria | Alta | Pendiente | RSK-T05: Validación incompleta |
| UNIT-DOM-07 | HDU-08 | OrderValidator — items vacíos rechazado | Unitaria | Alta | Pendiente | RSK-T05: Validación incompleta |
| UNIT-DOM-08 | HDU-08 | OrderValidator — producto inactivo rechazado | Unitaria | Alta | Pendiente | RSK-T05: Validación incompleta |
| UNIT-DOM-09 | — | OrderValidator — tableId = 1 aceptado (boundary) | Unitaria | Media | Pendiente | — |
| UNIT-DOM-10 | — | OrderValidator — tableId = 12 aceptado (boundary) | Unitaria | Media | Pendiente | — |
| UNIT-DOM-11 | — | Order.markAsDeleted() — establece deleted y deletedAt | Unitaria | Alta | Pendiente | RSK-T04: Fallas en soft delete |
| UNIT-DOM-12 | — | OrderPlacedEventValidator — eventVersion != 1 rechazado | Unitaria | Alta | Pendiente | RSK-T07: Contrato de eventos roto |
| UNIT-DOM-13 | — | OrderPlacedEventValidator — eventType incorrecto rechazado | Unitaria | Alta | Pendiente | RSK-T07: Contrato de eventos roto |
| UNIT-DOM-14 | — | DateRangeFilter — start > end rechazado | Unitaria | Alta | Pendiente | — |
| UNIT-DOM-15 | — | ReportAggregationService — solo READY agrega revenue | Unitaria | Alta | Pendiente | — |

### 4.6 Pruebas manuales (smoke test)

| ID | Historia | Escenario | Tipo | Prioridad | Estado | Riesgo asociado |
|---|---|---|---|---|---|---|
| MAN-01 | HDU-01 | Flujo completo: crear orden y verificar Location | Manual | Alta | Pendiente | RSK-G02: Integración tardía |
| MAN-02 | HDU-07 | Flujo: orden → IN_PREPARATION → READY | Manual | Alta | Pendiente | RSK-G02: Integración tardía |
| MAN-03 | — | Verificar evento en RabbitMQ Management UI | Manual | Media | Pendiente | RSK-T07: Contrato de eventos |
| MAN-04 | — | Verificar kitchen-worker procesa evento correctamente | Manual | Alta | Pendiente | RSK-T07: Contrato de eventos |
| MAN-05 | — | Reporte refleja orden completada | Manual | Alta | Pendiente | RSK-G02: Integración tardía |
| MAN-06 | HDU-04 | Soft delete y verificación en listado | Manual | Alta | Pendiente | RSK-T04: Fallas en soft delete |
| MAN-07 | HDU-06 | Provocar error 500 y verificar no filtración | Manual | Crítica | Pendiente | RSK-T03: Exposición de datos |

---

## 5. Gestión de riesgos

### 5.1 Riesgos del producto técnico

Estos riesgos afectan directamente la calidad del software entregado. Su materialización implica defectos en producción, vulnerabilidades de seguridad o comportamiento incorrecto del sistema.

#### RSK-T01 — Baja cobertura de pruebas en servicios críticos

| Atributo | Valor |
|---|---|
| **Descripción** | `report-service` no tiene pruebas documentadas en el informe de calidad. El `GlobalExceptionHandler` allí es inexistente. Una cobertura inferior al umbral del 80% en las capas de servicio permite que defectos de lógica de negocio lleguen a producción sin detección temprana. |
| **Probabilidad** | Alta |
| **Impacto** | Alto |
| **Nivel** | **Crítico** |
| **Estrategia de mitigación** | Priorizar la escritura de pruebas de integración para `report-service` (INT-RPT-01 a INT-RPT-06). Establecer barrera de calidad en CI: build falla si cobertura < 80% en `com.restaurant.*.service`. Generar reporte Jacoco por módulo. |

#### RSK-T02 — Códigos HTTP incorrectos devueltos por la API

| Atributo | Valor |
|---|---|
| **Descripción** | La auditoría documentó que `MethodArgumentTypeMismatchException` retorna `500` en lugar de `400`; que transiciones inválidas retornan `400` en lugar de `409`; y que productos inactivos retornan `404` en lugar de `422`. El frontend interpreta estos códigos para decidir el flujo de UX. Un código incorrecto genera comportamiento impredecible en la interfaz. |
| **Probabilidad** | Alta (ya materializado según auditoría) |
| **Impacto** | Alto |
| **Nivel** | **Crítico** |
| **Estrategia de mitigación** | Implementar handler de `MethodArgumentTypeMismatchException` en `GlobalExceptionHandler` de `order-service`. Cambiar `InvalidStatusTransitionException` a `409`. Cambiar producto inactivo a `422`. Cada corrección debe tener test de integración correspondiente (INT-GET-03, INT-PATCH-03, INT-POST-06). Ejecutar la suite completa tras cada cambio. |

#### RSK-T03 — Exposición de datos sensibles en respuestas de error

| Atributo | Valor |
|---|---|
| **Descripción** | El handler genérico de `500` en `order-service` expone `ex.getMessage()`, que puede contener trazas SQL, nombres de clases internas, rutas de archivos y detalles de conexión a base de datos. `report-service` no tiene handler global y retorna HTML con stack trace completo. Esta información facilita ataques de reconocimiento de infraestructura. |
| **Probabilidad** | Alta (ya materializado según auditoría) |
| **Impacto** | Crítico |
| **Nivel** | **Crítico** |
| **Estrategia de mitigación** | Reemplazar `ex.getMessage()` por mensaje genérico en el catch-all de `order-service`. Crear `@RestControllerAdvice` en `report-service` con handler genérico que retorne `ErrorResponse` con mensaje fijo. Pruebas INT-SEC-01, INT-SEC-02 y INT-ERR-05 validan la corrección. |

#### RSK-T04 — Fallas en la implementación de soft delete

| Atributo | Valor |
|---|---|
| **Descripción** | Las operaciones DELETE actualmente retornan `204 No Content` sin metadatos en `order-service`. El bulk delete descarta el conteo retornado por el servicio. No existe header de confirmación para la eliminación masiva. Un soft delete inconsistente puede resultar en pérdida de trazabilidad de auditoría o eliminaciones masivas accidentales. |
| **Probabilidad** | Media |
| **Impacto** | Alto |
| **Nivel** | **Alto** |
| **Estrategia de mitigación** | Modificar `DELETE /orders/{id}` para retornar `200` con `DeleteResponse(deletedId, deletedAt)`. Modificar `DELETE /orders` para exigir `X-Confirm-Destructive: true` y retornar `BulkDeleteResponse(deletedCount, deletedAt)`. Pruebas INT-DEL-01, INT-DEL-07, INT-DELALL-01 a INT-DELALL-05 validan la corrección. |

#### RSK-T05 — Validación incompleta de reglas de negocio

| Atributo | Valor |
|---|---|
| **Descripción** | `tableId` solo valida `>= 1` sin límite superior de 12 según la regla de negocio. Un `tableId` de 999 sería aceptado, creando datos inconsistentes. Adicionalmente, la validación se dispersa entre anotaciones Bean Validation en DTO y lógica imperativa en `OrderValidator`, sin un punto único de autoridad. |
| **Probabilidad** | Media |
| **Impacto** | Medio |
| **Nivel** | **Medio** |
| **Estrategia de mitigación** | Agregar `@Max(12)` a `CreateOrderRequest.tableId`. Agregar validación `tableId > 12` en `OrderValidator.validateTableId()`. Pruebas UNIT-DOM-05, UNIT-DOM-06, INT-POST-02, INT-POST-03 validan ambos límites. |

#### RSK-T06 — Inconsistencias en la respuesta REST entre servicios

| Atributo | Valor |
|---|---|
| **Descripción** | `order-service` usa `ErrorResponse` estructurado. `report-service` retorna body vacío en errores `400` y HTML en errores no controlados. El frontend necesita parsear respuestas de error de forma homogénea; la inconsistencia obliga a lógica defensiva diferenciada por servicio (observable en `HttpError` y mock-fallback en `src/api/`). |
| **Probabilidad** | Alta (ya materializado) |
| **Impacto** | Medio |
| **Nivel** | **Alto** |
| **Estrategia de mitigación** | Crear DTO `ErrorResponse` en `report-service` idéntico al de `order-service`. Crear `GlobalExceptionHandler` en `report-service`. Pruebas INT-RPT-02, INT-RPT-03, INT-RPT-04, INT-ERR-04 y INT-SEC-02 validan la homogenización. |

#### RSK-T07 — Ruptura del contrato de eventos AMQP

| Atributo | Valor |
|---|---|
| **Descripción** | El esquema de `OrderPlacedEvent` se define independientemente en `kitchen-worker` y `report-service`. No existe Schema Registry ni pruebas de contrato producer-consumer. Un cambio en la estructura del evento en `order-service` (e.g., renombrar un campo) rompe silenciosamente ambos consumidores. La detección solo ocurre en runtime. |
| **Probabilidad** | Media |
| **Impacto** | Alto |
| **Nivel** | **Alto** |
| **Estrategia de mitigación** | A corto plazo: pruebas manuales MAN-03 y MAN-04 verifican el flujo. UNIT-DOM-12 y UNIT-DOM-13 validan el `OrderPlacedEventValidator`. A mediano plazo: ADR-005 evaluará la adopción de Consumer-Driven Contract Testing para automatizar la validación del esquema. |

---

### 5.2 Riesgos de gestión del proyecto

Estos riesgos afectan la capacidad del equipo para ejecutar el plan de pruebas dentro del cronograma y presupuesto definidos. No son defectos del software, sino impedimentos organizacionales, de proceso o de comunicación.

#### RSK-G01 — Mala estimación del esfuerzo de corrección

| Atributo | Valor |
|---|---|
| **Descripción** | Las correcciones identificadas en la auditoría REST abarcan múltiples capas (handlers de excepción, controladores, DTOs, validadores) en dos servicios distintos. La subestimación del esfuerzo puede ocasionar que las correcciones se implementen parcialmente, dejando el sistema en un estado inconsistente donde algunos endpoints cumplen el contrato y otros no. |
| **Probabilidad** | Media |
| **Impacto** | Alto |
| **Nivel** | **Alto** |
| **Estrategia de mitigación** | Descomponer las correcciones en tareas atómicas alineadas con las HDU (HDU-01 a HDU-08). Cada tarea tiene criterios de aceptación verificables. Estimar en pares (DEV + QA) usando las pruebas de integración como unidad de esfuerzo. Aplicar buffer del 20% sobre la estimación base. |

#### RSK-G02 — Falta de sincronización DEV-QA

| Atributo | Valor |
|---|---|
| **Descripción** | Si los desarrolladores implementan correcciones sin conocer los criterios de aceptación Gherkin, el código podría cumplir la tarea superficialmente pero fallar en las pruebas de contrato. Ejemplo: corregir el código HTTP a `409` pero no ajustar la estructura `ErrorResponse` a `"Conflict"`. |
| **Probabilidad** | Media |
| **Impacto** | Medio |
| **Nivel** | **Medio** |
| **Estrategia de mitigación** | Compartir este plan de pruebas con el equipo de desarrollo antes de iniciar implementación. Cada HDU incluye criterios Gherkin explícitos que sirven como especificación ejecutable. Establecer sesión de refinamiento donde DEV y QA revisan juntos las pruebas INT-* correspondientes a cada HDU. |

#### RSK-G03 — Integración tardía entre servicios

| Atributo | Valor |
|---|---|
| **Descripción** | Los tres servicios se desarrollan y testean de forma semi-aislada (H2 en lugar de PostgreSQL, Mockito en lugar de RabbitMQ real). Si la integración completa con Docker Compose solo se verifica al final del sprint, defectos de compatibilidad entre servicios (e.g., formato de evento, timing de mensajes, configuración de colas) se descubren demasiado tarde para corregir sin riesgo. |
| **Probabilidad** | Alta |
| **Impacto** | Alto |
| **Nivel** | **Crítico** |
| **Estrategia de mitigación** | Ejecutar el smoke test manual completo (MAN-01 a MAN-07) al menos dos veces por semana contra el stack Docker. Automatizar el script `scripts/smoke-complete.sh` en CI como puerta de calidad pre-merge. Los defectos encontrados en integración tienen prioridad sobre nuevas funcionalidades. |

#### RSK-G04 — Requisitos cambiantes durante la fase de corrección

| Atributo | Valor |
|---|---|
| **Descripción** | Las decisiones arquitectónicas pendientes (ADR-001 a ADR-007) pueden alterar el contrato REST durante la implementación. Por ejemplo, ADR-001 (migración a `/api/v1/`) cambiaría todas las URLs de los tests; ADR-007 (diferenciación `401`/`403`) cambiaría los códigos esperados en pruebas de seguridad. Cambios de este tipo durante la ejecución del plan invalidan pruebas ya escritas. |
| **Probabilidad** | Baja |
| **Impacto** | Alto |
| **Nivel** | **Medio** |
| **Estrategia de mitigación** | Congelar las ADR pendientes durante la Fase 0 (corrección de contratos). Las ADR se evalúan en Fase 1+. Si un ADR debe ejecutarse de forma urgente, actualizar este plan de pruebas como prerequisito antes de implementar el cambio. Cada actualización incrementa la versión del plan (v3.1, v3.2, etc.). |

#### RSK-G05 — Deuda técnica acumulada retrasa las pruebas

| Atributo | Valor |
|---|---|
| **Descripción** | El registro de deuda técnica (`DEUDA_TECNICA.md`) documenta 3 items en progreso y 1 pendiente, incluyendo la separación de capas del backend (DT-007) y observabilidad/CI (DT-011). Si la deuda técnica no pagada interfiere con la escritura de pruebas (e.g., no se puede mockear un componente porque las dependencias están acopladas), el esfuerzo de testing se incrementa de forma no planificada. |
| **Probabilidad** | Media |
| **Impacto** | Medio |
| **Nivel** | **Medio** |
| **Estrategia de mitigación** | Priorizar las correcciones que desbloquean pruebas sobre las refactorizaciones internas. Identificar explícitamente los tests que requieren desacoplamiento previo (aquellos que necesitan mockear `OrderPlacedEventPublisherPort` u otros puertos). Si el acoplamiento impide una prueba unitaria, degradar a prueba de integración con MockMvc como alternativa temporal documentada. |

---

### 5.3 Matriz resumen de riesgos

| ID | Categoría | Riesgo | Prob. | Impacto | Nivel | Pruebas vinculadas |
|---|---|---|---|---|---|---|
| RSK-T01 | Técnico | Baja cobertura en servicios críticos | Alta | Alto | **Crítico** | INT-RPT-01 a INT-RPT-06 |
| RSK-T02 | Técnico | Códigos HTTP incorrectos | Alta | Alto | **Crítico** | INT-GET-03, INT-PATCH-03..05, INT-POST-06, INT-LIST-05 |
| RSK-T03 | Técnico | Exposición de datos sensibles | Alta | Crítico | **Crítico** | INT-SEC-01..04, INT-ERR-05, MAN-07 |
| RSK-T04 | Técnico | Fallas en soft delete | Media | Alto | **Alto** | INT-DEL-01..07, INT-DELALL-01..05 |
| RSK-T05 | Técnico | Validación incompleta | Media | Medio | **Medio** | UNIT-DOM-05..08, INT-POST-02..04 |
| RSK-T06 | Técnico | Inconsistencias REST entre servicios | Alta | Medio | **Alto** | INT-RPT-02..04, INT-ERR-01..04 |
| RSK-T07 | Técnico | Ruptura del contrato de eventos | Media | Alto | **Alto** | UNIT-DOM-12..13, MAN-03..04 |
| RSK-G01 | Gestión | Mala estimación del esfuerzo | Media | Alto | **Alto** | — |
| RSK-G02 | Gestión | Falta de sincronización DEV-QA | Media | Medio | **Medio** | — |
| RSK-G03 | Gestión | Integración tardía entre servicios | Alta | Alto | **Crítico** | MAN-01..07 |
| RSK-G04 | Gestión | Requisitos cambiantes (ADRs pendientes) | Baja | Alto | **Medio** | — |
| RSK-G05 | Gestión | Deuda técnica retrasa pruebas | Media | Medio | **Medio** | — |

---

## Apéndice — Trazabilidad HDU ↔ Pruebas ↔ Riesgos

| HDU | Título | Pruebas asociadas | Riesgos cubiertos |
|---|---|---|---|
| HDU-01 | Creación con 201 y Location | INT-POST-01, INT-POST-09, MAN-01 | RSK-T01, RSK-T02 |
| HDU-02 | Listas vacías con 200 OK | INT-LIST-02, INT-RPT-05, INT-MENU-01 | RSK-T02 |
| HDU-03 | Errores de tipo retornan 400 | INT-GET-03, INT-PATCH-08, INT-LIST-05, INT-DEL-06 | RSK-T02 |
| HDU-04 | Soft delete con respuesta consistente | INT-DEL-01..07, INT-DELALL-01..06, INT-LIST-06, INT-GET-04, MAN-06 | RSK-T04 |
| HDU-05 | Estructura de error unificada | INT-ERR-01..06, INT-RPT-02, INT-RPT-04 | RSK-T06 |
| HDU-06 | Seguridad en mensajes de error | INT-SEC-01..04, INT-ERR-05, INT-PATCH-06..07, INT-DEL-04..05, MAN-07 | RSK-T03 |
| HDU-07 | Conflictos de estado retornan 409 | INT-PATCH-01..05, UNIT-DOM-01..04 | RSK-T02 |
| HDU-08 | Reglas de negocio retornan 422 | INT-POST-06, INT-RPT-03, UNIT-DOM-05..08 | RSK-T02, RSK-T05 |

---

*Plan de pruebas v3.0 — Fase de Re-Arquitectura y API REST (DEV). Evolución del TEST_PLAN v1 (módulo de reportes) hacia cobertura completa del sistema conforme al contrato formalizado en ARCHITECTURE.md.*
