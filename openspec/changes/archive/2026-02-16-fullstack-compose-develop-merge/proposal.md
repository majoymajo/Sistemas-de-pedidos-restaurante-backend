## Why

Necesitamos que la integracion full‑stack se ejecute en un unico entorno reproducible (Docker Compose) para QA y estres, evitando configuraciones locales divergentes y estandarizando puertos y dependencias.

## What Changes

- Agregar infraestructura Docker Compose que levante frontend, order-service, kitchen-worker, postgres y rabbitmq en un solo stack.
- Estandarizar puertos y variables de entorno (backend en 8080, frontend en 5173, rabbitmq 5672/15672, postgres 5432).
- Ejecutar el frontend dentro de Docker Compose (no fuera) para pruebas integradas.
- Documentar el flujo de ejecucion y verificacion (incluyendo comprobacion de RabbitMQ).
- Preparar rama de integracion para merge hacia develop siguiendo GitFlow.

## Capabilities

### New Capabilities
- `fullstack-compose`: Infraestructura Docker Compose para ejecutar front, back, worker, postgres y rabbitmq con puertos estandarizados y verificacion de integracion.

### Modified Capabilities
- (none)

## Impact

- Nuevos/actualizados archivos de infraestructura: `docker-compose.yml`, `.env.example`, posibles Dockerfiles.
- Configuracion de servicios backend/frontend para puertos y variables estandarizadas.
- Documentacion del setup local y checklist de verificacion (README / docs).
