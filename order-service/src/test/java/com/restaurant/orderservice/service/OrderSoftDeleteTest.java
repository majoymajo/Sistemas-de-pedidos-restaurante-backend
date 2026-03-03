package com.restaurant.orderservice.service;

import com.restaurant.orderservice.application.port.out.OrderPlacedEventPublisherPort;
import com.restaurant.orderservice.dto.OrderResponse;
import com.restaurant.orderservice.entity.Order;
import com.restaurant.orderservice.enums.OrderStatus;
import com.restaurant.orderservice.exception.OrderNotFoundException;
import com.restaurant.orderservice.repository.OrderRepository;
import com.restaurant.orderservice.service.command.OrderCommandExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test unitario para soft delete de pedidos.
 * 
 * Cumple con Copilot Instructions:
 * - Sección 4: Security - Destructive Operations
 * - "Implementar soft delete (campo is_deleted, deleted_at, etc.)"
 * - Sección 7: Testing Strategy - TDD Obligatorio
 * 
 * Validaciones:
 * - Los pedidos no se eliminan físicamente
 * - Se marca el flag 'deleted' como true
 * - Se registra timestamp de eliminación (auditoría)
 * - Los pedidos eliminados no aparecen en consultas activas
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Order Soft Delete Tests")
class OrderSoftDeleteTest {

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
    @DisplayName("deleteOrder debe marcar orden como eliminada, no eliminar físicamente")
    void shouldMarkOrderAsDeletedNotPhysicallyDelete() {
        // Given
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setTableId(5);
        order.setStatus(OrderStatus.PENDING);
        order.setDeleted(false);
        
        when(orderRepository.findByIdActive(orderId)).thenReturn(Optional.of(order));
        
        // When
        orderService.deleteOrder(orderId);
        
        // Then
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(orderCaptor.capture());
        verify(orderRepository, never()).delete(any(Order.class)); // ⚠️ NO debe llamar delete()
        
        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.isDeleted()).isTrue();
        assertThat(savedOrder.getDeletedAt()).isNotNull();
        assertThat(savedOrder.getDeletedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("deleteOrder debe lanzar excepción si orden no existe")
    void shouldThrowExceptionWhenOrderNotFound() {
        // Given
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findByIdActive(orderId)).thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> orderService.deleteOrder(orderId))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(orderId.toString());
        
        verify(orderRepository, never()).save(any());
        verify(orderRepository, never()).delete(any());
    }

    @Test
    @DisplayName("getOrders(null) debe usar findAllActive para excluir eliminados")
    void shouldUseActiveFilterWhenGettingAllOrders() {
        // Given
        when(orderMapper.mapToOrderResponseList(any())).thenReturn(List.of());
        
        // When
        orderService.getOrders(null);
        
        // Then
        verify(orderRepository, times(1)).findAllActive();
        verify(orderRepository, never()).findAll(); // No debe usar findAll()
    }

    @Test
    @DisplayName("getOrderById debe usar findByIdActive para excluir eliminados")
    void shouldUseActiveFilterWhenGettingOrderById() {
        // Given
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setDeleted(false);
        
        OrderResponse mockResponse = new OrderResponse();
        mockResponse.setId(orderId);
        
        when(orderRepository.findByIdActive(orderId)).thenReturn(Optional.of(order));
        when(orderMapper.mapToOrderResponse(any(Order.class))).thenReturn(mockResponse);
        
        // When
        orderService.getOrderById(orderId);
        
        // Then
        verify(orderRepository, times(1)).findByIdActive(orderId);
        verify(orderRepository, never()).findById(orderId); // No debe usar findById()
    }

    @Test
    @DisplayName("getOrders(status) debe usar findByStatusInActive para excluir eliminados")
    void shouldUseActiveFilterWhenGettingOrdersByStatus() {
        // Given
        OrderStatus status = OrderStatus.PENDING;
        List<OrderStatus> statuses = List.of(status);
        when(orderMapper.mapToOrderResponseList(any())).thenReturn(List.of());
        
        // When
        orderService.getOrders(statuses);
        
        // Then
        verify(orderRepository, times(1)).findByStatusInActive(statuses);
        verify(orderRepository, never()).findByStatus(status); // No debe usar findByStatus()
    }

    @Test
    @DisplayName("deleteAllOrders debe marcar todas las órdenes como eliminadas")
    void shouldMarkAllOrdersAsDeletedNotPhysicallyDelete() {
        // Given
        Order order1 = new Order();
        order1.setId(UUID.randomUUID());
        order1.setDeleted(false);
        
        Order order2 = new Order();
        order2.setId(UUID.randomUUID());
        order2.setDeleted(false);
        
        when(orderRepository.findAllActive()).thenReturn(java.util.List.of(order1, order2));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArguments()[0]);
        
        // When
        var response = orderService.deleteAllOrders();
        
        // Then
        assertThat(response.getDeletedCount()).isEqualTo(2);
        verify(orderRepository, times(2)).save(any(Order.class));
        verify(orderRepository, never()).deleteAll(); // ⚠️ NO debe llamar deleteAll()
        verify(orderRepository, never()).delete(any(Order.class)); // ⚠️ NO debe llamar delete()
    }

    @Test
    @DisplayName("Orden eliminada no debe aparecer en consultas activas")
    void deletedOrderShouldNotAppearInActiveQueries() {
        // Given
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findByIdActive(orderId)).thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> orderService.getOrderById(orderId))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
