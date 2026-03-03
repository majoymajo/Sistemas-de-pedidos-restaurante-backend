## ADDED Requirements

### Requirement: Kitchen worker MUST use isolated persistence
El sistema SHALL aislar la persistencia de `kitchen-worker` en una base de datos independiente de `order-service`, aplicando el principio database-per-service.

#### Scenario: Kitchen worker starts with dedicated database
- **WHEN** se levanta el stack de desarrollo
- **THEN** `kitchen-worker` se conecta a `kitchen_db` y `order-service` mantiene `restaurant_db`

### Requirement: Kitchen worker projection MUST use owned table
`kitchen-worker` SHALL persistir su proyeccion local en una tabla propia y no depender de la tabla `orders` de `order-service`.

#### Scenario: Event creates local kitchen projection
- **WHEN** `kitchen-worker` procesa `order.placed`
- **THEN** crea/actualiza un registro en `kitchen_orders` sin leer/escribir la tabla `orders` del `order-service`
