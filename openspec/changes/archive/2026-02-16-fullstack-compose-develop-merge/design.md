## Context

El repositorio separa frontend y backend en ramas distintas y la integracion se ha validado localmente con servicios levantados a mano. Para QA y pruebas de estres necesitamos un entorno reproducible que levante todo el stack (frontend, order-service, kitchen-worker, postgres y rabbitmq) en un solo Docker Compose. Se requiere estandarizar puertos (backend 8080, frontend 5173, rabbit 5672/15672, postgres 5432) y usar una sola base de datos para simplificar la operacion.

## Goals / Non-Goals

**Goals:**
- Proveer un `docker-compose.yml` que levante front, back, worker, postgres y rabbitmq en un solo entorno.
- Asegurar que el frontend consuma la API real dentro de compose.
- Documentar pasos de ejecucion y verificacion, incluyendo chequeo de RabbitMQ.
- Preparar el flujo de merge a `develop` siguiendo GitFlow.

**Non-Goals:**
- No cambiar reglas de negocio ni contratos API/eventos.
- No introducir CI/CD ni despliegues en produccion.

## Decisions

- **Docker Compose como orquestador local**: simplifica el setup y evita configuraciones inconsistentes. Alternativa: scripts manuales (descartada por fragilidad).
- **Frontend dentro de Compose**: garantiza entorno integrado. Alternativa: correr frontend fuera de compose (descartada por inconsistencia).
- **Una sola base de datos Postgres**: reduce complejidad para QA. Alternativa: una DB por servicio (descartada por overhead).
- **Puertos estandarizados**: 8080 backend, 5173 frontend, 5432 postgres, 5672/15672 rabbitmq. Alternativa: puertos variables por maquina (descartada).

## Risks / Trade-offs

- [Riesgo] Cambios locales previos en puertos pueden romper scripts → Mitigacion: documentar `.env.example` y pasos de migracion.
- [Riesgo] Docker en Windows puede tener performance variable → Mitigacion: instrucciones claras y troubleshooting.
- [Trade-off] Una sola DB reduce aislamiento entre servicios → Mitigacion: QA aceptable, se documenta que es para entorno local.
