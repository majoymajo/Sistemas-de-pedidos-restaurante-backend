package com.restaurant.orderservice.service;

import com.restaurant.orderservice.application.port.out.OrderPlacedEventPublisherPort;
import com.restaurant.orderservice.dto.CreateOrderRequest;
import com.restaurant.orderservice.dto.OrderItemRequest;
import com.restaurant.orderservice.dto.OrderResponse;
import com.restaurant.orderservice.entity.Order;
import com.restaurant.orderservice.entity.OrderItem;
import com.restaurant.orderservice.enums.OrderStatus;
import com.restaurant.orderservice.exception.EventPublicationException;
import com.restaurant.orderservice.exception.InvalidOrderException;
import com.restaurant.orderservice.exception.OrderNotFoundException;
import com.restaurant.orderservice.repository.OrderRepository;
import com.restaurant.orderservice.service.command.OrderCommandExecutor;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

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
    void createOrder_withValidData_createsOrderAndPublishesEvent() {
        CreateOrderRequest request = new CreateOrderRequest(
                5,
                List.of(new OrderItemRequest(1L, 2, "Sin cebolla"))
        );

        Order savedOrder = buildOrder(UUID.randomUUID(), OrderStatus.PENDING);
        savedOrder.setItems(List.of(buildItem(savedOrder, 1L, 2, "Sin cebolla")));

        OrderResponse expectedResponse = OrderResponse.builder()
                .id(savedOrder.getId())
                .tableId(savedOrder.getTableId())
                .status(savedOrder.getStatus())
                .items(new ArrayList<>())
                .createdAt(savedOrder.getCreatedAt())
                .updatedAt(savedOrder.getUpdatedAt())
                .build();

        doNothing().when(orderValidator).validateCreateOrderRequest(request);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        doNothing().when(orderCommandExecutor).execute(any());
        when(orderMapper.mapToOrderResponse(savedOrder)).thenReturn(expectedResponse);

        OrderResponse response = orderService.createOrder(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(savedOrder.getId());
        assertThat(response.getTableId()).isEqualTo(5);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);

        verify(orderValidator).validateCreateOrderRequest(request);
        verify(orderRepository).save(any(Order.class));
        verify(orderCommandExecutor).execute(any());
        verify(orderMapper).mapToOrderResponse(savedOrder);
    }

    @Test
    void createOrder_whenEventPublicationFails_propagatesException() {
        CreateOrderRequest request = new CreateOrderRequest(
                5,
                List.of(new OrderItemRequest(1L, 1, null))
        );

        Order savedOrder = buildOrder(UUID.randomUUID(), OrderStatus.PENDING);
        savedOrder.setItems(List.of(buildItem(savedOrder, 1L, 1, null)));

        doNothing().when(orderValidator).validateCreateOrderRequest(request);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        doThrow(new EventPublicationException("Broker unavailable", new RuntimeException("broker down")))
                .when(orderCommandExecutor).execute(any());

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(EventPublicationException.class)
                .hasMessageContaining("Broker unavailable");

        verify(orderValidator).validateCreateOrderRequest(request);
        verify(orderRepository).save(any(Order.class));
        verify(orderCommandExecutor).execute(any());
        verify(orderMapper, never()).mapToOrderResponse(any(Order.class));
    }

    @Test
    void createOrder_withInvalidData_throwsValidationException() {
        CreateOrderRequest request = new CreateOrderRequest(0, List.of());
        doThrow(new InvalidOrderException("Table ID must be a positive integer"))
                .when(orderValidator).validateCreateOrderRequest(request);

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Table ID");

        verify(orderValidator).validateCreateOrderRequest(request);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getOrderById_withValidId_returnsMappedOrder() {
        UUID orderId = UUID.randomUUID();
        Order order = buildOrder(orderId, OrderStatus.PENDING);
        OrderResponse expected = OrderResponse.builder()
                .id(orderId)
                .tableId(order.getTableId())
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();

        when(orderRepository.findByIdActive(orderId)).thenReturn(Optional.of(order));
        when(orderMapper.mapToOrderResponse(order)).thenReturn(expected);

        OrderResponse response = orderService.getOrderById(orderId);

        assertThat(response.getId()).isEqualTo(orderId);
        verify(orderMapper).mapToOrderResponse(order);
    }

    @Test
    void getOrderById_withUnknownId_throwsOrderNotFound() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findByIdActive(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(orderId))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(orderId.toString());
    }

    @Test
    void getOrders_withoutFilter_returnsAllOrders() {
        Order order1 = buildOrder(UUID.randomUUID(), OrderStatus.PENDING);
        Order order2 = buildOrder(UUID.randomUUID(), OrderStatus.IN_PREPARATION);
        List<Order> orders = List.of(order1, order2);

        List<OrderResponse> mapped = List.of(
                OrderResponse.builder().id(order1.getId()).status(order1.getStatus()).tableId(order1.getTableId()).items(new ArrayList<>()).build(),
                OrderResponse.builder().id(order2.getId()).status(order2.getStatus()).tableId(order2.getTableId()).items(new ArrayList<>()).build()
        );

        when(orderRepository.findAllActive()).thenReturn(orders);
        when(orderMapper.mapToOrderResponseList(orders)).thenReturn(mapped);

        List<OrderResponse> result = orderService.getOrders(null);

        assertThat(result).hasSize(2);
        verify(orderRepository).findAllActive();
        verify(orderRepository, never()).findByStatusInActive(any());
    }

    @Test
    void getOrders_withFilter_returnsFilteredOrders() {
        List<OrderStatus> filter = List.of(OrderStatus.PENDING);
        Order order1 = buildOrder(UUID.randomUUID(), OrderStatus.PENDING);
        List<Order> orders = List.of(order1);

        List<OrderResponse> mapped = List.of(
                OrderResponse.builder().id(order1.getId()).status(order1.getStatus()).tableId(order1.getTableId()).items(new ArrayList<>()).build()
        );

        when(orderRepository.findByStatusInActive(filter)).thenReturn(orders);
        when(orderMapper.mapToOrderResponseList(orders)).thenReturn(mapped);

        List<OrderResponse> result = orderService.getOrders(filter);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository).findByStatusInActive(filter);
        verify(orderRepository, never()).findAllActive();
    }

    @Test
    void updateOrderStatus_withValidOrder_updatesAndMapsResponse() {
        UUID orderId = UUID.randomUUID();
        Order current = buildOrder(orderId, OrderStatus.PENDING);
        Order updated = buildOrder(orderId, OrderStatus.IN_PREPARATION);

        OrderResponse expected = OrderResponse.builder()
                .id(orderId)
                .tableId(updated.getTableId())
                .status(OrderStatus.IN_PREPARATION)
                .items(new ArrayList<>())
                .createdAt(updated.getCreatedAt())
                .updatedAt(updated.getUpdatedAt())
                .build();

        when(orderRepository.findByIdActive(orderId)).thenReturn(Optional.of(current));
        when(orderRepository.save(any(Order.class))).thenReturn(updated);
        when(orderMapper.mapToOrderResponse(updated)).thenReturn(expected);

        OrderResponse response = orderService.updateOrderStatus(orderId, OrderStatus.IN_PREPARATION);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.IN_PREPARATION);
        verify(orderRepository).save(any(Order.class));
        verify(orderMapper).mapToOrderResponse(updated);
    }

    @Test
    void updateOrderStatus_withUnknownOrder_throwsOrderNotFound() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findByIdActive(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, OrderStatus.READY))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(orderId.toString());

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void deleteOrder_withExistingOrder_deletesOrder() {
        UUID orderId = UUID.randomUUID();
        Order order = buildOrder(orderId, OrderStatus.PENDING);
        when(orderRepository.findByIdActive(orderId)).thenReturn(Optional.of(order));

        orderService.deleteOrder(orderId);

        assertThat(order.isDeleted()).isTrue();
        assertThat(order.getDeletedAt()).isNotNull();
        verify(orderRepository).save(order);
    }

    @Test
    void deleteOrder_withUnknownOrder_throwsOrderNotFound() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findByIdActive(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.deleteOrder(orderId))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(orderId.toString());

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void deleteAllOrders_returnsDeletedCount() {
        Order order1 = buildOrder(UUID.randomUUID(), OrderStatus.PENDING);
        Order order2 = buildOrder(UUID.randomUUID(), OrderStatus.IN_PREPARATION);
        List<Order> activeOrders = new ArrayList<>(List.of(order1, order2));

        when(orderRepository.findAllActive()).thenReturn(activeOrders);

        var response = orderService.deleteAllOrders();

        assertThat(response.getDeletedCount()).isEqualTo(2);
        verify(orderRepository).findAllActive();
        verify(orderRepository).save(order1);
        verify(orderRepository).save(order2);
        assertThat(order1.isDeleted()).isTrue();
        assertThat(order2.isDeleted()).isTrue();
    }

    private static Order buildOrder(UUID id, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setTableId(5);
        order.setStatus(status);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setItems(new ArrayList<>());
        return order;
    }

    private static OrderItem buildItem(Order order, long productId, int quantity, String note) {
        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setOrder(order);
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setNote(note);
        return item;
    }

    // Additional comprehensive branch coverage tests
    @Test
    void createOrder_withMultipleItems_createsAllItems() {
        // Test stream mapping branch
        CreateOrderRequest request = new CreateOrderRequest(
                3,
                List.of(
                        new OrderItemRequest(1L, 2, "No onions"),
                        new OrderItemRequest(2L, 1, null),
                        new OrderItemRequest(3L, 3, "Extra spicy")
                )
        );

        Order savedOrder = buildOrder(UUID.randomUUID(), OrderStatus.PENDING);
        savedOrder.setItems(List.of(
                buildItem(savedOrder, 1L, 2, "No onions"),
                buildItem(savedOrder, 2L, 1, null),
                buildItem(savedOrder, 3L, 3, "Extra spicy")
        ));

        OrderResponse expectedResponse = OrderResponse.builder()
                .id(savedOrder.getId())
                .tableId(savedOrder.getTableId())
                .status(savedOrder.getStatus())
                .items(new ArrayList<>())
                .createdAt(savedOrder.getCreatedAt())
                .updatedAt(savedOrder.getUpdatedAt())
                .build();

        doNothing().when(orderValidator).validateCreateOrderRequest(request);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        doNothing().when(orderCommandExecutor).execute(any());
        when(orderMapper.mapToOrderResponse(savedOrder)).thenReturn(expectedResponse);

        OrderResponse response = orderService.createOrder(request);

        assertThat(response).isNotNull();
        verify(orderValidator).validateCreateOrderRequest(request);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void updateOrderStatus_multipleTransitions_executesSequentially() {
        // Test multiple status updates in sequence
        UUID orderId = UUID.randomUUID();
        Order pendingOrder = buildOrder(orderId, OrderStatus.PENDING);
        Order prepOrder = buildOrder(orderId, OrderStatus.IN_PREPARATION);
        Order readyOrder = buildOrder(orderId, OrderStatus.READY);

        OrderResponse pendingResponse = buildOrderResponse(pendingOrder);
        OrderResponse prepResponse = buildOrderResponse(prepOrder);
        OrderResponse readyResponse = buildOrderResponse(readyOrder);

        // First transition: PENDING -> IN_PREPARATION
        when(orderRepository.findByIdActive(orderId)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any())).thenReturn(prepOrder);
        when(orderMapper.mapToOrderResponse(prepOrder)).thenReturn(prepResponse);

        OrderResponse response1 = orderService.updateOrderStatus(orderId, OrderStatus.IN_PREPARATION);
        assertThat(response1.getStatus()).isEqualTo(OrderStatus.IN_PREPARATION);

        // Second transition: IN_PREPARATION -> READY
        when(orderRepository.findByIdActive(orderId)).thenReturn(Optional.of(prepOrder));
        when(orderRepository.save(any())).thenReturn(readyOrder);
        when(orderMapper.mapToOrderResponse(readyOrder)).thenReturn(readyResponse);

        OrderResponse response2 = orderService.updateOrderStatus(orderId, OrderStatus.READY);
        assertThat(response2.getStatus()).isEqualTo(OrderStatus.READY);
    }

    @Test
    void getOrders_withMixedStatuses_returnsOrdersFromMultipleStatuses() {
        // Test filtering with multiple status values
        Order pending = buildOrder(UUID.randomUUID(), OrderStatus.PENDING);
        Order inPrep = buildOrder(UUID.randomUUID(), OrderStatus.IN_PREPARATION);
        Order ready = buildOrder(UUID.randomUUID(), OrderStatus.READY);
        
        List<OrderStatus> statuses = List.of(
                OrderStatus.PENDING, 
                OrderStatus.IN_PREPARATION,
                OrderStatus.READY
        );
        List<Order> orders = List.of(pending, inPrep, ready);

        List<OrderResponse> expected = List.of(
                buildOrderResponse(pending),
                buildOrderResponse(inPrep),
                buildOrderResponse(ready)
        );

        when(orderRepository.findByStatusInActive(statuses)).thenReturn(orders);
        when(orderMapper.mapToOrderResponseList(orders)).thenReturn(expected);

        List<OrderResponse> result = orderService.getOrders(statuses);

        assertThat(result).hasSize(3);
        verify(orderRepository).findByStatusInActive(statuses);
    }

    private static OrderResponse buildOrderResponse(Order order) {
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
