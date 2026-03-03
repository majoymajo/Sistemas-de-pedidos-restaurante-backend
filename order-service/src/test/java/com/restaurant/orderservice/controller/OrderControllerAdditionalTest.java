package com.restaurant.orderservice.controller;

import com.restaurant.orderservice.dto.OrderItemRequest;
import com.restaurant.orderservice.dto.OrderResponse;
import com.restaurant.orderservice.dto.CreateOrderRequest;
import com.restaurant.orderservice.enums.OrderStatus;
import com.restaurant.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderController Additional Tests")
class OrderControllerAdditionalTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private OrderResponse sampleResponse;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        sampleResponse = OrderResponse.builder()
                .id(orderId)
                .tableId(1)
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /orders should return 201 Created with order response")
    void createOrder_withValidRequest_shouldReturn201() {
        CreateOrderRequest request = new CreateOrderRequest(1, List.of(new OrderItemRequest(1L, 2, null)));

        when(orderService.createOrder(request)).thenReturn(sampleResponse);

        ResponseEntity<OrderResponse> response = orderController.createOrder(request);

        assertThat(response.getStatusCodeValue()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(orderId);
        verify(orderService).createOrder(request);
    }

    @Test
    @DisplayName("GET /orders/{id} should return 200 with order response")
    void getOrderById_withValidId_shouldReturn200() {
        when(orderService.getOrderById(orderId)).thenReturn(sampleResponse);

        ResponseEntity<OrderResponse> response = orderController.getOrderById(orderId);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(orderId);
        verify(orderService).getOrderById(orderId);
    }

    @Test
    @DisplayName("GET /orders should return 200 with list of orders")
    void getOrders_withoutFilter_shouldReturn200() {
        OrderResponse response1 = buildResponse(UUID.randomUUID(), OrderStatus.PENDING);
        OrderResponse response2 = buildResponse(UUID.randomUUID(), OrderStatus.IN_PREPARATION);

        when(orderService.getOrders(null)).thenReturn(List.of(response1, response2));

        ResponseEntity<List<OrderResponse>> response = orderController.getOrders(null);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(2);
        verify(orderService).getOrders(null);
    }

    @Test
    @DisplayName("GET /orders with status filter should return filtered orders")
    void getOrders_withStatusFilter_shouldReturnFiltered() {
        OrderResponse response = buildResponse(UUID.randomUUID(), OrderStatus.READY);

        when(orderService.getOrders(List.of(OrderStatus.READY))).thenReturn(List.of(response));

        ResponseEntity<List<OrderResponse>> result = orderController.getOrders(List.of(OrderStatus.READY));

        assertThat(result.getStatusCodeValue()).isEqualTo(200);
        assertThat(result.getBody()).hasSize(1);
        assertThat(result.getBody().get(0).getStatus()).isEqualTo(OrderStatus.READY);
    }

    @Test
    @DisplayName("GET /orders with empty list should return all orders")
    void getOrders_withEmptyStatusList_shouldReturnAll() {
        OrderResponse response1 = buildResponse(UUID.randomUUID(), OrderStatus.PENDING);
        OrderResponse response2 = buildResponse(UUID.randomUUID(), OrderStatus.IN_PREPARATION);

        when(orderService.getOrders(List.of())).thenReturn(List.of(response1, response2));

        ResponseEntity<List<OrderResponse>> result = orderController.getOrders(List.of());

        assertThat(result.getStatusCodeValue()).isEqualTo(200);
        assertThat(result.getBody()).hasSize(2);
    }

    @Test
    @DisplayName("GET /orders should return empty list when no orders")
    void getOrders_whenNoOrders_shouldReturnEmptyList() {
        when(orderService.getOrders(null)).thenReturn(List.of());

        ResponseEntity<List<OrderResponse>> response = orderController.getOrders(null);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEmpty();
    }

    // Helper method
    private OrderResponse buildResponse(UUID id, OrderStatus status) {
        return OrderResponse.builder()
                .id(id)
                .tableId(1)
                .status(status)
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
