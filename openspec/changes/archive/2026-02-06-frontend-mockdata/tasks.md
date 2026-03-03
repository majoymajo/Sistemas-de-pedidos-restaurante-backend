## 1. Contract Alignment

- [x] 1.1 Actualizar tipos de dominio y estados a PENDING/IN_PREPARATION/READY
- [x] 1.2 Ajustar etiquetas y transiciones de estado en UI de cocina y cliente

## 2. Mock API In-Memory

- [x] 2.1 Crear store mock en memoria con productos y pedidos iniciales
- [x] 2.2 Reemplazar getMenu/createOrder/getOrder/listOrders/patchOrderStatus para usar mock
- [x] 2.3 Desconectar http real y asegurar que no se hagan llamadas externas

## 3. UI Ajustes y Documentacion

- [x] 3.1 Simplificar acceso a cocina (sin PIN) en modo mock
- [x] 3.2 Verificar flujo cliente end-to-end con mock (mesa->menu->carrito->confirmacion->estado)
- [x] 3.3 Actualizar README/.env con instrucciones de modo mock
