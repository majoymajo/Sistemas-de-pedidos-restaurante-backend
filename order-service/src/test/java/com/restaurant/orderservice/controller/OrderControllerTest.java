package com.restaurant.orderservice.controller;

import com.restaurant.orderservice.dto.*;
import com.restaurant.orderservice.enums.OrderStatus;
import com.restaurant.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderController.
 * 
 * Tests the REST endpoints for order operations including creation,
 * retrieval, filtering, and status updates.
 */
@ExtendWith(MockitoExtension.class)
class OrderControllerTest {
    
    @Mock
    private OrderService orderService;
    
    @InjectMocks
    private OrderController orderController;
    
    private CreateOrderRequest createOrderRequest;
    private OrderResponse orderResponse;
    private UUID orderId;
    
    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        
        // Setup CreateOrderRequest
        OrderItemRequest itemRequest = new OrderItemRequest(1L, 2, "No onions");
        createOrderRequest = new CreateOrderRequest(5, Arrays.asList(itemRequest));
        
        // Setup OrderResponse
        OrderItemResponse itemResponse = OrderItemResponse.builder()
                .id(1L)
                .productId(1L)
                .quantity(2)
                .note("No onions")
                .build();
        
        orderResponse = OrderResponse.builder()
                .id(orderId)
                .tableId(5)
                .status(OrderStatus.PENDING)
                .items(Arrays.asList(itemResponse))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void createOrder_WithValidRequest_Returns201Created() {
        // Arrange
        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(orderResponse);
        
        // Act
        ResponseEntity<OrderResponse> response = orderController.createOrder(createOrderRequest);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(orderId);
        assertThat(response.getBody().getTableId()).isEqualTo(5);
        assertThat(response.getBody().getStatus()).isEqualTo(OrderStatus.PENDING);
        
        verify(orderService, times(1)).createOrder(any(CreateOrderRequest.class));
    }
    
    @Test
    void getOrderById_WithValidId_Returns200OK() {
        // Arrange
        when(orderService.getOrderById(orderId)).thenReturn(orderResponse);
        
        // Act
        ResponseEntity<OrderResponse> response = orderController.getOrderById(orderId);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(orderId);
        
        verify(orderService, times(1)).getOrderById(orderId);
    }
    
    @Test
    void getOrders_WithoutStatusFilter_Returns200OK() {
        // Arrange
        List<OrderResponse> orders = Arrays.asList(orderResponse);
        when(orderService.getOrders(null)).thenReturn(orders);
        
        // Act
        ResponseEntity<List<OrderResponse>> response = orderController.getOrders(null);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getId()).isEqualTo(orderId);
        
        verify(orderService, times(1)).getOrders(null);
    }
    
    @Test
    void getOrders_WithStatusFilter_Returns200OK() {
        // Arrange
        List<OrderResponse> orders = Arrays.asList(orderResponse);
        when(orderService.getOrders(List.of(OrderStatus.PENDING))).thenReturn(orders);
        
        // Act
        ResponseEntity<List<OrderResponse>> response = orderController.getOrders(List.of(OrderStatus.PENDING));
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
        
        verify(orderService, times(1)).getOrders(List.of(OrderStatus.PENDING));
    }
    
    @Test
    void updateOrderStatus_WithValidRequest_Returns200OK() {
        // Arrange
        UpdateStatusRequest updateRequest = new UpdateStatusRequest(OrderStatus.IN_PREPARATION);
        OrderResponse updatedResponse = OrderResponse.builder()
                .id(orderId)
                .tableId(5)
                .status(OrderStatus.IN_PREPARATION)
                .items(orderResponse.getItems())
                .createdAt(orderResponse.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();
        
        when(orderService.updateOrderStatus(orderId, OrderStatus.IN_PREPARATION))
                .thenReturn(updatedResponse);
        
        // Act
        ResponseEntity<OrderResponse> response = orderController.updateOrderStatus(orderId, updateRequest);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(orderId);
        assertThat(response.getBody().getStatus()).isEqualTo(OrderStatus.IN_PREPARATION);
        
        verify(orderService, times(1)).updateOrderStatus(orderId, OrderStatus.IN_PREPARATION);
    }

    @Test
    void deleteOrder_WithValidId_Returns200WithMetadata() {
        // Arrange
        DeleteOrderResponse deleteResponse = DeleteOrderResponse.builder()
                .deletedId(orderId.toString())
                .deletedAt(LocalDateTime.now().toString())
                .deletedBy("KITCHEN")
                .build();
        when(orderService.deleteOrder(orderId)).thenReturn(deleteResponse);

        // Act
        ResponseEntity<DeleteOrderResponse> response = orderController.deleteOrder(orderId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDeletedId()).isEqualTo(orderId.toString());
        verify(orderService, times(1)).deleteOrder(orderId);
    }

    @Test
    void deleteAllOrders_WithConfirmation_Returns200WithCount() {
        // Arrange
        DeleteAllOrdersResponse deleteAllResponse = DeleteAllOrdersResponse.builder()
                .deletedCount(4)
                .deletedAt(LocalDateTime.now().toString())
                .deletedBy("KITCHEN")
                .build();
        when(orderService.deleteAllOrders()).thenReturn(deleteAllResponse);

        // Act
        ResponseEntity<?> response = orderController.deleteAllOrders("true");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        verify(orderService, times(1)).deleteAllOrders();
    }
}
