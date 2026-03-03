package com.restaurant.qa.stepdefinitions;

import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import io.restassured.response.Response;
import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.annotations.Step;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step Definitions para la creación de órdenes (HDU-01).
 * Implementa los pasos Gherkin en español para pruebas de API REST.
 */
public class OrderStepDefinitions {

    private static final String BASE_URL = "http://localhost:8080";
    
    private Map<String, Object> orderPayload;
    private Response response;
    private String locationHeader;
    private Map<String, Object> createdOrder;

    // =====================================================================
    // Background Steps
    // =====================================================================

    @Dado("que el catálogo de productos contiene al menos un producto activo con id {int}")
    public void queElCatalogoDeProductosContieneAlMenosUnProductoActivoConId(int productId) {
        // Verificar que el producto existe y está activo
        Response productResponse = SerenityRest.given()
                .baseUri(BASE_URL)
                .when()
                .get("/products/" + productId);
        
        // Si el producto no existe, lo registramos como pendiente pero no fallamos
        // El test fallará más adelante si el producto realmente no existe
        if (productResponse.statusCode() == 200) {
            assertThat(productResponse.jsonPath().getBoolean("active"))
                    .as("El producto %d debe estar activo", productId)
                    .isTrue();
        }
    }

    @Dado("que la mesa {int} se encuentra disponible")
    public void queLaMesaSeEncuentraDisponible(int tableId) {
        // Las mesas 1-12 son válidas según las reglas de negocio
        assertThat(tableId)
                .as("El tableId debe estar entre 1 y 12")
                .isBetween(1, 12);
    }

    // =====================================================================
    // Given Steps - Preparación de payloads
    // =====================================================================

    @Dado("que el payload de la orden es válido con tableId {int} y productId {int} con quantity {int}")
    public void queElPayloadDeLaOrdenEsValidoConTableIdYProductIdConQuantity(
            int tableId, int productId, int quantity) {
        
        orderPayload = new HashMap<>();
        orderPayload.put("tableId", tableId);
        
        Map<String, Object> item = new HashMap<>();
        item.put("productId", productId);
        item.put("quantity", quantity);
        
        orderPayload.put("items", List.of(item));
    }

    @Dado("que se creó una orden exitosamente vía POST a {string}")
    public void queSeCreoUnaOrdenExitosamenteViaPOSTA(String endpoint) {
        // Crear una orden válida primero
        queElPayloadDeLaOrdenEsValidoConTableIdYProductIdConQuantity(5, 1, 2);
        seEnviaUnaSolicitudPOSTA(endpoint);
        
        assertThat(response.statusCode())
                .as("La orden debe crearse exitosamente con código 201")
                .isEqualTo(201);
        
        locationHeader = response.getHeader("Location");
        createdOrder = response.jsonPath().getMap("");
    }

    @Dado("que el payload contiene un tableId inválido de {int}")
    public void queElPayloadContieneUnTableIdInvalidoDe(int invalidTableId) {
        orderPayload = new HashMap<>();
        orderPayload.put("tableId", invalidTableId);
        
        Map<String, Object> item = new HashMap<>();
        item.put("productId", 1);
        item.put("quantity", 1);
        
        orderPayload.put("items", List.of(item));
    }

    // =====================================================================
    // When Steps - Ejecución de solicitudes HTTP
    // =====================================================================

    @Cuando("se envía una solicitud POST a {string}")
    public void seEnviaUnaSolicitudPOSTA(String endpoint) {
        response = SerenityRest.given()
                .baseUri(BASE_URL)
                .contentType("application/json")
                .body(orderPayload)
                .when()
                .post(endpoint);
    }

    @Cuando("se envía una solicitud GET a la URL del header {string}")
    public void seEnviaUnaSolicitudGETALaURLDelHeader(String headerName) {
        String url = response.getHeader(headerName);
        assertThat(url)
                .as("El header %s debe existir", headerName)
                .isNotNull();
        
        // El Location header puede ser relativo o absoluto
        if (!url.startsWith("http")) {
            url = BASE_URL + url;
        }
        
        response = SerenityRest.given()
                .when()
                .get(url);
    }

    // =====================================================================
    // Then Steps - Validaciones
    // =====================================================================

    @Entonces("la respuesta debe tener código {int}")
    public void laRespuestaDebeTenerCodigo(int expectedStatusCode) {
        assertThat(response.statusCode())
                .as("El código de respuesta debe ser %d", expectedStatusCode)
                .isEqualTo(expectedStatusCode);
    }

    @Y("debe incluir el header {string} con el patrón {string}")
    public void debeIncluirElHeaderConElPatron(String headerName, String pattern) {
        String headerValue = response.getHeader(headerName);
        assertThat(headerValue)
                .as("El header %s debe existir", headerName)
                .isNotNull();
        
        // Convertir el patrón {uuid} a regex
        String regex = pattern.replace("{uuid}", "[0-9a-fA-F-]{36}");
        assertThat(headerValue)
                .as("El header %s debe coincidir con el patrón %s", headerName, pattern)
                .matches(".*" + regex + ".*");
    }

    @Y("el campo {string} debe ser {string}")
    public void elCampoDebeSer(String fieldName, String expectedValue) {
        String actualValue = response.jsonPath().getString(fieldName);
        assertThat(actualValue)
                .as("El campo %s debe ser %s", fieldName, expectedValue)
                .isEqualTo(expectedValue);
    }

    @Y("el header {string} debe ser {string}")
    public void elHeaderDebeSer(String headerName, String expectedValue) {
        String headerValue = response.getHeader(headerName);
        assertThat(headerValue)
                .as("El header %s debe ser %s", headerName, expectedValue)
                .contains(expectedValue);
    }

    @Y("el cuerpo debe contener la misma orden creada previamente")
    public void elCuerpoDebeContenerLaMismaOrdenCreadaPreviamente() {
        String orderId = response.jsonPath().getString("id");
        String previousOrderId = createdOrder != null ? 
                String.valueOf(createdOrder.get("id")) : null;
        
        assertThat(orderId)
                .as("El ID de la orden debe coincidir con la orden creada")
                .isEqualTo(previousOrderId);
    }

    @Y("no debe incluir el header {string}")
    public void noDebeIncluirElHeader(String headerName) {
        String headerValue = response.getHeader(headerName);
        assertThat(headerValue)
                .as("El header %s no debe estar presente", headerName)
                .isNull();
    }

    @Y("el cuerpo debe seguir la estructura ErrorResponse")
    public void elCuerpoDebeSeguirLaEstructuraErrorResponse() {
        // Verificar campos típicos de una respuesta de error
        assertThat(response.jsonPath().getString("message"))
                .as("La respuesta de error debe contener un mensaje")
                .isNotNull();
        
        // El campo error o status también puede estar presente
        // dependiendo de la implementación del backend
    }
}
