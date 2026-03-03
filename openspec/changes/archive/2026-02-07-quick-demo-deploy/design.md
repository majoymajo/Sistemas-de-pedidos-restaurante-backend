## Context

Se requiere un demo publico temporal sin costo. El stack ya corre localmente via Docker Compose (frontend + order-service + kitchen-worker + postgres + rabbitmq). Para exponerlo sin infraestructura permanente, se usara Cloudflare Quick Tunnel (`cloudflared`) con URLs temporales. Debe mantenerse el uso de `develop` y sincronizar cualquier cambio reciente (.env corregido).

## Goals / Non-Goals

**Goals:**
- Levantar el stack completo desde `develop` con Docker Compose.
- Publicar URLs temporales para frontend y backend usando Quick Tunnel.
- Configurar el frontend para consumir el backend publico durante la demo.
- Documentar el flujo de comandos y validacion para repetir el demo.

**Non-Goals:**
- No implementar CI/CD ni hosting permanente.
- No exponer RabbitMQ UI al publico (solo local).
- No cambiar contratos API ni logica de negocio.

## Decisions

- **Docker Compose como base**: asegura entorno reproducible y evita instalaciones manuales.
- **Quick Tunnel (cloudflared)**: permite URLs publicas temporales sin cuenta ni costo.
- **Exponer solo frontend + backend**: RabbitMQ y Postgres permanecen internos.
- **Actualizar develop antes del demo**: para incluir el fix del `.env` y evitar inconsistencias de puertos.

## Risks / Trade-offs

- [Riesgo] URLs temporales cambian al cerrar terminal → Mitigacion: dejar procesos activos y documentar que son temporales.
- [Riesgo] Latencia variable del tunnel → Mitigacion: limitar alcance del demo y validar endpoints principales.
- [Riesgo] Exposicion publica sin autenticacion → Mitigacion: usar demo corta y no compartir credenciales sensibles.

## Migration Plan

1. Checkout `develop` y pull latest (incluye fix de .env).
2. `docker compose up -d --build`.
3. Instalar `cloudflared` si no existe.
4. Crear tunnel para backend (puerto 8080).
5. Ajustar `.env` del frontend para apuntar al backend publico y rebuild frontend.
6. Crear tunnel para frontend (puerto 5173).
7. Validar flujo: menu, crear pedido, ver cocina.

## Open Questions

- ¿Se requiere exponer Kitchen Board con autenticacion? (actualmente no).
