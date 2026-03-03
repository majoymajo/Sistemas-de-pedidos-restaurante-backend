package com.restaurant.orderservice.service;

import com.restaurant.orderservice.dto.OrderResponse;
import com.restaurant.orderservice.entity.Order;
import com.restaurant.orderservice.enums.OrderStatus;
import com.restaurant.orderservice.exception.OrderNotFoundException;
import com.restaurant.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Branch coverage tests for OrderService.getOrders method.
 * Tests all branches in the status filtering logic.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceGetOrdersBranchTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderValidator orderValidator;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    @Test
    void getOrderById_withValidId_returnsOrder() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        Order order = buildOrder(orderId, OrderStatus.PENDING);
        OrderResponse response = buildOrderResponse(order);

        when(orderRepository.findByIdActive(orderId)).thenReturn(Optional.of(order));
        when(orderMapper.mapToOrderResponse(order)).thenReturn(response);

        // Act
        OrderResponse result = orderService.getOrderById(orderId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(orderId);
        verify(orderRepository).findByIdActive(orderId);
        verify(orderMapper).mapToOrderResponse(order);
    }

    @Test
    void getOrderById_withInvalidId_throwsOrderNotFoundException() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findByIdActive(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.getOrderById(orderId))
                .isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository).findByIdActive(orderId);
        verify(orderMapper, never()).mapToOrderResponse(any());
    }

    @Test
    void getOrders_withNullStatus_returnsAllActiveOrders() {
        // Arrange
        Order order1 = buildOrder(UUID.randomUUID(), OrderStatus.PENDING);
        Order order2 = buildOrder(UUID.randomUUID(), OrderStatus.IN_PREPARATION);
        
        when(orderRepository.findAllActive()).thenReturn(List.of(order1, order2));
        when(orderMapper.mapToOrderResponseList(any())).thenReturn(
                List.of(buildOrderResponse(order1), buildOrderResponse(order2))
        );

        // Act
        List<OrderResponse> result = orderService.getOrders(null);

        // Assert
        assertThat(result).hasSize(2);
        verify(orderRepository).findAllActive();
        verify(orderRepository, never()).findByStatusInActive(any());
    }

    @Test
    void getOrders_withEmptyStatusList_returnsAllActiveOrders() {
        // Arrange
        Order order1 = buildOrder(UUID.randomUUID(), OrderStatus.READY);
        
        when(orderRepository.findAllActive()).thenReturn(List.of(order1));
        when(orderMapper.mapToOrderResponseList(any())).thenReturn(
                List.of(buildOrderResponse(order1))
        );

        // Act
        List<OrderResponse> result = orderService.getOrders(new ArrayList<>());

        // Assert
        assertThat(result).hasSize(1);
        verify(orderRepository).findAllActive();
        verify(orderRepository, never()).findByStatusInActive(any());
    }

    @Test
    void getOrders_withSingleStatusFilter_returnsFilteredOrders() {
        // Arrange
        Order order = buildOrder(UUID.randomUUID(), OrderStatus.READY);
        List<OrderStatus> filter = List.of(OrderStatus.READY);
        
        when(orderRepository.findByStatusInActive(filter)).thenReturn(List.of(order));
        when(orderMapper.mapToOrderResponseList(any())).thenReturn(
                List.of(buildOrderResponse(order))
        );

        // Act
        List<OrderResponse> result = orderService.getOrders(filter);

        // Assert
        assertThat(result).hasSize(1);
        verify(orderRepository).findByStatusInActive(filter);
        verify(orderRepository, never()).findAllActive();
    }

    @Test
    void getOrders_withMultipleStatusFilters_returnsFilteredOrders() {
        // Arrange
        Order order1 = buildOrder(UUID.randomUUID(), OrderStatus.PENDING);
        Order order2 = buildOrder(UUID.randomUUID(), OrderStatus.READY);
        List<OrderStatus> filter = List.of(OrderStatus.PENDING, OrderStatus.READY);
        
        when(orderRepository.findByStatusInActive(filter)).thenReturn(List.of(order1, order2));
        when(orderMapper.mapToOrderResponseList(any())).thenReturn(
                List.of(buildOrderResponse(order1), buildOrderResponse(order2))
        );

        // Act
        List<OrderResponse> result = orderService.getOrders(filter);

        // Assert
        assertThat(result).hasSize(2);
        verify(orderRepository).findByStatusInActive(filter);
        verify(orderRepository, never()).findAllActive();
    }

    @Test
    void getOrders_withStatusFilterNoMatches_returnsEmptyList() {
        // Arrange
        List<OrderStatus> filter = List.of(OrderStatus.READY);
        
        when(orderRepository.findByStatusInActive(filter)).thenReturn(new ArrayList<>());
        when(orderMapper.mapToOrderResponseList(any())).thenReturn(new ArrayList<>());

        // Act
        List<OrderResponse> result = orderService.getOrders(filter);

        // Assert
        assertThat(result).isEmpty();
        verify(orderRepository).findByStatusInActive(filter);
    }

    // Helper methods
    private Order buildOrder(UUID orderId, OrderStatus status) {
        Order order = new Order();
        order.setId(orderId);
        order.setTableId(1);
        order.setStatus(status);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setItems(new ArrayList<>());
        return order;
    }

    private OrderResponse buildOrderResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .tableId(order.getTableId())
                .status(order.getStatus())
                .items(new ArrayList<>())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
