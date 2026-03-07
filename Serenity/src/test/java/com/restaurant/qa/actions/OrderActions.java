package com.restaurant.qa.actions;

import com.restaurant.qa.config.TestConstants;
import com.restaurant.qa.config.TestEnvironment;
import com.restaurant.qa.models.OrderRequest;
import io.restassured.response.Response;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.rest.SerenityRest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Acciones de API para el servicio de órdenes.
 * Cada método está anotado con @Step para generar reportes detallados en Serenity.
 */
public class OrderActions {

    private static final Logger logger = LoggerFactory.getLogger(OrderActions.class);

    private Response lastResponse;
    private String locationHeader;

    // ========================================================================
    // POST Actions
    // ========================================================================

    @Step("Crear nueva orden con payload: {0}")
    public Response createOrder(OrderRequest orderRequest) {
        logger.info("Creando orden para mesa {}", orderRequest.getTableId());
        
        lastResponse = SerenityRest.given()
                .baseUri(TestEnvironment.getBaseUrl())
                .contentType(TestConstants.CONTENT_TYPE_JSON)
                .body(orderRequest)
                .when()
                .post(TestConstants.ORDERS_ENDPOINT);
        
        // Capturar Location header si existe
        locationHeader = lastResponse.getHeader(TestConstants.HEADER_LOCATION);
        
        logger.info("Respuesta recibida con código: {}", lastResponse.statusCode());
        return lastResponse;
    }

    // ========================================================================
    // GET Actions
    // ========================================================================

    @Step("Obtener orden por ID: {0}")
    public Response getOrderById(String orderId) {
        logger.info("Obteniendo orden con ID: {}", orderId);
        
        lastResponse = SerenityRest.given()
                .baseUri(TestEnvironment.getBaseUrl())
                .when()
                .get(TestConstants.ORDERS_ENDPOINT + "/" + orderId);
        
        return lastResponse;
    }

    @Step("Obtener orden desde URL del header Location")
    public Response getOrderFromLocationHeader() {
        assertThat(locationHeader)
                .as("El header Location debe existir")
                .isNotNull();
        
        String url = locationHeader;
        if (!url.startsWith("http")) {
            url = TestEnvironment.getBaseUrl() + url;
        }
        
        logger.info("Obteniendo recurso desde Location: {}", url);
        
        lastResponse = SerenityRest.given()
                .when()
                .get(url);
        
        return lastResponse;
    }

    @Step("Obtener recurso desde URL: {0}")
    public Response getOrderByUrl(String url) {
        logger.info("Obteniendo recurso desde URL: {}", url);
        
        lastResponse = SerenityRest.given()
                .when()
                .get(url);
        
        return lastResponse;
    }

    @Step("Listar todas las órdenes")
    public Response getAllOrders() {
        logger.info("Listando todas las órdenes");
        
        lastResponse = SerenityRest.given()
                .baseUri(TestEnvironment.getBaseUrl())
                .when()
                .get(TestConstants.ORDERS_ENDPOINT);
        
        return lastResponse;
    }

    // ========================================================================
    // PATCH Actions (Protected)
    // ========================================================================

    @Step("Actualizar estado de orden {0} a {1}")
    public Response updateOrderStatus(String orderId, String newStatus) {
        logger.info("Actualizando orden {} a estado {}", orderId, newStatus);
        
        lastResponse = SerenityRest.given()
                .baseUri(TestEnvironment.getBaseUrl())
                .header(TestEnvironment.getKitchenTokenHeader(), TestEnvironment.getKitchenToken())
                .contentType(TestConstants.CONTENT_TYPE_JSON)
                .body("{\"status\": \"" + newStatus + "\"}")
                .when()
                .patch(TestConstants.ORDERS_ENDPOINT + "/" + orderId + "/status");
        
        return lastResponse;
    }

    // ========================================================================
    // DELETE Actions (Protected)
    // ========================================================================

    @Step("Eliminar orden {0}")
    public Response deleteOrder(String orderId) {
        logger.info("Eliminando orden: {}", orderId);
        
        lastResponse = SerenityRest.given()
                .baseUri(TestEnvironment.getBaseUrl())
                .header(TestEnvironment.getKitchenTokenHeader(), TestEnvironment.getKitchenToken())
                .when()
                .delete(TestConstants.ORDERS_ENDPOINT + "/" + orderId);
        
        return lastResponse;
    }

    // ========================================================================
    // Getters
    // ========================================================================

    public Response getLastResponse() {
        return lastResponse;
    }

    public String getLocationHeader() {
        return locationHeader;
    }
}
