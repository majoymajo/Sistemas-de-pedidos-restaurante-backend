# Documento de Requisitos

## Introducción

Este documento especifica los requisitos para un MVP de sistema backend de pedidos de restaurante utilizando arquitectura de microservicios. El sistema permite al personal del restaurante gestionar pedidos a través de una API REST, con procesamiento asíncrono manejado por un servicio de trabajador de cocina. Los dos microservicios se comunican a través del broker de mensajes RabbitMQ, con PostgreSQL proporcionando persistencia de datos.

## Glosario

- **Order_Service**: El microservicio de API REST que maneja la creación, validación y persistencia de pedidos
- **Kitchen_Worker**: El microservicio consumidor que procesa eventos de pedidos y actualiza el estado del pedido
- **RabbitMQ**: El broker de mensajes que facilita la comunicación asíncrona entre microservicios
- **Order**: Una solicitud del cliente que contiene uno o más productos para una mesa específica
- **Product**: Un elemento del menú que puede ser pedido por los clientes
- **Order_Item**: Un elemento de línea dentro de un pedido que especifica un producto, cantidad y notas opcionales
- **Order_Status**: El estado actual de un pedido (PENDING, IN_PREPARATION, READY)
- **Message_Broker**: La instancia de RabbitMQ que gestiona colas de mensajes e intercambios
- **Dead_Letter_Queue**: Una cola que almacena mensajes que fallaron en el procesamiento para análisis posterior

## Requisitos

### Requisito 1: Gestión de Productos

**Historia de Usuario:** Como miembro del personal del restaurante, quiero ver los elementos del menú disponibles, para poder crear pedidos con productos válidos.

#### Criterios de Aceptación

1. EL Order_Service DEBERÁ exponer un endpoint GET /menu que devuelva productos activos
2. CUANDO se llame al endpoint /menu, EL Order_Service DEBERÁ devolver exactamente 3 productos activos con id, nombre y descripción
3. EL Order_Service DEBERÁ incluir solo productos donde isActive sea verdadero en la respuesta del menú
4. CUANDO un producto no exista en la base de datos, EL Order_Service DEBERÁ devolver una lista vacía sin error

### Requisito 2: Creación de Pedidos

**Historia de Usuario:** Como miembro del personal del restaurante, quiero crear pedidos para las mesas de clientes, para que la cocina pueda preparar los elementos solicitados.

#### Criterios de Aceptación

1. EL Order_Service DEBERÁ exponer un endpoint POST /orders que acepte tableId y una lista de elementos del pedido
2. CUANDO se cree un pedido, EL Order_Service DEBERÁ validar que todos los productIds referenciados existan y estén activos
3. CUANDO se cree un pedido con datos válidos, EL Order_Service DEBERÁ persistir el pedido con estado PENDING en la base de datos PostgreSQL
4. CUANDO se cree un pedido, EL Order_Service DEBERÁ generar un UUID único como identificador del pedido
5. CUANDO se cree un pedido, EL Order_Service DEBERÁ establecer automáticamente las marcas de tiempo createdAt y updatedAt
6. SI un productId referenciado no existe o está inactivo, ENTONCES EL Order_Service DEBERÁ rechazar el pedido y devolver una respuesta de error
7. SI el tableId falta o es inválido, ENTONCES EL Order_Service DEBERÁ rechazar el pedido y devolver una respuesta de error
8. SI la lista de elementos del pedido está vacía, ENTONCES EL Order_Service DEBERÁ rechazar el pedido y devolver una respuesta de error

### Requisito 3: Publicación de Eventos de Pedidos

**Historia de Usuario:** Como arquitecto de sistemas, quiero que los pedidos se publiquen como eventos, para que la cocina pueda procesarlos de forma asíncrona.

#### Criterios de Aceptación

1. CUANDO un pedido se cree y persista exitosamente, EL Order_Service DEBERÁ publicar un evento "order.placed" a RabbitMQ
2. EL Order_Service DEBERÁ publicar eventos a un intercambio de tópicos con clave de enrutamiento "order.placed"
3. EL Order_Service DEBERÁ incluir orderId, tableId, array de items y marca de tiempo createdAt en la carga útil del evento
4. SI la publicación a RabbitMQ falla, ENTONCES EL Order_Service DEBERÁ registrar el error y devolver una respuesta exitosa (el pedido ya está persistido)
5. EL Order_Service DEBERÁ serializar las cargas útiles de eventos en formato JSON

### Requisito 4: Recuperación de Pedidos

**Historia de Usuario:** Como miembro del personal del restaurante, quiero recuperar detalles de pedidos, para poder verificar el estado y contenido del pedido.

#### Criterios de Aceptación

1. EL Order_Service DEBERÁ exponer un endpoint GET /orders/{id} que acepte un identificador UUID de pedido
2. CUANDO se proporcione un ID de pedido válido, EL Order_Service DEBERÁ devolver el pedido completo con todos los elementos, estado y marcas de tiempo
3. SI el ID del pedido no existe, ENTONCES EL Order_Service DEBERÁ devolver una respuesta 404 Not Found
4. SI el formato del ID del pedido es inválido, ENTONCES EL Order_Service DEBERÁ devolver una respuesta 400 Bad Request

### Requisito 5: Filtrado de Pedidos

**Historia de Usuario:** Como miembro del personal del restaurante, quiero filtrar pedidos por estado, para poder ver pedidos en etapas específicas de preparación.

#### Criterios de Aceptación

1. EL Order_Service DEBERÁ exponer un endpoint GET /orders con un parámetro de consulta status opcional
2. CUANDO se proporcione el parámetro status, EL Order_Service DEBERÁ devolver solo pedidos que coincidan con ese estado
3. CUANDO se omita el parámetro status, EL Order_Service DEBERÁ devolver todos los pedidos
4. EL Order_Service DEBERÁ soportar filtrado por valores de estado PENDING, IN_PREPARATION y READY
5. SI se proporciona un valor de estado inválido, ENTONCES EL Order_Service DEBERÁ devolver una respuesta 400 Bad Request

### Requisito 6: Actualizaciones de Estado de Pedidos

**Historia de Usuario:** Como miembro del personal del restaurante, quiero actualizar manualmente el estado del pedido, para poder corregir o avanzar estados de pedidos cuando sea necesario.

#### Criterios de Aceptación

1. EL Order_Service DEBERÁ exponer un endpoint PATCH /orders/{id}/status que acepte un valor de estado
2. CUANDO se solicite una actualización de estado válida, EL Order_Service DEBERÁ actualizar el estado del pedido y la marca de tiempo updatedAt
3. EL Order_Service DEBERÁ aceptar PENDING, IN_PREPARATION y READY como valores de estado válidos
4. SI el ID del pedido no existe, ENTONCES EL Order_Service DEBERÁ devolver una respuesta 404 Not Found
5. SI el valor del estado es inválido, ENTONCES EL Order_Service DEBERÁ devolver una respuesta 400 Bad Request

### Requisito 7: Procesamiento Asíncrono de Pedidos

**Historia de Usuario:** Como trabajador de cocina, quiero que los pedidos se reciban automáticamente para procesamiento, para poder preparar pedidos de clientes sin sondeo manual.

#### Criterios de Aceptación

1. EL Kitchen_Worker DEBERÁ escuchar la cola "order.placed" vinculada al intercambio de tópicos
2. CUANDO se reciba un evento "order.placed", EL Kitchen_Worker DEBERÁ deserializar la carga útil JSON
3. CUANDO se reciba un evento "order.placed", EL Kitchen_Worker DEBERÁ registrar los detalles del pedido incluyendo orderId y tableId
4. CUANDO se procese un evento de pedido, EL Kitchen_Worker DEBERÁ actualizar el estado del pedido a IN_PREPARATION en la base de datos
5. CUANDO se procese un evento de pedido, EL Kitchen_Worker DEBERÁ actualizar la marca de tiempo updatedAt
6. SI el ID del pedido en el evento no existe en la base de datos, ENTONCES EL Kitchen_Worker DEBERÁ registrar un error y reconocer el mensaje
7. SI el procesamiento del mensaje falla después de reintentos, ENTONCES EL Kitchen_Worker DEBERÁ enrutar el mensaje a la Dead Letter Queue

### Requisito 8: Configuración del Broker de Mensajes

**Historia de Usuario:** Como arquitecto de sistemas, quiero entrega confiable de mensajes entre servicios, para que los pedidos no se pierdan durante el procesamiento.

#### Criterios de Aceptación

1. EL Message_Broker DEBERÁ usar un intercambio de tópicos llamado "order.exchange" para eventos de pedidos
2. EL Message_Broker DEBERÁ enrutar mensajes con clave de enrutamiento "order.placed" a la cola de cocina
3. EL Message_Broker DEBERÁ configurar una Dead Letter Queue para procesamiento de mensajes fallidos
4. EL Message_Broker DEBERÁ persistir mensajes en disco para sobrevivir reinicios del broker
5. EL Kitchen_Worker DEBERÁ reconocer mensajes solo después de procesamiento exitoso

### Requisito 9: Persistencia de Datos

**Historia de Usuario:** Como arquitecto de sistemas, quiero que los datos de pedidos se almacenen de forma confiable, para que el historial de pedidos se preserve a través de reinicios de servicios.

#### Criterios de Aceptación

1. EL Order_Service DEBERÁ usar PostgreSQL para persistir entidades Product, Order y OrderItem
2. EL Order_Service DEBERÁ usar migraciones Flyway para gestionar versiones del esquema de base de datos
3. EL Order_Service DEBERÁ inicializar la base de datos con al menos 3 productos activos en el primer inicio
4. EL Kitchen_Worker DEBERÁ usar la misma base de datos PostgreSQL para actualizar el estado del pedido
5. CUANDO la base de datos no esté disponible, EL Order_Service DEBERÁ devolver una respuesta 503 Service Unavailable

### Requisito 10: Documentación de API

**Historia de Usuario:** Como desarrollador, quiero documentación de API, para poder entender y probar los endpoints disponibles.

#### Criterios de Aceptación

1. EL Order_Service DEBERÁ exponer documentación Swagger/OpenAPI en /swagger-ui.html
2. EL Order_Service DEBERÁ documentar todos los endpoints REST con esquemas de solicitud/respuesta
3. EL Order_Service DEBERÁ incluir cargas útiles de solicitud de ejemplo en la documentación de API
4. EL Order_Service DEBERÁ documentar todos los códigos de estado HTTP posibles para cada endpoint

### Requisito 11: Manejo de Errores

**Historia de Usuario:** Como desarrollador, quiero respuestas de error consistentes, para poder manejar fallos apropiadamente en aplicaciones cliente.

#### Criterios de Aceptación

1. CUANDO falle la validación, EL Order_Service DEBERÁ devolver una respuesta 400 Bad Request con detalles del error
2. CUANDO no se encuentre un recurso, EL Order_Service DEBERÁ devolver una respuesta 404 Not Found con un mensaje descriptivo
3. CUANDO ocurra un error interno, EL Order_Service DEBERÁ devolver una respuesta 500 Internal Server Error
4. CUANDO la base de datos no esté disponible, EL Order_Service DEBERÁ devolver una respuesta 503 Service Unavailable
5. EL Order_Service DEBERÁ incluir una marca de tiempo y mensaje de error en todas las respuestas de error
6. EL Order_Service DEBERÁ registrar todos los errores con suficiente contexto para depuración

### Requisito 12: Configuración de Servicios

**Historia de Usuario:** Como operador de sistemas, quiero configuración externalizada, para poder desplegar servicios en diferentes entornos sin cambios de código.

#### Criterios de Aceptación

1. EL Order_Service DEBERÁ leer detalles de conexión de base de datos desde application.yml
2. EL Order_Service DEBERÁ leer detalles de conexión de RabbitMQ desde application.yml
3. EL Kitchen_Worker DEBERÁ leer detalles de conexión de base de datos desde application.yml
4. EL Kitchen_Worker DEBERÁ leer detalles de conexión de RabbitMQ desde application.yml
5. EL Order_Service DEBERÁ exponer su API REST en un puerto configurable (predeterminado 8080)
6. EL Kitchen_Worker DEBERÁ ejecutarse en un puerto configurable (predeterminado 8081)
