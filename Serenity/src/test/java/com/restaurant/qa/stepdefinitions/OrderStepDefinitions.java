package com.restaurant.qa.stepdefinitions;

import com.restaurant.qa.actions.OrderActions;
import com.restaurant.qa.actions.ProductActions;
import com.restaurant.qa.actions.ValidationActions;
import com.restaurant.qa.config.TestConstants;
import com.restaurant.qa.config.TestEnvironment;
import com.restaurant.qa.data.OrderDataBuilder;
import com.restaurant.qa.models.OrderRequest;
import com.restaurant.qa.models.OrderResponse;

import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import io.restassured.response.Response;
import net.serenitybdd.annotations.Steps;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step Definitions para la creación de órdenes (HDU-01).
 * Implementa los pasos Gherkin en español para pruebas de API REST.
 * Delega a clases de acciones para mantener los steps delgados.
 */
public class OrderStepDefinitions {

    @Steps
    private OrderActions orderActions;
    
    @Steps
    private ProductActions productActions;
    
    @Steps
    private ValidationActions validationActions;
    
    private OrderRequest orderPayload;
    private Response response;
    private OrderResponse createdOrder;

    // =====================================================================
    // Background Steps
    // =====================================================================

    @Dado("que el catálogo de productos contiene al menos un producto activo con id {int}")
    public void queElCatalogoDeProductosContieneAlMenosUnProductoActivoConId(int productId) {
        productActions.verifyProductExistsAndActive(productId);
    }

    @Dado("que la mesa {int} se encuentra disponible")
    public void queLaMesaSeEncuentraDisponible(int tableId) {
        assertThat(tableId)
                .as("El tableId debe estar entre %d y %d", 
                    TestConstants.MIN_TABLE_ID, TestConstants.MAX_TABLE_ID)
                .isBetween(TestConstants.MIN_TABLE_ID, TestConstants.MAX_TABLE_ID);
    }

    // =====================================================================
    // Given Steps - Preparación de payloads
    // =====================================================================

    @Dado("que el payload de la orden es válido con tableId {int} y productId {int} con quantity {int}")
    public void queElPayloadDeLaOrdenEsValidoConTableIdYProductIdConQuantity(
            int tableId, int productId, int quantity) {
        
        orderPayload = OrderDataBuilder.aValidOrder()
                .forTable(tableId)
                .withItem(productId, quantity)
                .build();
    }

    @Dado("que se creó una orden exitosamente vía POST a {string}")
    public void queSeCreoUnaOrdenExitosamenteViaPOSTA(String endpoint) {
        // Crear una orden válida primero
        orderPayload = OrderDataBuilder.aValidOrder()
                .forTable(5)
                .withItem(1, 2)
                .build();
        
        response = orderActions.createOrder(orderPayload);
        
        validationActions.verifyStatusCode(response, 201);
        
        createdOrder = response.as(OrderResponse.class);
    }

    @Dado("que el payload contiene un tableId inválido de {int}")
    public void queElPayloadContieneUnTableIdInvalidoDe(int invalidTableId) {
        orderPayload = OrderDataBuilder.anInvalidTableOrder()
                .forTable(invalidTableId)
                .build();
    }

    // =====================================================================
    // When Steps - Ejecución de solicitudes HTTP
    // =====================================================================

    @Cuando("se envía una solicitud POST a {string}")
    public void seEnviaUnaSolicitudPOSTA(String endpoint) {
        response = orderActions.createOrder(orderPayload);
    }

    @Cuando("se envía una solicitud GET a la URL del header {string}")
    public void seEnviaUnaSolicitudGETALaURLDelHeader(String headerName) {
        validationActions.verifyHeaderExists(response, headerName);
        // Obtener la URL del Location header y hacer GET
        String url = response.getHeader(headerName);
        if (!url.startsWith("http")) {
            url = TestEnvironment.getBaseUrl() + url;
        }
        response = orderActions.getOrderByUrl(url);
    }

    // =====================================================================
    // Then Steps - Validaciones
    // =====================================================================

    @Entonces("la respuesta debe tener código {int}")
    public void laRespuestaDebeTenerCodigo(int expectedStatusCode) {
        validationActions.verifyStatusCode(response, expectedStatusCode);
    }

    @Y("debe incluir el header {string} con el patrón {string}")
    public void debeIncluirElHeaderConElPatron(String headerName, String pattern) {
        validationActions.verifyHeaderMatchesPattern(response, headerName, pattern);
    }

    @Y("el campo {string} debe ser {string}")
    public void elCampoDebeSer(String fieldName, String expectedValue) {
        validationActions.verifyFieldEquals(response, fieldName, expectedValue);
    }

    @Y("el header {string} debe ser {string}")
    public void elHeaderDebeSer(String headerName, String expectedValue) {
        String headerValue = response.getHeader(headerName);
        assertThat(headerValue)
                .as("El header %s debe contener %s", headerName, expectedValue)
                .contains(expectedValue);
    }

    @Y("el cuerpo debe contener la misma orden creada previamente")
    public void elCuerpoDebeContenerLaMismaOrdenCreadaPreviamente() {
        String orderId = response.jsonPath().getString("id");
        String previousOrderId = createdOrder != null ? 
                createdOrder.getId() : null;
        
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
        validationActions.verifyErrorResponseStructure(response);
    }
}
