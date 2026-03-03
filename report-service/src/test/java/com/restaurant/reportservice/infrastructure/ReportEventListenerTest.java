package com.restaurant.reportservice.infrastructure;

import com.restaurant.reportservice.application.command.OrderPlacedCommand;
import com.restaurant.reportservice.application.command.OrderReadyCommand;
import com.restaurant.reportservice.enums.OrderStatus;
import com.restaurant.reportservice.event.OrderPlacedEvent;
import com.restaurant.reportservice.event.OrderReadyEvent;
import com.restaurant.reportservice.exception.InvalidEventContractException;
import com.restaurant.reportservice.exception.UnsupportedEventVersionException;
import com.restaurant.reportservice.listener.ReportEventListener;
import com.restaurant.reportservice.service.OrderEventProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for AMQP event listener.
 * Verifies event consumption, validation, and error handling.
 */
@ExtendWith(MockitoExtension.class)
class ReportEventListenerTest {

    @Mock
    private OrderEventProcessingService orderEventProcessingService;

    private ReportEventListener reportEventListener;

    @BeforeEach
    void setUp() {
        reportEventListener = new ReportEventListener(orderEventProcessingService);
    }

    @Test
    @DisplayName("handleOrderReadyEvent should have @RabbitListener annotation")
    void handleOrderReadyEventShouldHaveRabbitListenerAnnotation() throws NoSuchMethodException {
        Method method = ReportEventListener.class.getMethod("handleOrderReadyEvent", OrderReadyEvent.class);
        RabbitListener annotation = method.getAnnotation(RabbitListener.class);
        assertNotNull(annotation, "handleOrderReadyEvent must have @RabbitListener annotation");
        assertTrue(annotation.queues().length > 0, "queues must not be empty");
    }

    @Test
    @DisplayName("Should process valid order.placed event successfully")
    void shouldProcessValidOrderPlacedEvent() {
        // Arrange
        OrderPlacedEvent event = createValidOrderPlacedEvent();

        // Act
        reportEventListener.handleOrderPlacedEvent(event);

        // Assert
        verify(orderEventProcessingService).processOrderPlaced(any());
    }

    @Test
    @DisplayName("Should process valid order.ready event successfully")
    void shouldProcessValidOrderReadyEvent() {
        // Arrange
        OrderReadyEvent event = createValidOrderReadyEvent();

        // Act
        reportEventListener.handleOrderReadyEvent(event);

        // Assert
        verify(orderEventProcessingService).processOrderReady(any());
    }

    @Test
    @DisplayName("Should reject event with unsupported version")
    void shouldRejectUnsupportedEventVersion() {
        // Arrange
        OrderPlacedEvent event = createValidOrderPlacedEvent();
        event.setEventVersion(2); // Unsupported version

        doThrow(new UnsupportedEventVersionException(2))
                .when(orderEventProcessingService).processOrderPlaced(any());

        // Act & Assert
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> 
            reportEventListener.handleOrderPlacedEvent(event)
        );
        verify(orderEventProcessingService).processOrderPlaced(any());
    }

    @Test
    @DisplayName("Should reject event with invalid contract")
    void shouldRejectInvalidEventContract() {
        // Arrange
        OrderPlacedEvent event = createValidOrderPlacedEvent();
        event.getPayload().setOrderId(null); // Invalid: missing orderId

        doThrow(new InvalidEventContractException("orderId is required"))
                .when(orderEventProcessingService).processOrderPlaced(any());

        // Act & Assert
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> 
            reportEventListener.handleOrderPlacedEvent(event)
        );
    }

    @Test
    @DisplayName("Should process order.placed event idempotently")
    void shouldProcessOrderPlacedIdempotently() {
        // Arrange
        OrderPlacedEvent event = createValidOrderPlacedEvent();

        // Act
        reportEventListener.handleOrderPlacedEvent(event);
        reportEventListener.handleOrderPlacedEvent(event); // Send twice

        // Assert
        verify(orderEventProcessingService, times(2)).processOrderPlaced(any());
    }

    private OrderPlacedEvent createValidOrderPlacedEvent() {
        OrderPlacedEvent event = new OrderPlacedEvent();
        event.setEventId(UUID.randomUUID());
        event.setEventType("order.placed");
        event.setEventVersion(1);
        event.setOccurredAt(LocalDateTime.now());

        OrderPlacedEvent.Payload payload = new OrderPlacedEvent.Payload();
        payload.setOrderId(UUID.randomUUID());
        payload.setTableId(1);
        payload.setCreatedAt(LocalDateTime.now());
        payload.setItems(Collections.emptyList());

        event.setPayload(payload);
        return event;
    }

    private OrderReadyEvent createValidOrderReadyEvent() {
        OrderReadyEvent event = new OrderReadyEvent();
        event.setEventId(UUID.randomUUID());
        event.setEventType("order.ready");
        event.setEventVersion(1);
        event.setOccurredAt(LocalDateTime.now());

        OrderReadyEvent.Payload payload = new OrderReadyEvent.Payload();
        payload.setOrderId(UUID.randomUUID());
        payload.setStatus("READY");
        payload.setUpdatedAt(LocalDateTime.now());

        event.setPayload(payload);
        return event;
    }
}

