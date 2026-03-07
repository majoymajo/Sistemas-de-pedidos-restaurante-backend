package com.restaurant.orderservice.service;

import com.restaurant.orderservice.dto.OrderResponse;
import com.restaurant.orderservice.entity.Order;
import com.restaurant.orderservice.entity.OrderItem;
import com.restaurant.orderservice.enums.OrderStatus;
import com.restaurant.orderservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OrderMapperAdditionalTest {

    private OrderMapper orderMapper;
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        orderMapper = new OrderMapper(productRepository);
    }

    @Test
    void mapToOrderResponse_withEmptyItems_returnsEmptyItemsList() {
        UUID orderId = UUID.randomUUID();
        LocalDateTime now =LocalDateTime.now();

        Order order = new Order();
        order.setId(orderId);
        order.setTableId(1);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order.setItems(List.of());

        OrderResponse response = orderMapper.mapToOrderResponse(order);

        assertThat(response.getId()).isEqualTo(orderId);
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTableId()).isEqualTo(1);
    }

    @Test
    void mapToOrderResponse_withMultipleItems_returnsAllItems() {
        UUID orderId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Order order = new Order();
        order.setId(orderId);
        order.setTableId(2);
        order.setStatus(OrderStatus.IN_PREPARATION);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setProductId(10L);
        item1.setQuantity(2);
        item1.setNote("sin cebolla");

        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setProductId(20L);
        item2.setQuantity(1);
        item2.setNote(null);

        order.setItems(List.of(item1, item2));

        OrderResponse response = orderMapper.mapToOrderResponse(order);

        assertThat(response.getId()).isEqualTo(orderId);
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.IN_PREPARATION);
    }

    @Test
    void mapToOrderResponseList_withMultipleOrders_returnsAllMappedOrders() {
        UUID orderId1 = UUID.randomUUID();
        UUID orderId2 = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Order order1 = new Order();
        order1.setId(orderId1);
        order1.setTableId(1);
        order1.setStatus(OrderStatus.PENDING);
        order1.setCreatedAt(now);
        order1.setUpdatedAt(now);
        order1.setItems(new ArrayList<>());

        Order order2 = new Order();
        order2.setId(orderId2);
        order2.setTableId(2);
        order2.setStatus(OrderStatus.READY);
        order2.setCreatedAt(now);
        order2.setUpdatedAt(now);
        order2.setItems(new ArrayList<>());

        List<OrderResponse> responses = orderMapper.mapToOrderResponseList(List.of(order1, order2));

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo(orderId1);
        assertThat(responses.get(1).getId()).isEqualTo(orderId2);
    }

    @Test
    void mapToOrderResponseList_withEmptyList_returnsEmptyList() {
        List<OrderResponse> responses = orderMapper.mapToOrderResponseList(List.of());

        assertThat(responses).isEmpty();
    }

    @Test
    void mapToOrderResponse_preservesAllOrderFields() {
        UUID orderId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now().minusHours(1);
        LocalDateTime updatedAt = LocalDateTime.now();

        Order order = new Order();
        order.setId(orderId);
        order.setTableId(5);
        order.setStatus(OrderStatus.READY);
        order.setCreatedAt(createdAt);
        order.setUpdatedAt(updatedAt);
        order.setItems(new ArrayList<>());

        OrderResponse response = orderMapper.mapToOrderResponse(order);

        assertThat(response.getId()).isEqualTo(orderId);
        assertThat(response.getTableId()).isEqualTo(5);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.READY);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
    }
}
