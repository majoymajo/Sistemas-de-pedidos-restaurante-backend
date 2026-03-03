# Quick Demo Deploy (temporal)

## URLs (temporales)

- Frontend: https://apparatus-doing-necklace-zen.trycloudflare.com
- Backend: https://absent-forum-rogers-wallet.trycloudflare.com

Nota: las URLs de Quick Tunnel cambian si se cierra la terminal de `cloudflared`.

## Pasos para levantar demo

1. Actualizar develop:
   - `git checkout develop`
   - `git pull origin develop`
2. Levantar stack:
   - `docker compose up -d --build`
3. Tunnels:
   - Backend: `cloudflared tunnel --url http://localhost:8080`
   - Frontend: `cloudflared tunnel --url http://localhost:5173`
4. Configurar frontend:
   - `.env`:
     - `VITE_API_BASE_URL=<URL_BACKEND_PUBLICA>`
     - `VITE_USE_MOCK=false`
   - Rebuild: `docker compose up -d --build frontend`

## Validacion minima

1. Abrir frontend publico.
2. Ver menu, crear pedido.
3. Ir a cocina y confirmar el pedido.

## Como detener

- `docker compose down`
- Cerrar las ventanas de `cloudflared`
