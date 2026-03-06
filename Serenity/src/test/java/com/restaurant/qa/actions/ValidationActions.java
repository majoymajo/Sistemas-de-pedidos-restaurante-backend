package com.restaurant.qa.actions;

import com.restaurant.qa.config.TestConstants;
import io.restassured.response.Response;
import net.serenitybdd.annotations.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Acciones de validación reutilizables para verificar respuestas HTTP.
 * Cada método está anotado con @Step para reportes detallados.
 */
public class ValidationActions {

    private static final Logger logger = LoggerFactory.getLogger(ValidationActions.class);

    // ========================================================================
    // Status Code Validations
    // ========================================================================

    @Step("Verificar que el código de respuesta es {1}")
    public void verifyStatusCode(Response response, int expectedCode) {
        logger.info("Verificando código de respuesta: esperado={}, actual={}", 
                expectedCode, response.statusCode());
        
        assertThat(response.statusCode())
                .as("El código de respuesta debe ser %d", expectedCode)
                .isEqualTo(expectedCode);
    }

    @Step("Verificar respuesta exitosa (2xx)")
    public void verifySuccessResponse(Response response) {
        assertThat(response.statusCode())
                .as("La respuesta debe ser exitosa (2xx)")
                .isBetween(200, 299);
    }

    @Step("Verificar respuesta de error cliente (4xx)")
    public void verifyClientErrorResponse(Response response) {
        assertThat(response.statusCode())
                .as("La respuesta debe ser error de cliente (4xx)")
                .isBetween(400, 499);
    }

    // ========================================================================
    // Header Validations
    // ========================================================================

    @Step("Verificar que el header {1} existe")
    public void verifyHeaderExists(Response response, String headerName) {
        String headerValue = response.getHeader(headerName);
        
        assertThat(headerValue)
                .as("El header %s debe existir", headerName)
                .isNotNull();
    }

    @Step("Verificar que el header {1} no existe")
    public void verifyHeaderDoesNotExist(Response response, String headerName) {
        String headerValue = response.getHeader(headerName);
        
        assertThat(headerValue)
                .as("El header %s no debe existir", headerName)
                .isNull();
    }

    @Step("Verificar que el header {1} contiene '{2}'")
    public void verifyHeaderContains(Response response, String headerName, String expectedValue) {
        String headerValue = response.getHeader(headerName);
        
        assertThat(headerValue)
                .as("El header %s debe contener %s", headerName, expectedValue)
                .contains(expectedValue);
    }

    @Step("Verificar que el header {1} coincide con el patrón '{2}'")
    public void verifyHeaderMatchesPattern(Response response, String headerName, String pattern) {
        String headerValue = response.getHeader(headerName);
        
        assertThat(headerValue)
                .as("El header %s debe existir", headerName)
                .isNotNull();
        
        // Convertir patrón de placeholder a regex
        String regex = pattern
                .replace("{uuid}", "[0-9a-fA-F-]{36}")
                .replace("{id}", "\\d+");
        
        assertThat(headerValue)
                .as("El header %s debe coincidir con el patrón %s", headerName, pattern)
                .matches(".*" + regex + ".*");
    }

    // ========================================================================
    // Body Validations
    // ========================================================================

    @Step("Verificar que el campo '{1}' es '{2}'")
    public void verifyFieldEquals(Response response, String fieldName, String expectedValue) {
        String actualValue = response.jsonPath().getString(fieldName);
        
        assertThat(actualValue)
                .as("El campo %s debe ser %s", fieldName, expectedValue)
                .isEqualTo(expectedValue);
    }

    @Step("Verificar que el campo '{1}' no es nulo")
    public void verifyFieldNotNull(Response response, String fieldName) {
        Object actualValue = response.jsonPath().get(fieldName);
        
        assertThat(actualValue)
                .as("El campo %s no debe ser nulo", fieldName)
                .isNotNull();
    }

    @Step("Verificar estructura de error estándar")
    public void verifyErrorResponseStructure(Response response) {
        assertThat(response.jsonPath().getString("message"))
                .as("La respuesta de error debe contener campo 'message'")
                .isNotNull();
    }

    @Step("Verificar que el ID coincide con la orden creada: {1}")
    public void verifyOrderIdMatches(Response response, String expectedOrderId) {
        String actualId = response.jsonPath().getString("id");
        
        assertThat(actualId)
                .as("El ID de la orden debe coincidir")
                .isEqualTo(expectedOrderId);
    }
}
