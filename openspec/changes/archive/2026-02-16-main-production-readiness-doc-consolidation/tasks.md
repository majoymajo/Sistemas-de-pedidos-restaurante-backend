## 1. Baseline y sincronizacion de ramas

- [x] 1.1 Actualizar rama `develop` con `git pull --rebase origin develop` y resolver conflictos pendientes
- [x] 1.2 Verificar estado limpio inicial de trabajo (sin artefactos temporales ni archivos build no versionables)
- [x] 1.3 Confirmar diferencias entre `develop` y `main` para planear promotion controlada

## 2. Menu real y backend production-ready

- [x] 2.1 Expandir dataset de productos en backend para que `GET /menu` entregue catalogo completo con precio, categoria e imagen
- [x] 2.2 Ajustar contratos/DTOs del menu para soportar metadata requerida por frontend redisenado
- [x] 2.3 Validar que creacion de pedido y cambios de estado de cocina operan con el catalogo expandido
- [x] 2.4 Documentar configuracion de seguridad para endpoints protegidos de cocina (header/token esperado)

## 3. Frontend sin mocks por defecto en main

- [x] 3.1 Configurar entorno de produccion para `VITE_USE_MOCK=false` por defecto en la ruta de despliegue principal
- [x] 3.2 Mantener modo mock solo como opcion explicita de desarrollo y fallback controlado
- [x] 3.3 Validar flujo completo cliente y cocina en modo real (mesa, menu, carrito, confirmacion, estado, board cocina)
- [x] 3.4 Asegurar que los errores de integracion API se muestran en UI sin fallback silencioso a datos mock

## 4. Consolidacion documental por dominio

- [x] 4.1 Inventariar archivos Markdown de `docs/` y clasificar por auditoria, calidad y deuda tecnica
- [x] 4.2 Fusionar archivos extras redundantes en un unico documento significativo por dominio, preservando trazabilidad
- [x] 4.3 Mantener separados los entregables clave del reto (auditoria, calidad, deuda tecnica) sin mezclar contenido semantico
- [x] 4.4 Actualizar enlaces internos y eliminar referencias a archivos removidos

## 5. README canonico con Mermaid

- [x] 5.1 Redisenar `README.md` como punto de entrada unico con quickstart local y Docker Compose
- [x] 5.2 Agregar diagramas Mermaid para arquitectura de servicios y flujo cliente-cocina
- [x] 5.3 Incluir seccion de modos de ejecucion (desarrollo mock vs produccion API real)
- [x] 5.4 Incluir mapa de evidencia hacia auditoria, calidad, deuda tecnica y OpenSpec

## 6. Verificacion final, commits y promotion a main

- [x] 6.1 Ejecutar validaciones tecnicas (`npm run build`, compose up --build, smoke tests de endpoints y flujo UI)
- [x] 6.2 Registrar evidencia de validacion final en documentacion de calidad
- [x] 6.3 Crear commits atomicos por dominio (backend, frontend, docs, openspec) en `develop`
- [x] 6.4 Hacer `git push origin develop` con todos los cambios estabilizados
- [x] 6.5 Integrar `develop` en `main` con merge controlado y ejecutar verificacion post-merge
- [x] 6.6 Hacer `git push origin main` y dejar constancia de release productivo
