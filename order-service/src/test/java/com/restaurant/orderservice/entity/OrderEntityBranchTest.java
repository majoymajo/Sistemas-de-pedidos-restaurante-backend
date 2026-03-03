package com.restaurant.orderservice.entity;

import com.restaurant.orderservice.enums.OrderStatus;
import com.restaurant.orderservice.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Branch coverage tests for Order entity.
 * Tests lifecycle callbacks and state transition methods.
 */
class OrderEntityBranchTest {

    private Order order;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        order = new Order();
        order.setId(orderId);
        order.setTableId(5);
        order.setStatus(OrderStatus.PENDING);
        order.setItems(new ArrayList<>());
    }

    @Test
    void markAsDeleted_setsDeletedFlagAndTimestamp() {
        // Act
        order.markAsDeleted();

        // Assert
        assertThat(order.isDeleted()).isTrue();
        assertThat(order.getDeletedAt()).isNotNull();
    }

    @Test
    void markAsDeleted_calledMultipleTimes_overwritesTimestamp() {
        // Act
        order.markAsDeleted();
        LocalDateTime firstDeletion = order.getDeletedAt();
        
        try {
            Thread.sleep(10); // Small delay to ensure timestamp difference
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        order.markAsDeleted();
        LocalDateTime secondDeletion = order.getDeletedAt();

        // Assert
        assertThat(order.isDeleted()).isTrue();
        assertThat(secondDeletion).isAfterOrEqualTo(firstDeletion);
    }

    @Test
    void updateStatus_fromPendingToInPreparation_succeeds() {
        // Arrange
        order.setStatus(OrderStatus.PENDING);

        // Act
        order.updateStatus(OrderStatus.IN_PREPARATION);

        // Assert
        assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_PREPARATION);
    }

    @Test
    void updateStatus_fromInPreparationToReady_succeeds() {
        // Arrange
        order.setStatus(OrderStatus.IN_PREPARATION);

        // Act
        order.updateStatus(OrderStatus.READY);

        // Assert
        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY);
    }

    @Test
    void updateStatus_withInvalidTransition_throwsException() {
        // Arrange
        order.setStatus(OrderStatus.PENDING);

        // Act & Assert
        assertThatThrownBy(() -> order.updateStatus(OrderStatus.READY))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void updateStatus_fromReadyToAnyState_throwsException() {
        // Arrange
        order.setStatus(OrderStatus.READY);

        // Act & Assert
        assertThatThrownBy(() -> order.updateStatus(OrderStatus.PENDING))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void constructorDefaults_setsDefaultValues() {
        // Arrange & Act
        Order newOrder = new Order();

        // Assert
        assertThat(newOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(newOrder.getItems()).isEmpty();
        assertThat(newOrder.isDeleted()).isFalse();
        assertThat(newOrder.getDeletedAt()).isNull();
    }

    @Test
    void allArgsConstructor_setsAllFields() {
        // Arrange
        UUID id = UUID.randomUUID();
        ArrayList<OrderItem> items = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Act
        Order newOrder = new Order(
                id,
                5,
                OrderStatus.PENDING,
                items,
                now,
                now,
                false,
                null
        );

        // Assert
        assertThat(newOrder.getId()).isEqualTo(id);
        assertThat(newOrder.getTableId()).isEqualTo(5);
        assertThat(newOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(newOrder.getItems()).isEqualTo(items);
        assertThat(newOrder.isDeleted()).isFalse();
        assertThat(newOrder.getDeletedAt()).isNull();
    }

    @Test
    void orderWithMultipleItems_maintainsItemList() {
        // Arrange
        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setProductId(1L);
        item1.setQuantity(2);
        item1.setOrder(order);

        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setProductId(3L);
        item2.setQuantity(1);
        item2.setOrder(order);

        // Act
        order.setItems(new ArrayList<>());
        order.getItems().add(item1);
        order.getItems().add(item2);

        // Assert
        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getItems().get(0).getProductId()).isEqualTo(1L);
        assertThat(order.getItems().get(1).getProductId()).isEqualTo(3L);
    }

    @Test
    void deletedOrderCanStillHaveStatus() {
        // Arrange
        order.setStatus(OrderStatus.IN_PREPARATION);

        // Act
        order.markAsDeleted();

        // Assert - Order can be deleted while in any status
        assertThat(order.isDeleted()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_PREPARATION);
        assertThat(order.getDeletedAt()).isNotNull();
    }
}
