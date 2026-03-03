# Plan de Pruebas - Funcionalidad de Reportes

## Introducción

Este documento reconstruye la estrategia y diseño de pruebas aplicado al módulo de reportes del sistema de pedidos de restaurante, basándose en el análisis de los casos de prueba existentes en el backend (`report-service`) y frontend (páginas y hooks de reportes).

El módulo de reportes proporciona:
- Agregación de datos de órdenes completadas (estado READY)
- Generación de reportes por rango de fechas
- Cálculo de ingresos totales y desglose por producto
- Interfaz de usuario para visualización de órdenes y reportes

Scope de pruebas:
- Backend: `report-service` (controlador, servicios, repositorio, validación de fechas)
- Frontend: Página de reportes, hooks de controlador, y fachada de integración con API

---

## Principios de Pruebas Aplicados

### 1. Principio de Independencia

**Descripción:** Cada caso de prueba es completamente independiente y no depende del resultado de otras pruebas.

**Evidencia en el código:**
- En `DateRangeFilterTest.java`: Cada test crea su propio rango de fechas y valida un único aspecto del comportamiento
- En `useOrdersReportController.test.ts`: Se utiliza `beforeEach()` con `vi.clearAllMocks()` para aislar cada prueba
- En `ReportAggregationServiceTest.java`: Cada test instancia un nuevo servicio en `setUp()` con estado limpio

**Por qué aplica:** El sistema está diseñado de modo que las pruebas pueden ejecutarse en cualquier orden y el resultado es determinista. No hay estado compartido entre pruebas.

### 2. Principio de Cobertura de Casos Normales y Anormales (Error Guessing)

**Descripción:** Las pruebas validan tanto el comportamiento esperado como los caminos de error.

**Evidencia en el código:**
- Casos normales: `shouldReturnValidReportForValidDateRange()`, `shouldCloadOrdersExitosamente()`
- Casos anormales: `shouldReturn400ForInvalidDateFormat()`, `maneja errores correctamente()`
- Condiciones de frontera: `shouldReturnZeroMetricsWhenNoData()`, `estado empty cuando no hay orders`

**Por qué aplica:** Los tests cubren tanto rutas de éxito como excepciones, validando que el sistema responda correctamente en condiciones adversas.

### 3. Principio de Automatización Completa (TDD Base)

**Descripción:** Existe evidencia de que las pruebas fueron diseñadas antes o en paralelo con la implementación.

**Evidencia en el código:**
- Los nombres de test describen el comportamiento esperado antes de existir la implementación (pattern: `Should<Behavior><Condition>`)
- La estructura es consistente: Arrange → Act → Assert
- Hay pruebas de contrato de API (`ReportControllerIntegrationTest`) que especifican el comportamiento del endpoint

**Por qué aplica:** El patrón de nombres y la cobertura sugieren un enfoque cercano a Test-Driven Development donde los casos se escribieron para especificar comportamiento antes de implementar.

---

## Niveles de Prueba

### Nivel 1: Pruebas Unitarias (Unit Tests)

**Definición:** Pruebas que validan componentes aislados (clases, métodos) sin dependencias externas reales.

**Componentes Probados:**

- **DateRangeFilter (Validación de fechas)**
  - Archivo test: `DateRangeFilterTest.java`
  - Casos validados: Validación de rango válido, rechazo de null, comparación startDate > endDate
  - Técnica: Equivalence partitioning + boundary value analysis

- **ReportAggregationService (Lógica de agregación)**
  - Archivo test: `ReportAggregationServiceTest.java`
  - Casos validados: Agregación de revenue, suma de cantidades por producto, filtrado de READY orders
  - Técnica: Equivalence partitioning (órdenes READY vs. no READY)

- **OrdersReportFacade (Integración con API)**
  - Archivo test: `OrdersReportFacade.test.ts`
  - Casos validados: Llamadas correctas a API, propagación de errores, manejo de arrays vacíos
  - Técnica: Mock-based unit testing

- **useOrdersReportController (Hook del cliente)**
  - Archivo test: `useOrdersReportController.test.ts`
  - Casos validados: Estados (loading, error, success), reload, API calls
  - Técnica: Hook testing con vitest + react testing library

- **useReportController (Hook de reportes por fecha)**
  - Archivo test: `useReportController.test.ts`
  - Casos validados: Estados asincronos, manejo de promesas, limpieza de errores previos
  - Técnica: Hook async testing

### Nivel 2: Pruebas de Integración (Integration Tests)

**Definición:** Pruebas que validan la integración entre componentes con dependencias reales (base de datos, API).

**Componentes Probados:**

- **ReportController + ReportService (Endpoint REST)**
  - Archivo test: `ReportControllerIntegrationTest.java`
  - Infraestructura: H2 in-memory database, MockMvc, Spring Test Context
  - Casos validados:
    - Generación de reporte con datos válidos
    - Validación de parámetros (formato, rango)
    - Comportamiento con datos ausentes
    - Filtrado de órdenes READY solo
  - Técnica: Integration testing con base de datos

- **OrderReportRepository (Acceso a datos)**
  - Archivo test: `OrderReportRepositoryIntegrationTest.java`
  - Infraestructura: @DataJpaTest, TestEntityManager, H2
  - Casos validados:
    - Persistencia de órdenes con items
    - Búsqueda por estado
    - Búsqueda por rango de fechas
  - Técnica: JPA repository testing

- **ReportService (Orquestación)**
  - Archivo test: `ReportServiceTest.java`
  - Infraestructura: Mockito para dependencias, testing puro de orquestación
  - Casos validados: Coordinación entre filtrador, repositorio y agregador
  - Técnica: Unit test con mocks (integración lógica sin DB)

### Nivel 3: Pruebas de Componente (Component Tests)

**Definición:** Pruebas de componentes React que validan rendering, estado y interacción del usuario.

**Componentes Probados:**

- **OrdersReportPage**
  - Archivo test: `OrdersReportPage.test.tsx`
  - Casos validados:
    - Rendering del estado de carga (Loading)
    - Rendering del estado de error con botón Retry
    - Rendering de tabla de órdenes
    - Interacción del usuario (click en Reload, navegación)
  - Técnica: Component rendering testing con vitest

### Nivel 4: Pruebas Basadas en Propiedades (Property-Based Tests)

**Definición:** Pruebas que validan invariantes matemáticos o lógicos usando generación aleatoria de datos.

**Componentes Probados:**

- **ReportAggregationService (Invariantes de agregación)**
  - Archivo test: `ReportAggregationPropertyTest.java`
  - Framework: jqwik
  - Invariantes validadas:
    - Total de ingresos = suma de (precio × cantidad) para todas las órdenes READY
    - Cantidad total de órdenes READY = conteo de órdenes con status READY
    - Cantidad de producto = suma de cantidades del producto en órdenes READY
    - Lista vacía siempre produce métricas cero
  - Estrategia: Generación de listas de órdenes con estados y productos variados

---

## Técnicas de Diseño de Casos de Prueba

### Técnica 1: Partición en Clases de Equivalencia

**Definición:** Dividir el dominio de entrada en clases donde se asume igual comportamiento del sistema.

**Aplicación en Reportes:**

#### DateRangeFilter (Dominio: rango de fechas)

| Clase de Equivalencia | Entrada Ejemplo | Comportamiento Esperado | Test |
|---|---|---|---|
| Rango válido (start ≤ end) | start=2026-02-01, end=2026-02-28 | Aceptado, crea DateRange | `shouldAcceptValidDateRange()` |
| Rango inválido (start > end) | start=2026-02-28, end=2026-02-01 | Lanzar InvalidDateRangeException | `shouldRejectInvalidDateRange()` |
| Null startDate | start=null, end=2026-02-28 | Lanzar InvalidDateRangeException | `shouldRejectNullStartDate()` |
| Null endDate | start=2026-02-01, end=null | Lanzar InvalidDateRangeException | `shouldRejectNullEndDate()` |
| Mismo día (start = end) | start=2026-02-15, end=2026-02-15 | Aceptado | `shouldAcceptSameDateRange()` |

#### ReportAggregationService (Dominio: lista de órdenes y sus estados)

| Clase de Equivalencia | Entrada Ejemplo | Comportamiento Esperado | Test |
|---|---|---|---|
| Órdenes READY | [READY, READY] | Ambas incluidas en agregación | `shouldAggregateRevenueFromMultipleReadyOrders()` |
| Órdenes NO-READY | [PENDING, IN_PREPARATION] | Excluidas de agregación | `shouldAggregateOnlyReadyOrders()` |
| Lista vacía | [] | Métricas = 0 | `shouldReturnZeroMetricsForEmptyList()` |
| Mix de estados | [READY, PENDING, IN_PREPARATION] | Solo READY contadas | `shouldAggregateOnlyReadyOrders()` |

#### ReportController (Dominio: parámetros HTTP de fecha)

| Clase de Equivalencia | Entrada | Respuesta HTTP | Test |
|---|---|---|---|
| Formato válido (ISO 8601) | "2026-02-01", "2026-02-28" | 200 OK + ReportResponseDTO | `shouldReturnValidReportForValidDateRange()` |
| Formato inválido | "invalid-date", "2026-02-28" | 400 Bad Request | `shouldReturn400ForInvalidDateFormat()` |
| Rango inválido | "2026-02-28", "2026-02-01" | 400 Bad Request | `shouldReturn400WhenStartDateAfterEndDate()` |
| Parámetros faltantes | (sin startDate o endDate) | 400 Bad Request | `shouldReturn400WhenParametersMissing()` |

#### Frontend Hooks (Dominio: respuesta del API)

| Clase de Equivalencia | Entrada | Estado Resultante | Test |
|---|---|---|---|
| Éxito (órdenes cargadas) | Promise resolved | initialLoading=false, orders=[], error="" | `carga orders exitosamente()` |
| Error de red | Promise rejected | initialLoading=false, error=msg, orders=[] | `maneja errores correctamente()` |
| Lista vacía | [] | initialLoading=false, orders=[], error="" | `estado empty cuando no hay orders` |

---

### Técnica 2: Análisis de Valores Límite (Boundary Value Analysis)

**Definición:** Seleccionar valores en los límites de las clases de equivalencia, pues ahí típicamente ocurren defectos.

**Aplicación en Reportes:**

#### DateRangeFilter (Límites temporales)

| Límite | Caso Límite | Caso Dentro de Límite | Caso Fuera de Límite | Test |
|---|---|---|---|---|
| Start boundary | timestamp = start.atStartOfDay() (00:00:00) | start.atTime(12,0) | start.minusDay().atTime(23,59) | `shouldIncludeStartBoundary()` |
| End boundary | timestamp = end.atTime(23,59,59) | end.atTime(12,0) | end.plusDay().atTime(0,0) | `shouldIncludeEndBoundary()` |
| Igualdad de fechas | start = end | (el rango contiene solo un día) | start > end | `shouldAcceptSameDateRange()` |

#### ReportAggregationService (Límites de agregación)

| Límite | caso Límite | Test |
|---|---|---|
| Revenue cero | Órdenes READY sin items con precio | Revenue = 0.00 (implícito en `shouldReturnZeroMetricsForEmptyList()`) |
| Cantidad mínima | 1 producto, 1 orden, 1 item, cantidad=1 | Implícito en cases unitarios |
| Cantidad máxima | Múltiples órdenes, múltiples productos | `shouldHandleOrdersWithMultipleItems()` |

#### ReportController (Límites de parámetros)

| Límite | Valor Límite | Valor Válido | Test |
|---|---|---|---|
| Primer día del año | "2026-01-01" | "2026-02-15" | Implícito en valid ranges |
| Último día del año | "2026-12-31" | "2026-02-15" | Implícito en valid ranges |
| Mismo día (start = end) | "2026-02-15" = "2026-02-15" | Incluido en reportes | Implícito en boundary logic |

---

### Técnica 3: Tablas de Decisión

**Definición:** Crear una tabla que captura combinaciones de condiciones y sus acciones resultantes.

**Aplicación: Generación de Reporte de Órdenes (ReportController)**

| Condición: Fechas Válidas | Condición: Órdenes Existen | Condición: Órdenes READY | Acción: Status HTTP | Acción: Métricas | Test |
|---|---|---|---|---|---|
| Sí | Sí | Sí | 200 OK | revenue > 0, readyCount > 0 | `shouldReturnValidReportForValidDateRange()` |
| Sí | Sí | No | 200 OK | revenue = 0, readyCount = 0 | `shouldOnlyIncludeReadyOrders()` |
| Sí | No | N/A | 200 OK | revenue = 0, readyCount = 0 | `shouldReturnZeroMetricsWhenNoData()` |
| No (start > end) | N/A | N/A | 400 Bad Request | Error message | `shouldReturn400WhenStartDateAfterEndDate()` |
| No (formato inválido) | N/A | N/A | 400 Bad Request | Error message | `shouldReturn400ForInvalidDateFormat()` |
| N/A (faltante) | N/A | N/A | 400 Bad Request | Error message | `shouldReturn400WhenParametersMissing()` |

**Aplicación: Estado del Hook useOrdersReportController**

| Evento | Estado Inicial | Acción | Estado Final | error | orders | Test |
|---|---|---|---|---|---|---|
| Montaje del componente | (initial) | fetch orders | loading=true | "" | [] | `inicia en estado de carga` |
| Resolución exitosa | loading=true | API success | loading=false | "" | [Order...] | `carga orders exitosamente()` |
| Resolución con error | loading=true | API error | loading=false | "msg" | [] | `maneja errores correctamente()` |
| Usuario hace reload | loading=false | fetch orders | loading=true | "" | [Order...] | `permite refrescar los datos` |

---

## Escenarios de Prueba

### Backend: report-service

#### Escenario 1: Generación exitosa de reporte con datos válidos

**Descripción:** Verificar que el sistema genera correctamente un reporte para un rango de fechas válido con órdenes completadas.

**Precondiciones:**
- Base de datos contiene órdenes con status READY creadas dentro del rango 2026-02-10 a 2026-02-20
- Marco de fechas es válido: startDate ≤ endDate

**Pasos:**
1. Llamar GET `/reports?startDate=2026-02-10&endDate=2026-02-20`
2. Verificar que la respuesta tiene status 200 OK
3. Validar que `totalReadyOrders` corresponde al conteo de órdenes READY en rango
4. Validar que `totalRevenue` = suma de (precio × cantidad) para todos los items en órdenes READY
5. Validar que `productBreakdown` agrupa items por productId con cantidades sumadas

**Resultado esperado:** ReportResponseDTO con métricas calculadas correctamente

**Técnica:** Integration testing (ReportControllerIntegrationTest)

---

#### Escenario 2: Validación de rango de fechas inválido

**Descripción:** Sistema rechaza rangos donde startDate > endDate.

**Precondiciones:** Ninguna

**Pasos:**
1. Llamar GET `/reports?startDate=2026-02-28&endDate=2026-02-01`
2. Verificar que la respuesta tiene status 400 Bad Request

**Resultado esperado:** Error status 400, mensaje indicando rango inválido

**Técnica:** Boundary value analysis + integration testing

---

#### Escenario 3: Formato de fecha inválido

**Descripción:** Sistema rechaza parámetros de fecha en formato incorrecto.

**Precondiciones:** Ninguna

**Pasos:**
1. Llamar GET `/reports?startDate=invalid-date&endDate=2026-02-28`
2. Verificar que la respuesta tiene status 400 Bad Request

**Resultado esperado:** Error status 400

**Técnica:** Error guessing + equivalence partitioning

---

#### Escenario 4: Agregación de ingresos invariante

**Descripción:** Verificar matemáticamente que el ingreso total siempre es precisamente suma(precio × cantidad) para órdenes READY.

**Precondiciones:** Se generan órdenes variadas aleatoriamente (jqwik)

**Pasos:**
1. Generar lista aleatoria de órdenes con estados mezclados
2. Llamar `aggregationService.aggregate(orders)`
3. Verificar que `summary.totalRevenue == suma(precio × cantidad)` para órdenes con status READY

**Resultado esperado:** Invariante matemática respetada en todos los casos generados aleatoriamente

**Técnica:** Property-based testing (ReportAggregationPropertyTest)

---

#### Escenario 5: Solo órdenes READY son incluidas

**Descripción:** Verificar que órdenes con status PENDING o IN_PREPARATION no afectan métricas.

**Precondiciones:**
- Base de datos contiene:
  - 1 orden READY con 1 item Hamburguesa qty=1 precio=15.50
  - 1 orden PENDING con 5 items Pizza qty=5 precio=20.00

**Pasos:**
1. Llamar GET `/reports?startDate=2026-02-01&endDate=2026-02-28`
2. Verificar `totalReadyOrders == 1`
3. Verificar `totalRevenue == 15.50`
4. Verificar `productBreakdown.size() == 1` y solo contiene Hamburguesa

**Resultado esperado:** Pizza (de orden PENDING) no aparece en reporte

**Técnica:** Equivalence partitioning (órdenes READY vs NO-READY)

---

#### Escenario 6: Reporte vacío cuando no hay órdenes en rango

**Descripción:** Sistema retorna métricas cero cuando rango de fechas no tiene órdenes.

**Precondiciones:**
- Base de datos contiene órdenes creadas fuera del rango solicitado

**Pasos:**
1. Llamar GET `/reports?startDate=2026-02-01&endDate=2026-02-28` (sin órdenes en ese rango)
2. Verificar status 200 OK
3. Verificar `totalReadyOrders == 0`
4. Verificar `totalRevenue == 0.00`
5. Verificar `productBreakdown.isEmpty()`

**Resultado esperado:** ReportResponseDTO con todos los valores en cero/vacío

**Técnica:** Boundary value analysis + equivalence class (lista vacía)

---

#### Escenario 7: Desglose de producto agrupa correctamente

**Descripción:** Verificar que items del mismo producto en múltiples órdenes se suman en el desglose.

**Precondiciones:**
- Orden 1: Hamburguesa qty=2 precio=15.50
- Orden 2: Hamburguesa qty=3 precio=15.50
- Orden 3: Pizza qty=1 precio=20.00

**Pasos:**
1. Llamar GET `/reports?startDate=2026-02-01&endDate=2026-02-28`
2. Verificar `productBreakdown` contiene:
   - Hamburguesa: quantitySold=5, totalAccumulated=77.50
   - Pizza: quantitySold=1, totalAccumulated=20.00

**Resultado esperado:** Cantidades sumadas por producto, ingresos acumulados correctos

**Técnica:** Equivalence partitioning (múltiples órdenes) + decision table

---

### Frontend: Página de Reportes

#### Escenario 8: Carga inicial de órdenes

**Descripción:** Página muestra estado de carga durante la obtención de órdenes.

**Precondiciones:**
- useOrdersReportController hook está configurado
- API listOrders() devuelve una Promise que aún no se resuelve

**Pasos:**
1. Renderizar OrdersReportPage
2. Verificar que el mensaje "Cargando reporte de órdenes..." es visible
3. Verificar que initialLoading == true

**Resultado esperado:** Componente Loading es renderizado

**Técnica:** Component testing + mock de hook

---

#### Escenario 9: Renderización exitosa de tabla de órdenes

**Descripción:** Une órdenes se muestran correctamente en formato tabla.

**Precondiciones:**
- API listOrders() retorna un array de órdenes válidas
- initialLoading == false
- error == ""

**Pasos:**
1. Renderizar OrdersReportPage con órdenes cargadas
2. Verificar que tabla es visible
3. Verificar que cada fila contiene: ID, Mesa, Estado, Items, Fecha
4. Verificar que número de filas == número de órdenes

**Resultado esperado:** Tabla con órdenes renderizada correctamente

**Técnica:** Component testing

---

#### Escenario 10: Manejo de error con botón Retry

**Descripción:** Cuando API falla, se muestra error y usuario puede reintentar.

**Precondiciones:**
- API listOrders() rechaza con Error("Network error")
- initialLoading == false
- error == "Network error"

**Pasos:**
1. Renderizar OrdersReportPage con estado de error
2. Verificar que ErrorState es visible con "Error al cargar órdenes"
3. Verificar que botón "Reintentar" es visible
4. Usuario hace click en botón Reintentar
5. Verificar que reload() es llamado

**Resultado esperado:** Componente ErrorState con botón de reintento funcional

**Técnica:** Component testing + user event simulation

---

#### Escenario 11: Eliminación de órdenes no hay datos

**Descripción:** Página muestra mensaje vacío cuando no hay órdenes.

**Precondiciones:**
- API listOrders() retorna []
- initialLoading == false
- error == ""

**Pasos:**
1. Renderizar OrdersReportPage
2. Verificar que tabla NO es visible
3. Verificar que mensaje "No hay órdenes para mostrar" es visible

**Resultado esperado:** Mensaje de estado vacío

**Técnica:** Component testing + equivalence class (lista vacía)

---

#### Escenario 12: Hook initializa en estado de carga

**Descripción:** useOrdersReportController comienza con initialLoading=true hasta que API resuelve.

**Precondiciones:** ninguna

**Pasos:**
1. Renderizar hook useOrdersReportController
2. Verificar que initialLoading == true inmediatamente
3. Verificar que orders == []
4. Verificar que error == ""
5. Esperar a que Promise se resuelva
6. Verificar que initialLoading == false

**Resultado esperado:** Estado inicial de carga transitado correctamente a success/error

**Técnica:** Hook unit testing + async assertions

---

#### Escenario 13: Reload reconecta con API

**Descripción:** Función reload() reinicia la carga de órdenes desde API.

**Precondiciones:**
- Hook ya cargó órdenes exitosamente
- Usuario requiere actualización de datos

**Pasos:**
1. Llamar reload()
2. Verificar que listOrders() es llamado nuevamente
3. Esperar resolución
4. Verificar que orders está actualizado

**Resultado esperado:** Nueva carga de datos desde API completada

**Técnica:** Hook unit testing

---

#### Escenario 14: Propagación de errores del API

**Descripción:** Errores del API se propagan al hook y luego al componente.

**Precondiciones:**
- API getReport() rechaza con Error("Rango de fechas inválido")

**Pasos:**
1. Llamar fetchReport("2026-03-01", "2026-02-01")
2. Esperar rechazo
3. Verificar que loading == false
4. Verificar que error.includes("Rango de fechas inválido")
5. Verificar que report == null

**Resultado esperado:** Error capturado y expuesto en estado del hook

**Técnica:** Hook unit testing + error handling testing

---

#### Escenario 15: Limpieza de error anterior en reintento exitoso

**Descripción:** Cuando se realiza una nueva solicitud exitosa después de error, el error anterior se borra.

**Precondiciones:**
- Llamada previa falló con error
- error tiene valor de error anterior
- Nueva llamada resuelve exitosamente

**Pasos:**
1. Llamar fetchReport() une devuelve éxito
2. Verificar que error == ""
3. Verificar que report contiene datos válidos

**Resultado esperado:** Error anterior limpiado, nuevo data cargado

**Técnica:** Hook unit testing + state management testing

---

#### Escenario 16: Facade propaga errores sin capturar

**Descripción:** Errores de API no se manejan en Facade, se propagan al hook.

**Precondiciones:**
- listOrders() rechaza

**Pasos:**
1. Llamar facade.fetchAllOrders()
2. Verificar que promesa es rechazada
3. Verificar que error es el mismo del API

**Resultado esperado:** Error propagado sin transformación

**Técnica:** Facade unit testing

---

## Matriz de Cobertura por Técnica

| Técnica | Aplicación | Archivo Test | Cantidad de Tests |
|---|---|---|---|
| Equivalence Partitioning | DateRangeFilter, ReportAggregationService, ReportController | DateRangeFilterTest, ReportAggregationServiceTest, ReportControllerIntegrationTest | ~12 |
| Boundary Value Analysis | Fecha start/end, agregación vacía vs. llena | DateRangeFilterTest | ~5 |
| Decision Table | Lógica de filtrado de estados, reintento de hooks | ReportAggregationServiceTest, useOrdersReportController.test.ts | ~6 |
| Property-Based Testing | Invariantes matemáticos de agregación | ReportAggregationPropertyTest | 5 properties |
| Component Testing | Rendering, interacción en React | OrdersReportPage.test.tsx | ~4 |
| Hook Testing | Estado asincrónico, callbacks | useOrdersReportController.test.ts, useReportController.test.ts | ~10 |
| Integration Testing | API REST + DB | ReportControllerIntegrationTest, OrderReportRepositoryIntegrationTest | ~10 |

---

## Observaciones Finales

### Fortalezas del Plan de Pruebas Actual

1. **Cobertura multinívelI:** Pruebas unitarias, integración, componente y property-based validati todas las capas.
2. **Nombres descriptivos:** Los tests usan lenguaje de especificación (describe qué se prueba, bajo qué condiciones, qué se espera).
3. **Independencia asegurada:** Uso consistente de beforeEach(), limpiezaIMOCKS, y fixtures aisladas.
4. **Técnicas explícitas:** Las pruebas demuestran aplicación clara de equivalence partitioning y boundary value analysis.
5. **Validación de invariantes:** Property-based testing valida leyes matemática de agregación.

### Áreas Implícitas pero No Explícitamente Documentadas

1. **Pruebas de carga/rendimiento:** No hay tests de volumen (e.g., reporte con 10,000 órdenes).
2. **Concurrencia:** No hay validación de acceso concurrente al reporte.
3. **Integración end-to-end:** Los escenarios e2e con navegación frontend-backend están implícitos pero no automatizados en código.

---

## Referencias

- Código fuente: `report-service/src/main/java`
- Tests backend: `report-service/src/test/java`
- Tests frontend: `src/pages/reports/__tests__/`
- Implementación frontend: `src/pages/reports/`
