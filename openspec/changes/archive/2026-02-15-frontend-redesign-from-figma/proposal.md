## Why

El frontend actual cumple la operacion base, pero no refleja la experiencia visual y de usabilidad deseada para un producto de cara a usuarios finales. Necesitamos adoptar el diseno validado en `figma-front` para mejorar claridad, consistencia y conversion del flujo cliente/cocina, manteniendo compatibilidad con las APIs reales y las rutas actuales del proyecto.

## What Changes

- Redisenar el flujo cliente (mesa -> menu -> carrito -> confirmacion -> estado) usando componentes estilo shadcn/tailwind y el lenguaje visual aprobado en Figma.
- Mostrar estado de mesas en seleccion de mesa (ocupada/vacia) con feedback visual claro (ocupada en rojo, vacia en verde).
- Mantener categorias en pestañas para menu y mejorar controles de cantidad por producto.
- Conservar soporte de notas adicionales por item o pedido antes de confirmar.
- Mantener confirmacion de pedido con visual fuerte del `orderId`.
- Mantener pantalla de seguimiento de estado del pedido para cliente.
- Redisenar acceso de cocina con PIN y mantener tablero de cocina con resumen de pedidos.
- Mantener actualizacion frecuente del tablero de cocina y cambios de estado de pedido desde UI.
- Alinear tokens visuales, componentes y layout al diseno de `figma-front`, sin romper contratos funcionales existentes.

## Capabilities

### New Capabilities
- `frontend-redesign`: Define los requisitos funcionales y de experiencia de usuario del nuevo frontend basado en Figma para los flujos de cliente y cocina, sobre integracion API real.

### Modified Capabilities
- `frontend-mockdata`: Ajustar requerimientos para que el flujo redisenado conserve paridad funcional tanto en modo mock como en modo API real (sin cambiar contratos de pedido/estado).

## Impact

- Frontend React/Vite: rutas, layouts, paginas y componentes UI en `src/`.
- Estilos y design tokens (Tailwind + componentes reutilizables estilo shadcn).
- Integracion con capas API existentes (`src/api/*`) y estados de pedido de cocina.
- Posibles ajustes de dependencias frontend para soporte de componentes y tema.
