# layered-order-architecture Specification

## Purpose
TBD - created by archiving change auditoria-fase-1-ejecucion. Update Purpose after archive.
## Requirements
### Requirement: Event publication flow MUST expose explicit application port
`order-service` SHALL publicar eventos de integracion a traves de un puerto de aplicacion, evitando acoplamiento directo de la logica de negocio con detalles de mensajeria.

#### Scenario: Order creation delegates to output port
- **WHEN** se crea una orden valida
- **THEN** el flujo de aplicacion delega la publicacion a un puerto (`OrderPlacedEventPublisherPort`) en lugar de invocar infraestructura directamente

### Requirement: Messaging adapter MUST remain in infrastructure layer
La serializacion y envio de mensajes RabbitMQ SHALL residir en un adaptador de infraestructura separado de dominio y aplicacion.

#### Scenario: Rabbit transport changes without domain rewrite
- **WHEN** se requiere ajustar headers, exchange o estructura del mensaje
- **THEN** los cambios se realizan en el adaptador de infraestructura sin modificar entidades de dominio

