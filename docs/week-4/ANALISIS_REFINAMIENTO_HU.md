# Tabla Comparativa de Historias de Usuario

---

## Tabla Comparativa

### HDU-01 — Respuesta 201 Created con Header Location en Creación de Órdenes

| Aspecto | HU Original (TEST_PLAN_V3) | HU Refinada (HDU_refinadas) | Diferencias Detectadas |
|---|---|---|---|
| **Rol del Actor** | Desarrollador del Frontend | Desarrollador del Frontend del Restaurante | ✅ Más específico: "del Restaurante" añade contexto de dominio |
| **Objetivo Principal** | El endpoint responda con `201 Created` y header `Location` | Que el endpoint de creación de órdenes responda con `201 Created` y header `Location` con la URI del recurso | ✅ Más explícito: especifica "URI del recurso" (antes era implícito) |
| **Valor de Negocio** | Para redirigir o consultar el estado de la nueva orden | Para que la aplicación pueda redirigir o consultar el estado de manera estandarizada y eficiente | ✅ Ampliado: añade "estandarizada" (conformidad REST) e "eficiente" (optimización UX) |
| **Criterios de Aceptación** | 3 escenarios Gherkin: Creación exitosa, Datos inválidos, Verificación de acceso | 3 escenarios Gherkin: Creación exitosa, Datos inválidos, Verificación de acceso | ✅ Idéntica en cobertura, **pero refinada en detalle**: |
| | Escenario 1: POST con tableId 1-12 y producto activo → 201 Created, header Location | Escenario 1: POST con tableId ∈ [1,12] y al menos un producto activo → 201 Created, Location URL válida, evento `order.placed` publicado en RabbitMQ | ✅ **Añadido explícitamente**: validación del rango 1-12, publicación de evento, formato URL del header |
| | Escenario 2: POST con tableId 13 → 400 Bad Request, sin Location | Escenario 2: POST con tableId fuera de rango (13) → 400 Bad Request, sin Location | ✅ Idéntica |
| | Escenario 3: GET a URL del header con token → 200 OK, detalle con PENDING | Escenario 3: GET a la URL del header con X-Kitchen-Token → 200 OK, detalle con estado PENDING e items | ✅ Más explícito: menciona "X-Kitchen-Token" (antes era token genérico), especifica "items" en respuesta |
| **Reglas de Negocio Explícitas** | Rango de tableId implícito | tableId debe estar entre 1 y 12; al menos un producto activo | ✅ **Sección dedicada "Notas Adicionales"** con sub-historias sugeridas, dependencias (UUID previo), riesgos |
| **Restricciones Técnicas** | Genera UUID | Requiere persistencia en `restaurant_db` para generación de UUID previo al header; el UUID es la fuente de verdad | ✅ Especifica qué BD (`restaurant_db`), momento de generación (previo al header) |
| **Seguridad** | Acceso con token | Acceso con token de cocina (`X-Kitchen-Token`) | ✅ Especifica el nombre del header de seguridad |
| **Precisión INVEST** | ✓ Independiente, negociable, valiosa, estimable, pequeña, testable | ✓ Mismo nivel + más testable por criterios explícitos | ✅ Mejora en **testabilidad** (criterios más específicos) y **estimabilidad** (dependencias claras) |
| **Aserciones Transversales Añadidas** | No especificadas | Sección "Aserciones transversales para todos los 201": Content-Type, Location pattern, status = PENDING, createdAt ISO 8601 | ✅ **Mejora crítica**: proporciona checklist reutilizable para todas las respuestas 201 |
| **Riesgos Documentados** | Implícitos | Explícitamente: ruptura con Frontend anterior, latencia bajo Reverse Proxy | ✅ Identifica y comunica riesgos de implementación |

---

### HDU-02 — Respuesta 200 OK con Lista Vacía en Endpoints sin Registros

| Aspecto | HU Original (TEST_PLAN_V3) | HU Refinada (HDU_refinadas) | Diferencias Detectadas |
|---|---|---|---|
| **Rol del Actor** | Desarrollador Frontend del Sistema de Pedidos | Desarrollador Frontend del Sistema de Pedidos | ✅ Idéntico |
| **Objetivo Principal** | Los endpoints de consulta retornen `200 OK` con lista vacía | Los endpoints de consulta de órdenes, menú y reportes retornen `200 OK` con lista vacía | ✅ Más específico: enumera explícitamente los 3 endpoints (`/orders`, `/menu`, `/reports`) |
| **Valor de Negocio** | Asegurar consistencia del contrato y evitar fallos en renderizado | Asegurar la consistencia del contrato de la API y evitar fallos en el renderizado por datos inexistentes | ✅ Idéntica, pero más clara: explica la causa del fallo en frontend ("datos inexistentes") |
| **Criterios de Aceptación** | 3 escenarios (Menú sin productos, Reportes sin ventas, Acceso no autorizado) | 3 escenarios con especificación Gherkin estructurada | ✅ Idéntica en cobertura |
| | CA-1: Menú sin activos → 200 OK, body `[]` | CA-1: Menú sin activos → 200 OK, body exactamente `[]` (especifica JSON puro) | ✅ Más preciso: "exactamente" enfatiza que NO es `null` |
| | CA-2: Reportes sin ventas en rango → 200 OK, lista vacía | CA-2: Reportes sin ventas en rango → 200 OK, lista en respuesta vacía | ✅ Idéntica |
| | CA-3: Sin X-Kitchen-Token → 401 Unauthorized, sin arreglo | CA-3: Sin X-Kitchen-Token → 401 Unauthorized, no retorna arreglo | ✅ Idéntica |
| **Reglas de Negocio Explícitas** | Implícitas | Explícitamente: listas se inicializan con `new ArrayList<>()` para que Jackson serialice `[]` no `null` | ✅ **Decisión técnica importante**: especifica mecanismo de implementación (Jackson, ArrayList inicializado) |
| **Restricciones Técnicas** | Implícitas | Dependencias documentadas: DTOs deben inicializar listas | ✅ Comunica restricción técnica clave para implementación correcta |
| **Precisión INVEST** | ✓ Independiente, valiosa, estimable, pequeña, testable | ✓ Mismo nivel + más estimable (decisión técnica clara) | ✅ Mejora en **estimabilidad**: el equipo sabe exactamente qué cambiar en DTOs |
| **Riesgos Documentados** | Implícitos | Explícitamente: confusión frontend entre vacío y error, ocultamiento de errores 400 bajo arreglo vacío | ✅ Identifica antimpatrones a evitar |
| **Sub-historias Sugeridas** | No mencionadas | Estandarización en `order-service` y `report-service` (segregación de trabajo) | ✅ Proporciona decomposición operativa |

---

### HDU-03 — Validación de Estructura y Tipo de Datos con Respuesta 400

| Aspecto | HU Original (TEST_PLAN_V3) | HU Refinada (HDU_refinadas) | Diferencias Detectadas |
|---|---|---|---|
| **Rol del Actor** | Desarrollador del Frontend del Sistema de Pedidos | Desarrollador del Frontend del Sistema de Pedidos | ✅ Idéntico |
| **Objetivo Principal** | La API valide estructura y tipo de datos recibidos | La API valide estructura y tipo de datos recibidos | ✅ Idéntico |
| **Valor de Negocio** | Recibir 400 Bad Request con mensaje descriptivo | Recibir 400 Bad Request que detalle el error de entrada | ✅ Idéntico, reforzado |
| **Criterios de Aceptación** | 3 escenarios: JSON malformado, tipo incorrecto, petición válida | 3 escenarios Gherkin con ejemplos concretos | ✅ Idéntica en cobertura |
| | Escenario 1: JSON inválido → 400, mensaje descriptivo | Escenario 1: JSON inválido (ej. body mal formado) → 400, mensaje descriptivo del error de formato | ✅ Añade "ej. body mal formado" para claridad |
| | Escenario 2: tableId = "mesa_uno" → 400, detallar tipo | Escenario 2: tableId = "mesa_uno" (string en lugar de int) → 400, detallar tipo esperado vs recibido | ✅ Más explícito: especifica el tipo esperado (int) y recibido (string) |
| | Escenario 3: JSON válido → 201, evento publicado | Escenario 3: JSON válido y tipos correctos → 201, evento order.placed publicado en RabbitMQ | ✅ Más explícito: "JSON válido" + "tipos correctos" + especifica evento y destino (RabbitMQ) |
| **Reglas de Negocio Explícitas** | Implícitas | Explícitamente: tableId espera número entero | ✅ Documenta la expectativa de tipo |
| **Restricciones Técnicas** | Implícitas | Explícitamente: DTOs anotados correctamente para validación de Jackson | ✅ Comunica responsabilidad técnica |
| **Seguridad** | No mencionada | No mencionada (heredado) | ⚠️ Neutral |
| **Precisión INVEST** | ✓ Independiente, valiosa, estimable, testable | ✓ Mismo + más estimable | ✅ Mejora en **claridad de entrega**: sabe que necesita handlers de tipo mismatch |
| **Riesgos Documentados** | No mencionados | Impacto en Frontend (puede no estar preparado), saturación de logs | ✅ Identifica dependencias de frontend |
| **Sub-historias Sugeridas** | No mencionadas | RFC 7807, validación de formatos ISO 8601 | ✅ Proporciona estándares a implementar |


