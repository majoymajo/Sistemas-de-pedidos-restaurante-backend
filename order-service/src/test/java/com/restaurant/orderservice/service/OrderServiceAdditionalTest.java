package com.restaurant.orderservice.service;

import com.restaurant.orderservice.application.port.out.OrderPlacedEventPublisherPort;
import com.restaurant.orderservice.dto.DeleteAllOrdersResponse;
import com.restaurant.orderservice.dto.DeleteOrderResponse;
import com.restaurant.orderservice.dto.OrderResponse;
import com.restaurant.orderservice.entity.Order;
import com.restaurant.orderservice.enums.OrderStatus;
import com.restaurant.orderservice.exception.OrderNotFoundException;
import com.restaurant.orderservice.repository.OrderRepository;
import com.restaurant.orderservice.service.command.OrderCommandExecutor;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceAdditionalTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderValidator orderValidator;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderPlacedEventPublisherPort orderPlacedEventPublisherPort;

    @Mock
    private OrderCommandExecutor orderCommandExecutor;

    @InjectMocks
    private OrderService orderService;

    @Test
    void getOrders_withEmptyList_returnsEmptyList() {
        when(orderRepository.findAllActive()).thenReturn(new ArrayList<>());
        when(orderMapper.mapToOrderResponseList(new ArrayList<>())) .thenReturn(new ArrayList<>());

        List<OrderResponse> result = orderService.getOrders(null);

        assertThat(result).isEmpty();
        verify(orderRepository).findAllActive();
    }

    @Test
    void getOrders_withStatusFilterEmpty_returnsAllOrders() {
        Order order = buildOrder(UUID.randomUUID(), OrderStatus.PENDING);
        when(orderRepository.findAllActive()).thenReturn(List.of(order));
        when(orderMapper.mapToOrderResponseList(any())).thenReturn(List.of(buildOrderResponse(order)));

        List<OrderResponse> result = orderService.getOrders(new ArrayList<>());

        assertThat(result).hasSize(1);
        verify(orderRepository).findAllActive();
    }

    @Test
    void getOrders_withSingleStatusFilter_returnFiltered() {
        Order order = buildOrder(UUID.randomUUID(), OrderStatus.READY);
        List<OrderStatus> filter = List.of(OrderStatus.READY);

        when(orderRepository.findByStatusInActive(filter)).thenReturn(List.of(order));
        when(orderMapper.mapToOrderResponseList(any())).thenReturn(List.of(buildOrderResponse(order)));

        List<OrderResponse> result = orderService.getOrders(filter);

        assertThat(result).hasSize(1);
        verify(orderRepository).findByStatusInActive(filter);
        verify(orderRepository, never()).findAllActive();
    }

    @Test
    void getOrders_withMultipleStatusFilters_returnFiltered() {
        Order order1 = buildOrder(UUID.randomUUID(), OrderStatus.PENDING);
        Order order2 = buildOrder(UUID.randomUUID(), OrderStatus.READY);
        List<OrderStatus> filter = List.of(OrderStatus.PENDING, OrderStatus.READY);

        when(orderRepository.findByStatusInActive(filter)).thenReturn(List.of(order1, order2));
        when(orderMapper.mapToOrderResponseList(any())).thenReturn(List.of(buildOrderResponse(order1), buildOrderResponse(order2)));

        List<OrderResponse> result = orderService.getOrders(filter);

        assertThat(result).hasSize(2);
        verify(orderRepository).findByStatusInActive(filter);
    }

    @Test
    void updateOrderStatus_withValidId_updatesStatusSuccessfully() {
        UUID orderId = UUID.randomUUID();
        Order order = buildOrder(orderId, OrderStatus.PENDING);
        Order updatedOrder = buildOrder(orderId, OrderStatus.IN_PREPARATION);

        when(orderRepository.findByIdActive(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);
        when(orderMapper.mapToOrderResponse(updatedOrder)).thenReturn(buildOrderResponse(updatedOrder));

        OrderResponse response = orderService.updateOrderStatus(orderId, OrderStatus.IN_PREPARATION);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.IN_PREPARATION);
        verify(orderRepository).findByIdActive(orderId);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void updateOrderStatus_withUnknownId_throwsOrderNotFoundException() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findByIdActive(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, OrderStatus.IN_PREPARATION))
                .isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void deleteOrder_withValidId_marksAsDeleted() {
        UUID orderId = UUID.randomUUID();
        Order order = buildOrder(orderId, OrderStatus.PENDING);

        when(orderRepository.findByIdActive(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        DeleteOrderResponse response = orderService.deleteOrder(orderId);

        assertThat(response.getDeletedId()).isEqualToIgnoringCase(orderId.toString());
        verify(orderRepository).findByIdActive(orderId);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void deleteOrder_withUnknownId_throwsOrderNotFoundException() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findByIdActive(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.deleteOrder(orderId))
                .isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void deleteAllOrders_withMultipleActiveOrders_deletesAll() {
        Order order1 = buildOrder(UUID.randomUUID(), OrderStatus.PENDING);
        Order order2 = buildOrder(UUID.randomUUID(), OrderStatus.IN_PREPARATION);
        Order order3 = buildOrder(UUID.randomUUID(), OrderStatus.READY);

        when(orderRepository.findAllActive()).thenReturn(List.of(order1, order2, order3));
        when(orderRepository.save(any(Order.class))).thenReturn(order1);

        DeleteAllOrdersResponse response = orderService.deleteAllOrders();

        assertThat(response.getDeletedCount()).isEqualTo(3);
        verify(orderRepository).findAllActive();
        verify(orderRepository, times(3)).save(any(Order.class));
    }

    @Test
    void deleteAllOrders_withNoActiveOrders_returnsZero() {
        when(orderRepository.findAllActive()).thenReturn(new ArrayList<>());

        DeleteAllOrdersResponse response = orderService.deleteAllOrders();

        assertThat(response.getDeletedCount()).isEqualTo(0);
        verify(orderRepository).findAllActive();
        verify(orderRepository, never()).save(any());
    }

    @Test
    void deleteAllOrders_withSingleActiveOrder_deletesSingle() {
        Order order = buildOrder(UUID.randomUUID(), OrderStatus.PENDING);

        when(orderRepository.findAllActive()).thenReturn(List.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        DeleteAllOrdersResponse response = orderService.deleteAllOrders();

        assertThat(response.getDeletedCount()).isEqualTo(1);
        verify(orderRepository).findAllActive();
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
