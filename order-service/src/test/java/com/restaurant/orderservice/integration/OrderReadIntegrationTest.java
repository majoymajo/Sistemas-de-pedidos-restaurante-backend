package com.restaurant.orderservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.orderservice.application.port.out.OrderPlacedEventPublisherPort;
import com.restaurant.orderservice.entity.Order;
import com.restaurant.orderservice.entity.OrderItem;
import com.restaurant.orderservice.entity.Product;
import com.restaurant.orderservice.enums.OrderStatus;
import com.restaurant.orderservice.repository.OrderRepository;
import com.restaurant.orderservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for GET /orders and GET /orders/{id} endpoints.
 * Covers TEST_PLAN_V3 scenarios INT-LIST-01..06 and INT-GET-01..04.
 *
 * @see docs/week-3-review/TEST_PLAN_V3.md §2.2, §2.3
 */
@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestWebConfig.class)
class OrderReadIntegrationTest {

    private static final String TOKEN_HEADER = "X-Kitchen-Token";
    private static final String VALID_TOKEN = "test-kitchen-token-2026";

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

        activeProduct = new Product();
        activeProduct.setName("Hamburguesa");
        activeProduct.setDescription("Hamburguesa clásica");
        activeProduct.setPrice(BigDecimal.valueOf(15.50));
        activeProduct.setCategory("principales");
        activeProduct.setIsActive(true);
        activeProduct = productRepository.save(activeProduct);
    }

    /**
     * Helper: Creates and persists an order with the given status.
     */
    private Order createOrder(int tableId, OrderStatus status) {
        Order order = new Order();
        order.setTableId(tableId);
        order.setStatus(status);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductId(activeProduct.getId());
        item.setQuantity(1);
        order.setItems(List.of(item));

        return orderRepository.save(order);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  INT-LIST: GET /orders
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /orders — Listar órdenes")
    class ListOrdersTests {

        // ── INT-LIST-01 ──────────────────────────────────────────────────

        @Test
        @DisplayName("INT-LIST-01: GET /orders con órdenes existentes retorna 200 con array")
        void getOrders_withExistingOrders_returns200WithArray() throws Exception {
            // Arrange
            createOrder(5, OrderStatus.PENDING);

            // Act & Assert
            mockMvc.perform(get("/orders")
                            .header(TOKEN_HEADER, VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$[0].id").exists())
                    .andExpect(jsonPath("$[0].tableId").exists())
                    .andExpect(jsonPath("$[0].status").exists())
                    .andExpect(header().string("Content-Type", containsString("application/json")));
        }

        // ── INT-LIST-02 ──────────────────────────────────────────────────

        @Test
        @DisplayName("INT-LIST-02: GET /orders sin resultados retorna 200 con arreglo vacío []")
        void getOrders_withNoOrders_returns200WithEmptyArray() throws Exception {
            // Arrange — no orders in DB

            // Act & Assert
            mockMvc.perform(get("/orders")
                            .header(TOKEN_HEADER, VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)))
                    .andExpect(content().json("[]"));
        }

        // ── INT-LIST-03 ──────────────────────────────────────────────────

        @Test
        @DisplayName("INT-LIST-03: GET /orders?status=PENDING retorna solo órdenes PENDING")
        void getOrders_filterByPending_returnsOnlyPending() throws Exception {
            // Arrange
            createOrder(1, OrderStatus.PENDING);
            Order inPrepOrder = createOrder(2, OrderStatus.PENDING);
            inPrepOrder.setStatus(OrderStatus.IN_PREPARATION);
            orderRepository.save(inPrepOrder);

            // Act & Assert
            mockMvc.perform(get("/orders")
                            .param("status", "PENDING")
                            .header(TOKEN_HEADER, VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].status", everyItem(is("PENDING"))));
        }

        // ── INT-LIST-04 ──────────────────────────────────────────────────

        @Test
        @DisplayName("INT-LIST-04: GET /orders?status=PENDING,IN_PREPARATION retorna múltiples estados")
        void getOrders_filterByMultipleStatuses_returnsMatching() throws Exception {
            // Arrange
            createOrder(1, OrderStatus.PENDING);
            Order inPrep = createOrder(2, OrderStatus.PENDING);
            inPrep.setStatus(OrderStatus.IN_PREPARATION);
            orderRepository.save(inPrep);
            Order ready = createOrder(3, OrderStatus.PENDING);
            ready.setStatus(OrderStatus.IN_PREPARATION);
            ready.setStatus(OrderStatus.READY);
            orderRepository.save(ready);

            // Act & Assert
            mockMvc.perform(get("/orders")
                            .param("status", "PENDING", "IN_PREPARATION")
                            .header(TOKEN_HEADER, VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].status",
                            everyItem(anyOf(is("PENDING"), is("IN_PREPARATION")))));
        }

        // ── INT-LIST-05 ──────────────────────────────────────────────────

        /**
         * Expected to FAIL initially.
         * Invalid enum value conversion throws MethodArgumentTypeMismatchException
         * which is caught by the generic Exception handler → 500.
         * Needs a specific handler for MethodArgumentTypeMismatchException → 400.
         */
        @Test
        @DisplayName("INT-LIST-05: GET /orders?status=INVALID_STATUS retorna 400 (no 500)")
        void getOrders_withInvalidStatus_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/orders")
                            .param("status", "INVALID_STATUS")
                            .header(TOKEN_HEADER, VALID_TOKEN))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.message").exists());
        }

        // ── INT-LIST-06 ──────────────────────────────────────────────────

        @Test
        @DisplayName("INT-LIST-06: GET /orders excluye órdenes eliminadas lógicamente")
        void getOrders_excludesSoftDeletedOrders() throws Exception {
            // Arrange
            Order activeOrder = createOrder(1, OrderStatus.PENDING);
            Order deletedOrder = createOrder(2, OrderStatus.PENDING);
            deletedOrder.markAsDeleted();
            orderRepository.save(deletedOrder);

            // Act & Assert
            mockMvc.perform(get("/orders")
                            .header(TOKEN_HEADER, VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is(activeOrder.getId().toString())));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  INT-GET: GET /orders/{id}
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /orders/{id} — Obtener orden por ID")
    class GetOrderByIdTests {

        // ── INT-GET-01 ───────────────────────────────────────────────────

        @Test
        @DisplayName("INT-GET-01: GET /orders/{id} con orden existente retorna 200 con estructura completa")
        void getOrderById_existingOrder_returns200WithFullStructure() throws Exception {
            // Arrange
            Order order = createOrder(5, OrderStatus.PENDING);

            // Act & Assert
            mockMvc.perform(get("/orders/{id}", order.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(order.getId().toString())))
                    .andExpect(jsonPath("$.tableId", is(5)))
                    .andExpect(jsonPath("$.status", is("PENDING")))
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists())
                    .andExpect(header().string("Content-Type", containsString("application/json")));
        }

        // ── INT-GET-02 ───────────────────────────────────────────────────

        @Test
        @DisplayName("INT-GET-02: GET /orders/{id} con UUID inexistente retorna 404")
        void getOrderById_nonExistentUuid_returns404() throws Exception {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();

            // Act & Assert
            mockMvc.perform(get("/orders/{id}", nonExistentId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Not Found")))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        // ── INT-GET-03 ───────────────────────────────────────────────────

        /**
         * Expected to FAIL initially.
         * Invalid UUID format causes MethodArgumentTypeMismatchException
         * caught by generic handler → 500. Needs specific handler → 400.
         */
        @Test
        @DisplayName("INT-GET-03: GET /orders/abc con UUID inválido retorna 400 (no 500)")
        void getOrderById_invalidUuidFormat_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/orders/abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        // ── INT-GET-04 ───────────────────────────────────────────────────

        @Test
        @DisplayName("INT-GET-04: GET /orders/{id} con orden eliminada retorna 404")
        void getOrderById_softDeletedOrder_returns404() throws Exception {
            // Arrange
            Order order = createOrder(5, OrderStatus.PENDING);
            order.markAsDeleted();
            orderRepository.save(order);

            // Act & Assert
            mockMvc.perform(get("/orders/{id}", order.getId()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Not Found")));
        }
    }
}
