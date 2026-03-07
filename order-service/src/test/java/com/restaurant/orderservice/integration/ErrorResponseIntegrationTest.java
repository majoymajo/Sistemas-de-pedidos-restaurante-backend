package com.restaurant.orderservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.orderservice.application.port.out.OrderPlacedEventPublisherPort;
import com.restaurant.orderservice.dto.CreateOrderRequest;
import com.restaurant.orderservice.dto.OrderItemRequest;
import com.restaurant.orderservice.entity.Product;
import com.restaurant.orderservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración para validar la estructura de {@code ErrorResponse}
 * en los endpoints del order-service.
 *
 * <p>Cubre los escenarios INT-ERR-01 a INT-ERR-06 del TEST_PLAN_V3.
 * Algunos tests documentan defectos conocidos y se espera que fallen
 * hasta que se apliquen las correcciones correspondientes.</p>
 */
@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestWebConfig.class)
class ErrorResponseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @MockBean
    private OrderPlacedEventPublisherPort eventPublisherPort;

    private Product activeProduct;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        Product product = new Product();
        product.setName("Hamburguesa");
        product.setDescription("Test product");
        product.setPrice(BigDecimal.valueOf(15.50));
        product.setCategory("principales");
        product.setIsActive(true);
        activeProduct = productRepository.save(product);
    }

    /**
     * INT-ERR-01: ErrorResponse contiene exactamente 4 campos requeridos.
     *
     * <p>Resultado esperado: PASS — El DTO {@code ErrorResponse} define
     * exactamente los campos {@code timestamp}, {@code status}, {@code error}
     * y {@code message}.</p>
     */
    @Test
    @DisplayName("INT-ERR-01: ErrorResponse tiene exactamente 4 campos requeridos: timestamp, status, error, message")
    void errorResponse_shouldHaveExactlyFourFields() throws Exception {
        String nonExistentId = UUID.randomUUID().toString();

        mockMvc.perform(get("/orders/{id}", nonExistentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.*", hasSize(4)));
    }

    /**
     * INT-ERR-02: El campo {@code status} coincide con el código HTTP de la respuesta.
     *
     * <p>Resultado esperado: PASS — El handler {@code handleOrderNotFound}
     * devuelve {@code status=404} que coincide con el HTTP status 404.</p>
     */
    @Test
    @DisplayName("INT-ERR-02: El campo status coincide con el código de estado HTTP")
    void statusField_shouldMatchHttpStatusCode() throws Exception {
        String nonExistentId = UUID.randomUUID().toString();

        mockMvc.perform(get("/orders/{id}", nonExistentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    /**
     * INT-ERR-03: El campo {@code error} debe ser texto estándar HTTP (ej. "Bad Request").
     *
     * <p>Resultado esperado: FAIL — El handler {@code handleValidationErrors}
     * devuelve {@code error="Validation Failed"} en lugar del texto estándar
     * {@code "Bad Request"} para respuestas 400.</p>
     */
    @Test
    @DisplayName("INT-ERR-03: El campo error es texto estándar HTTP (ej. 'Bad Request', 'Not Found')")
    void errorField_shouldBeStandardHttpStatusText() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setTableId(1);
        request.setItems(List.of());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    /**
     * INT-ERR-04: Todas las respuestas de error tienen Content-Type application/json.
     *
     * <p>Resultado esperado: PASS — Spring Boot con {@code @RestControllerAdvice}
     * serializa las respuestas de error como JSON por defecto.</p>
     */
    @Test
    @DisplayName("INT-ERR-04: Todas las respuestas de error tienen Content-Type application/json")
    void errorResponses_shouldHaveJsonContentType() throws Exception {
        String nonExistentId = UUID.randomUUID().toString();

        mockMvc.perform(get("/orders/{id}", nonExistentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", containsString("application/json")));
    }

    /**
     * INT-ERR-05: Las respuestas 500 NO deben exponer detalles internos de excepciones.
     *
     * <p>Resultado esperado: FAIL — El handler {@code handleGenericError} utiliza
     * {@code ex.getMessage()} directamente, lo que expone el mensaje de
     * {@code HttpMessageNotReadableException} que contiene detalles de parseo JSON.
     * Esto constituye un problema de seguridad (information disclosure).</p>
     */
    @Test
    @DisplayName("INT-ERR-05: Las respuestas de error por JSON malformado NO exponen detalles internos de excepción")
    void malformedJsonError_shouldNotExposeExceptionDetails() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", not(containsString("JSON"))))
                .andExpect(jsonPath("$.message", not(containsString("parse"))))
                .andExpect(jsonPath("$.message", not(containsString("Unexpected"))));
    }

    /**
     * INT-ERR-06: El campo {@code timestamp} tiene formato ISO 8601.
     *
     * <p>Resultado esperado: PASS — Spring Boot serializa {@code LocalDateTime}
     * en formato ISO 8601 por defecto (ej. {@code 2026-02-25T14:30:00}).</p>
     */
    @Test
    @DisplayName("INT-ERR-06: El campo timestamp tiene formato ISO 8601")
    void timestampField_shouldBeIso8601Format() throws Exception {
        String nonExistentId = UUID.randomUUID().toString();

        mockMvc.perform(get("/orders/{id}", nonExistentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp", matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")));
    }
}
