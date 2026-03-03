package com.restaurant.orderservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.orderservice.application.port.out.OrderPlacedEventPublisherPort;
import com.restaurant.orderservice.dto.UpdateStatusRequest;
import com.restaurant.orderservice.entity.Order;
import com.restaurant.orderservice.entity.OrderItem;
import com.restaurant.orderservice.entity.Product;
import com.restaurant.orderservice.enums.OrderStatus;
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
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PATCH /orders/{id}/status endpoint.
 * Covers TEST_PLAN_V3 scenarios INT-PATCH-01 through INT-PATCH-10.
 *
 * @see docs/week-3-review/TEST_PLAN_V3.md §2.4
 */
@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestWebConfig.class)
class OrderStatusIntegrationTest {

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

        activeProduct = new Product();
        activeProduct.setName("Hamburguesa");
        activeProduct.setDescription("Hamburguesa clásica");
        activeProduct.setPrice(BigDecimal.valueOf(15.50));
        activeProduct.setCategory("principales");
        activeProduct.setIsActive(true);
        activeProduct = productRepository.save(activeProduct);
    }

    private Order createOrderWithStatus(OrderStatus status) {
        Order order = new Order();
        order.setTableId(5);
        order.setStatus(OrderStatus.PENDING);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductId(activeProduct.getId());
        item.setQuantity(1);
        order.setItems(List.of(item));

        order = orderRepository.save(order);

        // Advance to target status via valid transitions
        if (status == OrderStatus.IN_PREPARATION || status == OrderStatus.READY) {
            order.setStatus(OrderStatus.IN_PREPARATION);
            order = orderRepository.save(order);
        }
        if (status == OrderStatus.READY) {
            order.setStatus(OrderStatus.READY);
            order = orderRepository.save(order);
        }
        return order;
    }

    private String statusBody(String status) throws Exception {
        return "{\"status\":\"" + status + "\"}";
    }

    // ── INT-PATCH-01: Transición válida PENDING → IN_PREPARATION ────────

    @Test
    @DisplayName("INT-PATCH-01: PATCH PENDING → IN_PREPARATION retorna 200 con estado actualizado")
    void patchStatus_pendingToInPreparation_returns200() throws Exception {
        // Arrange
        Order order = createOrderWithStatus(OrderStatus.PENDING);

        // Act & Assert
        mockMvc.perform(patch("/orders/{id}/status", order.getId())
                        .header(TOKEN_HEADER, VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("IN_PREPARATION")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PREPARATION")))
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(header().string("Content-Type", containsString("application/json")));
    }

    // ── INT-PATCH-02: Transición válida IN_PREPARATION → READY ──────────

    @Test
    @DisplayName("INT-PATCH-02: PATCH IN_PREPARATION → READY retorna 200 con estado actualizado")
    void patchStatus_inPreparationToReady_returns200() throws Exception {
        // Arrange
        Order order = createOrderWithStatus(OrderStatus.IN_PREPARATION);

        // Act & Assert
        mockMvc.perform(patch("/orders/{id}/status", order.getId())
                        .header(TOKEN_HEADER, VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("READY")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("READY")));
    }

    // ── INT-PATCH-03: Transición inválida PENDING → READY ───────────────

    /**
     * Expected to FAIL initially.
     * Current GlobalExceptionHandler maps InvalidStatusTransitionException to 400.
     * TEST_PLAN requires 409 Conflict.
     */
    @Test
    @DisplayName("INT-PATCH-03: PATCH PENDING → READY retorna 409 Conflict (no 400)")
    void patchStatus_pendingToReady_returns409() throws Exception {
        // Arrange
        Order order = createOrderWithStatus(OrderStatus.PENDING);

        // Act & Assert
        mockMvc.perform(patch("/orders/{id}/status", order.getId())
                        .header(TOKEN_HEADER, VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("READY")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ── INT-PATCH-04: Transición inválida READY → IN_PREPARATION ────────

    /**
     * Expected to FAIL initially — same reason as INT-PATCH-03.
     */
    @Test
    @DisplayName("INT-PATCH-04: PATCH READY → IN_PREPARATION retorna 409 Conflict")
    void patchStatus_readyToInPreparation_returns409() throws Exception {
        // Arrange
        Order order = createOrderWithStatus(OrderStatus.READY);

        // Act & Assert
        mockMvc.perform(patch("/orders/{id}/status", order.getId())
                        .header(TOKEN_HEADER, VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("IN_PREPARATION")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ── INT-PATCH-05: Retroceso a PENDING ───────────────────────────────

    /**
     * Expected to FAIL initially — same reason as INT-PATCH-03.
     */
    @Test
    @DisplayName("INT-PATCH-05: PATCH retroceso a PENDING retorna 409 Conflict")
    void patchStatus_retrogressToPending_returns409() throws Exception {
        // Arrange
        Order order = createOrderWithStatus(OrderStatus.IN_PREPARATION);

        // Act & Assert
        mockMvc.perform(patch("/orders/{id}/status", order.getId())
                        .header(TOKEN_HEADER, VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("PENDING")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")));
    }

    // ── INT-PATCH-06: Sin header X-Kitchen-Token ────────────────────────

    @Test
    @DisplayName("INT-PATCH-06: PATCH sin token retorna 401 Unauthorized")
    void patchStatus_withoutToken_returns401() throws Exception {
        // Arrange
        Order order = createOrderWithStatus(OrderStatus.PENDING);

        // Act & Assert — NO token header
        mockMvc.perform(patch("/orders/{id}/status", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("IN_PREPARATION")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ── INT-PATCH-07: Token inválido ────────────────────────────────────

    /**
     * Expected to FAIL initially.
     * Both missing and invalid tokens throw KitchenAccessDeniedException → 401.
     * TEST_PLAN requires invalid token → 403 Forbidden.
     */
    @Test
    @DisplayName("INT-PATCH-07: PATCH con token inválido retorna 403 Forbidden (no 401)")
    void patchStatus_withInvalidToken_returns403() throws Exception {
        // Arrange
        Order order = createOrderWithStatus(OrderStatus.PENDING);

        // Act & Assert
        mockMvc.perform(patch("/orders/{id}/status", order.getId())
                        .header(TOKEN_HEADER, INVALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("IN_PREPARATION")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ── INT-PATCH-08: UUID inválido en path ─────────────────────────────

    /**
     * Expected to FAIL initially.
     * MethodArgumentTypeMismatchException → generic handler → 500.
     * Needs specific handler → 400.
     */
    @Test
    @DisplayName("INT-PATCH-08: PATCH /orders/not-a-uuid/status retorna 400 (no 500)")
    void patchStatus_invalidUuidInPath_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(patch("/orders/not-a-uuid/status")
                        .header(TOKEN_HEADER, VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("IN_PREPARATION")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    // ── INT-PATCH-09: Orden inexistente ─────────────────────────────────

    @Test
    @DisplayName("INT-PATCH-09: PATCH sobre UUID válido pero inexistente retorna 404")
    void patchStatus_nonExistentOrder_returns404() throws Exception {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(patch("/orders/{id}/status", nonExistentId)
                        .header(TOKEN_HEADER, VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("IN_PREPARATION")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    // ── INT-PATCH-10: Body sin campo status ─────────────────────────────

    @Test
    @DisplayName("INT-PATCH-10: PATCH con body vacío {} retorna 400 (@NotNull)")
    void patchStatus_emptyBody_returns400() throws Exception {
        // Arrange
        Order order = createOrderWithStatus(OrderStatus.PENDING);

        // Act & Assert
        mockMvc.perform(patch("/orders/{id}/status", order.getId())
                        .header(TOKEN_HEADER, VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists());
    }
}
