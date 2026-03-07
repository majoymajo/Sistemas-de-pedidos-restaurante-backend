package com.restaurant.orderservice.service;

import com.restaurant.orderservice.dto.OrderItemResponse;
import com.restaurant.orderservice.dto.OrderResponse;
import com.restaurant.orderservice.entity.Order;
import com.restaurant.orderservice.entity.OrderItem;
import com.restaurant.orderservice.entity.Product;
import com.restaurant.orderservice.enums.OrderStatus;
import com.restaurant.orderservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Branch coverage tests for OrderMapper.
 * Tests the null product handling branch in mapToOrderItemResponse.
 */
@ExtendWith(MockitoExtension.class)
class OrderMapperBranchTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderMapper orderMapper;

    @BeforeEach
    void setUp() {
    }

    @Test
    void mapToOrderResponse_withMissingProduct_returnsDefaultProductName() {
        // Arrange - Create an order with an item for a product that doesn't exist in the database
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setTableId(5);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setProductId(999L); // Non-existent product
        item.setQuantity(2);
        item.setNote("Sin cebolla");
        item.setOrder(order);

        order.setItems(List.of(item));

        // When productRepository returns empty list (product not found)
        when(productRepository.findAllById(any())).thenReturn(new ArrayList<>());

        // Act
        OrderResponse response = orderMapper.mapToOrderResponse(order);

        // Assert - Verify that the default product name is used
        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        OrderItemResponse itemResponse = response.getItems().get(0);
        assertThat(itemResponse.getProductName()).isEqualTo("Producto desconocido");
        assertThat(itemResponse.getQuantity()).isEqualTo(2);
    }

    @Test
    void mapToOrderResponse_withMultipleItems_handlesPartiallyMissingProducts() {
        // Arrange - Create an order with multiple items, only one product exists
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setTableId(3);
        order.setStatus(OrderStatus.IN_PREPARATION);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setProductId(1L);
        item1.setQuantity(2);
        item1.setNote(null);
        item1.setOrder(order);

        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setProductId(999L); // Non-existent product
        item2.setQuantity(1);
        item2.setNote("Extra spicy");
        item2.setOrder(order);

        order.setItems(List.of(item1, item2));

        // Create a product for item1
        Product existingProduct = new Product();
        existingProduct.setId(1L);
        existingProduct.setName("Hamburguesa");

        when(productRepository.findAllById(any())).thenReturn(List.of(existingProduct));

        // Act
        OrderResponse response = orderMapper.mapToOrderResponse(order);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(2);
        
        // First item should have product name
        assertThat(response.getItems().get(0).getProductName()).isEqualTo("Hamburguesa");
        
        // Second item should have default name (product not found)
        assertThat(response.getItems().get(1).getProductName()).isEqualTo("Producto desconocido");
    }

    @Test
    void mapToOrderResponse_withEmptyItems_returnsEmptyItemList() {
        // Arrange
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setTableId(7);
        order.setStatus(OrderStatus.READY);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setItems(new ArrayList<>());

        when(productRepository.findAllById(any())).thenReturn(new ArrayList<>());

        // Act
        OrderResponse response = orderMapper.mapToOrderResponse(order);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    void mapToOrderResponseList_withMultipleOrders_mapsSucessfully() {
        // Arrange
        List<Order> orders = new ArrayList<>();
        
        Order order1 = new Order();
        order1.setId(UUID.randomUUID());
        order1.setTableId(1);
        order1.setStatus(OrderStatus.PENDING);
        order1.setCreatedAt(LocalDateTime.now());
        order1.setUpdatedAt(LocalDateTime.now());
        order1.setItems(new ArrayList<>());
        orders.add(order1);
        
        Order order2 = new Order();
        order2.setId(UUID.randomUUID());
        order2.setTableId(2);
        order2.setStatus(OrderStatus.IN_PREPARATION);
        order2.setCreatedAt(LocalDateTime.now());
        order2.setUpdatedAt(LocalDateTime.now());
        order2.setItems(new ArrayList<>());
        orders.add(order2);

        when(productRepository.findAllById(any())).thenReturn(new ArrayList<>());

        // Act
        List<OrderResponse> responses = orderMapper.mapToOrderResponseList(orders);

        // Assert
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getTableId()).isEqualTo(1);
        assertThat(responses.get(1).getTableId()).isEqualTo(2);
    }
}
