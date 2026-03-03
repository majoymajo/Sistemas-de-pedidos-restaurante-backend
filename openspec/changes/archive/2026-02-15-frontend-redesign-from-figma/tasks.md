## 1. UI Foundation

- [x] 1.1 Definir y aplicar tokens visuales base (colores, radios, tipografia, estados) para alinear el frontend al estilo de `figma-front`.
- [x] 1.2 Incorporar componentes UI reutilizables necesarios (button, card, input, tabs, badge, toast, dialog/sheet) sin agregar librerias no usadas.
- [x] 1.3 Adaptar layout y navegacion global para mantener rutas actuales (`/client/*`, `/kitchen/*`) con nueva capa visual.

## 2. Client Flow Redesign

- [x] 2.1 Redisenar pantalla de seleccion de mesa mostrando estado ocupada/vacia con codificacion visual (rojo/verde).
- [x] 2.2 Implementar regla de ocupacion de mesas usando pedidos activos para poblar el estado visual.
- [x] 2.3 Redisenar pantalla de menu con pestañas por categoria y controles de cantidad por item.
- [x] 2.4 Redisenar carrito preservando notas por item/pedido y recálculo correcto de totales.
- [x] 2.5 Redisenar confirmacion para mostrar `orderId` retornado y acceso directo al tracking.
- [x] 2.6 Redisenar pantalla de estado de pedido manteniendo consulta por ID y refresco periodico.

## 3. Kitchen Flow Redesign

- [x] 3.1 Implementar pantalla de acceso a cocina con PIN y feedback de validacion.
- [x] 3.2 Redisenar tablero de cocina con columnas por estado y resumen visible de cada pedido.
- [x] 3.3 Mantener/ajustar polling de cocina para reflejar nuevos pedidos y cambios de estado en tiempo acotado.
- [x] 3.4 Integrar transiciones de estado desde UI de cocina sin romper contrato API (`PENDING -> IN_PREPARATION -> READY`).

## 4. Data Integration and Compatibility

- [x] 4.1 Adaptar componentes redisenados para consumir `src/api/*` y `src/store/*` existentes en modo API real.
- [x] 4.2 Actualizar soporte mock para paridad funcional del flujo redisenado (ocupacion, notas, tabs, resumen de cocina).
- [x] 4.3 Normalizar mapeo de estados y textos UI entre contratos backend (ENGLISH enum) y etiquetas visibles en espanol.

## 5. Validation and Hardening

- [x] 5.1 Ejecutar smoke tests funcionales del flujo cliente completo (mesa -> menu -> carrito -> confirmacion -> estado).
- [x] 5.2 Ejecutar smoke tests del flujo cocina (PIN -> tablero -> cambio de estados -> reflejo en cliente/kitchen).
- [x] 5.3 Verificar responsividad (mobile cliente, desktop cocina) y corregir regresiones visuales clave.
- [x] 5.4 Documentar decisiones finales de PIN, regla de ocupacion y estrategia Tailwind (v3 compat vs migracion separada).
