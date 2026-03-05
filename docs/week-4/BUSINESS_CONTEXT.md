# Contexto de Negocio del Proyecto

**Sistema de Pedidos de Restaurante**

------------------------------------------------------------------------

## 1. Descripción del Proyecto

**Nombre del Proyecto:**\
Sistema de Pedidos de Restaurante

**Objetivo del Proyecto:**\
Implementar un sistema de gestión de pedidos para restaurante basado en
una arquitectura de microservicios orientada a eventos. El sistema
desacopla el proceso de toma de pedidos y la preparación en cocina
mediante comunicación asíncrona utilizando RabbitMQ.

El sistema busca:

-   Mejorar la **escalabilidad** mediante microservicios independientes.
-   Garantizar **resiliencia** mediante colas de mensajería.
-   Permitir **procesamiento asíncrono de órdenes** entre servicios.
-   Asegurar la **calidad del software** mediante automatización de
    pruebas con Serenity BDD.

El sistema permite que:

-   Clientes realicen pedidos desde una interfaz web.
-   Personal de cocina gestione la preparación de pedidos.
-   Administradores consulten reportes de ventas.
-   QA valide el sistema mediante pruebas automatizadas.

------------------------------------------------------------------------

## 2. Flujos Críticos del Negocio

### Principales Flujos de Trabajo

1.  **Creación de órdenes desde el frontend**
    -   El usuario selecciona productos del menú.
    -   Se envía una solicitud HTTP al `order-service`.
2.  **Registro de la orden**
    -   El `order-service` valida reglas de negocio.
    -   La orden se guarda en la base de datos `restaurant_db`.
3.  **Publicación de evento**
    -   El `order-service` publica el evento `order.placed` en RabbitMQ.
4.  **Procesamiento asíncrono**
    -   El servicio `kitchen-worker` consume el evento.
    -   La orden pasa al estado **IN_PREPARATION**.
5.  **Gestión del estado de la orden**
    -   El personal de cocina puede marcar la orden como **READY**.
6.  **Generación de reportes**
    -   El `report-service` procesa eventos y genera métricas de ventas.

### Módulos o Funcionalidades Críticas

**order-service** - API REST principal para gestión de órdenes y menú.

**kitchen-worker** - Consumidor de eventos que procesa pedidos en
cocina.

**report-service** - Generación de reportes y métricas de negocio.

**Automatización QA** - Serenity BDD - Cucumber - REST Assured

------------------------------------------------------------------------

## 3. Reglas de Negocio y Restricciones

### Reglas de Negocio Relevantes

-   `tableId` debe estar **entre 1 y 12**.
-   Una orden debe contener **al menos un ítem**.
-   El **producto debe existir** en el menú.
-   El producto debe estar **activo** para poder ser ordenado.
-   `quantity` debe ser **mayor o igual a 1**.
-   El evento `order.placed` debe tener **eventVersion = 1**.
-   Los endpoints de cocina requieren el header **X-Kitchen-Token**.

### Regulaciones o Normativas

-   Validación estricta de **contratos REST mediante OpenAPI**.
-   Principio de **seguridad por defecto**.
-   Protección de endpoints críticos.
-   Uso de **soft delete** para órdenes.

------------------------------------------------------------------------

## 4. Perfiles de Usuario y Roles

### Cliente

-   Realiza pedidos desde la aplicación web.

### Personal de Cocina

-   Visualiza órdenes pendientes.
-   Cambia el estado de órdenes a **READY**.

### Administrador

-   Consulta reportes de ventas.

### QA (Quality Assurance)

-   Ejecuta pruebas automatizadas.
-   Valida contratos de API.

### Permisos y Limitaciones

-   Solo cocina puede cambiar estados finales de órdenes.
-   Endpoints críticos requieren token.
-   Los clientes no requieren autenticación en el MVP.
-   Operaciones destructivas están protegidas.

------------------------------------------------------------------------

## 5. Condiciones del Entorno Técnico

### Plataformas Soportadas

-   Aplicación Web SPA (React + TypeScript)
-   Backend basado en microservicios
-   Infraestructura con contenedores Docker

### Tecnologías e Integraciones Clave

**Backend** - Java 17 - Spring Boot - Spring Data JPA - Spring AMQP

**Frontend** - React - TypeScript - Vite - TailwindCSS

**Infraestructura** - PostgreSQL - RabbitMQ - Docker - Docker Compose

**Automatización QA** - Serenity BDD - Cucumber - REST Assured - JUnit 5

------------------------------------------------------------------------

## 6. Casos Especiales o Excepciones

### Versionado de Eventos

Si:

eventVersion != 1

Entonces el mensaje se envía directamente a la **Dead Letter Queue
(DLQ)**.

### Idempotencia del Kitchen Worker

El `kitchen-worker` implementa idempotencia mediante:

UPSERT de órdenes

Esto evita duplicados cuando RabbitMQ reintenta mensajes.

### Soft Delete de Órdenes

Las órdenes no se eliminan físicamente de la base de datos.

En su lugar:

deleted = true

Esto permite auditoría, recuperación y trazabilidad de pedidos.
