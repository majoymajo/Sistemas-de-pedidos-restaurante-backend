package com.restaurant.reportservice.service;

import com.restaurant.reportservice.application.command.OrderPlacedCommand;
import com.restaurant.reportservice.application.command.OrderReadyCommand;
import com.restaurant.reportservice.entity.OrderItemReportEntity;
import com.restaurant.reportservice.entity.OrderReportEntity;
import com.restaurant.reportservice.enums.OrderStatus;
import com.restaurant.reportservice.repository.OrderReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderEventProcessingService.
 * Verifies order event projection logic (order.placed and order.ready).
 */
@ExtendWith(MockitoExtension.class)
class OrderEventProcessingServiceTest {

    @Mock
    private OrderReportRepository orderReportRepository;

    @Captor
    private ArgumentCaptor<OrderReportEntity> orderCaptor;

    private OrderEventProcessingService service;

    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-02-19T15:30:00Z"), ZoneId.of("UTC"));
        service = new OrderEventProcessingService(orderReportRepository, fixedClock);
    }

    // ── processOrderPlaced tests ────────────────────────────────────────

    @Test
    @DisplayName("Should save new order when order does not exist")
    void shouldSaveNewOrderWhenOrderDoesNotExist() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        int tableId = 3;
        LocalDateTime createdAt = LocalDateTime.of(2026, 2, 19, 12, 0);

        OrderPlacedCommand command = OrderPlacedCommand.builder()
                .orderId(orderId)
                .tableId(tableId)
                .createdAt(createdAt)
                .items(List.of())
                .build();

        when(orderReportRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act
        service.processOrderPlaced(command);

        // Assert
        verify(orderReportRepository).save(orderCaptor.capture());
        OrderReportEntity saved = orderCaptor.getValue();

        assertEquals(orderId, saved.getId());
        assertEquals(tableId, saved.getTableId());
        assertEquals(OrderStatus.PENDING, saved.getStatus());
        assertEquals(createdAt, saved.getCreatedAt());
        assertNotNull(saved.getReceivedAt());
    }

    @Test
    @DisplayName("Should skip save when order already exists (idempotent)")
    void shouldSkipSaveWhenOrderAlreadyExists() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        OrderReportEntity existingOrder = OrderReportEntity.builder()
                .id(orderId)
                .tableId(1)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .receivedAt(LocalDateTime.now())
                .build();

        OrderPlacedCommand command = OrderPlacedCommand.builder()
                .orderId(orderId)
                .tableId(1)
                .createdAt(LocalDateTime.now())
                .build();

        when(orderReportRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));

        // Act
        service.processOrderPlaced(command);

        // Assert
        verify(orderReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should map command items to entity items correctly")
    void shouldMapCommandItemsToEntityItems() {
        // Arrange
        UUID orderId = UUID.randomUUID();

        OrderPlacedCommand.OrderItemCommand item1 = OrderPlacedCommand.OrderItemCommand.builder()
                .productId(10L)
                .productName("Burger")
                .quantity(2)
                .price(new BigDecimal("12.50"))
                .build();

        OrderPlacedCommand.OrderItemCommand item2 = OrderPlacedCommand.OrderItemCommand.builder()
                .productId(20L)
                .productName("Fries")
                .quantity(1)
                .price(new BigDecimal("5.00"))
                .build();

        OrderPlacedCommand command = OrderPlacedCommand.builder()
                .orderId(orderId)
                .tableId(7)
                .createdAt(LocalDateTime.now())
                .items(List.of(item1, item2))
                .build();

        when(orderReportRepository.findById(any())).thenReturn(Optional.empty());

        // Act
        service.processOrderPlaced(command);

        // Assert
        verify(orderReportRepository).save(orderCaptor.capture());
        OrderReportEntity saved = orderCaptor.getValue();

        assertEquals(2, saved.getItems().size());

        OrderItemReportEntity savedItem1 = saved.getItems().get(0);
        assertEquals(10L, savedItem1.getProductId());
        assertEquals("Burger", savedItem1.getProductName());
        assertEquals(2, savedItem1.getQuantity());
        assertEquals(new BigDecimal("12.50"), savedItem1.getPrice());
        assertSame(saved, savedItem1.getOrder());

        OrderItemReportEntity savedItem2 = saved.getItems().get(1);
        assertEquals(20L, savedItem2.getProductId());
        assertEquals("Fries", savedItem2.getProductName());
        assertEquals(1, savedItem2.getQuantity());
        assertEquals(new BigDecimal("5.00"), savedItem2.getPrice());
        assertSame(saved, savedItem2.getOrder());
    }

    @Test
    @DisplayName("Should handle null items gracefully")
    void shouldHandleNullItemsGracefully() {
        // Arrange
        UUID orderId = UUID.randomUUID();

        OrderPlacedCommand command = OrderPlacedCommand.builder()
                .orderId(orderId)
                .tableId(4)
                .createdAt(LocalDateTime.now())
                .items(null)
                .build();

        when(orderReportRepository.findById(any())).thenReturn(Optional.empty());

        // Act — should not throw
        assertDoesNotThrow(() -> service.processOrderPlaced(command));

        // Assert
        verify(orderReportRepository).save(orderCaptor.capture());
        OrderReportEntity saved = orderCaptor.getValue();

        assertEquals(orderId, saved.getId());
        assertTrue(saved.getItems().isEmpty());
    }

    @Test
    @DisplayName("Should handle empty items list gracefully")
    void shouldHandleEmptyItemsGracefully() {
        // Arrange
        UUID orderId = UUID.randomUUID();

        OrderPlacedCommand command = OrderPlacedCommand.builder()
                .orderId(orderId)
                .tableId(2)
                .createdAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();

        when(orderReportRepository.findById(any())).thenReturn(Optional.empty());

        // Act
        service.processOrderPlaced(command);

        // Assert
        verify(orderReportRepository).save(orderCaptor.capture());
        OrderReportEntity saved = orderCaptor.getValue();

        assertEquals(orderId, saved.getId());
        assertTrue(saved.getItems().isEmpty());
    }

    // ── processOrderReady tests ─────────────────────────────────────────

    @Test
    @DisplayName("Should update existing order status to READY")
    void shouldUpdateExistingOrderToReady() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        OrderReportEntity existingOrder = OrderReportEntity.builder()
                .id(orderId)
                .tableId(5)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.of(2026, 2, 19, 10, 0))
                .receivedAt(LocalDateTime.of(2026, 2, 19, 10, 1))
                .build();

        OrderReadyCommand command = OrderReadyCommand.builder()
                .orderId(orderId)
                .status(OrderStatus.READY)
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderReportRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));

        // Act
        service.processOrderReady(command);

        // Assert
        verify(orderReportRepository).save(orderCaptor.capture());
        OrderReportEntity saved = orderCaptor.getValue();

        assertEquals(OrderStatus.READY, saved.getStatus());
        assertEquals(orderId, saved.getId());
    }

    @Test
    @DisplayName("Should create stub order when order not found (upsert)")
    void shouldCreateStubOrderWhenOrderNotFound() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        LocalDateTime updatedAt = LocalDateTime.of(2026, 2, 19, 14, 30);

        OrderReadyCommand command = OrderReadyCommand.builder()
                .orderId(orderId)
                .status(OrderStatus.READY)
                .updatedAt(updatedAt)
                .build();

        when(orderReportRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act
        service.processOrderReady(command);

        // Assert
        verify(orderReportRepository).save(orderCaptor.capture());
        OrderReportEntity saved = orderCaptor.getValue();

        assertEquals(orderId, saved.getId());
        assertEquals(0, saved.getTableId());
        assertEquals(OrderStatus.READY, saved.getStatus());
        assertEquals(updatedAt, saved.getCreatedAt());
        assertNotNull(saved.getReceivedAt());
    }

    @Test
    @DisplayName("Should preserve existing order data on status update to READY")
    void shouldPreserveExistingOrderDataOnStatusUpdate() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        LocalDateTime originalCreatedAt = LocalDateTime.of(2026, 2, 19, 9, 0);
        LocalDateTime originalReceivedAt = LocalDateTime.of(2026, 2, 19, 9, 1);

        OrderItemReportEntity item = OrderItemReportEntity.builder()
                .id(1L)
                .productId(42L)
                .productName("Pizza")
                .quantity(3)
                .price(new BigDecimal("15.00"))
                .build();

        OrderReportEntity existingOrder = OrderReportEntity.builder()
                .id(orderId)
                .tableId(5)
                .status(OrderStatus.PENDING)
                .createdAt(originalCreatedAt)
                .receivedAt(originalReceivedAt)
                .items(new ArrayList<>(List.of(item)))
                .build();
        item.setOrder(existingOrder);

        OrderReadyCommand command = OrderReadyCommand.builder()
                .orderId(orderId)
                .status(OrderStatus.READY)
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderReportRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));

        // Act
        service.processOrderReady(command);

        // Assert
        verify(orderReportRepository).save(orderCaptor.capture());
        OrderReportEntity saved = orderCaptor.getValue();

        assertEquals(OrderStatus.READY, saved.getStatus());
        assertEquals(5, saved.getTableId());
        assertEquals(originalCreatedAt, saved.getCreatedAt());
        assertEquals(originalReceivedAt, saved.getReceivedAt());
        assertEquals(1, saved.getItems().size());
        assertEquals("Pizza", saved.getItems().get(0).getProductName());
    }

    // ── Clock injection tests ───────────────────────────────────────────

    @Test
    @DisplayName("Should use injected clock for receivedAt timestamp")
    void shouldUseInjectedClockForReceivedAt() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        OrderPlacedCommand command = OrderPlacedCommand.builder()
                .orderId(orderId)
                .tableId(1)
                .createdAt(LocalDateTime.of(2026, 2, 19, 12, 0))
                .items(List.of())
                .build();
        when(orderReportRepository.findById(any())).thenReturn(Optional.empty());

        // Act
        service.processOrderPlaced(command);

        // Assert
        verify(orderReportRepository).save(orderCaptor.capture());
        OrderReportEntity saved = orderCaptor.getValue();
        assertEquals(LocalDateTime.now(fixedClock), saved.getReceivedAt());
    }

    @Test
    @DisplayName("Should use injected clock for receivedAt on upsert READY")
    void shouldUseInjectedClockForReceivedAtOnUpsertReady() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        OrderReadyCommand command = OrderReadyCommand.builder()
                .orderId(orderId)
                .status(OrderStatus.READY)
                .updatedAt(LocalDateTime.of(2026, 2, 19, 14, 0))
                .build();
        when(orderReportRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act
        service.processOrderReady(command);

        // Assert
        verify(orderReportRepository).save(orderCaptor.capture());
        OrderReportEntity saved = orderCaptor.getValue();
        assertEquals(LocalDateTime.now(fixedClock), saved.getReceivedAt());
    }
}
