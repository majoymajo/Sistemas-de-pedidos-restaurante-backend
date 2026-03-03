package com.restaurant.reportservice.domain;

import com.restaurant.reportservice.domain.model.OrderReport;
import com.restaurant.reportservice.domain.model.OrderItemReport;
import com.restaurant.reportservice.domain.model.ReportSummary;
import com.restaurant.reportservice.domain.service.ReportAggregationService;
import com.restaurant.reportservice.enums.OrderStatus;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for report aggregation using jqwik.
 * Validates invariants across generated datasets.
 */
class ReportAggregationPropertyTest {

    private final ReportAggregationService aggregationService = new ReportAggregationService();

    @Property
    @Label("Total revenue should equal sum of all item prices × quantities for READY orders")
    void totalRevenueShouldEqualSumOfAllItems(@ForAll("readyOrders") List<OrderReport> orders) {
        // Calculate expected revenue manually
        BigDecimal expectedRevenue = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.READY)
                .flatMap(order -> order.getItems().stream())
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Act
        ReportSummary summary = aggregationService.aggregate(orders);

        // Assert
        assertEquals(expectedRevenue, summary.getTotalRevenue());
    }

    @Property
    @Label("Total ready orders count should match READY status orders")
    void totalReadyOrdersShouldMatchReadyStatusCount(@ForAll("mixedStatusOrders") List<OrderReport> orders) {
        // Calculate expected count
        long expectedCount = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.READY)
                .count();

        // Act
        ReportSummary summary = aggregationService.aggregate(orders);

        // Assert
        assertEquals((int) expectedCount, summary.getTotalReadyOrders());
    }

    @Property
    @Label("Product quantity sold should equal sum of quantities across all READY orders")
    void productQuantityShouldMatchSumAcrossOrders(@ForAll("ordersWithSameProduct") List<OrderReport> orders) {
        // All orders contain items with productId = 1
        int expectedQuantity = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.READY)
                .flatMap(order -> order.getItems().stream())
                .filter(item -> item.getProductId().equals(1L))
                .mapToInt(OrderItemReport::getQuantity)
                .sum();

        // Act
        ReportSummary summary = aggregationService.aggregate(orders);

        // Assert
        if (expectedQuantity > 0) {
            assertEquals(1, summary.getProductBreakdown().size());
            assertEquals(expectedQuantity, summary.getProductBreakdown().get(0).getQuantitySold());
        }
    }

    @Property
    @Label("Empty order list should always produce zero metrics")
    void emptyOrderListShouldProduceZeroMetrics() {
        // Act
        ReportSummary summary = aggregationService.aggregate(List.of());

        // Assert
        assertEquals(BigDecimal.ZERO, summary.getTotalRevenue());
        assertEquals(0, summary.getTotalReadyOrders());
        assertTrue(summary.getProductBreakdown().isEmpty());
    }

    @Property
    @Label("Non-READY orders should never contribute to revenue")
    void nonReadyOrdersShouldNotContributeToRevenue(@ForAll("nonReadyOrders") List<OrderReport> orders) {
        // Act
        ReportSummary summary = aggregationService.aggregate(orders);

        // Assert
        assertEquals(BigDecimal.ZERO, summary.getTotalRevenue());
        assertEquals(0, summary.getTotalReadyOrders());
        assertTrue(summary.getProductBreakdown().isEmpty());
    }

    // Providers for property-based testing

    @Provide
    Arbitrary<List<OrderReport>> readyOrders() {
        return Arbitraries.of(OrderStatus.READY)
                .flatMap(this::orderWithStatus)
                .list().ofMinSize(1).ofMaxSize(20);
    }

    @Provide
    Arbitrary<List<OrderReport>> mixedStatusOrders() {
        return orderWithStatus(null)
                .list().ofMinSize(1).ofMaxSize(30);
    }

    @Provide
    Arbitrary<List<OrderReport>> ordersWithSameProduct() {
        return Arbitraries.of(OrderStatus.READY, OrderStatus.PENDING, OrderStatus.IN_PREPARATION)
                .flatMap(status -> orderWithStatus(status).map(order -> {
                    // Force all items to have productId = 1
                    List<OrderItemReport> items = order.getItems().stream()
                            .map(item -> OrderItemReport.builder()
                                    .productId(1L)
                                    .productName("Test Product")
                                    .quantity(item.getQuantity())
                                    .price(item.getPrice())
                                    .build())
                            .collect(Collectors.toList());
                    return OrderReport.builder()
                            .id(order.getId())
                            .tableId(order.getTableId())
                            .status(order.getStatus())
                            .items(items)
                            .createdAt(order.getCreatedAt())
                            .receivedAt(order.getReceivedAt())
                            .build();
                }))
                .list().ofMinSize(1).ofMaxSize(15);
    }

    @Provide
    Arbitrary<List<OrderReport>> nonReadyOrders() {
        return Arbitraries.of(OrderStatus.PENDING, OrderStatus.IN_PREPARATION)
                .flatMap(this::orderWithStatus)
                .list().ofMinSize(1).ofMaxSize(20);
    }

    private Arbitrary<OrderReport> orderWithStatus(OrderStatus fixedStatus) {
        Arbitrary<OrderStatus> statusArbitrary = fixedStatus != null 
                ? Arbitraries.just(fixedStatus)
                : Arbitraries.of(OrderStatus.READY, OrderStatus.PENDING, OrderStatus.IN_PREPARATION);

        return Combinators.combine(
                statusArbitrary,
                orderItems(),
                Arbitraries.integers().between(1, 12)
        ).as((status, items, tableId) -> OrderReport.builder()
                .id(UUID.randomUUID())
                .tableId(tableId)
                .status(status)
                .items(items)
                .createdAt(LocalDateTime.now())
                .receivedAt(LocalDateTime.now())
                .build());
    }

    private Arbitrary<List<OrderItemReport>> orderItems() {
        return orderItem().list().ofMinSize(1).ofMaxSize(5);
    }

    private Arbitrary<OrderItemReport> orderItem() {
        return Combinators.combine(
                Arbitraries.longs().between(1, 100),
                Arbitraries.integers().between(1, 10),
                Arbitraries.bigDecimals()
                        .between(BigDecimal.ONE, new BigDecimal("100.00"))
                        .ofScale(2)
        ).as((productId, quantity, price) -> OrderItemReport.builder()
                .productId(productId)
                .productName("Product " + productId)
                .quantity(quantity)
                .price(price)
                .build());
    }
}
