package com.restaurant.orderservice.integration;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for DELETE /orders/{id} and DELETE /orders endpoints.
 * Covers TEST_PLAN_V3 scenarios INT-DEL-01..07 and INT-DELALL-01..06.
 *
 * @see docs/week-3-review/TEST_PLAN_V3.md §2.5, §2.6
 */
@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestWebConfig.class)
class OrderDeleteIntegrationTest {

    private static final String TOKEN_HEADER = "X-Kitchen-Token";
    private static final String VALID_TOKEN = "test-kitchen-token-2026";
    private static final String INVALID_TOKEN = "wrong-token";
    private static final String CONFIRM_HEADER = "X-Confirm-Destructive";

    @Autowired
    private MockMvc mockMvc;

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

    private Order createActiveOrder(int tableId) {
        Order order = new Order();
        order.setTableId(tableId);
        order.setStatus(OrderStatus.PENDING);
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductId(activeProduct.getId());
        item.setQuantity(1);
        order.setItems(List.of(item));
        return orderRepository.save(order);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  INT-DEL: DELETE /orders/{id}
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /orders/{id} — Soft delete individual")
    class DeleteSingleOrderTests {

        // ── INT-DEL-01 ───────────────────────────────────────────────────

        /**
         * Expected to FAIL initially.
         * Current code returns 204 No Content with empty body.
         * TEST_PLAN requires 200 OK with {deletedId, deletedAt}.
         */
        @Test
        @DisplayName("INT-DEL-01: DELETE orden existente retorna 200 con deletedId y deletedAt")
        void deleteOrder_existing_returns200WithMetadata() throws Exception {
            // Arrange
            Order order = createActiveOrder(5);

            // Act & Assert
            mockMvc.perform(delete("/orders/{id}", order.getId())
                            .header(TOKEN_HEADER, VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deletedId", is(order.getId().toString())))
                    .andExpect(jsonPath("$.deletedAt").exists());

            // Verify: GET after delete returns 404
            mockMvc.perform(get("/orders/{id}", order.getId()))
                    .andExpect(status().isNotFound());
        }

        // ── INT-DEL-02 ───────────────────────────────────────────────────

        @Test
        @DisplayName("INT-DEL-02: DELETE orden ya eliminada retorna 404 (idempotencia)")
        void deleteOrder_alreadyDeleted_returns404() throws Exception {
            // Arrange
            Order order = createActiveOrder(5);
            order.markAsDeleted();
            orderRepository.save(order);

            // Act & Assert
            mockMvc.perform(delete("/orders/{id}", order.getId())
                            .header(TOKEN_HEADER, VALID_TOKEN))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Not Found")));
        }

        // ── INT-DEL-03 ───────────────────────────────────────────────────

        @Test
        @DisplayName("INT-DEL-03: DELETE orden inexistente retorna 404")
        void deleteOrder_nonExistent_returns404() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/orders/{id}", UUID.randomUUID())
                            .header(TOKEN_HEADER, VALID_TOKEN))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Not Found")));
        }

        // ── INT-DEL-04 ───────────────────────────────────────────────────

        @Test
        @DisplayName("INT-DEL-04: DELETE sin token retorna 401 Unauthorized")
        void deleteOrder_withoutToken_returns401() throws Exception {
            // Arrange
            Order order = createActiveOrder(5);

            // Act & Assert
            mockMvc.perform(delete("/orders/{id}", order.getId()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status", is(401)))
                    .andExpect(jsonPath("$.error").exists());
        }

        // ── INT-DEL-05 ───────────────────────────────────────────────────

        /**
         * Expected to FAIL initially.
         * Both missing and invalid tokens throw KitchenAccessDeniedException → 401.
         * TEST_PLAN requires invalid token → 403 Forbidden.
         */
        @Test
        @DisplayName("INT-DEL-05: DELETE con token inválido retorna 403 Forbidden")
        void deleteOrder_withInvalidToken_returns403() throws Exception {
            // Arrange
            Order order = createActiveOrder(5);

            // Act & Assert
            mockMvc.perform(delete("/orders/{id}", order.getId())
                            .header(TOKEN_HEADER, INVALID_TOKEN))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)))
                    .andExpect(jsonPath("$.error", is("Forbidden")));
        }

        // ── INT-DEL-06 ───────────────────────────────────────────────────

        /**
         * Expected to FAIL initially.
         * MethodArgumentTypeMismatchException → generic handler → 500.
         */
        @Test
        @DisplayName("INT-DEL-06: DELETE /orders/not-uuid retorna 400 (no 500)")
        void deleteOrder_invalidUuid_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/orders/not-uuid")
                            .header(TOKEN_HEADER, VALID_TOKEN))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error").exists());
        }

        // ── INT-DEL-07 ───────────────────────────────────────────────────

        /**
         * Expected to FAIL initially.
         * Current code returns 204 with no body, so no deletedAt field.
         */
        @Test
        @DisplayName("INT-DEL-07: DELETE verifica metadatos de auditoría — deletedAt es ISO 8601")
        void deleteOrder_responseContainsValidDeletedAtTimestamp() throws Exception {
            // Arrange
            Order order = createActiveOrder(5);

            // Act & Assert
            mockMvc.perform(delete("/orders/{id}", order.getId())
                            .header(TOKEN_HEADER, VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deletedAt").exists())
                    .andExpect(jsonPath("$.deletedAt", matchesPattern(
                            "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  INT-DELALL: DELETE /orders
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /orders — Soft delete masivo")
    class DeleteAllOrdersTests {

        // ── INT-DELALL-01 ────────────────────────────────────────────────

        /**
         * Expected to FAIL initially.
         * Current code returns 204 No Content, does not require X-Confirm-Destructive header,
         * and does not return {deletedCount, deletedAt}.
         */
        @Test
        @DisplayName("INT-DELALL-01: DELETE /orders con confirmación retorna 200 con deletedCount y deletedAt")
        void deleteAllOrders_withConfirmation_returns200WithCount() throws Exception {
            // Arrange
            createActiveOrder(1);
            createActiveOrder(2);
            createActiveOrder(3);

            // Act & Assert
            mockMvc.perform(delete("/orders")
                            .header(TOKEN_HEADER, VALID_TOKEN)
                            .header(CONFIRM_HEADER, "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deletedCount", is(3)))
                    .andExpect(jsonPath("$.deletedAt").exists());
        }

        // ── INT-DELALL-02 ────────────────────────────────────────────────

        /**
         * Expected to FAIL initially.
         * Current code does not check for X-Confirm-Destructive header.
         */
        @Test
        @DisplayName("INT-DELALL-02: DELETE /orders sin header de confirmación retorna 400")
        void deleteAllOrders_withoutConfirmationHeader_returns400() throws Exception {
            // Arrange
            createActiveOrder(1);

            // Act & Assert — no X-Confirm-Destructive header
            mockMvc.perform(delete("/orders")
                            .header(TOKEN_HEADER, VALID_TOKEN))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.message").exists());
        }

        // ── INT-DELALL-03 ────────────────────────────────────────────────

        /**
         * Expected to FAIL initially — same as INT-DELALL-02.
         */
        @Test
        @DisplayName("INT-DELALL-03: DELETE /orders con confirmación=false retorna 400")
        void deleteAllOrders_withConfirmationFalse_returns400() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/orders")
                            .header(TOKEN_HEADER, VALID_TOKEN)
                            .header(CONFIRM_HEADER, "false"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error").exists());
        }

        // ── INT-DELALL-04 ────────────────────────────────────────────────

        @Test
        @DisplayName("INT-DELALL-04: DELETE /orders sin token retorna 401 Unauthorized")
        void deleteAllOrders_withoutToken_returns401() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/orders")
                            .header(CONFIRM_HEADER, "true"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status", is(401)));
        }

        // ── INT-DELALL-05 ────────────────────────────────────────────────

        /**
         * Expected to FAIL initially — depends on INT-DELALL-01 behavior.
         */
        @Test
        @DisplayName("INT-DELALL-05: Después de DELETE /orders, GET /orders retorna []")
        void deleteAllOrders_thenGet_returnsEmptyArray() throws Exception {
            // Arrange
            createActiveOrder(1);
            createActiveOrder(2);

            // Act — delete all
            mockMvc.perform(delete("/orders")
                            .header(TOKEN_HEADER, VALID_TOKEN)
                            .header(CONFIRM_HEADER, "true"))
                    .andExpect(status().isOk());

            // Assert — list should be empty
            mockMvc.perform(get("/orders")
                            .header(TOKEN_HEADER, VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        // ── INT-DELALL-06 ────────────────────────────────────────────────

        /**
         * Expected to FAIL initially — depends on INT-DELALL-01 behavior.
         */
        @Test
        @DisplayName("INT-DELALL-06: DELETE /orders sin órdenes activas retorna 200 con deletedCount=0")
        void deleteAllOrders_noActiveOrders_returns200WithCountZero() throws Exception {
            // Arrange — no orders in DB

            // Act & Assert
            mockMvc.perform(delete("/orders")
                            .header(TOKEN_HEADER, VALID_TOKEN)
                            .header(CONFIRM_HEADER, "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deletedCount", is(0)));
        }
    }
}
