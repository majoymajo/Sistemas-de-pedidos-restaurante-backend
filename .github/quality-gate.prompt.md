# Quality Gate — Sistema de Pedidos de Restaurante

## Propósito

Este prompt actúa como **quality gate obligatorio** antes de hacer merge a `develop` o promover código a `main`.
Debe ejecutarse como checklist de validación manual o delegarse a un agente de IA para verificación automatizada.

> ⚠️ Contexto: proyecto brownfield con arquitectura event-driven.
> Cualquier cambio debe preservar contratos existentes y no romper funcionalidad legacy.

---

## 1. Compilación y Build

### 1.1 Frontend
- [ ] `npm run build` finaliza sin errores de compilación
- [ ] No hay warnings de TypeScript strict (`tsconfig.json` con `strict: true`)
- [ ] No se introducen dependencias nuevas sin justificación documentada

### 1.2 Backend — order-service
- [ ] `mvn -pl order-service test` → `BUILD SUCCESS`, 0 failures, 0 errors
- [ ] `mvn -pl order-service compile` sin warnings críticos

### 1.3 Backend — kitchen-worker
- [ ] `mvn -pl kitchen-worker test` → `BUILD SUCCESS`, 0 failures, 0 errors
- [ ] `mvn -pl kitchen-worker compile` sin warnings críticos

### 1.4 Backend — report-service
- [ ] `mvn -pl report-service test` → `BUILD SUCCESS`, 0 failures, 0 errors

---

## 2. Arquitectura (NO NEGOCIABLE)

Estos puntos son invariantes del sistema. Cualquier violación es **bloqueante**.

### 2.1 Comunicación entre servicios
- [ ] `kitchen-worker` NO expone endpoints HTTP
- [ ] `order-service` NO hace llamadas REST a `kitchen-worker`
- [ ] La única integración entre `order-service` y `kitchen-worker` es vía **RabbitMQ**
- [ ] No se comparten bases de datos entre servicios (`restaurant_db` ≠ `kitchen_db`)

### 2.2 Capas backend (orden estricto)
- [ ] Controllers contienen SOLO lógica HTTP (mapeo request/response)
- [ ] Lógica de negocio reside en `application/` o `domain/`
- [ ] No hay acceso directo a repositorios desde controllers
- [ ] Código AMQP NO aparece en clases del paquete `domain/`

### 2.3 Frontend
- [ ] Reglas de negocio están en `domain/`, NO en componentes UI
- [ ] Transiciones de estado se gestionan exclusivamente vía `orderStatus.ts`
- [ ] Contratos HTTP definidos en `contracts.ts`, sin duplicación

---

## 3. Contratos y Eventos (BREAKING CHANGE = BLOQUEANTE)

### 3.1 Evento `order.placed`
- [ ] `eventVersion` es estrictamente `1`
- [ ] Cualquier evento con `eventVersion ≠ 1` se envía a DLQ sin reintentos
- [ ] El payload del evento NO ha cambiado su estructura sin migración documentada

### 3.2 REST API
- [ ] No se han eliminado o renombrado endpoints existentes
- [ ] No se han cambiado tipos de campos en DTOs públicos
- [ ] Nuevos endpoints están documentados (OpenAPI / Swagger)

### 3.3 Base de datos
- [ ] Cambios de schema se gestionan con Flyway migrations
- [ ] No hay ALTER destructivos sin estrategia de rollback
- [ ] Migraciones son idempotentes y testeadas con H2

---

## 4. Reglas de Negocio (PRESERVACIÓN OBLIGATORIA)

| Regla | Validación | Ubicación canónica |
|---|---|---|
| `tableId` entre 1 y 12 | Backend rechaza valores fuera de rango | `OrderValidator.java` |
| Mínimo 1 item por pedido | Backend rechaza pedidos vacíos | `OrderValidator.java` |
| `productId` debe existir y estar activo | Validación contra `Product.is_active` | `OrderValidator.java` |
| Pedido nuevo siempre en `PENDING` | NUNCA se crea en otro estado | `OrderService.java` |
| Transiciones: `PENDING → IN_PREPARATION → READY` | Frontend en `orderStatus.ts`, backend debe tener guards | Ambos lados |
| `kitchen-worker` idempotente | Upsert si el pedido no existe | `OrderProcessingService.java` |

- [ ] Todas las reglas anteriores tienen al menos un test que las valida
- [ ] No se ha relajado ninguna validación existente

---

## 5. Seguridad

### 5.1 Secretos y tokens
- [ ] No hay secretos hardcodeados en código fuente
- [ ] No hay secretos en variables `VITE_*` expuestas al frontend
- [ ] Tokens de cocina se validan server-side (header `X-Kitchen-Token`)
- [ ] Configuración sensible se gestiona por variables de entorno

### 5.2 Autorización
- [ ] Endpoints de cocina requieren `X-Kitchen-Token` válido
- [ ] Operaciones destructivas (DELETE) tienen protección adicional (soft-delete, confirmación, audit log)
- [ ] El frontend NO es la única capa de autorización para ninguna operación crítica

### 5.3 Validación
- [ ] Toda validación crítica se ejecuta en backend (no solo frontend)
- [ ] Inputs de usuario están sanitizados antes de persistencia
- [ ] Errores de validación retornan códigos HTTP apropiados (400, 401, 403)

---

## 6. Testing (TDD OBLIGATORIO)

### 6.1 Proceso
- [ ] Tests escritos ANTES del código de producción (evidencia RED → GREEN → REFACTOR)
- [ ] No existe código de producción sin test correspondiente
- [ ] Commit history refleja ciclo TDD

### 6.2 Técnicas de diseño de test aplicadas
- [ ] Equivalence Partitioning para inputs con rangos
- [ ] Boundary Value Analysis para límites (tableId 0, 1, 12, 13)
- [ ] Decision Tables para lógica condicional compleja
- [ ] Property-based testing (jqwik) donde aplique

### 6.3 Cobertura por capa

| Capa | Framework | Requisito mínimo |
|---|---|---|
| Backend unit | JUnit 5 + Mockito | Validadores, servicios, domain |
| Backend integration | H2 + MockMvc + spring-rabbit-test | Controllers, AMQP listeners |
| Frontend domain | Vitest | Reglas de negocio, state transitions |
| Frontend hooks | Vitest + React Testing Library | Controllers/hooks con mocks |

- [ ] Tests unitarios aislados (sin estado compartido entre tests)
- [ ] `@BeforeEach` / `beforeEach` limpia estado correctamente
- [ ] Tests de integración usan infraestructura in-memory (H2, mocks)

---

## 7. Infraestructura y Deploy

### 7.1 Docker
- [ ] `docker compose -f infrastructure/docker/docker-compose.yml up -d --build` levanta todos los servicios
- [ ] Todos los servicios reportan estado `Up` / `healthy`:
  - `restaurant-frontend`
  - `restaurant-order-service`
  - `restaurant-kitchen-worker`
  - `restaurant-postgres` (healthy)
  - `kitchen-postgres` (healthy)
  - `restaurant-rabbitmq` (healthy)

### 7.2 Smoke tests post-deploy
- [ ] `GET /menu` retorna catálogo activo
- [ ] `POST /orders` crea pedido en estado `PENDING`
- [ ] `GET /orders/{id}` retorna pedido creado
- [ ] `PATCH /orders/{id}/status` con `X-Kitchen-Token` cambia estado correctamente
- [ ] Logs de `kitchen-worker` confirman recepción y procesamiento de `order.placed`

---

## 8. Deuda Técnica

- [ ] No se introduce deuda técnica nueva sin registro en `docs/quality/DEUDA_TECNICA.md`
- [ ] Items nuevos incluyen: ID, clasificación Fowler, impacto, prioridad, owner, trigger
- [ ] Deuda existente marcada como `EN_PROGRESO` no ha empeorado
- [ ] No se han reabierto items previamente marcados como `PAGADA`

---

## 9. Documentación

- [ ] Cambios de arquitectura documentados en `docs/HANDOVER_REPORT.md`
- [ ] Decisiones técnicas registradas con justificación
- [ ] Supuestos explícitos marcados como preguntas abiertas si no se han validado
- [ ] Si se modificaron endpoints, actualizar `docs/GUIA_ENDPOINTS_Y_DB.md`
- [ ] Trazabilidad cruzada: hallazgos de auditoría → deuda técnica → evidencia de calidad

---

## 10. Criterio de Aprobación

### Para merge a `develop`
Todos los checks de las secciones **1–6** y **8–9** deben estar marcados ✅.
Sección 7 es recomendada pero no bloqueante en desarrollo.

### Para promoción a `main`
**TODOS** los checks de **TODAS** las secciones (1–9) deben estar marcados ✅.
El stack docker debe estar verificado end-to-end con smoke tests.

### Bloqueantes absolutos (cualquiera de estos impide el merge)
- ❌ Test en rojo
- ❌ Violación de arquitectura (sección 2)
- ❌ Cambio de contrato sin migración (sección 3)
- ❌ Secreto expuesto (sección 5)
- ❌ Regla de negocio eliminada o relajada sin justificación (sección 4)
- ❌ Código de producción sin test (sección 6)

---

## Uso

### Como checklist manual
Copiar este archivo y marcar cada checkbox antes de crear el PR.

### Como prompt para agente de IA
```
Actúa como quality gate reviewer del proyecto Sistemas-de-pedidos-restaurante.
Evalúa los cambios del PR actual contra TODAS las secciones del quality gate
definido en .github/quality-gate.prompt.md.

Para cada sección, indica:
- ✅ PASA: si el criterio se cumple con evidencia
- ❌ FALLA: si hay una violación, indicando archivo y línea
- ⚠️ NO APLICA: si el cambio no afecta esa sección

Al final, emite un veredicto:
- APPROVED: todos los checks aplicables pasan
- CHANGES REQUESTED: hay al menos un ❌
- NEEDS REVIEW: hay checks que no se pueden verificar automáticamente

Adjunta evidencia técnica para cada decisión.
```

---

## Trazabilidad

| Documento | Relación |
|---|---|
| `docs/quality/CALIDAD.md` | Evidencia de ejecución de quality gates anteriores |
| `docs/quality/DEUDA_TECNICA.md` | Registro de deuda técnica activa |
| `docs/auditoria/AUDITORIA.md` | Hallazgos y remediaciones de auditoría |
| `.github/copilot-instructions.md` | Reglas de proyecto para Copilot |
| `docs/HANDOVER_REPORT.md` | Contexto de arquitectura y decisiones |
