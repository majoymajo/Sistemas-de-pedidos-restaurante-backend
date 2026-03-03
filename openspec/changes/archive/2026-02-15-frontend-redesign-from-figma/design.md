## Context

El proyecto ya tiene un frontend React + Vite con rutas de cliente y cocina, contratos API definidos y soporte mock/API real por bandera de entorno. El diseno exportado en `figma-front` aporta una experiencia visual superior, pero viene con stack y supuestos distintos (Tailwind v4, estado mock local, rutas simplificadas), por lo que requiere una integracion controlada para no romper flujos actuales ni contratos de backend.

## Goals / Non-Goals

**Goals:**
- Adoptar lenguaje visual y componentes del diseno Figma para cliente y cocina.
- Mantener compatibilidad con rutas actuales (`/client/*`, `/kitchen/*`) y contratos API existentes.
- Incorporar estados de mesa ocupada/vacia en seleccion de mesa.
- Mantener funcionalidades actuales: tabs de menu, cantidades, notas, confirmacion con order ID, estado de pedido, y tablero de cocina con cambios de estado y refresco frecuente.
- Preservar paridad funcional entre modo mock y modo API real.

**Non-Goals:**
- No redisenar contratos backend ni reglas de negocio de pedidos/estados.
- No introducir autenticacion segura de cocina a nivel servidor en este cambio (PIN solo puerta de acceso UI).
- No rehacer CI/CD ni infraestructura de despliegue.

## Decisions

- **Adopcion visual incremental en el frontend actual**: se reutiliza el repo actual como base funcional y se incorpora diseno/composicion de `figma-front` por pantallas y componentes.
  - Alternativa: copiar por completo `figma-front` dentro del proyecto.
  - Motivo de descarte: alto riesgo de regresion funcional por diferencias de rutas, estado y contratos API.

- **Compatibilidad con rutas existentes como contrato de navegacion**: se conserva el routing actual y se adapta la UI Figma a esas rutas.
  - Alternativa: migrar a esquema de rutas del export Figma.
  - Motivo de descarte: rompe bookmarks, pruebas existentes y acoplamientos actuales.

- **Capa de integracion UI->API sobre contratos existentes**: los componentes redisenados consumen `src/api/*` y `src/store/*` en lugar de usar estado mock embebido del export.
  - Alternativa: mantener contexto local mock como fuente principal.
  - Motivo de descarte: duplicacion de logica y divergencia entre mock y API real.

- **Modelo de ocupacion de mesa derivado desde datos disponibles**: ocupada/vacia se calcula desde pedidos activos (`PENDING`, `IN_PREPARATION`) y se proyecta en la grilla de mesas.
  - Alternativa: endpoint dedicado de mesas.
  - Motivo de descarte inicial: no existe en el alcance actual; se prioriza solucion frontend con datos existentes.

- **PIN de cocina como guard de frontend**: acceso a tablero protegido por PIN configurable en frontend para este alcance.
  - Alternativa: autenticacion backend completa.
  - Motivo de descarte: fuera de alcance funcional del rediseno UI.

- **Refresco periodico estable para cocina y estado de pedido**: mantener polling con intervalo fijo para reflejar cambios de pedidos y transiciones.
  - Alternativa: WebSockets/SSE.
  - Motivo de descarte: implica cambios backend no incluidos en el objetivo actual.

## Risks / Trade-offs

- [Riesgo] Diferencias entre export Figma y arquitectura actual generan deuda de adaptacion de componentes. -> Mitigacion: migracion por capas (tokens, primitives, paginas) y validacion funcional por flujo.
- [Riesgo] Ocupacion de mesas derivada desde pedidos puede no cubrir casos de negocio especiales. -> Mitigacion: documentar regla de ocupacion y dejar extension futura a endpoint dedicado.
- [Riesgo] Agregar demasiadas dependencias UI aumenta bundle y complejidad. -> Mitigacion: incorporar solo componentes necesarios para el alcance y podar imports no usados.
- [Trade-off] PIN de cocina en frontend mejora UX pero no aporta seguridad real. -> Mitigacion: tratarlo como control de acceso visual temporal y registrar hardening futuro.

## Migration Plan

1. Introducir base visual (tokens, estilos y primitives) compatible con el frontend existente.
2. Migrar flujo cliente completo con paridad funcional (mesa, menu, carrito, confirmacion, estado).
3. Migrar flujo cocina (login PIN + board + transiciones + resumen).
4. Verificar paridad en modo mock y modo API real para escenarios criticos.
5. Ejecutar smoke checks de rutas principales y corregir regresiones de usabilidad.

## Open Questions

- Confirmar si la regla de mesa ocupada debe incluir estado `READY` como ocupada o solo `PENDING/IN_PREPARATION`.
- Definir si el PIN de cocina sera valor fijo en frontend o configurable por entorno.
- Confirmar si se mantiene Tailwind v3 con compatibilidad de componentes o se migra a Tailwind v4 en un cambio separado.
