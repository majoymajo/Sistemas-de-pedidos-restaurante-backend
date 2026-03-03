## Context

El repositorio contiene un rediseño frontend amplio y una base backend operativa, pero el comportamiento visible depende de mock data para exponer el catalogo expandido. Esto genera divergencia entre demostracion local y estado real de produccion. Tambien existe dispersion documental en `docs/` (auditoria, calidad, deuda tecnica, refactor), con redundancias y multiples puntos de entrada.

Restricciones clave:
- El flujo funcional actual debe mantenerse (cliente y cocina).
- `main` debe quedar en modo API real por defecto para considerarse produccion.
- La consolidacion documental no debe mezclar dominios incompatibles (auditoria vs calidad vs deuda tecnica).
- El cambio debe quedar trazable mediante OpenSpec y con estrategia de merge controlada `develop -> main`.

Stakeholders:
- Equipo del reto tecnico (entregables de auditoria, calidad, deuda tecnica).
- Evaluadores de Sofka U.
- Equipo de desarrollo que mantendra el proyecto posterior al reto.

## Goals / Non-Goals

**Goals:**
- Dejar `main` ejecutable con Docker Compose en modo real (`VITE_USE_MOCK=false`) y endpoints funcionales.
- Alinear el menu servido por backend con la experiencia frontend redisenada (catalogo expandido con metadata visual).
- Definir una ruta de release reproducible: pull, verificacion, commit, push, merge a `main`.
- Consolidar Markdown por dominio para reducir ruido y mantener trazabilidad.
- Mejorar `README.md` como entrada principal del repo, incluyendo Mermaid para arquitectura/flujo.

**Non-Goals:**
- Reescribir toda la arquitectura backend o introducir nuevos microservicios.
- Reemplazar OpenSpec por otra metodologia de gestion de cambios.
- Eliminar totalmente el modo mock (se conserva para desarrollo local y fallback controlado).
- Cambiar el alcance funcional del producto mas alla de lo ya definido en `frontend-redesign`.

## Decisions

### Decision 1: Produccion en `main` usa API real por defecto

Se establece que los artefactos de despliegue para `main` deben configurar frontend en modo real (`VITE_USE_MOCK=false`) y consumir `order-service`.

Rationale:
- Evita demos inconsistentes entre entornos.
- Obliga a mantener contrato backend/frontend en vez de ocultar incompatibilidades con mock.

Alternativas consideradas:
- Mantener mock por defecto y activar real manualmente: descartado por riesgo de presentar estado no productivo.
- Eliminar mock completamente: descartado porque reduce velocidad de desarrollo y pruebas aisladas.

### Decision 2: Expandir catalogo desde backend como fuente canonica

El menu completo (items, precio, categoria, imagen) se servira desde backend para `main`. El frontend solo renderiza y valida.

Rationale:
- Centraliza datos de negocio en una sola fuente.
- Evita fallback silencioso a datasets divergentes.

Alternativas consideradas:
- Mantener catalogo grande solo en mock frontend: descartado por deuda de integracion.
- Consumir catalogo estatico embebido en frontend: descartado por duplicacion y falta de trazabilidad.

### Decision 3: Mock queda restringido a desarrollo/fallback explicitamente trazable

Se mantiene mock data solo para modo desarrollo local o contingencia definida, con bandera explicita y documentada.

Rationale:
- Preserva productividad local.
- Reduce impacto cuando backend no esta disponible en pruebas puntuales.

Alternativas consideradas:
- Fallback automatico siempre activo: descartado porque oculta fallas reales de backend.

### Decision 4: Consolidacion documental por dominio

Se define una estructura objetivo:
- `docs/auditoria/` como fuente unica de auditoria (hallazgos + evidencias + alcance).
- `docs/quality/` como fuente unica de calidad/pruebas.
- `docs/quality/DEUDA_TECNICA.md` como registro consolidado de deuda tecnica.
- Material extra repetido se fusiona y se elimina, manteniendo referencias historicas necesarias.

Rationale:
- Mejora navegabilidad y reduce inconsistencias.
- Mantiene separacion semantica exigida por el reto.

Alternativas consideradas:
- Fusionar todo en un unico markdown: descartado por perdida de claridad entre dominios.
- Mantener todos los md actuales: descartado por ruido y mantenimiento alto.

### Decision 5: README canonico con Mermaid y mapa de evidencia

`README.md` se convierte en punto de entrada unico con:
- arquitectura (Mermaid),
- flujo funcional cliente/cocina,
- quickstart local y docker,
- tabla de documentos de auditoria/calidad/deuda tecnica.

Rationale:
- Acelera onboarding del evaluador y del equipo.
- Reduce dependencia de guias dispersas.

Alternativas consideradas:
- Mantener README minimo y enlazar todo sin estructura: descartado por baja usabilidad.

## Risks / Trade-offs

- [Catalogo backend y frontend desalineados] -> Definir pruebas de contrato para `/menu` y smoke tests de UI en modo real.
- [Merge a `main` con cambios no verificados] -> Checklist pre-merge obligatorio (build, compose, endpoints, flujo cliente/cocina).
- [Consolidacion documental elimina contexto util] -> Fusionar contenido, no resumir ciegamente; conservar seccion de historial y referencias cruzadas.
- [Dependencia de imagenes remotas inestables] -> Priorizar URLs verificadas y definir fallback visual en frontend.
- [Falsa sensacion de salud por fallback mock] -> Documentar y limitar fallback, no usarlo en configuracion por defecto de `main`.

## Migration Plan

1. Sincronizar `develop` con remoto (`git pull --rebase origin develop`) y estabilizar cambios pendientes.
2. Implementar ajustes backend/frontend/documentacion definidos en esta iniciativa.
3. Ejecutar validaciones:
   - `npm run build`
   - `docker compose -f infrastructure/docker/docker-compose.yml up -d --build`
   - smoke tests de `/menu`, `/orders`, `/orders/{id}`, `/orders/{id}/status`.
4. Consolidar commits en `develop` con mensajes por dominio (frontend/backend/docs).
5. Push a `develop`.
6. Integrar a `main` mediante PR o merge controlado, ejecutar validacion post-merge en `main`.
7. Push final a `main` y registrar evidencia de version productiva.

Rollback:
- Si falla validacion en `main`, revertir merge commit y restaurar configuracion previa documentada.

## Open Questions

- Se persistira el catalogo expandido via seed SQL/Flyway o via capa de configuracion temporal en backend?
- Se desea exponer endpoint de salud estandar (`/actuator/health`) en entrega final para monitoreo?
- Se requiere versionado formal de docs consolidados (ejemplo changelog por fase) o solo estado final limpio?
