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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderMapper.
 * 
 * Tests entity-to-DTO mapping logic with N+1 optimization.
 */
@ExtendWith(MockitoExtension.class)
class OrderMapperTest {
    
    @Mock
    private ProductRepository productRepository;
    
    @InjectMocks
    private OrderMapper orderMapper;
    
    private Product product1;
    private Product product2;
    
    @BeforeEach
    void setUp() {
        product1 = new Product();
        product1.setId(1L);
        product1.setName("Pizza");
        product1.setIsActive(true);
        
        product2 = new Product();
        product2.setId(2L);
        product2.setName("Burger");
        product2.setIsActive(true);
    }
    
    @Test
    void mapToOrderResponse_withSingleItem_returnsCorrectResponse() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        Order order = new Order();
        order.setId(orderId);
        order.setTableId(5);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        
        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setOrder(order);
        item.setProductId(1L);
        item.setQuantity(2);
        item.setNote("No onions");
        
        order.setItems(List.of(item));
        
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(product1));
        
        // Act
        OrderResponse response = orderMapper.mapToOrderResponse(order);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(orderId);
        assertThat(response.getTableId()).isEqualTo(5);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getCreatedAt()).isEqualTo(now);
        assertThat(response.getUpdatedAt()).isEqualTo(now);
        assertThat(response.getItems()).hasSize(1);
        
        OrderItemResponse itemResponse = response.getItems().get(0);
        assertThat(itemResponse.getId()).isEqualTo(1L);
        assertThat(itemResponse.getProductId()).isEqualTo(1L);
        assertThat(itemResponse.getProductName()).isEqualTo("Pizza");
        assertThat(itemResponse.getQuantity()).isEqualTo(2);
        assertThat(itemResponse.getNote()).isEqualTo("No onions");
        
        // Verify batch loading (single query for all products)
        verify(productRepository, times(1)).findAllById(List.of(1L));
    }
    
    @Test
    void mapToOrderResponse_withMultipleItems_batchLoadsProducts() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        
        Order order = new Order();
        order.setId(orderId);
        order.setTableId(5);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        
        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setProductId(1L);
        item1.setQuantity(2);
        item1.setNote("No onions");
        
        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setProductId(2L);
        item2.setQuantity(1);
        item2.setNote("Extra cheese");
        
        order.setItems(List.of(item1, item2));
        
        when(productRepository.findAllById(anyList())).thenReturn(List.of(product1, product2));
        
        // Act
        OrderResponse response = orderMapper.mapToOrderResponse(order);
        
        // Assert
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getProductName()).isEqualTo("Pizza");
        assertThat(response.getItems().get(1).getProductName()).isEqualTo("Burger");
        
        // Verify N+1 optimization: only ONE batch query for all products
        verify(productRepository, times(1)).findAllById(anyList());
    }
    
    @Test
    void mapToOrderResponse_withDuplicateProducts_deduplicatesProductIds() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        
        Order order = new Order();
        order.setId(orderId);
        order.setTableId(5);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        
        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setProductId(1L);
        item1.setQuantity(2);
        
        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setProductId(1L);
        item2.setQuantity(3);
        
        order.setItems(List.of(item1, item2));
        
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(product1));
        
        // Act
        OrderResponse response = orderMapper.mapToOrderResponse(order);
        
        // Assert
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getProductName()).isEqualTo("Pizza");
        assertThat(response.getItems().get(1).getProductName()).isEqualTo("Pizza");
        
        // Verify deduplication: only queries for unique product ID once
        verify(productRepository, times(1)).findAllById(List.of(1L));
    }
    
    @Test
    void mapToOrderResponse_withMissingProduct_usesDefaultName() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        
        Order order = new Order();
        order.setId(orderId);
        order.setTableId(5);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        
        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setProductId(999L);
        item.setQuantity(1);
        
        order.setItems(List.of(item));
        
        when(productRepository.findAllById(List.of(999L))).thenReturn(List.of());
        
        // Act
        OrderResponse response = orderMapper.mapToOrderResponse(order);
        
        // Assert
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductName()).isEqualTo("Producto desconocido");
    }
    
    @Test
    void mapToOrderResponse_withEmptyItems_returnsEmptyItemsList() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        
        Order order = new Order();
        order.setId(orderId);
        order.setTableId(5);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setItems(new ArrayList<>());
        
        when(productRepository.findAllById(anyList())).thenReturn(List.of());
        
        // Act
        OrderResponse response = orderMapper.mapToOrderResponse(order);
        
        // Assert
        assertThat(response.getItems()).isEmpty();
        verify(productRepository, times(1)).findAllById(anyList());
    }
    
    @Test
    void mapToOrderResponseList_withMultipleOrders_mapsAllCorrectly() {
        // Arrange
        Order order1 = createTestOrder(OrderStatus.PENDING);
        Order order2 = createTestOrder(OrderStatus.IN_PREPARATION);
        
        when(productRepository.findAllById(anyList())).thenReturn(List.of());
        
        // Act
        List<OrderResponse> responses = orderMapper.mapToOrderResponseList(List.of(order1, order2));
        
        // Assert
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(responses.get(1).getStatus()).isEqualTo(OrderStatus.IN_PREPARATION);
    }
    
    @Test
    void mapToOrderResponseList_withEmptyList_returnsEmptyList() {
        // Act
        List<OrderResponse> responses = orderMapper.mapToOrderResponseList(List.of());
        
        // Assert
        assertThat(responses).isEmpty();
        verify(productRepository, never()).findAllById(anyList());
    }
    
    @Test
    void mapToOrderResponse_withNullNote_handlesGracefully() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        
        Order order = new Order();
        order.setId(orderId);
        order.setTableId(5);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        
        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setProductId(1L);
        item.setQuantity(2);
        item.setNote(null);
        
        order.setItems(List.of(item));
        
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(product1));
        
        // Act
        OrderResponse response = orderMapper.mapToOrderResponse(order);
        
        // Assert
        assertThat(response.getItems().get(0).getNote()).isNull();
    }

    @Test
    void mapToOrderResponse_withDuplicateProducts_avoidsNPlusOne() {
        // Arrange - Multiple items with the same product
        Order order = createTestOrder(OrderStatus.PENDING);
        
        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setProductId(1L);
        item1.setQuantity(2);
        item1.setNote("No onions");
        item1.setOrder(order);

        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setProductId(1L);
        item2.setQuantity(1);
        item2.setNote(null);
        item2.setOrder(order);

        order.setItems(List.of(item1, item2));

        // Should only query for unique product ID (1L), not twice
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(product1));

        // Act
        OrderResponse response = orderMapper.mapToOrderResponse(order);

        // Assert
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getProductName()).isEqualTo("Pizza");
        assertThat(response.getItems().get(1).getProductName()).isEqualTo("Pizza");
        verify(productRepository).findAllById(List.of(1L));
    }

    @Test
    void mapToOrderResponseList_emptyList_returnsEmptyList() {
        // Act
        List<OrderResponse> responses = orderMapper.mapToOrderResponseList(new ArrayList<>());

        // Assert
        assertThat(responses).isEmpty();
    }

    @Test
    void mapToOrderResponseList_multipleOrders_mapsAllCorrectly() {
        // Arrange
        Order order1 = createTestOrder(OrderStatus.PENDING);
        Order order2 = createTestOrder(OrderStatus.IN_PREPARATION);

        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setProductId(1L);
        item1.setQuantity(2);
        order1.setItems(List.of(item1));

        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setProductId(2L);
        item2.setQuantity(1);
        order2.setItems(List.of(item2));

        when(productRepository.findAllById(any())).thenReturn(List.of(product1, product2));

        // Act
        List<OrderResponse> responses = orderMapper.mapToOrderResponseList(List.of(order1, order2));

        // Assert
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(responses.get(1).getStatus()).isEqualTo(OrderStatus.IN_PREPARATION);
    }

    @Test
    void mapToOrderResponse_withAllStatusTypes_preservesStatus() {
        // Test all three status values
        Order pendingOrder = createTestOrder(OrderStatus.PENDING);
        Order inPrepOrder = createTestOrder(OrderStatus.IN_PREPARATION);
        Order readyOrder = createTestOrder(OrderStatus.READY);

        when(productRepository.findAllById(any())).thenReturn(new ArrayList<>());

        // Act & Assert
        assertThat(orderMapper.mapToOrderResponse(pendingOrder).getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(orderMapper.mapToOrderResponse(inPrepOrder).getStatus()).isEqualTo(OrderStatus.IN_PREPARATION);
        assertThat(orderMapper.mapToOrderResponse(readyOrder).getStatus()).isEqualTo(OrderStatus.READY);
    }
    
    private Order createTestOrder(OrderStatus status) {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setTableId(5);
        order.setStatus(status);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setItems(new ArrayList<>());
        return order;
    }
}
