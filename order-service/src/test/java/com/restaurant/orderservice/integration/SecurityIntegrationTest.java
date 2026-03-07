package com.restaurant.orderservice.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.orderservice.application.port.out.OrderPlacedEventPublisherPort;
import com.restaurant.orderservice.entity.Order;
import com.restaurant.orderservice.entity.OrderItem;
import com.restaurant.orderservice.entity.Product;
import com.restaurant.orderservice.repository.OrderRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para validar el comportamiento de seguridad
 * del order-service.
 *
 * <p>Cubre los escenarios INT-SEC-01, INT-SEC-03 e INT-SEC-04 del TEST_PLAN_V3.
 * INT-SEC-02 se cubre en report-service.
 *
 * <p>Defectos conocidos documentados:
 * <ul>
 *   <li>INT-SEC-01: handleGenericError expone ex.getMessage() — information disclosure</li>
 *   <li>INT-SEC-03: Token inválido retorna 401 en lugar de 403</li>
 * </ul>
 *
 * @see docs/week-3-review/TEST_PLAN_V3.md §2.10
 */
@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestWebConfig.class)
class SecurityIntegrationTest {

    private static final String TOKEN_HEADER = "X-Kitchen-Token";
    private static final String VALID_TOKEN = "test-kitchen-token-2026";
    private static final String INVALID_TOKEN = "wrong-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @MockBean
    private OrderPlacedEventPublisherPort eventPublisherPort;

    private Product activeProduct;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();

        Product product = new Product();
        product.setName("Hamburguesa");
        product.setDescription("Test product");
        product.setPrice(BigDecimal.valueOf(15.50));
        product.setCategory("principales");
        product.setIsActive(true);
        activeProduct = productRepository.save(product);
    }

    // ── INT-SEC-01: Respuestas 500 no exponen detalles internos ─────────

    /**
     * INT-SEC-01: Las respuestas de error 500 no deben exponer detalles
     * internos de excepciones (ex.getMessage(), class names, stack traces).
     *
     * <p>Resultado esperado: FAIL — El handler {@code handleGenericError}
     * utiliza {@code ex.getMessage()} directamente como campo {@code message},
     * exponiendo información interna que podría ser aprovechada por un atacante.
     * El mensaje debería ser genérico, ej. "An unexpected error occurred".</p>
     */
    @Test
    @DisplayName("INT-SEC-01: Respuestas de error por JSON malformado no exponen detalles internos")
    void malformedJsonError_shouldNotExposeInternalDetails() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{malformed"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", not(containsString("Exception"))))
                .andExpect(jsonPath("$.message", not(containsString("json"))))
                .andExpect(jsonPath("$.message", not(containsString("parse"))))
                .andExpect(jsonPath("$.message", not(containsString("com.fasterxml"))));
    }

    // ── INT-SEC-03: Token inválido retorna 403 Forbidden ────────────────

    /**
     * INT-SEC-03: Un token de cocina inválido debe retornar 403 Forbidden
     * (distinguido del 401 que se usa para token ausente).
     *
     * <p>Resultado esperado: FAIL — Tanto {@code KitchenTokenPresenceHandler}
     * (token ausente) como {@code KitchenTokenValueHandler} (token inválido)
     * lanzan {@code KitchenAccessDeniedException}, y el
     * {@code GlobalExceptionHandler.handleKitchenAccessDenied} siempre retorna 401.
     * Según el TEST_PLAN, un token presente pero inválido debería retornar 403.</p>
     */
    @Test
    @DisplayName("INT-SEC-03: Token inválido retorna 403 Forbidden (no 401)")
    void invalidToken_shouldReturn403Forbidden() throws Exception {
        mockMvc.perform(get("/orders")
                        .header(TOKEN_HEADER, INVALID_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    // ── INT-SEC-04: Token ausente retorna 401 Unauthorized ──────────────

    /**
     * INT-SEC-04: La ausencia del header X-Kitchen-Token en endpoints
     * protegidos debe retornar 401 Unauthorized.
     *
     * <p>Resultado esperado: PASS — {@code KitchenTokenPresenceHandler}
     * lanza {@code KitchenAccessDeniedException} cuando el token no está presente,
     * y {@code GlobalExceptionHandler.handleKitchenAccessDenied} retorna 401.</p>
     */
    @Test
    @DisplayName("INT-SEC-04: Token ausente retorna 401 Unauthorized")
    void missingToken_shouldReturn401Unauthorized() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    // ── Tests adicionales de seguridad ───────────────────────────────────

    /**
     * Verifica que los endpoints que NO requieren autenticación funcionan
     * sin el header X-Kitchen-Token.
     */
    @Test
    @DisplayName("INT-SEC-EXTRA: POST /orders no requiere token de cocina")
    void postOrders_shouldNotRequireToken() throws Exception {
        OrderItem item = new OrderItem();
        item.setProductId(activeProduct.getId());
        item.setQuantity(1);

        Order order = new Order();
        order.setTableId(1);
        order.setItems(List.of(item));

        // POST /orders should NOT require kitchen token
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tableId\":1,\"items\":[{\"productId\":" + activeProduct.getId() + ",\"quantity\":1}]}"))
                .andExpect(status().isCreated());
    }

    /**
     * Verifica que GET /orders/{id} no requiere token (endpoint público).
     */
    @Test
    @DisplayName("INT-SEC-EXTRA: GET /orders/{id} no requiere token de cocina")
    void getOrderById_shouldNotRequireToken() throws Exception {
        // Create an order first via POST
        String json = "{\"tableId\":1,\"items\":[{\"productId\":" + activeProduct.getId() + ",\"quantity\":1}]}";
        String responseBody = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Extract the order ID from the response
        JsonNode responseJson = objectMapper.readTree(responseBody);
        String orderId = responseJson.get("id").asText();

        // GET /orders/{id} should NOT require kitchen token
        mockMvc.perform(get("/orders/{id}", orderId))
                .andExpect(status().isOk());
    }

    /**
     * Verifica que GET /menu no requiere token (endpoint público).
     */
    @Test
    @DisplayName("INT-SEC-EXTRA: GET /menu no requiere token de cocina")
    void getMenu_shouldNotRequireToken() throws Exception {
        mockMvc.perform(get("/menu"))
                .andExpect(status().isOk());
    }
}
