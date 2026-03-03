## Why

Necesitamos una demo rapida con URL temporal para que cualquiera pueda probar el flujo completo (menu, pedido y cocina) sin instalar nada. Esto valida integracion end-to-end antes de mover cambios a `main`.

## What Changes

- Usar `develop` como base para levantar el stack completo con Docker Compose.
- Exponer backend y frontend mediante Cloudflare Quick Tunnel con URLs publicas temporales.
- Ajustar la configuracion del frontend para apuntar al backend publico durante la demo.
- Documentar el paso a paso (comandos y URLs) para repetir el demo.

## Capabilities

### New Capabilities
- `quick-demo-deploy`: Procedimiento para levantar el stack en `develop` y publicar una demo temporal via Quick Tunnel (frontend + backend).

### Modified Capabilities
<!-- None -->

## Impact

- Documentacion de despliegue rapido (comandos y validacion).
- Dependencia temporal en `cloudflared` para exponer servicios locales.
