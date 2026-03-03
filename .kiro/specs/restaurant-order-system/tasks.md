# Plan de Implementación: Sistema de Pedidos de Restaurante

## Resumen

Este plan de implementación desglosa el diseño del sistema de pedidos de restaurante en tareas discretas de codificación. El sistema consta de dos microservicios Java Spring Boot (Order Service y Kitchen Worker) que se comunican a través de RabbitMQ, con PostgreSQL para persistencia de datos.

## Tareas

- [x] 1. Configurar estructura de proyectos Maven
  - Crear proyecto Maven multi-módulo con dos submódulos: order-service y kitchen-worker
  - Configurar pom.xml padre con dependencias comunes (Spring Boot 3, Java 17, Lombok, PostgreSQL, Spring AMQP)
  - Configurar pom.xml de order-service con dependencias específicas (Spring Web, Spring Data JPA, Flyway, SpringDoc OpenAPI)
  - Configurar pom.xml de kitchen-worker con dependencias específicas (Spring Data JPA, Spring AMQP)
  - Agregar dependencias de prueba (JUnit 5, jqwik, Spring Boot Test, Spring Rabbit Test)
  - _Requisitos: 12.1, 12.2, 12.3, 12.4_

- [ ] 2. Implementar modelo de datos y migraciones
  - [x] 2.1 Crear enum OrderStatus
    - Implementar enum con valores PENDING, IN_PREPARATION, READY
    - _Requisitos: 2.3, 6.2, 7.4_
  
  - [x] 2.2 Crear entidad Product
    - Implementar clase con anotaciones JPA (@Entity, @Table, @Id, @GeneratedValue)
    - Campos: id (Long), name (String), description (String), isActive (Boolean)
    - Usar Lombok (@Data, @NoArgsConstructor, @AllArgsConstructor)
    - _Requisitos: 1.1, 1.3, 2.2, 9.1_
  
  - [x] 2.3 Crear entidad Order
    - Implementar clase con anotaciones JPA
    - Campos: id (UUID), tableId (Integer), status (OrderStatus), items (List<OrderItem>), createdAt, updatedAt
    - Implementar métodos @PrePersist y @PreUpdate para timestamps automáticos
    - Configurar relación @OneToMany con OrderItem (cascade ALL, orphanRemoval true)
    - _Requisitos: 2.3, 2.4, 2.5, 9.1_
  
  - [x] 2.4 Crear entidad OrderItem
    - Implementar clase con anotaciones JPA
    - Campos: id (Long), order (Order), productId (Long), quantity (Integer), note (String)
    - Configurar relación @ManyToOne con Order
    - _Requisitos: 2.1, 9.1_
  
  - [x] 2.5 Crear migraciones Flyway
    - Crear V1__create_products_table.sql con tabla products
    - Crear V2__create_orders_table.sql con tabla orders e índices
    - Crear V3__create_order_items_table.sql con tabla order_items, foreign keys e índices
    - Crear V4__insert_initial_products.sql con 3 productos iniciales (Pizza Margherita, Hamburguesa Clásica, Ensalada César)
    - _Requisitos: 9.2, 9.3_

- [ ] 3. Implementar repositorios
  - [x] 3.1 Crear ProductRepository
    - Extender JpaRepository<Product, Long>
    - Agregar método findByIsActiveTrue() para obtener productos activos
    - _Requisitos: 1.1, 1.3_
  
  - [x] 3.2 Crear OrderRepository en order-service
    - Extender JpaRepository<Order, UUID>
    - Agregar método findByStatus(OrderStatus status) para filtrado
    - _Requisitos: 4.1, 5.1, 6.1_
  
  - [x] 3.3 Crear OrderItemRepository
    - Extender JpaRepository<OrderItem, Long>
    - _Requisitos: 2.1_
  
  - [x] 3.4 Crear OrderRepository en kitchen-worker
    - Extender JpaRepository<Order, UUID>
    - Copiar entidad Order al proyecto kitchen-worker (solo campos necesarios)
    - _Requisitos: 7.4, 9.4_

- [ ] 4. Implementar DTOs y eventos
  - [x] 4.1 Crear DTOs de request
    - CreateOrderRequest con validaciones (@NotNull, @Min, @NotEmpty, @Valid)
    - OrderItemRequest con validaciones
    - UpdateStatusRequest con validación de enum
    - _Requisitos: 2.1, 2.7, 2.8, 6.1_
  
  - [x] 4.2 Crear DTOs de response
    - ProductResponse con campos id, name, description
    - OrderResponse con campos completos y lista de OrderItemResponse
    - OrderItemResponse con campos del item
    - ErrorResponse con timestamp, status, error, message
    - _Requisitos: 1.1, 4.1, 11.1, 11.2, 11.3, 11.4, 11.5_
  
  - [x] 4.3 Crear OrderPlacedEvent
    - Implementar clase Serializable con campos: orderId, tableId, items (List<OrderItemEventData>), createdAt
    - Crear clase interna OrderItemEventData con productId y quantity
    - Usar Lombok para getters/setters
    - _Requisitos: 3.1, 3.3, 3.5_

- [ ] 5. Implementar excepciones personalizadas y manejo global
  - [x] 5.1 Crear excepciones personalizadas
    - ProductNotFoundException con mensaje descriptivo
    - OrderNotFoundException con mensaje descriptivo
    - InvalidOrderException con mensaje descriptivo
    - _Requisitos: 2.6, 4.3, 6.4, 11.2_
  
  - [x] 5.2 Crear GlobalExceptionHandler
    - Implementar @RestControllerAdvice
    - Agregar @ExceptionHandler para ProductNotFoundException (404)
    - Agregar @ExceptionHandler para OrderNotFoundException (404)
    - Agregar @ExceptionHandler para InvalidOrderException (400)
    - Agregar @ExceptionHandler para MethodArgumentNotValidException (400)
    - Agregar @ExceptionHandler para DataAccessException (503)
    - Agregar @ExceptionHandler para Exception genérica (500)
    - Cada handler debe devolver ErrorResponse con timestamp
    - _Requisitos: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6_

- [ ] 6. Configurar RabbitMQ
  - [x] 6.1 Crear RabbitMQConfig en order-service
    - Declarar TopicExchange "order.exchange" (durable)
    - Declarar Queue "order.placed.queue" (durable)
    - Declarar Dead Letter Queue "order.placed.dlq"
    - Configurar binding con routing key "order.placed"
    - Configurar MessageConverter (Jackson2JsonMessageConverter)
    - _Requisitos: 3.2, 8.1, 8.2, 8.3, 8.4_
  
  - [x] 6.2 Crear RabbitMQConfig en kitchen-worker
    - Declarar TopicExchange "order.exchange"
    - Declarar Queue "order.placed.queue" con DLX configurado
    - Declarar Dead Letter Queue "order.placed.dlq"
    - Configurar binding con routing key "order.placed"
    - Configurar MessageConverter (Jackson2JsonMessageConverter)
    - Configurar retry policy (3 intentos, backoff exponencial)
    - _Requisitos: 7.1, 8.3, 8.5_

- [ ] 7. Implementar servicios de Order Service
  - [x] 7.1 Crear MenuService
    - Implementar método getActiveProducts() que use ProductRepository.findByIsActiveTrue()
    - Mapear entidades Product a ProductResponse
    - _Requisitos: 1.1, 1.2, 1.3_
  
  - [x] 7.2 Crear OrderEventPublisher
    - Inyectar RabbitTemplate
    - Implementar método publishOrderPlacedEvent(OrderPlacedEvent event)
    - Usar convertAndSend con exchange "order.exchange" y routing key "order.placed"
    - Agregar try-catch para logging de errores sin lanzar excepción
    - _Requisitos: 3.1, 3.2, 3.4, 3.5_
  
  - [x] 7.3 Crear OrderService
    - Implementar createOrder(CreateOrderRequest request)
      - Validar que todos los productIds existen y están activos
      - Lanzar ProductNotFoundException si algún producto no existe o está inactivo
      - Lanzar InvalidOrderException si tableId es inválido o items está vacío
      - Crear entidad Order con status PENDING
      - Crear OrderItems asociados
      - Guardar en OrderRepository
      - Construir OrderPlacedEvent y llamar a OrderEventPublisher
      - Mapear a OrderResponse y devolver
    - Implementar getOrderById(UUID orderId)
      - Buscar en OrderRepository
      - Lanzar OrderNotFoundException si no existe
      - Mapear a OrderResponse
    - Implementar getOrders(OrderStatus status)
      - Si status es null, devolver todos los pedidos
      - Si status no es null, usar findByStatus
      - Mapear a lista de OrderResponse
    - Implementar updateOrderStatus(UUID orderId, OrderStatus newStatus)
      - Buscar pedido, lanzar OrderNotFoundException si no existe
      - Actualizar status (updatedAt se actualiza automáticamente por @PreUpdate)
      - Guardar y mapear a OrderResponse
    - _Requisitos: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 3.1, 4.1, 4.2, 5.1, 5.2, 6.2_

- [ ] 8. Implementar controladores de Order Service
  - [x] 8.1 Crear MenuController
    - Endpoint GET /menu
    - Inyectar MenuService
    - Llamar a getActiveProducts() y devolver ResponseEntity con 200 OK
    - _Requisitos: 1.1_
  
  - [x] 8.2 Crear OrderController
    - Endpoint POST /orders con @Valid CreateOrderRequest
      - Llamar a orderService.createOrder()
      - Devolver ResponseEntity con 201 Created
    - Endpoint GET /orders/{id} con @PathVariable UUID id
      - Llamar a orderService.getOrderById()
      - Devolver ResponseEntity con 200 OK
    - Endpoint GET /orders con @RequestParam(required=false) OrderStatus status
      - Llamar a orderService.getOrders(status)
      - Devolver ResponseEntity con 200 OK
    - Endpoint PATCH /orders/{id}/status con @PathVariable UUID id y @Valid UpdateStatusRequest
      - Llamar a orderService.updateOrderStatus()
      - Devolver ResponseEntity con 200 OK
    - _Requisitos: 2.1, 4.1, 5.1, 6.1_

- [x] 9. Configurar Swagger/OpenAPI
  - Crear OpenAPIConfig con @Configuration
  - Configurar OpenAPI bean con información de API (título, versión, descripción)
  - Agregar ejemplos de request/response en anotaciones de controladores
  - Documentar códigos de estado HTTP en cada endpoint con @ApiResponse
  - _Requisitos: 10.1, 10.2, 10.3, 10.4_

- [ ] 10. Implementar Kitchen Worker
  - [x] 10.1 Crear OrderProcessingService
    - Inyectar OrderRepository
    - Implementar método processOrder(OrderPlacedEvent event)
      - Buscar Order por event.getOrderId()
      - Si no existe, log error y return (no lanzar excepción)
      - Si existe, actualizar status a IN_PREPARATION
      - Guardar en OrderRepository (updatedAt se actualiza automáticamente)
      - Log información de procesamiento exitoso
    - Agregar try-catch para logging de errores y re-lanzar excepción para trigger retry
    - _Requisitos: 7.2, 7.3, 7.4, 7.5, 7.6_
  
  - [x] 10.2 Crear OrderEventListener
    - Anotar con @Component
    - Inyectar OrderProcessingService
    - Implementar método handleOrderPlacedEvent(OrderPlacedEvent event)
      - Anotar con @RabbitListener(queues = "${rabbitmq.queue.name}")
      - Llamar a orderProcessingService.processOrder(event)
    - _Requisitos: 7.1, 7.2_

- [ ] 11. Crear archivos de configuración
  - [x] 11.1 Crear application.yml para order-service
    - Configurar spring.application.name = order-service
    - Configurar datasource (url, username, password, driver)
    - Configurar JPA (hibernate.ddl-auto=validate, show-sql=false, dialect)
    - Configurar Flyway (enabled=true, locations, baseline-on-migrate)
    - Configurar RabbitMQ (host, port, username, password)
    - Configurar server.port = 8080
    - Configurar propiedades custom de RabbitMQ (exchange.name, routing-key)
    - Configurar SpringDoc (api-docs.path, swagger-ui.path)
    - _Requisitos: 12.1, 12.2, 12.5_
  
  - [x] 11.2 Crear application.yml para kitchen-worker
    - Configurar spring.application.name = kitchen-worker
    - Configurar datasource (misma BD que order-service)
    - Configurar JPA (hibernate.ddl-auto=validate, show-sql=false)
    - Configurar RabbitMQ (host, port, username, password)
    - Configurar retry policy (enabled, initial-interval, max-attempts, multiplier, max-interval)
    - Configurar server.port = 8081
    - Configurar propiedades custom de RabbitMQ (exchange.name, queue.name, routing-key, dlq)
    - _Requisitos: 12.3, 12.4, 12.6_

- [x] 12. Checkpoint - Verificar compilación y configuración básica
  - Compilar ambos proyectos con mvn clean install
  - Verificar que no hay errores de compilación
  - Verificar que las migraciones Flyway están correctamente ubicadas
  - Preguntar al usuario si hay dudas o problemas

- [ ]* 13. Escribir pruebas unitarias para Order Service
  - [ ]* 13.1 Escribir MenuServiceTest
    - Test: getActiveProducts devuelve solo productos activos
    - Test: getActiveProducts devuelve lista vacía cuando no hay productos
    - _Requisitos: 1.3, 1.4_
  
  - [ ]* 13.2 Escribir OrderServiceTest
    - Test: createOrder con datos válidos crea pedido con status PENDING
    - Test: createOrder con producto inexistente lanza ProductNotFoundException
    - Test: createOrder con tableId inválido lanza InvalidOrderException
    - Test: createOrder con items vacío lanza InvalidOrderException
    - Test: getOrderById con ID válido devuelve pedido
    - Test: getOrderById con ID inexistente lanza OrderNotFoundException
    - Test: getOrders sin filtro devuelve todos los pedidos
    - Test: getOrders con filtro devuelve solo pedidos del estado especificado
    - Test: updateOrderStatus actualiza estado y updatedAt
    - _Requisitos: 2.3, 2.6, 2.7, 2.8, 4.2, 4.3, 5.2, 5.3, 6.2_
  
  - [ ]* 13.3 Escribir OrderEventPublisherTest
    - Test: publishOrderPlacedEvent envía mensaje a RabbitMQ
    - Test: publishOrderPlacedEvent no lanza excepción si RabbitMQ falla
    - _Requisitos: 3.1, 3.4_
  
  - [ ]* 13.4 Escribir GlobalExceptionHandlerTest
    - Test: ProductNotFoundException devuelve 404
    - Test: OrderNotFoundException devuelve 404
    - Test: InvalidOrderException devuelve 400
    - Test: MethodArgumentNotValidException devuelve 400
    - Test: DataAccessException devuelve 503
    - Test: Exception genérica devuelve 500
    - _Requisitos: 11.1, 11.2, 11.3, 11.4_

- [ ]* 14. Escribir pruebas basadas en propiedades para Order Service
  - [ ]* 14.1 Escribir MenuPropertyTests
    - **Propiedad 1: Filtrado de productos activos en menú**
    - Generar productos aleatorios con diferentes valores de isActive
    - Verificar que todos los productos devueltos tienen isActive = true
    - Configurar @Property con tries = 100
    - **Valida: Requisitos 1.3**
  
  - [ ]* 14.2 Escribir OrderCreationPropertyTests
    - **Propiedad 2: Validación de productos en creación de pedidos**
    - Generar solicitudes con productIds inválidos (inexistentes o inactivos)
    - Verificar que todas son rechazadas con error 400
    - **Valida: Requisitos 2.2, 2.6**
    
    - **Propiedad 3: Creación completa de pedidos válidos**
    - Generar solicitudes válidas aleatorias (tableId positivo, productos válidos, items no vacío)
    - Verificar que todos los pedidos tienen UUID único, status PENDING, timestamps establecidos, items asociados
    - **Valida: Requisitos 2.3, 2.4, 2.5**
    
    - **Propiedad 4: Validación de tableId en creación de pedidos**
    - Generar tableIds inválidos (null, cero, negativo)
    - Verificar que todos son rechazados con error 400
    - **Valida: Requisitos 2.7**
  
  - [ ]* 14.3 Escribir EventPublishingPropertyTests
    - **Propiedad 5: Publicación de eventos para pedidos exitosos**
    - Generar pedidos válidos aleatorios
    - Verificar que se publica evento para cada uno
    - **Valida: Requisitos 3.1**
    
    - **Propiedad 6: Contenido completo de eventos de pedidos**
    - Generar pedidos aleatorios
    - Verificar que eventos contienen orderId, tableId, items, createdAt
    - **Valida: Requisitos 3.3**
    
    - **Propiedad 7: Serialización JSON de eventos (Round Trip)**
    - Generar OrderPlacedEvent aleatorios
    - Serializar a JSON y deserializar
    - Verificar que el objeto resultante es equivalente al original
    - **Valida: Requisitos 3.5, 7.2**
  
  - [ ]* 14.4 Escribir OrderRetrievalPropertyTests
    - **Propiedad 8: Recuperación completa de pedidos existentes**
    - Generar y persistir pedidos aleatorios
    - Recuperar por ID y verificar que contienen todos los campos
    - **Valida: Requisitos 4.2**
    
    - **Propiedad 9: Error 404 para pedidos inexistentes**
    - Generar UUIDs aleatorios que no existen
    - Verificar que todos devuelven 404
    - **Valida: Requisitos 4.3**
  
  - [ ]* 14.5 Escribir OrderFilteringPropertyTests
    - **Propiedad 10: Filtrado correcto por estado**
    - Generar pedidos aleatorios con diferentes estados
    - Para cada estado válido, verificar que filtrado devuelve solo pedidos de ese estado
    - **Valida: Requisitos 5.2, 5.4**
    
    - **Propiedad 11: Validación de estado inválido en filtrado**
    - Generar valores de estado inválidos aleatorios
    - Verificar que todos devuelven 400
    - **Valida: Requisitos 5.5**
  
  - [ ]* 14.6 Escribir OrderStatusUpdatePropertyTests
    - **Propiedad 12: Actualización válida de estado de pedidos**
    - Generar pedidos aleatorios y estados válidos aleatorios
    - Actualizar estado y verificar que status y updatedAt cambian
    - Verificar que updatedAt es posterior al valor anterior
    - **Valida: Requisitos 6.2, 6.3**
    
    - **Propiedad 13: Error 404 en actualización de pedido inexistente**
    - Generar UUIDs aleatorios que no existen
    - Verificar que todos devuelven 404
    - **Valida: Requisitos 6.4**
    
    - **Propiedad 14: Validación de estado inválido en actualización**
    - Generar valores de estado inválidos aleatorios
    - Verificar que todos devuelven 400
    - **Valida: Requisitos 6.5**

- [ ]* 15. Escribir pruebas unitarias para Kitchen Worker
  - [ ]* 15.1 Escribir OrderProcessingServiceTest
    - Test: processOrder con orderId existente actualiza estado a IN_PREPARATION
    - Test: processOrder con orderId inexistente no lanza excepción
    - Test: processOrder actualiza updatedAt
    - _Requisitos: 7.4, 7.5, 7.6_
  
  - [ ]* 15.2 Escribir OrderEventListenerTest
    - Test: handleOrderPlacedEvent llama a OrderProcessingService
    - Test: handleOrderPlacedEvent deserializa evento correctamente
    - _Requisitos: 7.1, 7.2_

- [ ]* 16. Escribir pruebas basadas en propiedades para Kitchen Worker
  - [ ]* 16.1 Escribir OrderProcessingPropertyTests
    - **Propiedad 15: Procesamiento de eventos actualiza estado correctamente**
    - Generar eventos aleatorios con orderIds existentes
    - Verificar que estado cambia a IN_PREPARATION y updatedAt se actualiza
    - **Valida: Requisitos 7.4, 7.5**
    
    - **Propiedad 16: Manejo de eventos con pedidos inexistentes**
    - Generar eventos con orderIds que no existen
    - Verificar que se manejan gracefully sin lanzar excepciones
    - **Valida: Requisitos 7.6**

- [ ]* 17. Escribir pruebas de integración
  - Configurar TestContainers para PostgreSQL y RabbitMQ
  - Escribir OrderFlowIntegrationTest que verifique flujo completo:
    - Crear pedido vía POST /orders
    - Verificar que evento se publica a RabbitMQ
    - Verificar que Kitchen Worker consume evento
    - Verificar que estado del pedido cambia a IN_PREPARATION
    - Verificar que GET /orders/{id} devuelve pedido actualizado
  - _Requisitos: 2.1, 3.1, 7.1, 7.4_

- [x] 18. Checkpoint final - Verificar sistema completo
  - Iniciar PostgreSQL y RabbitMQ (Docker)
  - Ejecutar migraciones Flyway
  - Iniciar order-service y verificar que arranca sin errores
  - Iniciar kitchen-worker y verificar que arranca sin errores
  - Verificar Swagger UI en http://localhost:8080/swagger-ui.html
  - Verificar RabbitMQ Management en http://localhost:15672
  - Ejecutar todas las pruebas con mvn test
  - Preguntar al usuario si hay dudas o problemas

## Notas

- Las tareas marcadas con `*` son opcionales y pueden omitirse para un MVP más rápido
- Cada tarea referencia requisitos específicos para trazabilidad
- Los checkpoints aseguran validación incremental
- Las pruebas de propiedades validan propiedades de corrección universales
- Las pruebas unitarias validan ejemplos específicos y casos edge
- La configuración de jqwik debe usar mínimo 100 iteraciones por propiedad (@Property(tries = 100))
