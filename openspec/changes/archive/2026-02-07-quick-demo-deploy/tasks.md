## 1. Preparar develop

- [x] 1.1 Actualizar `develop` y validar que incluye el fix de `.env`
- [x] 1.2 Levantar stack con Docker Compose (`docker compose up -d --build`)
- [x] 1.3 Resetear datos locales para demo limpia (`docker compose down -v`)
- [x] 1.4 Aplicar fix de polling de cocina localmente para la demo

## 2. Exponer demo con Quick Tunnel

- [x] 2.1 Instalar `cloudflared` si no existe
- [x] 2.2 Crear tunnel para backend (8080) y copiar URL
- [x] 2.3 Ajustar `.env` del frontend con `VITE_API_BASE_URL` y rebuild frontend
- [x] 2.4 Crear tunnel para frontend (5173) y copiar URL
- [x] 2.5 Permitir dominios `trycloudflare.com` en `vite.config.ts`
- [x] 2.6 Permitir CORS para `https://*.trycloudflare.com` en Order Service

## 3. Validacion

- [x] 3.1 Verificar menu, crear pedido y ver cocina desde URL publica
- [x] 3.2 Documentar URLs y pasos de demo
