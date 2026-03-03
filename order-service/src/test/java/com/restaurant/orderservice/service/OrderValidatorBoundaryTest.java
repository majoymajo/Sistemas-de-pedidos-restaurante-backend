package com.restaurant.orderservice.service;

import com.restaurant.orderservice.dto.CreateOrderRequest;
import com.restaurant.orderservice.dto.OrderItemRequest;
import com.restaurant.orderservice.entity.Product;
import com.restaurant.orderservice.exception.InvalidOrderException;
import com.restaurant.orderservice.exception.ProductNotFoundException;
import com.restaurant.orderservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Boundary value tests for {@link OrderValidator}.
 * <p>
 * Covers TEST_PLAN_V3 cases UNIT-DOM-05 through UNIT-DOM-10.
 * Uses Boundary Value Analysis on tableId (valid range 1–12)
 * and Equivalence Partitioning on items list and product state.
 * </p>
 *
 * @see docs/week-3-review/TEST_PLAN_V3.md
 */
@ExtendWith(MockitoExtension.class)
class OrderValidatorBoundaryTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderValidator orderValidator;

    private Product activeProduct;
    private Product inactiveProduct;

    @BeforeEach
    void setUp() {
        activeProduct = new Product();
        activeProduct.setId(1L);
        activeProduct.setName("Pizza Margherita");
        activeProduct.setIsActive(true);

        inactiveProduct = new Product();
        inactiveProduct.setId(2L);
        inactiveProduct.setName("Discontinued Burger");
        inactiveProduct.setIsActive(false);
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private CreateOrderRequest buildRequest(Integer tableId, List<OrderItemRequest> items) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setTableId(tableId);
        request.setItems(items);
        return request;
    }

    private List<OrderItemRequest> validItems() {
        return List.of(new OrderItemRequest(1L, 2, null));
    }

    // ── UNIT-DOM-05: tableId = 0 (below minimum boundary) ────────────────

    @Test
    @DisplayName("UNIT-DOM-05 — tableId=0 should be rejected as below minimum boundary")
    void validateCreateOrderRequest_withTableIdZero_shouldReject() {
        // Arrange
        CreateOrderRequest request = buildRequest(0, validItems());

        // Act & Assert
        assertThatThrownBy(() -> orderValidator.validateCreateOrderRequest(request))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("positive");
    }

    // ── UNIT-DOM-06: tableId = 13 (above maximum boundary) ───────────────

    /**
     * EXPECTED TO FAIL — The current implementation only checks {@code tableId <= 0}.
     * There is no upper-bound validation for tableId > 12.
     * <p>
     * The business rule (copilot-instructions §3) states tableId must be
     * between 1 and 12, so this test documents the missing validation.
     * Once the upper-bound check is added to {@link OrderValidator#validateCreateOrderRequest},
     * this test should pass.
     * </p>
     */
    @Test
    @DisplayName("UNIT-DOM-06 — tableId=13 should be rejected as above maximum boundary [EXPECTED FAIL — missing upper bound check]")
    void validateCreateOrderRequest_withTableId13_shouldReject() {
        // Arrange
        CreateOrderRequest request = buildRequest(13, validItems());

        // Act & Assert
        // BUG: No upper-bound validation exists. tableId=13 will pass validation
        // instead of throwing InvalidOrderException.
        assertThatThrownBy(() -> orderValidator.validateCreateOrderRequest(request))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Table ID");
    }

    // ── UNIT-DOM-07: empty items list ────────────────────────────────────

    @Test
    @DisplayName("UNIT-DOM-07 — empty items list should be rejected")
    void validateCreateOrderRequest_withEmptyItems_shouldReject() {
        // Arrange
        CreateOrderRequest request = buildRequest(5, Collections.emptyList());

        // Act & Assert
        assertThatThrownBy(() -> orderValidator.validateCreateOrderRequest(request))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("at least one item");
    }

    // ── UNIT-DOM-08: inactive product should be distinguishable from not found ──

    /**
     * EXPECTED TO FAIL — The current implementation throws
     * {@link ProductNotFoundException} for BOTH non-existent and inactive products.
     * <p>
     * Per TEST_PLAN_V3, inactive products should produce a distinguishable error
     * (e.g. {@link InvalidOrderException} or a distinct message) so the client
     * receives HTTP 422 instead of 404.
     * Once the production code differentiates between "not found" and "inactive",
     * this test should pass.
     * </p>
     */
    @Test
    @DisplayName("UNIT-DOM-08 — inactive product should produce a distinct exception from non-existent product [EXPECTED FAIL — same exception type]")
    void validateCreateOrderRequest_withInactiveProduct_shouldRejectDistinctly() {
        // Arrange
        when(productRepository.findById(2L)).thenReturn(Optional.of(inactiveProduct));
        List<OrderItemRequest> items = List.of(new OrderItemRequest(2L, 1, null));
        CreateOrderRequest request = buildRequest(3, items);

        // Act & Assert
        // BUG: Both inactive and non-existent products currently throw
        // ProductNotFoundException with the same message format.
        // The inactive case should throw a DIFFERENT exception type
        // (e.g. InvalidOrderException) to allow the API to return 422 instead of 404.
        assertThatThrownBy(() -> orderValidator.validateCreateOrderRequest(request))
                .isNotInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("inactive");
    }

    // ── UNIT-DOM-09: tableId = 1 (lower boundary, accepted) ─────────────

    @Test
    @DisplayName("UNIT-DOM-09 — tableId=1 (lower boundary) should be accepted")
    void validateCreateOrderRequest_withTableId1_shouldAccept() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct));
        CreateOrderRequest request = buildRequest(1, validItems());

        // Act & Assert
        assertThatCode(() -> orderValidator.validateCreateOrderRequest(request))
                .doesNotThrowAnyException();
    }

    // ── UNIT-DOM-10: tableId = 12 (upper boundary, accepted) ────────────

    @Test
    @DisplayName("UNIT-DOM-10 — tableId=12 (upper boundary) should be accepted")
    void validateCreateOrderRequest_withTableId12_shouldAccept() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct));
        CreateOrderRequest request = buildRequest(12, validItems());

        // Act & Assert
        // NOTE: This passes because the current code has no upper-bound check,
        // so any positive tableId is accepted. Once UNIT-DOM-06 fix adds the
        // upper bound of 12, this test verifies 12 is still within range.
        assertThatCode(() -> orderValidator.validateCreateOrderRequest(request))
                .doesNotThrowAnyException();
    }
}
