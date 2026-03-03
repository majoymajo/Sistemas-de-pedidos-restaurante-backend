# 🏗️ Anatomía del Proyecto Serenity BDD — Explicación de Cada Archivo

**Perspectiva de:** Senior QA Engineer  
**Para:** Junior QA que necesita entender por qué cada archivo existe

---

## Estructura Completa

```
Serenity/
├── build.gradle                          ← 🧱 La base de todo (Java 24)
├── settings.gradle                       ← 🏷️ La tarjeta de identidad
│
└── src/test/
    ├── java/com/restaurant/qa/
    │   ├── runners/
    │   │   └── TestRunner.java           ← 🚀 El punto de entrada JUnit 5
    │   └── stepdefinitions/              ← ⚡ El corazón de la ejecución
    │       ├── OrderStepDefinitions.java ← 🐍 Mapeo Gherkin -> Java
    │       └── CucumberHooks.java        ← ⚓ Ganchos de configuración
    │
    └── resources/
        ├── serenity.conf                 ← ⚙️ El cerebro de configuración
        ├── cucumber.properties           ← 🔌 El cable que conecta todo
        ├── logback-test.xml              ← 📋 El filtro de ruido
        └── features/
            └── order_service/
                └── HDU01_create_order.feature  ← 📖 La historia del usuario
```

---

## 1. [build.gradle](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Serenity/build.gradle) — 🧱 La Base de Todo

### ¿Qué hace?
Es el **manifiesto del proyecto**. Le dice a Gradle: "estas son las herramientas que necesito, estas son las reglas para ejecutar pruebas, y estos son los reportes que quiero generar."

### Analogía
Piensa en [build.gradle](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Serenity/build.gradle) como la **receta de cocina** del proyecto. Lista todos los ingredientes (dependencias), los utensilios (plugins), y los pasos de preparación (tareas).

### ¿Por qué un Senior QA lo considera crítico?

| Sección | Qué hace | Por qué importa |
|---|---|---|
| **`plugins`** | Activa el plugin de Serenity Gradle | Sin esto, no hay reportes HTML. El comando `gradle aggregate` no existiría. |
| **`ext { versions }`** | Centraliza las versiones de todas las librerías | Evita el "dependency hell". Si Serenity sube de versión, cambias **un solo número** y todo se actualiza. |
| **`dependencies`** | Declara las 12 librerías necesarias | Cada una tiene un propósito. Si falta una, algo se rompe silenciosamente. |
| **`test { }`** | Configura cómo se ejecutan las pruebas | `useJUnitPlatform()` es la línea más importante — sin ella, Gradle no sabe que usamos JUnit 5. |
| **`tasks.register('smoke')`** | Crea tareas personalizadas | Permite ejecutar subconjuntos de pruebas. En la vida real, el CI ejecuta `gradle smoke` en cada commit y `gradle regression` de noche. |

### Las 12 dependencias explicadas como un mapa:

```
┌─────────────────────────────────────────────────────┐
│                    Tu Código de Pruebas             │
│   (Step Definitions, Tasks, Questions)              │
├──────────┬──────────┬───────────────────────────────┤
│ Serenity │ Serenity │ Serenity                      │
│ Core     │ Cucumber │ REST Assured                  │
│ (motor)  │ (puente) │ (HTTP auto-logging)           │
├──────────┴──────────┴───────────────────────────────┤
│ Serenity JUnit5  │  Serenity Screenplay (REST)      │
│ (lanzador)       │  (patrón actor-tarea-pregunta)   │
├──────────────────┴──────────────────────────────────┤
│ Cucumber Java    │  Cucumber JUnit Platform Engine  │
│ (@Given @When)   │  (descubre .feature files)       │
├──────────────────┴──────────────────────────────────┤
│ JUnit 5 API + Engine  │  JUnit Platform Suite       │
│ (framework base)      │  (@Suite annotation)        │
├───────────────────────┴─────────────────────────────┤
│ AssertJ  │  Lombok  │  SLF4J + Logback              │
│ (assert) │  (POJO)  │  (logging)                    │
└──────────┴──────────┴───────────────────────────────┘
```

### ¿Qué pasa si lo borras?
**Nada funciona.** No puedes compilar, no puedes ejecutar pruebas, no puedes generar reportes. Es el archivo más importante del proyecto.

---

## 2. [settings.gradle](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Serenity/settings.gradle) — 🏷️ La Tarjeta de Identidad

### ¿Qué hace?
Una sola línea: le dice a Gradle **cómo se llama** este proyecto.

```groovy
rootProject.name = 'restaurant-qa-automation'
```

### ¿Por qué un Senior QA lo considera necesario?
- Sin este archivo, Gradle usa el nombre de la carpeta como nombre del proyecto. Si alguien renombra la carpeta, los reportes cambian de nombre.
- En proyectos multi-módulo (como tu backend con `order-service`, `kitchen-worker`, `report-service`), este archivo es donde se registran los submódulos.
- **Es una línea, pero evita problemas futuros.**

### ¿Qué pasa si lo borras?
Gradle sigue funcionando, pero el nombre del proyecto será el nombre de la carpeta (`Serenity` en lugar de `restaurant-qa-automation`). Los reportes de Serenity mostrarán ese nombre.

---

## 3. [TestRunner.java](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Serenity/src/test/java/com/restaurant/qa/runners/TestRunner.java) — 🚀 El Botón de Encendido

### ¿Qué hace?
Es el **punto de entrada** que conecta tres tecnologías que normalmente no se conocen entre sí:

```
JUnit 5 Platform  ─→  Cucumber Engine  ─→  Serenity Reporter
```

### Analogía
Imagina que JUnit 5 es un **director de orquesta**, Cucumber es el **pianista**, y Serenity es el **ingeniero de sonido** que graba todo. [TestRunner.java](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Serenity/src/test/java/com/restaurant/qa/runners/TestRunner.java) es la **partitura** que le dice al director: "usa al pianista, y asegúrate de que el ingeniero esté grabando."

### Desglose línea por línea:

```java
@Suite                          // "Esto es una suite de pruebas JUnit 5"
@IncludeEngines("cucumber")     // "Usa el motor de Cucumber, no el de JUnit"
@SelectPackages("com.restaurant.qa")  // "Busca código en este paquete"

@ConfigurationParameter(
    key = GLUE_PROPERTY_NAME,
    value = "com.restaurant.qa.stepdefinitions")
    // "Los métodos @Given/@When/@Then están en este paquete"

@ConfigurationParameter(
    key = FEATURES_PROPERTY_NAME,
    value = "src/test/resources/features")
    // "Los archivos .feature están en esta carpeta"

@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "io.cucumber.core.plugin.SerenityReporterParallelPlugin")
    // "Envía todos los resultados al reporter de Serenity"

@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "not @wip")
    // "Ignora los escenarios marcados como trabajo en progreso"
```

### ¿Por qué la clase está vacía?
Porque **no es código ejecutable**. Es solo un **contenedor de anotaciones**. JUnit 5 Platform lee las anotaciones y actúa según ellas. El cuerpo de la clase es irrelevante.

### ¿Qué pasa si lo borras?
`gradle test` no encuentra ninguna prueba. Cucumber no sabe dónde buscar los [.feature](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Serenity/src/test/resources/features/order_service/HDU01_create_order.feature) files. El reporte de Serenity sale vacío.

---

## 4. [OrderStepDefinitions.java](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Serenity/src/test/java/com/restaurant/qa/stepdefinitions/OrderStepDefinitions.java) — ⚡ El Corazón de la Ejecución

### ¿Qué hace?
Es donde el **lenguaje humano (Gherkin)** se traduce en **instrucciones de máquina (Java)**. Cada línea que escribes en un archivo `.feature` tiene un método correspondiente aquí.

### ¿Por qué un Senior QA lo considera crítico?
- **Implementa la lógica de prueba:** Aquí es donde usas `SerenityRest` para hacer las llamadas a la API y `AssertJ` para validar los resultados.
- **Reusabilidad:** Un solo "Step Definition" puede ser usado por múltiples escenarios en diferentes archivos feature.
- **Abstracción:** El resto del equipo no necesita saber *cómo* se crea una orden, solo que el paso `Dado que el payload de la orden es válido` funciona.

### ¿Qué pasa si lo borras?
Cucumber encontrará los escenarios pero no sabrá qué hacer con ellos. Los reportes mostrarán los pasos como "Undefined" o "Pending".

---

## 5. [CucumberHooks.java](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Serenity/src/test/java/com/restaurant/qa/stepdefinitions/CucumberHooks.java) — ⚓ Ganchos de Configuración

### ¿Qué hace?
Contiene métodos anotados con `@Before` y `@After` que se ejecutan automáticamente antes y después de cada escenario.

### ¿Por qué existe?
- **Limpieza (Sandboxing):** Asegura que cada prueba empiece con un estado limpio.
- **Configuración dinámica:** Puede configurar variables de entorno o limpiar bases de datos antes de que la prueba comience.

---

## 6. [serenity.conf](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Serenity/src/test/resources/serenity.conf) — ⚙️ El Cerebro de Configuración

### ¿Qué hace?
Es el **archivo de configuración central** de Serenity BDD. Controla:
- Contra qué servidores se ejecutan las pruebas
- Cómo se generan los reportes
- Qué navegador usar (para pruebas de UI)
- Cuánto tiempo esperar antes de declarar un timeout

### Analogía
Si el proyecto fuera un carro, [serenity.conf](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Serenity/src/test/resources/serenity.conf) sería el **tablero de instrumentos**: velocímetro (timeouts), GPS (URLs de entorno), interruptor de luces (logging), y selector de modo (environment profiles).

### Las 7 secciones y su impacto:

| # | Sección | Pregunta que responde | Impacto si la configuras mal |
|---|---|---|---|
| 1 | **Metadatos** | "¿Cómo se llama este proyecto en los reportes?" | El reporte dice "Default Project" — nadie sabe qué proyecto es |
| 2 | **WebDriver** | "¿Qué navegador usar para pruebas de UI?" | Chrome se abre con ventana visible en CI, bloqueando el pipeline |
| 3 | **REST API** | "¿Cuál es la URL base de la API?" | Todas las pruebas apuntan a producción en lugar de staging 💀 |
| 4 | **Environments** | "¿Cómo cambio de local a staging a CI?" | Hardcodeas URLs y cuando cambias de entorno, tienes que editar 50 archivos |
| 5 | **Reportes** | "¿Qué detalle muestro en el reporte HTML?" | Los request/response HTTP no se logran — no puedes depurar fallos |
| 6 | **Cucumber** | "¿Dónde están mis .feature files?" | Cucumber no encuentra los escenarios, 0 pruebas ejecutadas |
| 7 | **Timeouts** | "¿Cuánto espero antes de fallar?" | Pruebas fallan por timeout contra servicios lentos, o esperan 5 min por cada fallo |

### La sección más poderosa: `environments`

```bash
# Mismo código, tres entornos diferentes:
gradle test                           # → apunta a localhost (Docker Compose)
gradle test -Denvironment=staging     # → apunta a staging
gradle test -Denvironment=ci          # → apunta a servicios Docker en CI
```

**Esto es oro para un Senior QA** porque significa que las mismas pruebas validan el mismo contrato en cada entorno. Si algo pasa en staging que no pasa en local, lo detectas sin cambiar una línea de código.

### ¿Qué pasa si lo borras?
Serenity usa valores por defecto que probablemente no son los correctos para tu proyecto. Las pruebas de API no saben a dónde apuntar. El reporte sale con un título genérico.

---

## 7. [cucumber.properties](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Serenity/src/test/resources/cucumber.properties) — 🔌 El Cable que Conecta Todo

### ¿Qué hace?
Es un **archivo de respaldo** que garantiza que la conexión Cucumber ↔ Serenity funcione **siempre**, sin importar cómo ejecutes las pruebas.

```properties
cucumber.plugin=io.cucumber.core.plugin.SerenityReporterParallelPlugin
cucumber.glue=com.restaurant.qa.stepdefinitions
cucumber.features=src/test/resources/features
```

### ¿Por qué existe si TestRunner.java ya tiene esa configuración?
Porque hay **dos caminos** para ejecutar pruebas:

| Camino | ¿Usa TestRunner.java? | ¿Usa cucumber.properties? |
|---|---|---|
| `gradle test` | ✅ Sí | También, como respaldo |
| Click derecho en IDE → "Run" en un [.feature](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Serenity/src/test/resources/features/order_service/HDU01_create_order.feature) | ❌ No | ✅ **Solo este** |
| `gradle test -Dcucumber.filter.tags="@smoke"` | ✅ Sí | También, como respaldo |

**Sin este archivo**, si un QA hace click derecho en un [.feature](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Serenity/src/test/resources/features/order_service/HDU01_create_order.feature) desde IntelliJ para ejecutarlo rápido, las pruebas corren **sin Serenity**. Los pasos se ejecutan pero no se genera reporte. El QA piensa que "funcionó" pero no hay evidencia.

### ¿Qué pasa si lo borras?
Las pruebas siguen corriendo vía `gradle test` porque [TestRunner.java](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Serenity/src/test/java/com/restaurant/qa/runners/TestRunner.java) tiene la misma configuración. Pero la ejecución directa desde el IDE pierde la integración con Serenity.

---

## 8. [logback-test.xml](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Serenity/src/test/resources/logback-test.xml) — 📋 El Filtro de Ruido

### ¿Qué hace?
Controla **qué mensajes de log aparecen en la consola** durante la ejecución de pruebas.

### ¿Por qué un Senior QA insiste en este archivo?

Sin él, la consola durante `gradle test` se ve así:
```
[DEBUG] net.serenitybdd.core.di.WebDriverInjectors - Injecting WebDriver...
[DEBUG] net.serenitybdd.core.steps.StepFactory - Creating step library...
[DEBUG] net.thucydides.core.steps.StepEventBus - Broadcasting step...
[INFO]  net.serenitybdd.rest.SerenityRest - POST http://localhost:8080/orders
[DEBUG] net.serenitybdd.core.reports.ReportService - Writing JSON report...
... (500+ líneas de ruido interno de Serenity)
```

Con [logback-test.xml](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Serenity/src/test/resources/logback-test.xml):
```
20:15:03.445 [main] DEBUG com.restaurant.qa - Creando orden para mesa 5
20:15:03.892 [main] INFO  com.restaurant.qa - POST /orders → 201 Created
20:15:04.001 [main] DEBUG com.restaurant.qa - Verificando header Location
```

### Las 3 reglas del archivo:

```xml
<!-- Serenity: solo WARN o peores → silencia el 95% del ruido -->
<logger name="net.serenitybdd" level="WARN"/>
<logger name="net.thucydides" level="WARN"/>

<!-- TU código: DEBUG completo → ves todo lo que necesitas -->
<logger name="com.restaurant.qa" level="DEBUG"/>
```

### ¿Qué pasa si lo borras?
Las pruebas siguen funcionando, pero la consola se inunda de logs internos de Serenity. Encontrar un error real entre cientos de líneas de debug se vuelve como buscar una aguja en un pajar.

---

## 9. [HDU01_create_order.feature](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Serenity/src/test/resources/features/order_service/HDU01_create_order.feature) — 📖 La Historia del Usuario

### ¿Qué hace?
Es un **escenario ejecutable** escrito en Gherkin que describe el comportamiento esperado de la API en lenguaje natural. Es simultáneamente:
1. **Documentación** — cualquier persona del equipo puede leerlo
2. **Especificación** — define exactamente qué debe pasar
3. **Prueba automatizada** — Cucumber lo ejecuta paso por paso

### Desglose:

```gherkin
# language: es                              ← Gherkin en español
@HDU-01 @smoke @regression                  ← Etiquetas para filtrado y trazabilidad

Feature: Creación de recursos con semántica HTTP correcta
  # ↑ Título que aparece en el reporte de Serenity

  Background:                                ← Se ejecuta ANTES de cada Scenario
    Given que el catálogo de productos contiene al menos un producto activo con id 1
    And que la mesa 5 se encuentra disponible

  @smoke                                     ← Este escenario también es "smoke test"
  Scenario: Crear una orden exitosamente retorna 201 con Location
    Given que el payload de la orden es válido con tableId 5 y productId 1...
    When se envía una solicitud POST a "/orders"
    Then la respuesta debe tener código 201
    And debe incluir el header "Location" con el patrón "/orders/{uuid}"
    And el campo "status" debe ser "PENDING"
```

### Las etiquetas y su poder:

| Etiqueta | `gradle smoke` | `gradle regression` | `gradle hdu -Ptag=HDU-01` |
|---|---|---|---|
| `@HDU-01` | ❌ No ejecuta | ✅ Ejecuta | ✅ Ejecuta |
| `@smoke` | ✅ Ejecuta | ✅ Ejecuta | ✅ Ejecuta |
| `@regression` | ❌ No ejecuta | ✅ Ejecuta | ✅ Ejecuta |
| `@wip` | ❌ No ejecuta | ❌ Excluido | ❌ Excluido |

### ¿Qué pasa si lo borras?
No tienes pruebas para la HDU-01. El reporte de Serenity muestra "0 escenarios" para esa feature. La cobertura de requisitos baja.

---

## 🔄 Flujo Completo: ¿Cómo Trabajan Juntos?

```
  gradle test
      │
      ▼
  build.gradle          → "Usa JUnit 5, descarga Serenity 4.2.12"
      │
      ▼
  TestRunner.java       → "Lanza Cucumber, busca features en src/test/resources/"
      │
      ▼
  *.feature files       → "Hay 3 escenarios en HDU01_create_order.feature"
      │
      ▼
  cucumber.properties   → "Envía todo al SerenityReporterParallelPlugin"
      │
      ▼
  serenity.conf         → "La API está en localhost:8080, timeout: 10s"
      │
      ▼
  logback-test.xml      → "Muestra solo logs de com.restaurant.qa"
      │
      ▼
  Step Definitions      → "Ejecuta POST /orders, valida 201"
      │
      ▼
  Serenity Aggregate    → "Genera reporte en target/site/serenity/"
      │
      ▼
  📊 index.html         → "Documentación viva con detalle de cada paso"
```

---

## 📊 Resumen: ¿Cuáles son los más críticos?

| Archivo | Criticidad | Si lo borras... |
|---|---|---|
| **build.gradle** | 🔴 Crítico | Nada compila ni ejecuta |
| **serenity.conf** | 🔴 Crítico | Pruebas no saben a dónde apuntar |
| **TestRunner.java** | 🔴 Crítico | `gradle test` no encuentra pruebas |
| **StepDefinitions** | 🔴 Crítico | Escenarios Gherkin no hacen nada |
| **cucumber.properties** | 🟡 Alto | Ejecución desde IDE pierde reportes |
| **settings.gradle** | 🟢 Bajo | Solo afecta el nombre del proyecto |
| **logback-test.xml** | 🟢 Bajo | Solo afecta legibilidad de consola |
| **\*.feature** | 🔴 Crítico | Sin features, no hay pruebas |

---

> **Consejo de Senior QA:** Estos 9 componentes son el **esqueleto funcional** actual. Un proyecto maduro evolucionaría hacia el Patrón Screenplay completo con Tasks y Questions, pero con estos cimientos ya tienes una automatización robusta y profesional.
