## Context

El frontend actual en `feature/front` consume endpoints reales y usa estados de pedido distintos al backend (SUBMITTED, READY, DELIVERED, CANCELED). El objetivo es poder ejecutar el UI localmente sin backend, con datos en memoria y contratos alineados a los estados del backend (PENDING, IN_PREPARATION, READY). No se requiere persistencia ni autenticación real; se prioriza un flujo demo determinista.

## Goals / Non-Goals

**Goals:**
- Desacoplar la UI de endpoints reales y reemplazarla por un mock in-memory.
- Alinear tipos, estados y transiciones con el backend Java (id UUID, estados PENDING/IN_PREPARATION/READY).
- Mantener intacta la experiencia de usuario (cliente y cocina) con acciones manuales de cambio de estado.
- Simplificar el acceso a cocina para el mock (sin PIN).

**Non-Goals:**
- No integrar el frontend con el backend real en esta etapa.
- No agregar persistencia entre recargas (localStorage solo para carrito si ya existe).
- No implementar autenticación ni seguridad real.
- No modificar el backend.

## Decisions

- **Mock en memoria dentro del layer API**: mover las funciones `getMenu`, `createOrder`, `getOrder`, `listOrders`, `patchOrderStatus` a un módulo mock que opere sobre arrays locales. Alternativa: mocking a nivel de UI; se descarta para mantener el contrato de API estable.
- **Estados alineados al backend**: usar `PENDING`, `IN_PREPARATION`, `READY` y actualizar etiquetas/acciones. Alternativa: mapear estados antiguos; se descarta para evitar ambigüedad y asegurar compatibilidad futura.
- **Acceso directo a cocina**: omitir validación de PIN en mock. Alternativa: conservar PIN local; se descarta por simplicidad y menos fricción.

## Risks / Trade-offs

- [Riesgo] Diferencias futuras entre mock y backend real ? Mitigación: mantener contratos/types alineados al backend y documentarlo en README.
- [Riesgo] Flujo de cocina muy simplificado ? Mitigación: limitar cambios a estados mínimos requeridos por backend.
- [Trade-off] Se pierde validación/errores reales ? Mitigación: agregar errores simulados solo si se requieren pruebas específicas.
