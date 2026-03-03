package com.restaurant.kitchenworker.listener;

import com.restaurant.kitchenworker.application.command.OrderPlacedCommand;
import com.restaurant.kitchenworker.event.OrderPlacedEvent;
import com.restaurant.kitchenworker.event.OrderPlacedEventValidator;
import com.restaurant.kitchenworker.exception.InvalidEventContractException;
import com.restaurant.kitchenworker.exception.UnsupportedEventVersionException;
import com.restaurant.kitchenworker.service.OrderProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ listener that consumes order placed events from the message queue.
 * 
 * This component listens to the configured RabbitMQ queue and delegates
 * event processing to the OrderProcessingService. It acts as the entry point
 * for asynchronous order processing in the Kitchen Worker service.
 * 
 * The listener is configured to:
 * - Listen to the queue specified in application.yml (rabbitmq.queue.name)
 * - Automatically deserialize JSON messages to OrderPlacedEvent objects
 * - Acknowledge messages after successful processing
 * - Retry failed messages according to the configured retry policy
 * - Route messages to the Dead Letter Queue after max retry attempts
 * 
 * Validates Requirements: 7.1, 7.2
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventListener {

    private final OrderProcessingService orderProcessingService;
    private final OrderPlacedEventValidator eventValidator;
    
    /**
     * Handles incoming order placed events from RabbitMQ.
     * 
     * This method is automatically invoked by Spring AMQP when a message
     * arrives in the configured queue. The message is automatically deserialized
     * from JSON to an OrderPlacedEvent object using the configured MessageConverter.
     * 
     * Processing flow:
     * 1. Receive and deserialize the OrderPlacedEvent from the queue
     * 2. Validate contract/version and map to application command
     * 3. Delegate processing to OrderProcessingService
     * 4. If processing succeeds, the message is acknowledged
     * 5. If processing fails, the exception triggers the retry mechanism
     * 
     * Error handling:
     * - Contract/version errors are rejected without requeue to move directly to DLQ
     * - Processing errors are rethrown to trigger configured retries
     * - After max retry attempts, the message is routed to the Dead Letter Queue
     * 
     * @param event The OrderPlacedEvent deserialized from the queue message
     * 
     * Validates Requirements:
     * - 7.1: Listen to the "order.placed" queue bound to the topic exchange
     * - 7.2: Deserialize JSON payload to OrderPlacedEvent
     */
    @RabbitListener(queues = "${rabbitmq.queue.name}")
    public void handleOrderPlacedEvent(OrderPlacedEvent event) {
        log.info(
                "Received order placed event from queue: eventId={}, orderId={}, tableId={}, version={}",
                event.getEventId(),
                event.resolveOrderId(),
                event.resolveTableId(),
                event.resolveVersion()
        );

        try {
            eventValidator.validate(event);

            OrderPlacedCommand command = OrderPlacedCommand.builder()
                    .orderId(event.resolveOrderId())
                    .tableId(event.resolveTableId())
                    .createdAt(event.resolveCreatedAt())
                    .build();

            orderProcessingService.processOrder(command);
        } catch (InvalidEventContractException | UnsupportedEventVersionException ex) {
            log.error("Rejecting invalid order.placed event: {}", ex.getMessage());
            throw new AmqpRejectAndDontRequeueException(ex.getMessage(), ex);
        }
    }
}
