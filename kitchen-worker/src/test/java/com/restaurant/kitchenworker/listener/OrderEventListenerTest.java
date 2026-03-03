package com.restaurant.kitchenworker.listener;

import com.restaurant.kitchenworker.application.command.OrderPlacedCommand;
import com.restaurant.kitchenworker.event.OrderPlacedEvent;
import com.restaurant.kitchenworker.event.OrderPlacedEventValidator;
import com.restaurant.kitchenworker.exception.UnsupportedEventVersionException;
import com.restaurant.kitchenworker.service.OrderProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderEventListener.
 * 
 * These tests verify that the listener correctly receives events from RabbitMQ
 * and delegates processing to the OrderProcessingService.
 * 
 * Validates Requirements: 7.1, 7.2
 */
@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {
    
    @Mock
    private OrderProcessingService orderProcessingService;

    @Mock
    private OrderPlacedEventValidator eventValidator;
    
    @InjectMocks
    private OrderEventListener orderEventListener;
    
    private OrderPlacedEvent testEvent;
    
    @BeforeEach
    void setUp() {
        // Create a test event with sample data
        UUID orderId = UUID.randomUUID();
        Integer tableId = 5;
        LocalDateTime createdAt = LocalDateTime.now();
        
        List<OrderPlacedEvent.OrderItemEventData> items = new ArrayList<>();
        items.add(new OrderPlacedEvent.OrderItemEventData(1L, 2));
        items.add(new OrderPlacedEvent.OrderItemEventData(2L, 1));

        testEvent = OrderPlacedEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("order.placed")
                .eventVersion(1)
                .occurredAt(LocalDateTime.now())
                .payload(OrderPlacedEvent.Payload.builder()
                        .orderId(orderId)
                        .tableId(tableId)
                        .items(items)
                        .createdAt(createdAt)
                        .build())
                .build();
    }
    
    /**
     * Test that handleOrderPlacedEvent calls OrderProcessingService.
     * 
     * Validates Requirement 7.1: Listen to the order.placed queue
     * Validates Requirement 7.2: Deserialize event and delegate processing
     */
    @Test
    void handleOrderPlacedEvent_ShouldCallOrderProcessingService() {
        // Arrange
        doNothing().when(eventValidator).validate(testEvent);
        doNothing().when(orderProcessingService).processOrder(any(OrderPlacedCommand.class));
        
        // Act
        orderEventListener.handleOrderPlacedEvent(testEvent);
        
        // Assert
        verify(eventValidator, times(1)).validate(testEvent);
        verify(orderProcessingService, times(1)).processOrder(any(OrderPlacedCommand.class));
    }
    
    /**
     * Test that handleOrderPlacedEvent correctly deserializes event data.
     * 
     * Validates Requirement 7.2: Deserialize JSON payload correctly
     */
    @Test
    void handleOrderPlacedEvent_ShouldDeserializeEventCorrectly() {
        // Arrange
        doNothing().when(eventValidator).validate(testEvent);
        doNothing().when(orderProcessingService).processOrder(any(OrderPlacedCommand.class));
        
        // Act
        orderEventListener.handleOrderPlacedEvent(testEvent);
        
        // Assert - verify the event passed to the service has the correct data
        verify(orderProcessingService).processOrder(argThat(command ->
            command.getOrderId().equals(testEvent.resolveOrderId()) &&
            command.getTableId().equals(testEvent.resolveTableId()) &&
            command.getCreatedAt().equals(testEvent.resolveCreatedAt())
        ));
    }
    
    /**
     * Test that exceptions from OrderProcessingService are propagated.
     * This allows the retry mechanism to be triggered.
     * 
     * Validates Requirement 7.7: Retry mechanism for failed messages
     */
    @Test
    void handleOrderPlacedEvent_ShouldPropagateExceptions() {
        // Arrange
        RuntimeException testException = new RuntimeException("Processing failed");
        doNothing().when(eventValidator).validate(testEvent);
        doThrow(testException).when(orderProcessingService).processOrder(any(OrderPlacedCommand.class));
        
        // Act & Assert
        assertThatThrownBy(() -> orderEventListener.handleOrderPlacedEvent(testEvent))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Processing failed");

        verify(orderProcessingService, times(1)).processOrder(any(OrderPlacedCommand.class));
    }

    @Test
    void handleOrderPlacedEvent_WithUnsupportedVersion_ShouldRejectWithoutRequeue() {
        doThrow(new UnsupportedEventVersionException(2)).when(eventValidator).validate(testEvent);

        assertThatThrownBy(() -> orderEventListener.handleOrderPlacedEvent(testEvent))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verify(orderProcessingService, never()).processOrder(any(OrderPlacedCommand.class));
    }
}
