package com.restaurant.qa.actions;

import com.restaurant.qa.config.TestEnvironment;
import io.restassured.response.Response;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.rest.SerenityRest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Acciones de API para el catálogo de productos.
 * Utilizado para verificar precondiciones en pruebas de órdenes.
 */
public class ProductActions {

    private static final Logger logger = LoggerFactory.getLogger(ProductActions.class);

    private Response lastResponse;

    @Step("Verificar que el producto {0} existe y está activo")
    public boolean verifyProductExistsAndActive(int productId) {
        logger.info("Verificando producto con ID: {}", productId);
        
        lastResponse = SerenityRest.given()
                .baseUri(TestEnvironment.getBaseUrl())
                .when()
                .get("/products/" + productId);
        
        if (lastResponse.statusCode() == 200) {
            boolean isActive = lastResponse.jsonPath().getBoolean("active");
            logger.info("Producto {} encontrado, activo: {}", productId, isActive);
            return isActive;
        }
        
        logger.warn("Producto {} no encontrado (status: {})", productId, lastResponse.statusCode());
        return false;
    }

    @Step("Obtener producto por ID: {0}")
    public Response getProductById(int productId) {
        logger.info("Obteniendo producto con ID: {}", productId);
        
        lastResponse = SerenityRest.given()
                .baseUri(TestEnvironment.getBaseUrl())
                .when()
                .get("/products/" + productId);
        
        return lastResponse;
    }

    @Step("Listar todos los productos")
    public Response getAllProducts() {
        logger.info("Listando todos los productos");
        
        lastResponse = SerenityRest.given()
                .baseUri(TestEnvironment.getBaseUrl())
                .when()
                .get("/products");
        
        return lastResponse;
    }

    public Response getLastResponse() {
        return lastResponse;
    }
}
