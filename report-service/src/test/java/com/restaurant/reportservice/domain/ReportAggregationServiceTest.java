package com.restaurant.reportservice.domain;

import com.restaurant.reportservice.domain.model.OrderReport;
import com.restaurant.reportservice.domain.model.OrderItemReport;
import com.restaurant.reportservice.domain.model.ReportSummary;
import com.restaurant.reportservice.domain.model.ProductSummary;
import com.restaurant.reportservice.domain.service.ReportAggregationService;
import com.restaurant.reportservice.enums.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Domain tests for report aggregation logic.
 * Tests pure business logic without infrastructure dependencies.
 */
class ReportAggregationServiceTest {

    private ReportAggregationService aggregationService;

    @BeforeEach
    void setUp() {
        aggregationService = new ReportAggregationService();
    }

    @Test
    @DisplayName("Should correctly aggregate revenue from multiple ready orders")
    void shouldAggregateRevenueFromMultipleReadyOrders() {
        // Arrange
        List<OrderReport> orders = Arrays.asList(
                createOrder(OrderStatus.READY, 
                    createItem(1L, "Hamburguesa", 2, new BigDecimal("15.50")),
                    createItem(2L, "Gaseosa", 1, new BigDecimal("3.00"))
                ),
                createOrder(OrderStatus.READY,
                    createItem(1L, "Hamburguesa", 1, new BigDecimal("15.50"))
                )
        );

        // Act
        ReportSummary summary = aggregationService.aggregate(orders);

        // Assert
        assertEquals(new BigDecimal("49.50"), summary.getTotalRevenue());
        assertEquals(2, summary.getTotalReadyOrders());
    }

    @Test
    @DisplayName("Should return zero metrics for empty order list")
    void shouldReturnZeroMetricsForEmptyList() {
        // Arrange
        List<OrderReport> emptyOrders = Collections.emptyList();

        // Act
        ReportSummary summary = aggregationService.aggregate(emptyOrders);

        // Assert
        assertEquals(BigDecimal.ZERO, summary.getTotalRevenue());
        assertEquals(0, summary.getTotalReadyOrders());
        assertTrue(summary.getProductBreakdown().isEmpty());
    }

    @Test
    @DisplayName("Should correctly sum quantities by product")
    void shouldSumQuantitiesByProduct() {
        // Arrange
        List<OrderReport> orders = Arrays.asList(
                createOrder(OrderStatus.READY,
                    createItem(1L, "Hamburguesa", 2, new BigDecimal("15.50"))
                ),
                createOrder(OrderStatus.READY,
                    createItem(1L, "Hamburguesa", 3, new BigDecimal("15.50"))
                ),
                createOrder(OrderStatus.READY,
                    createItem(2L, "Pizza", 1, new BigDecimal("20.00"))
                )
        );

        // Act
        ReportSummary summary = aggregationService.aggregate(orders);

        // Assert
        List<ProductSummary> breakdown = summary.getProductBreakdown();
        assertEquals(2, breakdown.size());
        
        ProductSummary hamburguesa = findProduct(breakdown, 1L);
        assertEquals(5, hamburguesa.getQuantitySold());
        assertEquals(new BigDecimal("77.50"), hamburguesa.getTotalAccumulated());
        
        ProductSummary pizza = findProduct(breakdown, 2L);
        assertEquals(1, pizza.getQuantitySold());
        assertEquals(new BigDecimal("20.00"), pizza.getTotalAccumulated());
    }

    @Test
    @DisplayName("Should aggregate only READY orders and ignore others")
    void shouldAggregateOnlyReadyOrders() {
        // Arrange
        List<OrderReport> orders = Arrays.asList(
                createOrder(OrderStatus.READY,
                    createItem(1L, "Hamburguesa", 1, new BigDecimal("15.50"))
                ),
                createOrder(OrderStatus.PENDING,
                    createItem(1L, "Hamburguesa", 10, new BigDecimal("15.50"))
                ),
                createOrder(OrderStatus.IN_PREPARATION,
                    createItem(2L, "Pizza", 5, new BigDecimal("20.00"))
                )
        );

        // Act
        ReportSummary summary = aggregationService.aggregate(orders);

        // Assert
        assertEquals(1, summary.getTotalReadyOrders());
        assertEquals(new BigDecimal("15.50"), summary.getTotalRevenue());
        assertEquals(1, summary.getProductBreakdown().size());
    }

    @Test
    @DisplayName("Should handle orders with multiple items correctly")
    void shouldHandleOrdersWithMultipleItems() {
        // Arrange
        OrderReport order = createOrder(OrderStatus.READY,
                createItem(1L, "Hamburguesa", 2, new BigDecimal("15.50")),
                createItem(2L, "Pizza", 1, new BigDecimal("20.00")),
                createItem(3L, "Gaseosa", 3, new BigDecimal("3.00"))
        );

        // Act
        ReportSummary summary = aggregationService.aggregate(Collections.singletonList(order));

        // Assert
        assertEquals(new BigDecimal("60.00"), summary.getTotalRevenue());
        assertEquals(3, summary.getProductBreakdown().size());
    }

    @Test
    @DisplayName("Should handle null or empty items list gracefully")
    void shouldHandleNullOrEmptyItems() {
        // Arrange
        OrderReport orderWithNoItems = OrderReport.builder()
                .id(UUID.randomUUID())
                .status(OrderStatus.READY)
                .items(Collections.emptyList())
                .createdAt(LocalDateTime.now())
                .build();

        // Act
        ReportSummary summary = aggregationService.aggregate(Collections.singletonList(orderWithNoItems));

        // Assert
        assertEquals(BigDecimal.ZERO, summary.getTotalRevenue());
        assertEquals(1, summary.getTotalReadyOrders());
        assertTrue(summary.getProductBreakdown().isEmpty());
    }

    @Test
    @DisplayName("Should calculate total accumulated per product correctly")
    void shouldCalculateTotalAccumulatedPerProduct() {
        // Arrange
        List<OrderReport> orders = Arrays.asList(
                createOrder(OrderStatus.READY,
                    createItem(1L, "Hamburguesa", 2, new BigDecimal("15.50"))
                ),
                createOrder(OrderStatus.READY,
                    createItem(1L, "Hamburguesa", 1, new BigDecimal("15.50"))
                )
        );

        // Act
        ReportSummary summary = aggregationService.aggregate(orders);

        // Assert
        ProductSummary product = summary.getProductBreakdown().get(0);
        assertEquals(new BigDecimal("46.50"), product.getTotalAccumulated());
    }

    // Helper methods
    private OrderReport createOrder(OrderStatus status, OrderItemReport... items) {
        return OrderReport.builder()
                .id(UUID.randomUUID())
                .tableId(1)
                .status(status)
                .items(Arrays.asList(items))
                .createdAt(LocalDateTime.now())
                .receivedAt(LocalDateTime.now())
                .build();
    }

    private OrderItemReport createItem(Long productId, String productName, Integer quantity, BigDecimal price) {
        return OrderItemReport.builder()
                .productId(productId)
                .productName(productName)
                .quantity(quantity)
                .price(price)
                .build();
    }

    private ProductSummary findProduct(List<ProductSummary> breakdown, Long productId) {
        return breakdown.stream()
                .filter(p -> p.getProductId().equals(productId))
                .findFirst()
                .orElseThrow();
    }
}
