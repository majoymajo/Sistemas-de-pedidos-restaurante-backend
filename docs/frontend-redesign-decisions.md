# Frontend Redesign Decisions

Fecha: 2026-02-15
Cambio OpenSpec: `frontend-redesign-from-figma`

## 1) PIN de cocina

- Decision: mantener PIN en frontend como control de acceso visual.
- Fuente del valor: `VITE_KITCHEN_PIN`.
- Valor por defecto: `cocina123`.
- Nota: esto no reemplaza autenticacion de backend.

## 2) Regla de ocupacion de mesas

- Decision: una mesa se considera ocupada si tiene pedidos en estado:
  - `PENDING`
  - `IN_PREPARATION`
- Razon: evita bloquear mesas con pedidos ya listos (`READY`) que pueden estar finalizando entrega.

## 3) Estrategia Tailwind

- Decision: mantener Tailwind v3 para compatibilidad con el frontend actual.
- Implementacion: aplicar tokens visuales con CSS variables y utilidades compatibles con v3.
- Razon: reduce riesgo de regresion y evita migracion de build tooling en el mismo cambio funcional.

