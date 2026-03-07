# Matriz de Pruebas — Master — Sistema de Pedidos de Restaurante

> **Documento consolidado** — Combina `TEST_CASES_AI.md` (Gema B, rama `feature/test-cases`) y `TEST_CASES_REFINED.md` (Probador humano, rama `feature/test-cases`).  
> **Alcance:** HDU-01 a HDU-08 — BDD Quality Engineering, Serenity BDD.  
> **Fecha consolidación:** Marzo 2026  
>
> **Estructura de cada sección:**
> - **Casos originales (IA):** Escenarios Gherkin generados por Gema B con análisis de partición de equivalencia y valores límite.
> - **Casos ajustados (Probador):** Tabla de refinamientos aplicados por el equipo QA humano con justificación de cada ajuste.

---

## HDU-01 — Respuesta 201 Created con Header Location

### Resumen Analítico (IA)

| Concepto | Análisis |
|----------|----------|
| **Reglas de Negocio** | Toda creación exitosa devuelve 201 Created; header Location con URI absoluta; orden inicia en PENDING |
| **Validaciones Técnicas** | tableId ∈ [1, 12]; items.length ≥ 1; productId activo en DB; UUID único |
| **Códigos HTTP Esperados** | 201 Created (flujo feliz); 400 Bad Request (validación); 422 Unprocessable Entity (negocio) |
| **Reglas de Máquina de Estados** | Nueva orden → PENDING; consumida por kitchen-worker |

### Casos Ajustados por el Probador

| ID | Caso Original (Gema B) | Ajuste Realizado por el Probador | ¿Por qué se ajustó? |
|---|---|---|---|
| **CT-01-001** | Crear orden válida con 1 item. Retorna → 201 Created. | Crear orden válida con 1 item. Retorna → 201 Created con header `Location` presente y formato UUID v4 verificado. | Confirmar que el header Location no solo existe sino que apunta a un recurso con UUID válido según RFC 4122. |
| **CT-01-002** | Crear orden válida con múltiples items. Retorna → 201 Created. | Crear orden con 3 items válidos. Retorna → 201 Created con header `Location` apuntando a UUID válido. Verificar que los 3 items se persistan correctamente en DB. | Un cliente puede pedir varios productos a la vez. El test confirma que todos los items se guardan sin perder ninguno. |
| **CT-01-003** | Crear orden en mesa al límite superior (tableId=12). Retorna → 201 Created. | Crear orden con tableId=12. Retorna → 201 Created. Verificar que el evento RabbitMQ se publique correctamente. | Mesa 12 es el límite superior permitido. El test asegura que valores al límite se acepten sin error. |
| **CT-01-004** | Crear orden en mesa al límite inferior (tableId=1). Retorna → 201 Created. | Crear orden con tableId=1. Retorna → 201 Created. Verificar que la orden se guarde correctamente en restaurant_db. | Mesa 1 es el límite inferior permitido. El test asegura que el valor mínimo sea válido. |
| **CT-01-005** | Orden con nota en item. Retorna → 201 Created. | Crear orden con item que incluye nota="Sin lácteos". Retorna → 201 Created. Verificar que la nota aparezca en la respuesta y en el evento RabbitMQ. | Los clientes pueden pedir personalizaciones. El test confirma que las notas se transmitan correctamente. |
| **CT-01-006** | tableId fuera de rango superior (13). Retorna → 400 o 422. | tableId=13 excede el máximo permitido. Retorna → 400 o 422. Verificar que NO se publique evento RabbitMQ. | Solo hay 12 mesas en el restaurante. Si el sistema acepta mesa 13, el pedido no tendrá destino físico. |
| **CT-01-007** | tableId fuera de rango inferior (0). Retorna → 400 o 422. | tableId=0 es menor que el mínimo (1). Retorna → 400 o 422. Verificar que la orden no se cree. | No existe mesa 0 en el negocio. El sistema debe rechazar este valor inmediatamente. |
| **CT-01-008** | tableId negativo (-5). Retorna → 400 o 422. | tableId=-5 es inválido. Retorna → 400 o 422 con mensaje descriptivo sobre el rango válido. | Un número negativo no tiene sentido para identificar mesas. |
| **CT-01-009** | Sin items en la orden. Retorna → 400 o 422. | Array items vacío `[]`. Retorna → 400 o 422. El error debe mencionar que se requiere al menos 1 item. | Una orden sin productos no tiene sentido en un restaurante. |
| **CT-01-010** | productId que no existe. Retorna → 400 o 422. | productId=999 no existe en menu_items. Retorna → 400 o 422. El error especifica que el producto no existe. | Si el sistema acepta IDs inventados, la cocina no sabrá qué preparar. |
| **CT-01-011** | productId inactivo (is_active=false). Retorna → 400 o 422. | Crear orden con productId=2 donde is_active=false. Retorna → 400 o 422. | Si un producto está desactivado, no debe poder pedirse. |
| **CT-01-012** | quantity=0. Retorna → 400 o 422. | quantity=0 en un item. Retorna → 400 o 422. El error menciona que la cantidad debe ser positiva. | Pedir 0 unidades no tiene sentido. |
| **CT-01-013** | quantity negativa (-3). Retorna → 400 o 422. | quantity=-3 es inválida. Retorna → 400 o 422 con mensaje descriptivo. | Una cantidad negativa es absurda para un pedido. |
| **CT-01-014** | tableId como string en lugar de número. Retorna → 400. | tableId="mesa5" (texto). Retorna → 400 Bad Request. El error indica problema de tipo de dato. | El backend espera un número. Si acepta strings, el cliente enviará datos mal formados. |
| **CT-01-015** | JSON malformado (llave sin cerrar). Retorna → 400. | JSON con formato inválido. Retorna → 400 Bad Request sin exponer detalles internos del parser. | Un JSON roto es error del cliente. |
| **CT-01-016** | Orden con máxima cantidad en un item. Retorna → 201 Created. | quantity=999999 en un item. Retorna → 201 Created. Verificar que la orden persista con esa cantidad exacta. | Sin límite superior explícito, el sistema debe aceptar cantidades grandes. |
| **CT-01-017** | Orden con múltiples items del mismo producto. Retorna → 201 Created. | items=[{productId: 1, qty: 2}, {productId: 1, qty: 3}]. Retorna → 201 Created. Verificar que ambos items se persistan por separado. | El cliente puede querer el mismo producto con notas diferentes. |
| **CT-01-018** | Nota vacía en item. Retorna → 201 Created. | nota="" (cadena vacía). Retorna → 201 Created. Verificar que se guarde como null o "". | Una nota vacía debe manejarse sin fallar. |
| **CT-01-019** | Nota con caracteres especiales. Retorna → 201 Created. | nota="¡Sin sal, pimentón! & <>". Retorna → 201 Created. Verificar que se persista exactamente igual. | Los clientes pueden escribir con símbolos o tildes. |
| **CT-01-020** | Header Location con UUID v4 válido. Retorna → 201 Created. | Examinar header `Location`. Retorna → 201 Created con formato "http://localhost:8080/orders/{UUID-v4}" válido según RFC 4122. | El header Location debe ser funcional para que el cliente consulte el recurso. |
| **CT-01-021** | Evento order.placed con eventVersion=1. Retorna → 201 Created. | Crear orden y verificar evento RabbitMQ. El evento debe tener eventVersion=1. | El kitchen-worker solo entiende eventos versión 1. |
| **CT-01-022** | Orden duplicada retorna 201 con diferente UUID. Retorna → 201 Created. | Enviar el mismo payload dos veces. Retorna → 201 en ambas con UUIDs DIFERENTES. | El sistema no implementa idempotencia. Cada petición genera una orden nueva. |
| **CT-01-023** | Evento publicado antes de que GET devuelva 201. Retorna → 201 Created. | Crear orden y consultar GET /orders/{id} inmediatamente. Verificar que el evento ya exista en RabbitMQ. | La orden debe ser visible en DB y eventos para asegurar consistencia inmediata. |
| **CT-01-024** | Header Content-Type en respuesta 201. Retorna → 201 Created. | Crear orden y verificar headers. Retorna → 201 con Content-Type: application/json. | El cliente necesita saber que la respuesta es JSON para parsearla. |
| **CT-01-025** | Cuerpo de respuesta 201 contiene createdAt y updatedAt. Retorna → 201 Created. | Crear orden y analizar respuesta. Retorna → 201 con campos id, tableId, status, items, createdAt, updatedAt. | El cliente necesita timestamps para rastrear la orden. |
| **CT-01-026** | GET a recurso creado retorna 200 OK sin autenticación. Retorna → 200 OK. | Crear orden, obtener Location y hacer GET sin token. Retorna → 200 OK. | El endpoint GET /orders/{id} es público. |
| **CT-01-027** | GET a recurso creado con X-Kitchen-Token retorna 200 OK. Retorna → 200 OK. | Crear orden y hacer GET con header X-Kitchen-Token. Retorna → 200 OK en estado PENDING. | La cocina necesita consultar órdenes usando su token. |
| **CT-01-028** | Orden recién creada tiene estado PENDING en GET. Retorna → 200 OK. | Crear orden y consultarla vía GET. Retorna → 200 OK con status="PENDING". | Toda orden nueva comienza en PENDING. |

---

## HDU-02 — Respuesta 200 OK con Lista Vacía

### Casos Ajustados por el Probador

| ID | Caso Original (Gema B) | Ajuste Realizado por el Probador | ¿Por qué se ajustó? |
|---|---|---|---|
| **CT-02-001** | GET /menu retorna 200 OK con array vacío. | GET /menu cuando la DB está vacía o todos los productos tienen is_active=false. Retorna → 200 OK con cuerpo exactamente `[]`, NO null. | Un menú vacío es un estado válido del negocio. |
| **CT-02-002** | GET /menu retorna 200 OK con productos cuando existen. | GET /menu con 3 productos activos. Retorna → 200 OK con array de 3 elementos con campos: id, name, description, price, category, imageUrl, isActive. | Confirma que cuando hay datos, se devuelven correctamente formateados. |
| **CT-02-003** | GET /menu filtra SOLO productos con is_active=true. | GET /menu con 5 productos (3 activos, 2 inactivos). Retorna → 200 OK con array de exactamente 3 productos. | Los productos desactivados no deben aparecer en el menú. |
| **CT-02-004** | GET /reports con rango sin órdenes READY retorna 200 OK con array vacío. | GET /reports para rango sin órdenes READY. Retorna → 200 OK con totalRevenue=0.0, orders=[], productSummary=[]. | Un período sin ventas es posible. Enviar ceros previene que el reporte colapse. |
| **CT-02-005** | GET /reports retorna totalRevenue=0 cuando no hay ventas. | GET /reports sin órdenes READY. Retorna → 200 OK con totalRevenue exactamente 0 o 0.0. | Sin ventas, los ingresos son cero. |
| **CT-02-006** | GET /reports con rango futuro sin datos. | GET /reports para fechas futuras (2099-01-01 a 2099-12-31). Retorna → 200 OK con todos los conteos en 0. | Consultar fechas futuras no debe romper el sistema. |
| **CT-02-007** | GET /orders retorna 200 OK con array vacío. | GET /orders cuando la tabla está vacía. Retorna → 200 OK con cuerpo exactamente `[]`. | Sin órdenes activas, la lista está vacía. No debe confundirse con error del servidor. |
| **CT-02-008** | GET /orders?status=PENDING retorna 200 OK con array vacío. | GET /orders?status=PENDING sin órdenes en ese estado. Retorna → 200 OK con `[]` sin contaminar con otros estados. | Filtrar por estado sin resultados es un caso normal. |
| **CT-02-009** | GET /menu sin autenticación retorna 200 OK. | GET /menu sin header X-Kitchen-Token. Retorna → 200 OK con menú completo. | El menú es público. |
| **CT-02-010** | GET /reports sin X-Kitchen-Token retorna 401. | GET /reports sin header X-Kitchen-Token. Retorna → 401 Unauthorized con error JSON estandarizado. | Los reportes son información sensible del negocio. |
| **CT-02-011** | GET /reports con X-Kitchen-Token inválido retorna 401. | GET /reports con token="invalid_token_xyz". Retorna → 401 Unauthorized sin revelar detalles del rechazo. | Un token falso no debe dar acceso. |
| **CT-02-012** | GET /orders sin X-Kitchen-Token pero con status=PENDING. Retorna → 401. | Retorna → 401 sin devolver lista. | Consultar órdenes por estado requiere autenticación. |
| **CT-02-013** | Respuesta de /menu es un array de objetos MenuItem. | GET /menu y verificar estructura. Retorna → 200 con Content-Type: application/json. | Mantener el contrato fijo evita que el frontend se rompa. |
| **CT-02-014** | Respuesta de /reports con estructura fija. | GET /reports. Retorna → 200 con {totalRevenue: number, orders: array, productSummary: array}. | La estructura fija del reporte permite que el frontend lo parsee sin sorpresas. |
| **CT-02-015** | Lista con 1 elemento retorna array con 1 elemento. | GET /menu con exactamente 1 producto activo. Retorna → 200 con array de 1 elemento válido. | Confirmar que listas con un solo elemento no se confundan con casos vacíos. |

---

## HDU-03 — Validación de Estructura y Tipo de Datos con 400

### Casos Ajustados por el Probador

| ID | Caso Original (Gema B) | Ajuste Realizado por el Probador | ¿Por qué se ajustó? |
|---|---|---|---|
| **CT-03-001** | JSON con llave sin cerrar. Retorna → 400. | JSON con llave sin cerrar sin exponer stack traces internos. Retorna → 400. | Un JSON malformado es error del cliente. El mensaje debe ser accionable sin revelar internals. |
| **CT-03-002** | JSON con comilla sin cerrar. Retorna → 400. | JSON con comilla sin cerrar sin exponer nombres de clases internas. Retorna → 400. | El mensaje debe guiar sin filtrar detalles del parser. |
| **CT-03-003** | JSON con caracteres no escapados. Retorna → 400. | JSON con caracteres no escapados capturado por @ExceptionHandler. Retorna → 400. | Confirma que este error es capturado por el @ExceptionHandler global con ErrorResponse estándar. |
| **CT-03-004** | Cuerpo vacío "" o sin body en POST /orders. Retorna → 400. | Probar body "" y request sin body por separado. Retorna → 400. | El defecto debe distinguir "body vacío" de "body malformado". |
| **CT-03-005** | tableId como string "mesa5". Retorna → 400. | Verificar que Jackson no convierta silenciosamente el string a número. Retorna → 400. | Si el sistema acepta strings donde espera números, el cliente nunca sabrá que envía datos mal tipados. |
| **CT-03-006** | tableId como objeto {}. Retorna → 400. | Retorna → 400 con ErrorResponse, no con 500 ni HTML. | Lo que se prueba es que el sistema falla inmediatamente de forma explícita. |
| **CT-03-007** | items como string "producto1,producto2". Retorna → 400. | El error indica que items debe ser un array. Retorna → 400. | El servidor rechaza el CSV porque espera un array JSON. |
| **CT-03-008** | productId como string en lugar de number. Retorna → 400. | El error indica que el tipo de dato no es el correcto. Retorna → 400. | El mensaje debe señalar el campo y el tipo esperado. |
| **CT-03-009** | quantity como string en lugar de number. Retorna → 400. | quantity debe ser numérico para cálculos. Retorna → 400. | El error debe ser preciso sobre el campo fallido. |
| **CT-03-010** | quantity como null. Retorna → 400 o 422. | El error menciona que quantity es requerida. Retorna → 400 o 422. | Si el cliente envía null, el servidor debe fallar con "quantity es requerido". |
| **CT-03-011** | Falta tableId en payload. Retorna → 400. | El error menciona que tableId es requerido. Retorna → 400. | Sin tableId no se sabe a dónde llevar el pedido. |
| **CT-03-012** | Falta items en payload. Retorna → 400. | El error menciona que items es requerido. Retorna → 400. | Una orden sin items es inválida. |
| **CT-03-013** | Falta productId en un item. Retorna → 400. | El error indica que productId es requerido dentro del arreglo. Retorna → 400. | Sin productId no se sabe qué producto ordenar. |
| **CT-03-014** | Falta quantity en un item. Retorna → 400 o 422. | La falta de cantidad no debe asumirse como 1 por defecto. Retorna → 400 o 422. | Debe ser rechazada explícitamente. |
| **CT-03-015** | note como número 123 en lugar de string. Retorna → 201. | Ningún ajuste adicional. | - |
| **CT-03-016** | Payload con campo adicional "extraField". Retorna → 201. | extraField es ignorado por completo. Retorna → 201. | Jackson ignora campos desconocidos por defecto. |
| **CT-03-017** | JSON válido con tipos correctos (flujo feliz). Retorna → 201. | JSON válido. Retorna → 201 con UUID v4. Verificar que se persistan todos sus campos. | Asegura que las operaciones ideales se almacenen correctamente. |
| **CT-03-018** | JSON válido con note: null. Retorna → 201. | note no debe serializarse accidentalmente al string "null". Retorna → 201. | Una nota nula debe parsearse bien. |
| **CT-03-019** | JSON con 5 items válidos. Retorna → 201. | Verificar que los 5 elementos existan en persistencia y respuesta. Retorna → 201. | Ratifica la robustez del endpoint para listados. |
| **CT-03-020** | Distinción entre 400 y 422. Retorna → 400 / 422. | 400 para malformaciones JSON; 422 para reglas lógicas del negocio violadas. | Si el cliente no conoce la naturaleza de su falla, no sabrá qué corregir. |
| **CT-03-021** | Arreglo limpio items: []. Retorna → 422. | El mensaje indica "al menos un item requerido". Retorna → 422. | Un array vacío respeta a Java pero lógicamente es inválido para el restaurante. |

---

## HDU-04 — Desactivación Lógica de Productos con Auditoría

### Casos Ajustados por el Probador

| ID | Caso Original (Gema B) | Ajuste Realizado por el Probador | ¿Por qué se ajustó? |
|---|---|---|---|
| **CT-04-001** | Desactivar producto activo (id=1). Retorna → 200 OK. | Retorna → 200 OK con metadata auditable, verificando estricto uso del dato `updatedBy`. | Las desactivaciones sin trazabilidad pierden auditoría. |
| **CT-04-002** | Desactivar producto activo, verificar fechas no cambian. Retorna → 200 OK. | Retorna → 200 OK confirmando la inmutabilidad histórica del timestamp primario `createdAt`. | El timestamp de creación no debe sobreescribirse. |
| **CT-04-003** | Desactivar producto y verificar GET /menu no lo trae. Retorna → 200 OK. | Retorna → 200 OK ratificando la ausencia del product en el listado. | Elementos extintos confunden a cocineros. |
| **CT-04-004** | Desactivar producto ya inactivo (id=2). Retorna → 200 OK. | Ningún cambio adicional. | - |
| **CT-04-005** | Múltiples DELETE al mismo producto. Retorna → 200 OK. | Ningún cambio adicional. | - |
| **CT-04-006** | DELETE a producto 999. Retorna → 404. | Retorna → 404 Not Found con mensaje estricto sobre el índice no encontrado. | Carencias de items irreales deben reportarse claramente. |
| **CT-04-007** | DELETE al identificador -1. Retorna → 404. | Retorna → 404 Not Found previniendo interacciones nocivas de dígitos incalculables. | IDs negativos deben ser rechazados. |
| **CT-04-008** | DELETE con nulos y límite 0. Retorna → 404. | Ningún cambio adicional. | - |
| **CT-04-009** | DELETE sin Header. Retorna → 401. | Retorna → 401 Unauthorized sin posibilidad de borrar o acceder a transacciones. | La autenticación es obligatoria para eliminar. |
| **CT-04-010** | DELETE con token dañado. Retorna → 401. | Retorna → 401 Unauthorized sin revelar el diagnóstico del token viciado. | Guardar silencio contra ataques neutraliza el mapeo a cibercriminales. |
| **CT-04-011** | DELETE sin rol superior. Retorna → 403. | Ningún cambio adicional. | - |
| **CT-04-012** | Respuesta OK incluye información base. Retorna → 200 OK. | Ningún cambio adicional. | - |
| **CT-04-013** | Respuesta 404 retorna JSON estandarizado de error. | Ningún cambio adicional. | El sistema debe devolver estructuras de error consistentes. |
| **CT-04-014** | Suspender recurso en lazo vivo tipo pedido PENDING. Retorna → 200 OK. | Retorna → 200 OK confirmando que pedidos históricos se aíslen y no corrompan órdenes previas. | Desactivar un producto no debe alterar órdenes ya existentes. |
| **CT-04-015** | Supresión genérica sobre totalidad base. Retorna → 200 con `[]`. | Retorna → 200 con arreglo de corchetes vacíos obviando interrupciones 404. | Un menú vacío es un estado válido del negocio. |
| **CT-04-016** | Crear orden referenciando productos anulados. Retorna → error. | Retorna → 400 o 422 abortando la orden con producto inactivo. | El sistema debe impedir pedir productos fuera de carta. |

---

## HDU-05 — Respuestas de Error JSON Estandarizadas (Desarrollador)

### Casos Ajustados por el Probador

| ID | Caso Original (Gema B) | Ajuste Realizado por el Probador | ¿Por qué se ajustó? |
|---|---|---|---|
| **CT-05-001** | Validación fallida: `tableId` fuera de rango. Retorna → 400. | Validación limitando de 0 a 13. Retorna → 400 con ErrorResponse que informa el rango válido (1-12). | Guiar hacia números permitidos suprime confusiones al instante. |
| **CT-05-002** | Validación fallida: sin items. Retorna → 400. | Ningún cambio adicional. | - |
| **CT-05-003** | Validación fallida: producto inexistente. Retorna → 400. | Validación de producto 999. Retorna → 400 detallando el ítem omitido en la DB. | Comunicar el ID agiliza correcciones operacionales. |
| **CT-05-004** | JSON malformado. Retorna → 400. | Retorna → 400 garantizando envoltorio total ocultando HTML nativo de Spring. | Exhibir fallas sucias al parsear devela información interna a atacantes. |
| **CT-05-005** | GET /orders obviando header auth. Retorna → 401. | Ningún cambio adicional. | - |
| **CT-05-006** | GET /reports con token inválido. Retorna → 401. | Retorna → 401 Unauthorized sin dar avisos sobre si el token expiró o es inválido. | Enmascarar las bases del rechazo evita dar pistas a atacantes. |
| **CT-05-007** | GET order con UUID no localizable. Retorna → 404. | Búsqueda con ID corrompido. Retorna → 404 Not Found. | IDs extraviados en el log facilitan revisiones a profundidad. |
| **CT-05-008** | GET hacia items no existentes. Retorna → 404. | Retorna → 404 replicando la estructura formal del error genérico original. | Unificar estructura para cualquier 404 simplifica el manejo en el cliente. |
| **CT-05-009** | Report-service al caerse su conexión. Retorna → 500. | Corte con proveedor DB. Retorna → 500 ocultando dirección web, credencial privada o puertos. | Liberar variables físicas sobre topologías activas viola la norma OWASP. |
| **CT-05-010** | Order-service con Excepciones sin mapear. Retorna → 500. | Simular hilos caídos de Java. Retorna → 500 capturado por `@ControllerAdvice` como JSON, no como HTML. | Atajar excepciones ciegas impide que Spring tire pantallas en blanco al usuario. |
| **CT-05-011** | Simetría estructural evaluable en sistema. Retorna → 400. | Inyectar igual fallo sobre 2 servicios (orden - reporte). Retorna → 400 con simetría en formato y tipados. | Requerir paralelismo puro evita tiempo perdido en frontend programando parseos desiguales. |
| **CT-05-012** | Confirmar formato `content-type`. Retorna → application/json. | Retorna → headers de naturaleza `application/json` en todas las respuestas de error. | Ignorar el type condena al fracaso todo procesador externo de clientes web. |
| **CT-05-013** | Validaciones superpuestas al tiempo. Retorna → 400. | Ejecutar llamadas sin mesa y datos `[]`. Retorna → 400. | Formalizar normativas conjuntas facilita las respuestas al cliente. |
| **CT-05-014** | Errores tolerantes a inyecciones. Retorna → Error estándar. | Infiltrar adjuntos extras al array del control `ErrorResponse`. Retorna → JSON estable. | Aceptar adjuntos progresivos permite crecimientos maduros en el backend. |
| **CT-05-015** | Transición asíncrona ilógica estado vivo. Retorna → 409. | Enviar parche PENDING a READY. Retorna → 409. | El freno garantiza fluidez oficial sin atajos perjudiciales. |

---

## HDU-06 — Respuestas de Error JSON Estandarizadas (Seguridad)

> Los casos de prueba de HDU-06 están integrados en la cobertura de HDU-05 (seguridad de respuestas de error). Ver `TEST_CASES_AI.md` para los escenarios Gherkin completos de HDU-06.

---

## HDU-07 — Validación de Máquina de Estados en Transiciones de Órdenes

### Casos Ajustados por el Probador

| ID | Caso Original (Gema B) | Ajuste Realizado por el Probador | ¿Por qué se ajustó? |
|---|---|---|---|
| **CT-07-001** | Transición válida PENDING → IN_PREPARATION. Retorna → 200 OK. | Verificar que el estado persiste correctamente en DB tras el PATCH y que la respuesta incluye el nuevo estado. Retorna → 200 OK. | Confirmar que la actualización es efectiva tanto en la respuesta como en persistencia, no solo en memoria. |
| **CT-07-002** | Salto de estado PENDING → READY (saltando IN_PREPARATION). Retorna → 409 Conflict. | PATCH a READY desde PENDING. Retorna → 409 Conflict con mensaje descriptivo indicando la transición inválida. | La máquina de estados no permite saltos. El error debe ser claro para el operador de cocina. |
| **CT-07-003** | Transición hacia atrás IN_PREPARATION → PENDING. Retorna → 409 Conflict. | PATCH a PENDING desde IN_PREPARATION. Retorna → 409 Conflict. El estado NO debe revertirse. | Retroceder en el flujo rompe la trazabilidad del pedido. |
| **CT-07-004** | PATCH sin X-Kitchen-Token. Retorna → 401 Unauthorized. | Ningún cambio adicional. | Solo el personal de cocina autenticado puede cambiar estados de órdenes. |
| **CT-07-005** | PATCH sobre orden inexistente. Retorna → 404 Not Found. | UUID aleatorio no existente en DB. Retorna → 404 con JSON estándar. | Una orden que no existe no puede actualizarse. |

### Revisión humana
> ⚠️ **Conflicto de código HTTP con HDU-08:** Los casos CT-07-002 y CT-07-003 retornan `409 Conflict` para transiciones de estado inválidas, mientras que HDU-08 (CT-08-002) usa `422 Unprocessable Entity` para el mismo tipo de error. Esto genera dos rutas de manejo de error en el frontend para la misma excepción de negocio, aumentando la complejidad y el riesgo de bugs. **Se recomienda unificar ambas historias en un único código HTTP** — `409 Conflict` es semánticamente más preciso para violaciones de máquina de estados.
>
> 💡 **Mejora:** El CT-05-015 en HDU-05 ya cubre parcialmente este escenario (transición PENDING → READY). Sería conveniente consolidar estos casos en la sección HDU-07 para evitar duplicación de cobertura entre matrices.

---

## HDU-08 — Respuesta 422 Unprocessable Entity para Errores de Validación de Negocio

### Casos Ajustados por el Probador

| ID | Caso Original (Gema B) | Ajuste Realizado por el Probador | ¿Por qué se ajustó? |
|---|---|---|---|
| **CT-08-001** | Crear orden con tableId mayor a 12. Retorna → 422 Unprocessable Entity. | POST /orders con tableId=13. Retorna → 422 con mensaje indicando rango válido (1-12). Verificar que no se persista la orden ni se publique evento RabbitMQ. | La validación de negocio debe impedir que el sistema acepte datos fuera del contrato antes de cualquier procesamiento. |
| **CT-08-002** | Transición de estado inválida READY → IN_PREPARATION. Retorna → 422 Unprocessable Entity. | PATCH de READY a IN_PREPARATION. Retorna → 422 con mensaje indicando que el estado no puede retroceder. | Esta transición viola las reglas de negocio de la máquina de estados. |
| **CT-08-003** | JSON malformado para confirmar distinción 400 vs 422. Retorna → 400 Bad Request. | POST con JSON con llave sin cerrar. Retorna → 400 (no 422), confirmando que el formato roto es error técnico, no de negocio. | La distinción 400/422 es clave para el frontend: 400 = corregir JSON; 422 = corregir lógica de negocio. |
| **CT-08-004** | Crear orden con items vacíos `[]`. Retorna → 422. | POST con items=[]. Retorna → 422 con mensaje "se requiere al menos un ítem". | Una orden sin ítems es inválida por regla de negocio, no por formato. |

### Revisión humana
> ⚠️ **Conflicto de código HTTP con HDU-07:** CT-08-002 retorna `422 Unprocessable Entity` para una transición de estado inválida (`READY → IN_PREPARATION`), mientras que HDU-07 retorna `409 Conflict` para el mismo tipo de fallo. Mantener dos códigos HTTP distintos para la misma excepción de negocio obliga al cliente a implementar dos handlers separados sin razón, generando complejidad innecesaria y posibles bugs. **Se recomienda consolidar en un único código HTTP** — idealmente `409 Conflict`.
>
> 💡 **Mejora de alcance:** CT-08-001 (validación de `tableId`) solapa con casos ya cubiertos en HDU-03 y HDU-05. Revisar si este caso debe mantenerse aquí o unificarse en la sección correspondiente para evitar duplicación y conflictos de criterio.

---

## Resumen General de Técnicas Aplicadas

| Técnica | Aplicación |
|---------|------------|
| **Partición de Equivalencia** | Valores válidos e inválidos para tableId, quantity, productId |
| **Análisis de Valores Límite** | tableId=[1,12], quantity≥1, campos requeridos |
| **Tabla de Decisión** | Combinaciones de validación en HDU-01, HDU-03 |
| **Máquina de Estados** | Flujo PENDING → IN_PREPARATION → READY en HDU-07/08 |
| **Caso de Uso** | Todos los escenarios end-to-end |
| **Seguridad** | Exposición de información técnica en errores (HDU-05, HDU-06) |

## Notas de Implementación

- Los escenarios Gherkin completos para automatización están en `TEST_CASES_AI.md`.
- Los ajustes del probador humano están integrados en este documento (columna "Ajuste Realizado por el Probador").
- Para HDU-07 y HDU-08: **pendiente decisión de equipo** sobre el código HTTP unificado para errores de máquina de estados (`409` vs `422`).
- Ver `HISTORIAS_USUARIO_MASTER.md` para las historias de usuario completas con criterios de aceptación Gherkin.
