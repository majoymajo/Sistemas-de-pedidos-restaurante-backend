## 1. Compose Setup

- [x] 1.1 Crear `docker-compose.yml` con frontend, order-service, kitchen-worker, postgres y rabbitmq
- [x] 1.2 Definir puertos estandarizados (8080, 5173, 5432, 5672/15672) y variables en `.env.example`
- [x] 1.3 Incluir red y volumenes persistentes para Postgres y RabbitMQ

## 2. Service Configuration

- [x] 2.1 Ajustar configuracion de backend para usar una sola DB en compose
- [x] 2.2 Configurar frontend dentro de compose con `VITE_USE_MOCK=false` y `VITE_API_BASE_URL`
- [x] 2.3 Documentar verificacion de RabbitMQ (UI management o cola)

## 3. Merge & Verification

- [x] 3.1 Crear rama de integracion desde develop y hacer merge de feature/Backend y feature/front-integration
- [x] 3.2 Resolver conflictos y asegurar compilacion
- [x] 3.3 Ejecutar smoke tests (GET /menu, POST /orders, GET /orders/{id}, UI cliente/cocina)
