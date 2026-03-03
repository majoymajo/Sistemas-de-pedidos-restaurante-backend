## Context

La vista de Cocina (`KitchenBoardPage`) hace polling cada 3s. Actualmente cada ciclo vuelve a poner `loading=true`, lo que dispara el componente `<Loading />` y provoca parpadeo: se ven pedidos por menos de un segundo y luego desaparecen. Esto da la percepcion de loop infinito y bloquea el flujo de cocina.

Restricciones:
- No cambiar el contrato del backend ni endpoints.
- Mantener polling en Cocina (requerido para ver pedidos en tiempo real).
- Soportar tanto modo mock como API real.

## Goals / Non-Goals

**Goals:**
- Mostrar loading solo en la carga inicial y al cambiar filtros.
- Mantener los pedidos visibles durante los refreshes.
- Evitar requests superpuestos (no solapar polls).
- Manejar errores de polling sin limpiar la UI.

**Non-Goals:**
- Cambiar endpoints, contratos o backend.
- Cambiar el flujo del cliente o la logica de estados del pedido.

## Decisions

- **Separar estado de carga inicial vs. refresco**: usar `isInitialLoading` para la primera carga y un `isRefreshing` opcional para el polling. El UI solo bloquea la vista con `Loading` en la carga inicial.
- **Mantener ultimo resultado exitoso**: el polling actualiza `orders` solo cuando hay respuesta valida; si hay error, conservar el estado anterior y mostrar un mensaje no bloqueante.
- **Evitar superposicion de polling**: usar un guard `inFlight` o un `AbortController` para no disparar otro fetch si el anterior sigue en curso. Alternativa preferida: `setTimeout` encadenado al terminar cada fetch (evita drift y overlap).
- **Cambio de filtros**: al cambiar filtros se considera una "nueva carga inicial" (limpia error, activa loading inicial, y dispara fetch inmediato), luego vuelve al polling.

## Risks / Trade-offs

- [Riesgo] El usuario puede ver pedidos "antiguos" si hay error de red. → Mitigacion: mostrar banner de error con opcion de reintentar.
- [Riesgo] Menor frecuencia de refresh si el request tarda. → Mitigacion: usar `setTimeout` post-respuesta para mantener polling estable sin solapamientos.

## Migration Plan

1. Ajustar `KitchenBoardPage` para separar carga inicial y refresh.
2. Implementar guard de polling (evitar overlap).
3. Validar que la lista no desaparece durante refresh.
4. Verificar flujo Cocina con API real y mock.

## Open Questions

- ¿Agregar indicador sutil de “Actualizando...” durante el polling (sin bloquear UI)?
