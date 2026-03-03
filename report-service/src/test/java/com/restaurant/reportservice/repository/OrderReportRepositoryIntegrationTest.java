package com.restaurant.reportservice.repository;

import com.restaurant.reportservice.entity.OrderReportEntity;
import com.restaurant.reportservice.entity.OrderItemReportEntity;
import com.restaurant.reportservice.enums.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for OrderReportRepository.
 * Uses H2 in-memory database via @DataJpaTest.
 */
@DataJpaTest
@ActiveProfiles("test")
class OrderReportRepositoryIntegrationTest {

    @Autowired
    private OrderReportRepository orderReportRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        orderReportRepository.deleteAll();
    }

    @Test
    @DisplayName("Should persist order with items successfully")
    void shouldPersistOrderWithItems() {
        // Arrange
        OrderReportEntity order = createOrderEntity(OrderStatus.READY,
                createItem(1L, "Hamburguesa", 2, new BigDecimal("15.50")),
                createItem(2L, "Gaseosa", 1, new BigDecimal("3.00"))
        );

        // Act
        OrderReportEntity saved = orderReportRepository.save(order);
        entityManager.flush();
        entityManager.clear();

        // Assert
        OrderReportEntity found = orderReportRepository.findById(saved.getId()).orElseThrow();
        assertEquals(2, found.getItems().size());
        assertEquals(OrderStatus.READY, found.getStatus());
    }

    @Test
    @DisplayName("Should find orders by status")
    void shouldFindOrdersByStatus() {
        // Arrange
        OrderReportEntity ready1 = createOrderEntity(OrderStatus.READY);
        OrderReportEntity ready2 = createOrderEntity(OrderStatus.READY);
        OrderReportEntity pending = createOrderEntity(OrderStatus.PENDING);

        orderReportRepository.saveAll(Arrays.asList(ready1, ready2, pending));
        entityManager.flush();

        // Act
        List<OrderReportEntity> readyOrders = orderReportRepository.findByStatus(OrderStatus.READY);

        // Assert
        assertEquals(2, readyOrders.size());
        assertTrue(readyOrders.stream().allMatch(o -> o.getStatus() == OrderStatus.READY));
    }

    @Test
    @DisplayName("Should find orders by status and created date range")
    void shouldFindOrdersByStatusAndDateRange() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2026, 2, 10, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 2, 20, 23, 59);

        OrderReportEntity inRange = createOrderEntityWithDate(
                OrderStatus.READY, LocalDateTime.of(2026, 2, 15, 12, 0));
        OrderReportEntity beforeRange = createOrderEntityWithDate(
                OrderStatus.READY, LocalDateTime.of(2026, 2, 5, 12, 0));
        OrderReportEntity afterRange = createOrderEntityWithDate(
                OrderStatus.READY, LocalDateTime.of(2026, 2, 25, 12, 0));

        orderReportRepository.saveAll(Arrays.asList(inRange, beforeRange, afterRange));
        entityManager.flush();

        // Act
        List<OrderReportEntity> results = orderReportRepository
                .findByStatusAndCreatedAtBetween(OrderStatus.READY, start, end);

        // Assert
        assertEquals(1, results.size());
        assertEquals(inRange.getId(), results.get(0).getId());
    }

    @Test
    @DisplayName("Should include orders at start and end boundaries (inclusive)")
    void shouldIncludeBoundaryDates() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2026, 2, 10, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 2, 20, 23, 59, 59);

        OrderReportEntity atStart = createOrderEntityWithDate(
                OrderStatus.READY, LocalDateTime.of(2026, 2, 10, 0, 0, 0));
        OrderReportEntity atEnd = createOrderEntityWithDate(
                OrderStatus.READY, LocalDateTime.of(2026, 2, 20, 23, 59, 59));

        orderReportRepository.saveAll(Arrays.asList(atStart, atEnd));
        entityManager.flush();

        // Act
        List<OrderReportEntity> results = orderReportRepository
                .findByStatusAndCreatedAtBetween(OrderStatus.READY, start, end);

        // Assert
        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("Should filter by status correctly in date range query")
    void shouldFilterByStatusInDateRangeQuery() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2026, 2, 10, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 2, 20, 23, 59);

        OrderReportEntity readyInRange = createOrderEntityWithDate(
                OrderStatus.READY, LocalDateTime.of(2026, 2, 15, 12, 0));
        OrderReportEntity pendingInRange = createOrderEntityWithDate(
                OrderStatus.PENDING, LocalDateTime.of(2026, 2, 15, 12, 0));

        orderReportRepository.saveAll(Arrays.asList(readyInRange, pendingInRange));
        entityManager.flush();

        // Act
        List<OrderReportEntity> results = orderReportRepository
                .findByStatusAndCreatedAtBetween(OrderStatus.READY, start, end);

        // Assert
        assertEquals(1, results.size());
        assertEquals(OrderStatus.READY, results.get(0).getStatus());
    }

    @Test
    @DisplayName("Should return empty list when no orders match criteria")
    void shouldReturnEmptyListWhenNoMatches() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2026, 2, 10, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 2, 20, 23, 59);

        // Act
        List<OrderReportEntity> results = orderReportRepository
                .findByStatusAndCreatedAtBetween(OrderStatus.READY, start, end);

        // Assert
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should cascade delete items when order is deleted")
    void shouldCascadeDeleteItems() {
        // Arrange
        OrderReportEntity order = createOrderEntity(OrderStatus.READY,
                createItem(1L, "Hamburguesa", 2, new BigDecimal("15.50"))
        );
        OrderReportEntity saved = orderReportRepository.save(order);
        entityManager.flush();

        // Act
        orderReportRepository.delete(saved);
        entityManager.flush();

        // Assert
        assertFalse(orderReportRepository.findById(saved.getId()).isPresent());
    }

    // Helper methods
    private OrderReportEntity createOrderEntity(OrderStatus status, OrderItemReportEntity... items) {
        return createOrderEntityWithDate(status, LocalDateTime.now(), items);
    }

    private OrderReportEntity createOrderEntityWithDate(OrderStatus status, LocalDateTime createdAt, 
                                                        OrderItemReportEntity... items) {
        OrderReportEntity order = OrderReportEntity.builder()
                .id(UUID.randomUUID())
                .tableId(1)
                .status(status)
                .createdAt(createdAt)
                .receivedAt(LocalDateTime.now())
                .build();

        for (OrderItemReportEntity item : items) {
            order.addItem(item);
        }

        return order;
    }

    private OrderItemReportEntity createItem(Long productId, String productName, 
                                             Integer quantity, BigDecimal price) {
        return OrderItemReportEntity.builder()
                .productId(productId)
                .productName(productName)
                .quantity(quantity)
                .price(price)
                .build();
    }
}
