package com.restaurant.orderservice.service;

import com.restaurant.orderservice.entity.Order;
import com.restaurant.orderservice.entity.OrderItem;
import com.restaurant.orderservice.enums.OrderStatus;
import com.restaurant.orderservice.domain.event.OrderPlacedDomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for OrderEventBuilder.
 * 
 * Tests event construction from domain entities.
 */
class OrderEventBuilderTest {
    
    private OrderEventBuilder orderEventBuilder;
    
    @BeforeEach
    void setUp() {
        orderEventBuilder = new OrderEventBuilder();
    }
    
    @Test
    void buildOrderPlacedEvent_withSingleItem_createsCorrectEvent() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();
        
        Order order = new Order();
        order.setId(orderId);
        order.setTableId(5);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(createdAt);
        
        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setProductId(10L);
        item.setQuantity(2);
        item.setNote("No onions");
        
        order.setItems(List.of(item));
        
        // Act
        OrderPlacedDomainEvent event = orderEventBuilder.buildOrderPlacedEvent(order);
        
        // Assert
        assertThat(event).isNotNull();
        assertThat(event.getOrderId()).isEqualTo(orderId);
        assertThat(event.getTableId()).isEqualTo(5);
        assertThat(event.getCreatedAt()).isEqualTo(createdAt);
        assertThat(event.getItems()).hasSize(1);
        
        OrderPlacedDomainEvent.OrderItemData eventItem = event.getItems().get(0);
        assertThat(eventItem.getProductId()).isEqualTo(10L);
        assertThat(eventItem.getQuantity()).isEqualTo(2);
    }
    
    @Test
    void buildOrderPlacedEvent_withMultipleItems_includesAllItems() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        
        Order order = new Order();
        order.setId(orderId);
        order.setTableId(3);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        
        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setProductId(10L);
        item1.setQuantity(2);
        
        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setProductId(20L);
        item2.setQuantity(1);
        
        OrderItem item3 = new OrderItem();
        item3.setId(3L);
        item3.setProductId(30L);
        item3.setQuantity(3);
        
        order.setItems(List.of(item1, item2, item3));
        
        // Act
        OrderPlacedDomainEvent event = orderEventBuilder.buildOrderPlacedEvent(order);
        
        // Assert
        assertThat(event.getItems()).hasSize(3);
        assertThat(event.getItems().get(0).getProductId()).isEqualTo(10L);
        assertThat(event.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(event.getItems().get(1).getProductId()).isEqualTo(20L);
        assertThat(event.getItems().get(1).getQuantity()).isEqualTo(1);
        assertThat(event.getItems().get(2).getProductId()).isEqualTo(30L);
        assertThat(event.getItems().get(2).getQuantity()).isEqualTo(3);
    }
    
    @Test
    void buildOrderPlacedEvent_withEmptyItems_createsEventWithEmptyItemsList() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        
        Order order = new Order();
        order.setId(orderId);
        order.setTableId(5);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setItems(new ArrayList<>());
        
        // Act
        OrderPlacedDomainEvent event = orderEventBuilder.buildOrderPlacedEvent(order);
        
        // Assert
        assertThat(event).isNotNull();
        assertThat(event.getItems()).isEmpty();
    }
    
    @Test
    void buildOrderPlacedEvent_doesNotIncludeItemNotes() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        
        Order order = new Order();
        order.setId(orderId);
        order.setTableId(5);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        
        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setProductId(10L);
        item.setQuantity(2);
        item.setNote("This note should not be in the event");
        
        order.setItems(List.of(item));
        
        // Act
        OrderPlacedDomainEvent event = orderEventBuilder.buildOrderPlacedEvent(order);
        
        // Assert
        OrderPlacedDomainEvent.OrderItemData eventItem = event.getItems().get(0);
        assertThat(eventItem.getProductId()).isEqualTo(10L);
        assertThat(eventItem.getQuantity()).isEqualTo(2);
        // OrderItemEventData only has productId and quantity, no note field
    }
    
    @Test
    void buildOrderPlacedEvent_preservesOrderMetadata() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        LocalDateTime specificTime = LocalDateTime.of(2026, 2, 12, 10, 30, 0);
        
        Order order = new Order();
        order.setId(orderId);
        order.setTableId(7);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(specificTime);
        order.setItems(new ArrayList<>());
        
        // Act
        OrderPlacedDomainEvent event = orderEventBuilder.buildOrderPlacedEvent(order);
        
        // Assert
        assertThat(event.getOrderId()).isEqualTo(orderId);
        assertThat(event.getTableId()).isEqualTo(7);
        assertThat(event.getCreatedAt()).isEqualTo(specificTime);
    }
    
    @Test
    void buildOrderPlacedEvent_withLargeQuantities_handlesCorrectly() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        
        Order order = new Order();
        order.setId(orderId);
        order.setTableId(5);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        
        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setProductId(10L);
        item.setQuantity(100);
        
        order.setItems(List.of(item));
        
        // Act
        OrderPlacedDomainEvent event = orderEventBuilder.buildOrderPlacedEvent(order);
        
        // Assert
        assertThat(event.getItems().get(0).getQuantity()).isEqualTo(100);
    }
    
    @Test
    void buildOrderPlacedEvent_maintainsItemOrder() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        
        Order order = new Order();
        order.setId(orderId);
        order.setTableId(5);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        
        List<OrderItem> items = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            OrderItem item = new OrderItem();
            item.setId((long) i);
            item.setProductId((long) (i * 10));
            item.setQuantity(i);
            items.add(item);
        }
        
        order.setItems(items);
        
        // Act
        OrderPlacedDomainEvent event = orderEventBuilder.buildOrderPlacedEvent(order);
        
        // Assert
        assertThat(event.getItems()).hasSize(5);
        for (int i = 0; i < 5; i++) {
            assertThat(event.getItems().get(i).getProductId()).isEqualTo((long) ((i + 1) * 10));
            assertThat(event.getItems().get(i).getQuantity()).isEqualTo(i + 1);
        }
    }
}
