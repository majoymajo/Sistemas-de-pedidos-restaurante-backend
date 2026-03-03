# ARCHITECTURE.md — Visión Arquitectónica del Sistema de Pedidos de Restaurante

**Versión:** 1.0  
**Fecha:** 25 de febrero de 2026  
**Fase:** Re-Arquitectura y API REST (DEV)  
**Referencia técnica:** `docs/week-3-review/REST_API_AUDIT.md`

---

## Índice

1. [Debate Arquitectónico](#1-debate-arquitectónico)
   - 1.1 [Estado actual del sistema](#11-estado-actual-del-sistema)
   - 1.2 [Dolores del monolito heredado](#12-dolores-del-monolito-heredado)
   - 1.3 [Contraste con Clean Architecture](#13-contraste-con-clean-architecture)
   - 1.4 [Análisis crítico: migración total vs. incremental](#14-análisis-crítico-migración-total-vs-incremental)
2. [Contrato de la API REST](#2-contrato-de-la-api-rest)
   - 2.1 [Convenciones generales](#21-convenciones-generales)
   - 2.2 [order-service](#22-order-service)
   - 2.3 [report-service](#23-report-service)
   - 2.4 [Estructura de error unificada](#24-estructura-de-error-unificada)
   - 2.5 [Políticas de soft delete](#25-políticas-de-soft-delete)

---

## 1. Debate Arquitectónico

### 1.1 Estado actual del sistema

El sistema se presenta como una arquitectura de tres servicios desplegados de forma independiente, comunicados mediante eventos asíncronos a través de RabbitMQ (topic exchange) y con bases de datos segregadas por servicio (database-per-service). A nivel de contenedores, el stack comprende:

| Servicio | Puerto | Base de datos | Rol |
|---|---|---|---|
| `order-service` | 8080 | `restaurant_db` | API REST principal, persistencia de órdenes, publicación de eventos |
| `kitchen-worker` | 8081 | `kitchen_db` | Consumidor de eventos, proyección de cocina |
| `report-service` | 8082 | `report_db` | Consumidor de eventos, proyección CQRS de reportes |

**Patrones arquitectónicos identificados:**

- **Arquitectura hexagonal parcial** en `order-service`: existe un puerto de salida (`OrderPlacedEventPublisherPort`) con su adaptador de infraestructura (`RabbitOrderPlacedEventPublisher`), eventos de dominio (`OrderPlacedDomainEvent`) y un patrón Command (`OrderCommandExecutor` + `PublishOrderPlacedEventCommand`).
- **Chain of Responsibility** para seguridad de cocina: cadena `KitchenEndpointScopeHandler → KitchenTokenPresenceHandler → KitchenTokenValueHandler`.
- **CQRS** en `report-service`: modelo de lectura separado con tablas `report_orders` y `report_order_items` que desnormalizan datos para consultas de reportería.
- **Dead Letter Queue (DLQ)** con reintentos exponenciales para mensajería fallida.
- **Soft delete** con campos `deleted` y `deleted_at` en la entidad `Order`.

Sin embargo, bajo la superficie de estos patrones, persisten características propias de un monolito técnicamente distribuido.

#### 1.1.1 Características de monolito cerrado

A pesar de la separación física en tres servicios, el sistema exhibe rasgos de un **monolito distribuido**:

1. **Acoplamiento semántico entre servicios.** Los tres servicios comparten el mismo enum `OrderStatus` (con los mismos valores `PENDING`, `IN_PREPARATION`, `READY`) sin un esquema compartido versionado. Cualquier adición o renombramiento de un estado requiere modificar los tres servicios simultáneamente.

2. **Duplicación de modelos sin contrato formal.** La clase `OrderPlacedEvent` se define de manera independiente en `kitchen-worker` y `report-service`, con lógica de compatibilidad (`resolveOrderId()`, `resolveTableId()`) que evidencia migración ad hoc del esquema del evento sin un registro de esquemas (Schema Registry) centralizado.

3. **Ausencia de contrato REST explícito.** `report-service` carece completamente de anotaciones OpenAPI. `order-service` documenta contratos que no corresponden a la implementación real (UUID inválido documentado como `400`, implementado como `500`).

4. **Configuración acoplada al framework.** La validación de reglas de negocio se dispersa entre anotaciones de Bean Validation en DTOs (`@Min`, `@NotNull`), lógica imperativa en `OrderValidator` y transiciones de estado en el enum `OrderStatus`. No existe un único punto de autoridad para las invariantes de dominio.

#### 1.1.2 Dependencias fuertes

| Dependencia | Servicio(s) | Naturaleza del acoplamiento |
|---|---|---|
| Spring Boot 3.2.0 | Todos | Framework invasivo: anotaciones JPA, starter AMQP, Bean Validation en DTOs |
| PostgreSQL | Todos (3 instancias) | Persistencia JPA directa sin abstracción de repositorio de dominio |
| RabbitMQ | Todos | Configuración de exchange/queue/DLQ replicada en cada servicio |
| Flyway | Todos | Migraciones SQL acopladas al motor PostgreSQL |
| Lombok | Todos | Generación de código invisibiliza la estructura real de las clases |
| Jackson | Todos | Serialización JSON como contrato implícito entre servicios |

#### 1.1.3 Problemas detectados en la auditoría REST

La auditoría técnica (`REST_API_AUDIT.md`) documentó las siguientes categorías de defectos funcionales:

| Categoría | Hallazgos clave | Severidad |
|---|---|---|
| **Contratos HTTP violados** | `MethodArgumentTypeMismatchException` retorna `500` en lugar de `400`; ausencia del header `Location` en `201 Created` | Alta |
| **Filtración de información interna** | El handler genérico de `500` expone `ex.getMessage()` con posibles trazas SQL, rutas de archivos y nombres de clases | Alta |
| **Manejo de errores inconsistente** | `report-service` retorna cuerpos vacíos en `400`; no posee `@RestControllerAdvice`; excepciones no controladas devuelven HTML | Crítica |
| **Confusión semántica de códigos** | `401` y `403` conflacionados; transiciones inválidas retornan `400` en lugar de `409`; productos inactivos retornan `404` en lugar de `422` | Media |
| **Operaciones destructivas sin protección** | `DELETE /orders` no exige confirmación; descarta el conteo de eliminación; no documenta respuestas de error | Crítica |
| **Validación incompleta** | `tableId` solo valida `>= 1` sin límite superior (`@Max(12)` ausente) | Media |

---

### 1.2 Dolores del monolito heredado

#### 1.2.1 Falta de separación de responsabilidades

La capa de controladores de `order-service` concentra responsabilidades que exceden la coordinación HTTP:

- `OrderController` contiene 7 endpoints con lógica de mapeo, orquestación de comandos y construcción de respuestas. Si bien la extracción de `OrderValidator` (motivada por el hallazgo H-ALTA-01 de la auditoría) mejoró la cohesión del servicio, la lógica de dominio permanece atada a abstracciones de Spring (`@Transactional`, `@Component`).
- `MenuController` accede directamente a `MenuService`, que a su vez invoca `ProductRepository` sin mediación de una capa de aplicación.
- `ReportController` realiza parsing manual de fechas (`LocalDate.parse()`) dentro del controlador, mezclando validación de entrada con lógica de presentación.

**Consecuencia:** Modificar una regla de negocio (e.g., añadir un estado `CANCELLED`) requiere cambios simultáneos en controladores, servicios, enums, DTOs, validadores, migraciones SQL y los tres servicios consumidores de eventos.

#### 1.2.2 Dificultad de testeo aislado

El suite actual comprende 61 tests en `order-service` y 13 en `kitchen-worker`. Sin embargo:

- Las pruebas de integración dependen de H2 como sustituto de PostgreSQL, introduciendo discrepancias de comportamiento (funciones SQL, tipos de datos, concurrencia).
- No existen pruebas de contrato (Consumer-Driven Contract Testing) que validen la compatibilidad del esquema de eventos entre productores y consumidores.
- La ausencia de interfaces de dominio puras dificulta la inyección de dobles de prueba sin recurrir a mocks de framework (`@MockBean`).
- `report-service` carece de pruebas documentadas en el informe de calidad.

#### 1.2.3 Riesgo de cambios colaterales

Sin contratos formalizados entre servicios, cualquier modificación al esquema de `OrderPlacedEvent` (e.g., renombrar un campo o agregar datos de items) obliga a:

1. Modificar `RabbitOrderPlacedEventPublisher` en `order-service`.
2. Actualizar `OrderPlacedEvent` + `OrderPlacedEventValidator` en `kitchen-worker`.
3. Actualizar `OrderPlacedEvent` + `OrderEventProcessingService` en `report-service`.
4. Desplegar los tres servicios en orden coordinado.

Este patrón contradice la premisa fundamental de la independencia de despliegue en una arquitectura de microservicios.

#### 1.2.4 Manejo inconsistente de errores

El sistema presenta tres estrategias de manejo de errores mutuamente incoherentes:

| Servicio | Estrategia | Deficiencia |
|---|---|---|
| `order-service` | `GlobalExceptionHandler` con 8 handlers específicos + catch-all | Catch-all expone detalles internos; `MethodArgumentTypeMismatchException` no capturada |
| `kitchen-worker` | Rechazo AMQP diferenciado (contrato → DLQ inmediato; otros → retry exponencial) | Correcto para su contexto, pero no expone métricas de fallos |
| `report-service` | `try-catch` inline en el controlador | Sin `@RestControllerAdvice`; errores desconocidos retornan HTML de Spring Boot |

**Consecuencia para el frontend:** El cliente React debe implementar estrategias defensivas diferentes según el servicio invocado, lo cual se refleja en la clase `HttpError` y la lógica de mock-fallback distribuida en cada función de `src/api/`.

#### 1.2.5 Ausencia de contrato explícito

No existe un artefacto de contrato compartido (OpenAPI spec generada, Protobuf, Avro, AsyncAPI) que sirva como fuente de verdad para:

- Los endpoints REST (solo annotations parciales en `order-service`).
- Los eventos AMQP (contrato implícito en clases Java duplicadas).
- Los DTOs de request/response (el frontend define `contracts.ts` de forma independiente).

La auditoría detectó drift concreto: el frontend envía `{ newStatus, status }` en `PATCH /orders/{id}/status`, mientras el backend solo espera `{ status }`.

---

### 1.3 Contraste con Clean Architecture

La propuesta de Clean Architecture (Robert C. Martin, 2012) establece una organización concéntrica donde las dependencias fluyen **exclusivamente hacia el interior**: las capas externas dependen de las internas, nunca al revés.

```
┌────────────────────────────────────────────────────────────────┐
│                        Infrastructure                          │
│  Controllers · Repositories · Messaging · Configuración        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                      Application                         │  │
│  │  Use Cases · Commands · Puertos de entrada y salida      │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │                     Domain                         │  │  │
│  │  │  Entidades · Value Objects · Eventos de dominio    │  │  │
│  │  │  Reglas de negocio · Interfaces de repositorio     │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
```

#### 1.3.1 Capa de Dominio

| Principio | Estado actual | Estado objetivo |
|---|---|---|
| Entidades puras sin anotaciones de framework | `Order.java` usa `@Entity`, `@Table`, `@Id`, `@Column`, `@Enumerated` de JPA | Entidad de dominio `Order` como POJO; entidad JPA como adaptador de persistencia |
| Value Objects inmutables | `tableId` es un `Integer` primitivo sin restricciones de dominio | `TableId` como value object: `1 ≤ value ≤ 12`, validado en construcción |
| Eventos de dominio desacoplados | `OrderPlacedDomainEvent` existe pero contiene dependencia implícita de Jackson para serialización | Evento de dominio puro; serialización delegada al adaptador de infraestructura |
| Invariantes encapsuladas | Transiciones de estado viven en `OrderStatus` (enum), validación de request en `OrderValidator` (servicio) | Agregado `Order` que encapsula todas sus invariantes internamente |

#### 1.3.2 Capa de Aplicación

| Principio | Estado actual | Estado objetivo |
|---|---|---|
| Casos de uso explícitos | `OrderService` contiene `createOrder()`, `getOrders()`, `updateStatus()`, `deleteOrder()`, `deleteAllOrders()` como métodos de un servicio monolítico | Un caso de uso por clase: `CreateOrderUseCase`, `UpdateOrderStatusUseCase`, etc. |
| Puertos de entrada | El controlador invoca directamente `OrderService` | Interfaces de puerto de entrada que el controlador invoca |
| Puertos de salida | `OrderPlacedEventPublisherPort` existe (patrón hexagonal) | Extender a `OrderRepositoryPort`, `ProductRepositoryPort` |
| Orquestación sin lógica de dominio | `OrderService` valida, persiste, construye eventos y publica | Servicio de aplicación orquesta; dominio decide |

#### 1.3.3 Capa de Infraestructura

| Principio | Estado actual | Estado objetivo |
|---|---|---|
| Independencia del framework | Spring Boot invasivo: `@Transactional`, `@Component`, `@Autowired` | Adapters implementan puertos; Spring como cableado externo |
| Independencia de la base de datos | Repositorios extienden `JpaRepository` directamente | Adapter JPA implementa `OrderRepositoryPort` |
| Controladores como adaptadores delgados | `OrderController` construye responses, maneja comandos, documenta Swagger | Controlador solo traduce HTTP ↔ DTO de aplicación |
| Manejo de errores centralizado | `GlobalExceptionHandler` en `order-service`; inexistente en `report-service` | `@RestControllerAdvice` uniforme en cada servicio, sin filtración de detalles internos |

#### 1.3.4 Beneficios proyectados

| Dimensión | Mejora esperada |
|---|---|
| **Testabilidad** | Dominio testeable sin Spring Context, sin base de datos, sin broker. Tests unitarios puros para invariantes de negocio. |
| **Mantenibilidad** | Cambiar de PostgreSQL a otro motor solo requiere un nuevo adaptador de persistencia. Cambiar de RabbitMQ a Kafka solo requiere un nuevo publisher adapter. |
| **Evolución de contratos** | Puertos de entrada/salida formalizan las interfaces entre capas; versionamiento de eventos en el adaptador. |
| **Seguridad de refactorización** | Las reglas de negocio, concentradas en el dominio, sobreviven a cambios de framework o infraestructura. |

---

### 1.4 Análisis crítico: migración total vs. incremental

#### 1.4.1 Beneficios reales en este proyecto

El sistema gestiona un dominio acotado (pedidos de restaurante) con una máquina de estados simple (`PENDING → IN_PREPARATION → READY`) y un catálogo estático. En este contexto:

- **La separación del dominio aporta valor tangible** al concentrar las reglas de transición de estado, la validación de `tableId` (1–12), la verificación de productos activos y la construcción de eventos en un único módulo testeable sin dependencias externas.
- **Los puertos de salida ya demuestran su utilidad** con `OrderPlacedEventPublisherPort`, que permite testear la publicación de eventos sin RabbitMQ.
- **La estandarización de errores es urgente**, no como ejercicio teórico sino como necesidad práctica documentada en la auditoría.

#### 1.4.2 Límites y sobrecostos

| Riesgo | Mitigación |
|---|---|
| Proliferación de clases (un caso de uso por clase × 3 servicios) | Aplicar solo en `order-service` donde la complejidad lo justifica; `report-service` y `kitchen-worker` son suficientemente simples para mantener servicios de aplicación unificados |
| Mapeo excesivo entre capas (Entity ↔ Domain ↔ DTO) | Usar mappers explícitos solo donde exista divergencia real entre la representación de dominio y la de persistencia; evitar mapeo ceremonial |
| Curva de aprendizaje del equipo | Migración incremental con bounded contexts aislados; documentación de decisiones en ADRs |
| Overhead de abstracciones en servicios simples | `kitchen-worker` (consumidor puro sin API REST) y `report-service` (proyección de lectura) no requieren arquitectura hexagonal completa |

#### 1.4.3 Estrategia recomendada: migración incremental por Strangler Fig

La migración **no debe ser total ni simultánea**. Se recomienda el patrón Strangler Fig aplicado internamente:

| Fase | Alcance | Criterio de aceptación |
|---|---|---|
| **Fase 0 — Corrección de contratos** | Corregir los defectos críticos de la auditoría REST sin reestructurar paquetes | Tests de contrato HTTP pasan; `report-service` tiene `GlobalExceptionHandler`; `500` genérico no expone detalles |
| **Fase 1 — Extracción del dominio** | Crear paquete `domain/` en `order-service` con entidades puras, value objects (`TableId`, `OrderId`) y eventos | Tests unitarios del dominio sin Spring Context |
| **Fase 2 — Puertos de aplicación** | Extraer interfaces `OrderRepositoryPort`, `ProductRepositoryPort`; refactorizar `OrderService` en casos de uso | Tests de aplicación con puertos mockeados |
| **Fase 3 — Adaptadores independientes** | Mover JPA entities a `infrastructure/persistence/`, controladores a `infrastructure/web/` | Compilación sin dependencias inversas verificada |
| **Fase 4 — Contrato de eventos** | Esquema versionado compartido para `OrderPlacedEvent`; validación por JSON Schema o Avro | Tests de contrato producer-consumer |

---

## 2. Contrato de la API REST

### 2.1 Convenciones generales

| Convención | Especificación |
|---|---|
| **Base URL** | `http://{host}:{port}` — sin prefijo de versión en esta fase (migración a `/api/v1/` planificada) |
| **Content-Type** | `application/json` obligatorio en requests con body |
| **Accept** | `application/json` |
| **Autenticación de cocina** | Header `X-Kitchen-Token` con valor configurado en servidor |
| **Identificadores** | Órdenes: `UUID v4`. Productos: `Long` autoincremental |
| **Timestamps** | ISO 8601 (`yyyy-MM-dd'T'HH:mm:ss`) |
| **Soft delete** | Todas las operaciones DELETE son lógicas; recursos eliminados se excluyen de queries por defecto |
| **Respuestas vacías** | Listas vacías retornan `200` con `[]`, nunca `204` |
| **Errores** | Estructura `ErrorResponse` uniforme en todos los servicios |

#### Códigos HTTP — Semántica obligatoria

| Código | Significado | Uso en este sistema |
|---|---|---|
| `200 OK` | Operación exitosa con cuerpo | GET, PATCH, DELETE con metadatos |
| `201 Created` | Recurso creado | POST /orders — **debe incluir header `Location`** |
| `204 No Content` | Éxito sin cuerpo | No utilizado (se prefiere `200` con metadatos) |
| `400 Bad Request` | Request malformado | JSON inválido, tipos incorrectos, violación de Bean Validation |
| `401 Unauthorized` | Credenciales ausentes | Header `X-Kitchen-Token` no presente |
| `403 Forbidden` | Credenciales insuficientes | Token presente pero inválido |
| `404 Not Found` | Recurso inexistente | Orden o producto no encontrado |
| `409 Conflict` | Conflicto de estado | Transición de estado inválida |
| `422 Unprocessable Entity` | Error semántico | Producto inactivo, rango de fechas inválido |
| `500 Internal Server Error` | Error inesperado | Mensaje genérico, **sin detalles internos** |
| `503 Service Unavailable` | Dependencia caída | Base de datos o broker inaccesible |

---

### 2.2 order-service

#### 2.2.1 POST /orders — Crear orden

| Atributo | Valor |
|---|---|
| **Descripción** | Crea una nueva orden de pedido asociada a una mesa del restaurante. La orden se persiste con estado `PENDING` y se publica un evento `order.placed` al broker de mensajería. |
| **Método** | `POST` |
| **Autenticación** | No requerida |

**Request body:**

```json
{
  "tableId": 5,
  "items": [
    { "productId": 1, "quantity": 2, "note": "Sin cebolla" },
    { "productId": 8, "quantity": 1 }
  ]
}
```

| Campo | Tipo | Obligatorio | Restricciones |
|---|---|---|---|
| `tableId` | `Integer` | Sí | `1 ≤ tableId ≤ 12` |
| `items` | `Array<OrderItemRequest>` | Sí | Mínimo 1 elemento |
| `items[].productId` | `Long` | Sí | Debe existir y estar activo |
| `items[].quantity` | `Integer` | Sí | `≥ 1` |
| `items[].note` | `String` | No | Texto libre, opcional |

**Respuestas:**

| Código | Condición | Response body |
|---|---|---|
| `201 Created` | Orden creada exitosamente | `OrderResponse` + header `Location: /orders/{id}` |
| `400 Bad Request` | `tableId` fuera de rango, items vacíos, JSON malformado | `ErrorResponse` |
| `404 Not Found` | Producto referenciado no existe | `ErrorResponse` |
| `422 Unprocessable Entity` | Producto existe pero está inactivo | `ErrorResponse` |
| `503 Service Unavailable` | Base de datos o broker inaccesible | `ErrorResponse` |

**Response body `201`:**

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "tableId": 5,
  "status": "PENDING",
  "items": [
    {
      "id": 1,
      "productId": 1,
      "quantity": 2,
      "note": "Sin cebolla",
      "productName": "Bandeja Paisa"
    },
    {
      "id": 2,
      "productId": 8,
      "quantity": 1,
      "note": null,
      "productName": "Limonada Natural"
    }
  ],
  "createdAt": "2026-02-25T14:30:00",
  "updatedAt": "2026-02-25T14:30:00"
}
```

**Headers de respuesta obligatorios:**

| Header | Valor | Ejemplo |
|---|---|---|
| `Content-Type` | `application/json` | — |
| `Location` | `/orders/{id}` | `/orders/a1b2c3d4-e5f6-7890-abcd-ef1234567890` |

---

#### 2.2.2 GET /orders — Listar órdenes

| Atributo | Valor |
|---|---|
| **Descripción** | Retorna las órdenes activas (no eliminadas) del sistema. Soporta filtrado por uno o múltiples estados. |
| **Método** | `GET` |
| **Autenticación** | Opcional (`X-Kitchen-Token` requerido para ciertos filtros según contexto) |

**Query parameters:**

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `status` | `String` (comma-separated) | No | Filtro por estado(s): `PENDING`, `IN_PREPARATION`, `READY` |

**Ejemplos de invocación:**

```
GET /orders
GET /orders?status=PENDING
GET /orders?status=PENDING,IN_PREPARATION,READY
```

**Respuestas:**

| Código | Condición | Response body |
|---|---|---|
| `200 OK` | Consulta exitosa (incluyendo resultado vacío) | `Array<OrderResponse>` |
| `400 Bad Request` | Valor de `status` no reconocido | `ErrorResponse` |

**Response body `200`:**

```json
[
  {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "tableId": 5,
    "status": "PENDING",
    "items": [ ... ],
    "createdAt": "2026-02-25T14:30:00",
    "updatedAt": "2026-02-25T14:30:00"
  }
]
```

**Notas de diseño:**
- Un resultado vacío retorna `200` con `[]`, no `204`.
- Los registros con `deleted = true` se excluyen automáticamente.
- Valores de enum inválidos en `status` deben retornar `400` con `ErrorResponse` estructurado (no `500`).

---

#### 2.2.3 GET /orders/{id} — Obtener orden por ID

| Atributo | Valor |
|---|---|
| **Descripción** | Retorna el detalle completo de una orden por su identificador UUID. |
| **Método** | `GET` |
| **Autenticación** | No requerida |

**Path parameters:**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `id` | `UUID` | Identificador único de la orden |

**Respuestas:**

| Código | Condición | Response body |
|---|---|---|
| `200 OK` | Orden encontrada | `OrderResponse` |
| `400 Bad Request` | Formato de UUID inválido | `ErrorResponse` |
| `404 Not Found` | Orden inexistente o eliminada | `ErrorResponse` |

**Requisito técnico:** Un `id` que no sea UUID válido debe retornar `400`, no `500`. Esto requiere un handler explícito para `MethodArgumentTypeMismatchException`.

---

#### 2.2.4 PATCH /orders/{id}/status — Actualizar estado de orden

| Atributo | Valor |
|---|---|
| **Descripción** | Actualiza el estado de una orden siguiendo la máquina de estados definida: `PENDING → IN_PREPARATION → READY`. Solo avances permitidos. |
| **Método** | `PATCH` |
| **Autenticación** | Requerida (`X-Kitchen-Token`) |

**Path parameters:**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `id` | `UUID` | Identificador único de la orden |

**Request body:**

```json
{
  "status": "IN_PREPARATION"
}
```

| Campo | Tipo | Obligatorio | Valores válidos |
|---|---|---|---|
| `status` | `String` | Sí | `IN_PREPARATION`, `READY` |

**Máquina de estados:**

```
PENDING ──→ IN_PREPARATION ──→ READY (terminal)
```

| Transición | Válida |
|---|---|
| `PENDING → IN_PREPARATION` | ✅ |
| `IN_PREPARATION → READY` | ✅ |
| `PENDING → READY` | ❌ |
| `READY → *` | ❌ |
| `* → PENDING` | ❌ |

**Respuestas:**

| Código | Condición | Response body |
|---|---|---|
| `200 OK` | Estado actualizado exitosamente | `OrderResponse` (recurso completo actualizado) |
| `400 Bad Request` | UUID inválido, body malformado, `status` nulo | `ErrorResponse` |
| `401 Unauthorized` | Header `X-Kitchen-Token` ausente | `ErrorResponse` |
| `403 Forbidden` | Token presente pero inválido | `ErrorResponse` |
| `404 Not Found` | Orden inexistente | `ErrorResponse` |
| `409 Conflict` | Transición de estado no permitida | `ErrorResponse` |

**Nota sobre `409`:** Las transiciones de estado inválidas representan un conflicto entre el estado solicitado y el estado actual del recurso. Semánticamente corresponden a `409 Conflict`, no a `400 Bad Request`.

---

#### 2.2.5 DELETE /orders/{id} — Eliminar orden (soft delete)

| Atributo | Valor |
|---|---|
| **Descripción** | Realiza una eliminación lógica (soft delete) de una orden específica. El registro persiste con `deleted = true` y `deleted_at` asignado. |
| **Método** | `DELETE` |
| **Autenticación** | Requerida (`X-Kitchen-Token`) |

**Path parameters:**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `id` | `UUID` | Identificador único de la orden |

**Respuestas:**

| Código | Condición | Response body |
|---|---|---|
| `200 OK` | Orden eliminada lógicamente | `DeleteResponse` con metadatos de auditoría |
| `400 Bad Request` | UUID inválido | `ErrorResponse` |
| `401 Unauthorized` | Header `X-Kitchen-Token` ausente | `ErrorResponse` |
| `403 Forbidden` | Token presente pero inválido | `ErrorResponse` |
| `404 Not Found` | Orden inexistente o ya eliminada | `ErrorResponse` |

**Response body `200`:**

```json
{
  "deletedId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "deletedAt": "2026-02-25T15:00:00"
}
```

**Justificación del `200` sobre `204`:** Las operaciones destructivas deben retornar metadatos de auditoría que confirmen la acción ejecutada, conforme a las políticas de trazabilidad del sistema.

---

#### 2.2.6 DELETE /orders — Eliminar todas las órdenes (soft delete)

| Atributo | Valor |
|---|---|
| **Descripción** | Realiza una eliminación lógica masiva de todas las órdenes activas. Operación destructiva que requiere confirmación explícita. |
| **Método** | `DELETE` |
| **Autenticación** | Requerida (`X-Kitchen-Token`) |

**Headers requeridos:**

| Header | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `X-Kitchen-Token` | `String` | Sí | Token de autenticación de cocina |
| `X-Confirm-Destructive` | `Boolean` | Sí | Confirmación explícita de operación destructiva |

**Respuestas:**

| Código | Condición | Response body |
|---|---|---|
| `200 OK` | Órdenes eliminadas lógicamente | `BulkDeleteResponse` con conteo y timestamp |
| `400 Bad Request` | Header `X-Confirm-Destructive` ausente o `false` | `ErrorResponse` |
| `401 Unauthorized` | Header `X-Kitchen-Token` ausente | `ErrorResponse` |
| `403 Forbidden` | Token presente pero inválido | `ErrorResponse` |

**Response body `200`:**

```json
{
  "deletedCount": 15,
  "deletedAt": "2026-02-25T15:00:00"
}
```

**Nota de diseño:** La exigencia del header `X-Confirm-Destructive: true` previene eliminaciones masivas accidentales por herramientas de testing, scripts automatizados o invocaciones erróneas.

---

#### 2.2.7 GET /menu — Obtener productos activos

| Atributo | Valor |
|---|---|
| **Descripción** | Retorna el catálogo de productos activos disponibles para ordenar. Solo incluye productos con `is_active = true`. |
| **Método** | `GET` |
| **Autenticación** | No requerida |

**Respuestas:**

| Código | Condición | Response body |
|---|---|---|
| `200 OK` | Consulta exitosa | `Array<ProductResponse>` |
| `503 Service Unavailable` | Base de datos inaccesible | `ErrorResponse` |

**Response body `200`:**

```json
[
  {
    "id": 1,
    "name": "Bandeja Paisa",
    "description": "Plato típico colombiano con frijoles, arroz, carne molida, chicharrón, huevo frito, plátano maduro, arepa y aguacate",
    "price": 28000.00,
    "category": "PRINCIPALES",
    "imageUrl": "/images/bandeja-paisa.jpg",
    "isActive": true
  }
]
```

**Consideraciones de rendimiento:**
- El menú cambia con baja frecuencia. Se recomienda header `Cache-Control: max-age=300` (5 minutos).
- Para catálogos extensos, se planifica soporte de paginación con `Pageable`.

---

### 2.3 report-service

#### 2.3.1 GET /reports — Generar reporte de ventas

| Atributo | Valor |
|---|---|
| **Descripción** | Genera un reporte de ventas agregado para un rango de fechas. Solo incluye órdenes con estado `READY` (completadas). Los datos provienen de la proyección CQRS alimentada por eventos `order.placed` y `order.ready`. |
| **Método** | `GET` |
| **Autenticación** | No requerida (planificada para fases futuras) |

**Query parameters:**

| Parámetro | Tipo | Obligatorio | Formato | Descripción |
|---|---|---|---|---|
| `startDate` | `String` | Sí | `YYYY-MM-DD` (ISO 8601) | Fecha de inicio del rango |
| `endDate` | `String` | Sí | `YYYY-MM-DD` (ISO 8601) | Fecha de fin del rango (inclusive) |

**Ejemplo de invocación:**

```
GET /reports?startDate=2026-02-01&endDate=2026-02-25
```

**Respuestas:**

| Código | Condición | Response body |
|---|---|---|
| `200 OK` | Reporte generado exitosamente | `ReportResponse` |
| `400 Bad Request` | Formato de fecha inválido | `ErrorResponse` con mensaje: `"Formato de fecha inválido. Esperado: YYYY-MM-DD"` |
| `422 Unprocessable Entity` | `startDate > endDate` | `ErrorResponse` con mensaje descriptivo |
| `503 Service Unavailable` | Base de datos inaccesible | `ErrorResponse` |

**Response body `200`:**

```json
{
  "totalReadyOrders": 42,
  "totalRevenue": 1250000.00,
  "productBreakdown": [
    {
      "productId": 1,
      "productName": "Bandeja Paisa",
      "quantitySold": 85,
      "totalAccumulated": 2380000.00
    },
    {
      "productId": 8,
      "productName": "Limonada Natural",
      "quantitySold": 120,
      "totalAccumulated": 600000.00
    }
  ]
}
```

**Requisitos técnicos:**
- El parsing de fechas debe delegarse a Spring (`@DateTimeFormat(iso = ISO.DATE)`), no implementarse manualmente en el controlador.
- Todas las respuestas de error deben usar la estructura `ErrorResponse`, nunca un body vacío.
- Excepciones no controladas deben retornar JSON estructurado (`@RestControllerAdvice`), no HTML.

---

### 2.4 Estructura de error unificada

Todos los servicios deben emplear la misma estructura de respuesta de error:

```json
{
  "timestamp": "2026-02-25T14:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Table ID must be between 1 and 12"
}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `timestamp` | `String` (ISO 8601) | Momento exacto del error |
| `status` | `Integer` | Código HTTP numérico |
| `error` | `String` | Nombre estándar del código HTTP |
| `message` | `String` | Descripción legible del error, **sin detalles de implementación** |

**Reglas de seguridad para el campo `message`:**

| Permitido | Prohibido |
|---|---|
| `"Table ID must be between 1 and 12"` | `"java.lang.NumberFormatException: For input string: \"abc\""` |
| `"Order not found"` | `"org.postgresql.util.PSQLException: ERROR: relation \"orders\" does not exist"` |
| `"An unexpected error occurred. Please contact support."` | Stack traces, rutas de archivos, queries SQL, nombres de clases internas |

---

### 2.5 Políticas de soft delete

| Política | Especificación |
|---|---|
| **Mecanismo** | Campo `deleted` (`boolean`, default `false`) + `deleted_at` (`timestamp`, nullable) |
| **Consultas por defecto** | Todos los endpoints GET excluyen registros con `deleted = true` |
| **Reactivación** | No soportada en esta fase. Un registro eliminado es irrecuperable vía API. |
| **Eliminación física** | No expuesta vía API. Solo disponible mediante procesos de mantenimiento de base de datos. |
| **Auditoría** | Toda operación DELETE retorna `deletedAt` como confirmación de la marca temporal aplicada |
| **Idempotencia** | `DELETE /orders/{id}` sobre una orden ya eliminada retorna `404`, no `200` |
| **Impacto en reportes** | `report-service` opera con su propia proyección; el soft delete en `order-service` no afecta las proyecciones ya persistidas |

---

## Apéndice A — Diagrama de arquitectura objetivo

```
                                    ┌─────────────────┐
                                    │    Frontend      │
                                    │  React + Vite    │
                                    └────────┬────────┘
                                             │ REST (JSON)
                                             ▼
┌────────────────────────────────────────────────────────────────────┐
│                        order-service :8080                         │
│                                                                    │
│  ┌──────────────┐   ┌──────────────────┐   ┌───────────────────┐  │
│  │ Infrastructure│   │   Application    │   │     Domain        │  │
│  │              │   │                  │   │                   │  │
│  │ Controllers  │──▶│ CreateOrderUse   │──▶│ Order (Aggregate) │  │
│  │ JPA Adapters │   │ Case             │   │ TableId (VO)      │  │
│  │ AMQP Adapter │◀──│ UpdateStatusUse  │   │ OrderStatus       │  │
│  │ Security     │   │ Case             │   │ DomainEvents      │  │
│  │ GlobalError  │   │ Ports (in/out)   │   │ Invariants        │  │
│  └──────────────┘   └──────────────────┘   └───────────────────┘  │
└───────────────────────────────┬────────────────────────────────────┘
                                │ order.placed (AMQP)
                                ▼
                       ┌─────────────────┐
                       │    RabbitMQ      │
                       │  order.exchange  │
                       └───┬─────────┬───┘
                           │         │
                ┌──────────▼──┐  ┌───▼──────────┐
                │kitchen-worker│  │report-service │
                │  :8081       │  │  :8082        │
                │  Listener    │  │  Listener     │
                │  Processing  │  │  CQRS Proj.   │
                │  kitchen_db  │  │  report_db    │
                └──────────────┘  └───────────────┘
```

---

## Apéndice B — Decisiones arquitectónicas pendientes

| ID | Decisión | Estado | Dependencia |
|---|---|---|---|
| ADR-001 | Migración a URI versionadas (`/api/v1/`) | Pendiente | Coordinación con frontend |
| ADR-002 | Introducción de Schema Registry para eventos AMQP | Pendiente | Evaluación de complejidad operativa |
| ADR-003 | Implementación de paginación en `GET /orders` y `GET /menu` | Pendiente | Definición de tamaño de página por defecto |
| ADR-004 | Introducción de `@Version` (optimistic locking) en `Order` | Pendiente | Análisis de concurrencia en actualización de estados |
| ADR-005 | Adopción de Consumer-Driven Contract Testing | Pendiente | Selección de herramienta (Pact, Spring Cloud Contract) |
| ADR-006 | Soporte de `ETag` / `If-Match` en endpoints de mutación | Pendiente | Depende de ADR-004 |
| ADR-007 | Autenticación diferenciada `401` vs `403` | En diseño | Refactorización de `KitchenAccessDeniedException` |

---

*Documento generado como artefacto de la Fase de Re-Arquitectura y API REST (DEV). Referencia técnica complementaria: `docs/week-3-review/REST_API_AUDIT.md`.*
