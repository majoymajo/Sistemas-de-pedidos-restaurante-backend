# 🏛️ Guion de Presentación: Arquitectura por Capas del Proyecto Serenity

---


## Introducción

> *"Quiero presentarles cómo organizamos el código de automatización.
> Lo dividimos en **6 capas con responsabilidades únicas**. La idea es simple:
> cada carpeta tiene su trabajo y no se mete con el trabajo de las otras.
> Esto hace que el framework sea mantenible, escalable y entendible para cualquier
> miembro del equipo, incluso si llega alguien nuevo mañana."*

---

## 📦 CAPA 1 — `runners/` (El Punto de Arranque)
**Archivo:** [TestRunner.java](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Sistemas-de-pedidos-restaurante-backend-develop/Serenity/src/test/java/com/restaurant/qa/runners/TestRunner.java)

### ¿Qué hace?
Es la única puerta de entrada a toda la suite. Le dice a JUnit 5:
- **Dónde están los features** (archivos Gherkin)
- **Dónde está el glue code** (step definitions)
- **Qué plugin de reporte usar** (SerenityReporter)
- **Qué tests ignorar** (los marcados `@wip`)

### Código real:
```java
@Suite
@IncludeEngines("cucumber")
@SelectPackages("com.restaurant.qa")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME,    value = "com.restaurant.qa.stepdefinitions")
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/resources/features")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME,   value = "io.cucumber.core.plugin.SerenityReporter")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @wip")
public class TestRunner {
    // Intencionalmente vacía — JUnit 5 la usa como portador de config
}
```

---

## 📦 CAPA 2 — `stepdefinitions/` (El Traductor)
**Archivos:** [OrderStepDefinitions.java](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Sistemas-de-pedidos-restaurante-backend-develop/Serenity/src/test/java/com/restaurant/qa/stepdefinitions/OrderStepDefinitions.java) + [CucumberHooks.java](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Sistemas-de-pedidos-restaurante-backend-develop/Serenity/src/test/java/com/restaurant/qa/stepdefinitions/CucumberHooks.java)

### ¿Qué hace?
Estos archivos son el **puente** entre el lenguaje de negocio (Gherkin en español)
y el código Java. Pero la regla de oro es: **aquí NO vive lógica técnica**.
Los steps solo delegan. El código real está en `actions/`.

### Código real — un step "delgado":
```java
// El step solo DELEGA. No sabe nada de HTTP.
@Cuando("se envía una solicitud POST a {string}")
public void seEnviaUnaSolicitudPOSTA(String endpoint) {
    response = orderActions.createOrder(orderPayload);  // ← delega a actions
}

// Lo que se construye en Given también delega a data/
@Dado("que el payload de la orden es válido con tableId {int} y productId {int} con quantity {int}")
public void queElPayloadDeLaOrdenEsValido(int tableId, int productId, int quantity) {
    orderPayload = OrderDataBuilder.aValidOrder()
            .forTable(tableId)
            .withItem(productId, quantity)
            .build();  // ← delega a data/
}
```

### [CucumberHooks.java](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Sistemas-de-pedidos-restaurante-backend-develop/Serenity/src/test/java/com/restaurant/qa/stepdefinitions/CucumberHooks.java) — Gestión del ciclo de vida:
```java
@BeforeAll  // Una sola vez al iniciar la suite
public static void beforeAllScenarios() {
    SerenityRest.enableLoggingOfRequestAndResponseIfValidationFails();
}

@Before     // Antes de CADA escenario
public void beforeEachScenario() {
    SerenityRest.reset();  // Limpia estado residual entre escenarios
}
```

---

## 📦 CAPA 3 — `actions/` (El Motor de Ejecución)
**Archivos:** [OrderActions.java](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Sistemas-de-pedidos-restaurante-backend-develop/Serenity/src/test/java/com/restaurant/qa/actions/OrderActions.java), [ProductActions.java](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Sistemas-de-pedidos-restaurante-backend-develop/Serenity/src/test/java/com/restaurant/qa/actions/ProductActions.java), [ValidationActions.java](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Sistemas-de-pedidos-restaurante-backend-develop/Serenity/src/test/java/com/restaurant/qa/actions/ValidationActions.java)

### ¿Qué hace?
Aquí vive toda la **lógica de interacción con la API**. Cada método está
anotado con `@Step`, lo que hace que Serenity lo registre automáticamente
en el reporte HTML con su descripción en lenguaje humano.

### Código real — [OrderActions.java](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Sistemas-de-pedidos-restaurante-backend-develop/Serenity/src/test/java/com/restaurant/qa/actions/OrderActions.java):
```java
@Step("Crear nueva orden con payload: {0}")
public Response createOrder(OrderRequest orderRequest) {
    lastResponse = SerenityRest.given()
            .baseUri(TestEnvironment.getBaseUrl())   // ← URL desde config/
            .contentType(TestConstants.CONTENT_TYPE_JSON) // ← constante desde config/
            .body(orderRequest)                       // ← modelo desde models/
            .when()
            .post(TestConstants.ORDERS_ENDPOINT);
    
    locationHeader = lastResponse.getHeader(TestConstants.HEADER_LOCATION);
    return lastResponse;
}

// Soporte para endpoints protegidos con token
@Step("Actualizar estado de orden {0} a {1}")
public Response updateOrderStatus(String orderId, String newStatus) {
    return SerenityRest.given()
            .header(TestEnvironment.getKitchenTokenHeader(), TestEnvironment.getKitchenToken())
            ...
}
```

### Código real — [ValidationActions.java](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Sistemas-de-pedidos-restaurante-backend-develop/Serenity/src/test/java/com/restaurant/qa/actions/ValidationActions.java) (reutilizable en TODAS las pruebas):
```java
@Step("Verificar que el código de respuesta es {1}")
public void verifyStatusCode(Response response, int expectedCode) {
    assertThat(response.statusCode())
            .as("El código de respuesta debe ser %d", expectedCode)
            .isEqualTo(expectedCode);
}

// Valida patrones dinámicos como UUIDs en headers
@Step("Verificar que el header {1} coincide con el patrón '{2}'")
public void verifyHeaderMatchesPattern(Response response, String headerName, String pattern) {
    String regex = pattern
            .replace("{uuid}", "[0-9a-fA-F-]{36}")  // Soporte para UUIDs
            .replace("{id}", "\\d+");
    assertThat(headerValue).matches(".*" + regex + ".*");
}
```

---

## 📦 CAPA 4 — `data/` (La Fábrica de Datos de Prueba)
**Archivo:** [OrderDataBuilder.java](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Sistemas-de-pedidos-restaurante-backend-develop/Serenity/src/test/java/com/restaurant/qa/data/OrderDataBuilder.java)

### ¿Qué hace?
Implementa el **patrón Builder** para crear datos de prueba de forma expresiva
y reutilizable. Evita tener JSONs quemados o IDs hardcodeados dispersos por el código.

### Código real:
```java
// Métodos de fábrica — escenarios predefinidos
public static OrderDataBuilder aValidOrder()            { ... } // mesa 5, producto 1
public static OrderDataBuilder anInvalidTableOrder()    { ... } // mesa 0 (inválida)
public static OrderDataBuilder anEmptyOrder()           { ... } // sin productos
public static OrderDataBuilder anOrderWithInvalidProduct() { ... } // producto 99999

// API fluida — se lee como inglés
orderPayload = OrderDataBuilder.aValidOrder()
        .forTable(5)          // "para la mesa 5"
        .withItem(1, 2)       // "con 2 unidades del producto 1"
        .build();

// Soporte para Boundary Value Analysis
public OrderDataBuilder forMinTable()     { this.tableId = TestConstants.MIN_TABLE_ID; }     // mesa 1
public OrderDataBuilder forMaxTable()     { this.tableId = TestConstants.MAX_TABLE_ID; }     // mesa 12
public OrderDataBuilder forTableAboveMax(){ this.tableId = TestConstants.MAX_TABLE_ID + 1; } // mesa 13 → error
```


## 📦 CAPA 5 — `config/` (La Fuente de Verdad)
**Archivos:** [TestConstants.java](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Sistemas-de-pedidos-restaurante-backend-develop/Serenity/src/test/java/com/restaurant/qa/config/TestConstants.java) + [TestEnvironment.java](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Sistemas-de-pedidos-restaurante-backend-develop/Serenity/src/test/java/com/restaurant/qa/config/TestEnvironment.java)

### ¿Qué hace?
Centraliza **dos tipos de información**:
1. [TestConstants](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Sistemas-de-pedidos-restaurante-backend-develop/Serenity/src/test/java/com/restaurant/qa/config/TestConstants.java#7-50) → valores fijos de negocio (endpoints, status codes, reglas de mesa)
2. [TestEnvironment](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Sistemas-de-pedidos-restaurante-backend-develop/Serenity/src/test/java/com/restaurant/qa/config/TestEnvironment.java#12-97) → valores dinámicos por ambiente (URLs, tokens, flags de CI)

### Código real — [TestConstants.java](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Sistemas-de-pedidos-restaurante-backend-develop/Serenity/src/test/java/com/restaurant/qa/config/TestConstants.java):
```java
// Endpoints — si el backend cambia la ruta, solo se modifica AQUÍ
public static final String ORDERS_ENDPOINT   = "/orders";
public static final String PRODUCTS_ENDPOINT = "/products";

// Reglas de negocio — documentación ejecutable
public static final int MIN_TABLE_ID = 1;
public static final int MAX_TABLE_ID = 12;

// Estados del dominio
public static final String STATUS_PENDING        = "PENDING";
public static final String STATUS_IN_PREPARATION = "IN_PREPARATION";
public static final String STATUS_READY          = "READY";
```

### Código real — [TestEnvironment.java](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Sistemas-de-pedidos-restaurante-backend-develop/Serenity/src/test/java/com/restaurant/qa/config/TestEnvironment.java):
```java
// Lee el environment activo desde la línea de comandos
// Uso: gradle test -Denvironment=staging
activeEnvironment = System.getProperty("environment", "default");

// Carga la configuración del ambiente desde serenity.conf
Config baseConfig = ConfigFactory.load("serenity");

// La URL base nunca está quemada en el código Java
public static String getBaseUrl() {
    return getProperty("restapi.baseurl", "http://localhost:8080");
}

// Soporte para autenticación por ambiente
public static String getKitchenToken() {
    return getProperty("kitchen.api.token", "");
}
```

---

## 📦 CAPA 6 — `models/` (El Contrato del API)
**Archivos:** [OrderRequest.java](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Sistemas-de-pedidos-restaurante-backend-develop/Serenity/src/test/java/com/restaurant/qa/models/OrderRequest.java), [OrderResponse.java](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Sistemas-de-pedidos-restaurante-backend-develop/Serenity/src/test/java/com/restaurant/qa/models/OrderResponse.java), [OrderItem.java](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Sistemas-de-pedidos-restaurante-backend-develop/Serenity/src/test/java/com/restaurant/qa/models/OrderItem.java), [ErrorResponse.java](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Sistemas-de-pedidos-restaurante-backend-develop/Serenity/src/test/java/com/restaurant/qa/models/ErrorResponse.java)

### ¿Qué hace?
Son los **DTOs (Data Transfer Objects)** que representan exactamente el contrato
JSON del API del backend. Con Lombok eliminamos el boilerplate, y con
`@JsonIgnoreProperties` garantizamos resiliencia ante cambios no disruptivos del backend.

### Código real — [OrderRequest.java](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Sistemas-de-pedidos-restaurante-backend-develop/Serenity/src/test/java/com/restaurant/qa/models/OrderRequest.java) (lo que ENVIAMOS):
```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderRequest {
    private Integer tableId;
    private List<OrderItem> items;
    // Lombok genera: getters, setters, equals, hashCode, toString, constructor
}
```

### Código real — [OrderResponse.java](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Sistemas-de-pedidos-restaurante-backend-develop/Serenity/src/test/java/com/restaurant/qa/models/OrderResponse.java) (lo que RECIBIMOS):
```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)  // ← Clave: resiliencia ante cambios
public class OrderResponse {
    private String  id;
    private Integer tableId;
    private String  status;
    private List<OrderItemResponse> items;
    private String  createdAt;
    private String  updatedAt;

    // Clase interna para cada ítem de la respuesta
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderItemResponse {
        private Long    id;
        private Integer productId;
        private String  productName;
        private Integer quantity;
        private Double  unitPrice;
        private Double  subtotal;
    }
}
```

---

## 🎯 Cierre Final

> *"Para resumir:
> - **`runners`** → Enciende el motor
> - **`stepdefinitions`** → Traduce negocio a código
> - **`actions`** → Ejecuta y valida llamadas HTTP
> - **`data`** → Construye los datos de forma expresiva
> - **`config`** → Centraliza URLs, tokens y constantes del dominio
> - **`models`** → Modela el contrato del API
>
> Ninguna capa sabe más de lo que necesita. Si el día de mañana el backend
> cambia un endpoint, solo actualizamos [TestConstants](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Sistemas-de-pedidos-restaurante-backend-develop/Serenity/src/test/java/com/restaurant/qa/config/TestConstants.java#7-50). Si cambia el servidor,
> solo actualizamos [serenity.conf](file:///c:/Users/usuario/iCloudDrive/SOFKA/Retos%20Marzo/Restaurants/Sistemas-de-pedidos-restaurante-backend-develop/Serenity/src/test/resources/serenity.conf). Si cambia el JSON, solo actualizamos el modelo.
> **Un cambio en un lugar, cero impacto en los demás.** Así es como QA escala."*
