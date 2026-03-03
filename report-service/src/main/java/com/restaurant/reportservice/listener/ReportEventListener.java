package com.restaurant.reportservice.listener;

import com.restaurant.reportservice.application.command.OrderPlacedCommand;
import com.restaurant.reportservice.application.command.OrderReadyCommand;
import com.restaurant.reportservice.enums.OrderStatus;
import com.restaurant.reportservice.event.OrderPlacedEvent;
import com.restaurant.reportservice.event.OrderReadyEvent;
import com.restaurant.reportservice.exception.InvalidEventContractException;
import com.restaurant.reportservice.exception.UnsupportedEventVersionException;
import com.restaurant.reportservice.service.OrderEventProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.stream.Collectors;

/**
 * AMQP event listener for order events.
 * Maps events to commands and delegates to processing service.
 * Catches domain exceptions and rejects messages to DLQ without requeue.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportEventListener {

    private final OrderEventProcessingService orderEventProcessingService;

    @RabbitListener(queues = "${rabbitmq.queue.name}")
    public void handleOrderPlacedEvent(OrderPlacedEvent event) {
        try {
            OrderPlacedCommand command = mapToPlacedCommand(event);
            orderEventProcessingService.processOrderPlaced(command);
        } catch (UnsupportedEventVersionException | InvalidEventContractException e) {
            log.error("Rejecting order.placed event: {}", e.getMessage());
            throw new AmqpRejectAndDontRequeueException(e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "${rabbitmq.queue.order-ready.name}")
    public void handleOrderReadyEvent(OrderReadyEvent event) {
        try {
            OrderReadyCommand command = mapToReadyCommand(event);
            orderEventProcessingService.processOrderReady(command);
        } catch (UnsupportedEventVersionException | InvalidEventContractException e) {
            log.error("Rejecting order.ready event: {}", e.getMessage());
            throw new AmqpRejectAndDontRequeueException(e.getMessage(), e);
        }
    }

    private OrderPlacedCommand mapToPlacedCommand(OrderPlacedEvent event) {
        OrderPlacedEvent.Payload payload = event.getPayload();

        return OrderPlacedCommand.builder()
                .orderId(payload.getOrderId())
                .tableId(payload.getTableId())
                .createdAt(payload.getCreatedAt())
                .items(payload.getItems() != null
                        ? payload.getItems().stream()
                            .map(item -> OrderPlacedCommand.OrderItemCommand.builder()
                                    .productId(item.getProductId())
                                    .productName(item.getProductName())
                                    .quantity(item.getQuantity())
                                    .price(item.getPrice())
                                    .build())
                            .collect(Collectors.toList())
                        : Collections.emptyList())
                .build();
    }

    private OrderReadyCommand mapToReadyCommand(OrderReadyEvent event) {
        OrderReadyEvent.Payload payload = event.getPayload();

        return OrderReadyCommand.builder()
                .orderId(payload.getOrderId())
                .status(OrderStatus.READY)
                .updatedAt(payload.getUpdatedAt())
                .build();
    }
}
