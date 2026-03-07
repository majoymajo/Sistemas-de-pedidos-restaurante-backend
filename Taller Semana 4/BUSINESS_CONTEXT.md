# Contexto de Negocio — Sistema de Pedidos de Restaurante

---

## 1. Descripción del Proyecto

| Campo | Detalle |
|-------|---------|
| **Nombre del Proyecto** | Sistema de Pedidos de Restaurante |
| **Objetivo** | Implementar un sistema basado en microservicios orientado a eventos que permita gestionar pedidos de restaurante, desacoplando la toma de pedidos y la preparación en cocina, asegurando escalabilidad, resiliencia y calidad mediante automatización de pruebas. |

---

## 2. Flujos Críticos del Negocio

### Principales Flujos de Trabajo

1. Creación de órdenes desde frontend.
2. Publicación del evento `order.placed` en RabbitMQ.
3. Procesamiento asíncrono por `kitchen-worker`.
4. Cambio de estados: `PENDING → IN_PREPARATION → READY`.
5. Generación de reportes de ventas por rango de fechas.

### Módulos o Funcionalidades Críticas

| Módulo | Descripción |
|--------|-------------|
| `order-service` | API REST principal |
| `kitchen-worker` | Procesamiento de eventos |
| `report-service` | Reportes y métricas |
| Suite de pruebas | Automatización con Serenity BDD |

---

## 3. Reglas de Negocio y Restricciones

### Reglas de Negocio Relevantes

- `tableId` debe estar entre **1 y 12**.
- Una orden debe contener **al menos un ítem**.
- El producto debe **existir y estar activo**.
- `quantity` debe ser **mayor o igual a 1**.
- `eventVersion` del evento `order.placed` debe ser exactamente **1**.
- Endpoints críticos requieren header **`X-Kitchen-Token`** válido.

### Regulaciones o Normativas

- Validación estricta de contratos REST (OpenAPI).
- Principio de seguridad por defecto (operaciones destructivas protegidas).

---

## 4. Perfiles de Usuario y Roles

### Roles del Sistema

| Rol | Responsabilidad |
|-----|----------------|
| **Cliente** | Crea pedidos desde la interfaz web |
| **Personal de Cocina** | Cambia estado de órdenes |
| **Administrador** | Consulta reportes |
| **QA** | Ejecuta pruebas automatizadas |

### Permisos y Limitaciones

- Solo cocina puede cambiar estado a `READY`.
- Solo endpoints protegidos requieren token.
- No hay autenticación para clientes en MVP.

---

## 5. Condiciones del Entorno Técnico

### Plataformas Soportadas

- Aplicación Web SPA (React + TypeScript).
- Backend con microservicios Spring Boot.

### Tecnologías e Integraciones Clave

| Tecnología | Rol |
|------------|-----|
| Java 17 + Spring Boot 3 | Backend principal |
| PostgreSQL (3 bases independientes) | Persistencia |
| RabbitMQ | Comunicación asíncrona AMQP |
| Docker y Docker Compose | Infraestructura de contenedores |
| Serenity BDD + Cucumber | Automatización QA |

---

## 6. Casos Especiales o Excepciones

| Caso | Comportamiento |
|------|---------------|
| `eventVersion != 1` | Envío a Dead Letter Queue sin reintentos |
| Procesamiento duplicado en `kitchen-worker` | Idempotencia implementada (upsert) |
| Eliminación de órdenes | Soft delete (no eliminación física) |
