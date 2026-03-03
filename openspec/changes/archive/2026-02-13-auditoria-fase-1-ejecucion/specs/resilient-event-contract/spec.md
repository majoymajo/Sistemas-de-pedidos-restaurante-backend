## ADDED Requirements

### Requirement: Order placed contract MUST be versioned
El evento `order.placed` SHALL incluir metadatos de contrato (`eventId`, `eventType`, `eventVersion`, `occurredAt`) para habilitar evolucion controlada y trazabilidad.

#### Scenario: Producer emits versioned event envelope
- **WHEN** `order-service` publica un evento `order.placed`
- **THEN** el mensaje incluye version explicita y metadata de rastreo ademas del payload funcional

### Requirement: Consumer MUST validate contract and reject unsupported versions
`kitchen-worker` SHALL validar contrato minimo y version soportada antes de procesar, y MUST rechazar sin requeue los mensajes invalidos para enrutar a DLQ.

#### Scenario: Unsupported version is rejected deterministically
- **WHEN** llega un `order.placed` con version no soportada
- **THEN** el listener rechaza el mensaje con `AmqpRejectAndDontRequeueException` y no ejecuta logica de negocio

### Requirement: Consumer MUST support controlled compatibility
`kitchen-worker` SHALL soportar compatibilidad acotada durante migracion de contrato, resolviendo campos desde envelope/payload y manteniendo fallback controlado.

#### Scenario: Mixed producer payload is processed
- **WHEN** llega un mensaje con layout legado durante ventana de transicion
- **THEN** el consumidor resuelve campos requeridos con funciones `resolve*` y procesa solo si la validacion contractual es satisfactoria
