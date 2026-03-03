package com.restaurant.orderservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.orderservice.application.port.out.OrderPlacedEventPublisherPort;
import com.restaurant.orderservice.dto.CreateOrderRequest;
import com.restaurant.orderservice.dto.OrderItemRequest;
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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for POST /orders endpoint.
 * Covers TEST_PLAN_V3 scenarios INT-POST-01 through INT-POST-09.
 *
 * @see docs/week-3-review/TEST_PLAN_V3.md §2.1
 */
@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestWebConfig.class)
class OrderCreateIntegrationTest {

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
    private Product inactiveProduct;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();

        activeProduct = new Product();
        activeProduct.setName("Hamburguesa");
        activeProduct.setDescription("Hamburguesa clásica");
        activeProduct.setPrice(BigDecimal.valueOf(15.50));
        activeProduct.setCategory("principales");
        activeProduct.setIsActive(true);
        activeProduct = productRepository.save(activeProduct);

        inactiveProduct = new Product();
        inactiveProduct.setName("Old Item");
        inactiveProduct.setDescription("Producto descontinuado");
        inactiveProduct.setPrice(BigDecimal.valueOf(10.00));
        inactiveProduct.setCategory("otros");
        inactiveProduct.setIsActive(false);
        inactiveProduct = productRepository.save(inactiveProduct);
    }

    // ── INT-POST-01: Creación exitosa con payload válido ─────────────────────

    @Test
    @DisplayName("INT-POST-01: POST /orders con payload válido retorna 201 con Location y status PENDING")
    void createOrder_withValidPayload_returns201WithLocationHeader() throws Exception {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                5,
                List.of(new OrderItemRequest(activeProduct.getId(), 2, null))
        );

        // Act & Assert
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productName").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", matchesPattern("/orders/[0-9a-f\\-]{36}")))
                .andExpect(header().string("Content-Type", containsString("application/json")));
    }

    // ── INT-POST-02: tableId menor a 1 ──────────────────────────────────────

    @Test
    @DisplayName("INT-POST-02: POST /orders con tableId=0 retorna 400 Bad Request")
    void createOrder_withTableIdZero_returns400() throws Exception {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                0,
                List.of(new OrderItemRequest(activeProduct.getId(), 1, null))
        );

        // Act & Assert
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ── INT-POST-03: tableId mayor a 12 ─────────────────────────────────────

    /**
     * Expected to FAIL initially.
     * Current code only validates tableId > 0 but does NOT enforce upper bound of 12.
     * Production code needs @Max(12) in CreateOrderRequest or upper bound check in OrderValidator.
     */
    @Test
    @DisplayName("INT-POST-03: POST /orders con tableId=13 retorna 400 Bad Request")
    void createOrder_withTableId13_returns400() throws Exception {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                13,
                List.of(new OrderItemRequest(activeProduct.getId(), 1, null))
        );

        // Act & Assert
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    // ── INT-POST-04: Items vacíos ───────────────────────────────────────────

    @Test
    @DisplayName("INT-POST-04: POST /orders con items vacíos retorna 400 Bad Request")
    void createOrder_withEmptyItems_returns400() throws Exception {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(5, List.of());

        // Act & Assert
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ── INT-POST-05: Producto inexistente ───────────────────────────────────

    @Test
    @DisplayName("INT-POST-05: POST /orders con producto inexistente retorna 404 Not Found")
    void createOrder_withNonExistentProduct_returns404() throws Exception {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                5,
                List.of(new OrderItemRequest(99999L, 1, null))
        );

        // Act & Assert
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ── INT-POST-06: Producto inactivo ──────────────────────────────────────

    /**
     * Expected to FAIL initially.
     * Current code throws ProductNotFoundException (404) for inactive products.
     * TEST_PLAN requires 422 Unprocessable Entity with a message distinguishing
     * "inactivo" from "no encontrado".
     */
    @Test
    @DisplayName("INT-POST-06: POST /orders con producto inactivo retorna 422 Unprocessable Entity")
    void createOrder_withInactiveProduct_returns422() throws Exception {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                5,
                List.of(new OrderItemRequest(inactiveProduct.getId(), 1, null))
        );

        // Act & Assert
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status", is(422)))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ── INT-POST-07: JSON malformado ────────────────────────────────────────

    /**
     * Expected to FAIL initially.
     * The generic Exception handler catches HttpMessageNotReadableException
     * and returns 500 instead of 400. Needs a specific handler.
     */
    @Test
    @DisplayName("INT-POST-07: POST /orders con JSON malformado retorna 400 Bad Request")
    void createOrder_withMalformedJson_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ── INT-POST-08: tableId nulo ───────────────────────────────────────────

    @Test
    @DisplayName("INT-POST-08: POST /orders con tableId nulo retorna 400 (Bean Validation @NotNull)")
    void createOrder_withNullTableId_returns400() throws Exception {
        // Arrange — JSON sin tableId
        String json = "{\"items\":[{\"productId\":" + activeProduct.getId() + ",\"quantity\":1}]}";

        // Act & Assert
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ── INT-POST-09: Header Location válido y accesible ─────────────────────

    /**
     * Expected to FAIL initially.
     * Current code does not set Location header on POST /orders.
     */
    @Test
    @DisplayName("INT-POST-09: GET al URL del header Location retorna la orden creada")
    void createOrder_locationHeaderPointsToCreatedOrder() throws Exception {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                5,
                List.of(new OrderItemRequest(activeProduct.getId(), 2, null))
        );

        // Act — create order and capture Location
        MvcResult createResult = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();

        String locationUrl = createResult.getResponse().getHeader("Location");

        // Assert — GET to Location URL should return the created order
        mockMvc.perform(get(locationUrl))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.tableId", is(5)))
                .andExpect(jsonPath("$.items", hasSize(1)));
    }
}
