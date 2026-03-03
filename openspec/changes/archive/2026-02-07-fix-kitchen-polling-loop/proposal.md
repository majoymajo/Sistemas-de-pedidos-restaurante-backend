## Why

La pantalla de Cocina entra en un loop de carga: muestra "Cargando pedidos...", luego aparecen por menos de un segundo y desaparecen. Esto impide usar la vista de cocina y bloquea QA/integracion con el backend.

## What Changes

- Definir comportamiento estable de polling para la pantalla de Cocina (sin parpadeo ni re-render de carga en cada refresco).
- Mantener la lista de pedidos visible mientras se hace refresh; usar estado de "loading" solo en la carga inicial.
- Manejar respuestas vacias o errores sin limpiar la UI; mostrar estado de error con opcion de reintento.
- Alinear el manejo del fetch de `orders?status=...` para evitar ciclos de render excesivos.

## Capabilities

### New Capabilities
- `kitchen-board-stable-polling`: La vista de Cocina mantiene pedidos visibles y refresca periodicamente sin entrar en loop de carga ni limpiar el estado en cada polling.

### Modified Capabilities
<!-- None -->

## Impact

- Frontend: `KitchenBoardPage` y capa de datos de pedidos (polling/refetch).
- Posibles ajustes en manejo de estado y mensajes de error/empty.
