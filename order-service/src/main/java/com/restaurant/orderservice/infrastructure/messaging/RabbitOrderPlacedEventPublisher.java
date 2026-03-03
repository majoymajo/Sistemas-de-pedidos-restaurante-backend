package com.restaurant.orderservice.infrastructure.messaging;

import com.restaurant.orderservice.application.port.out.OrderPlacedEventPublisherPort;
import com.restaurant.orderservice.domain.event.OrderPlacedDomainEvent;
import com.restaurant.orderservice.exception.EventPublicationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ adapter for the order placed event output port.
 */
@Component
@Slf4j
public class RabbitOrderPlacedEventPublisher implements OrderPlacedEventPublisherPort {

    private final RabbitTemplate rabbitTemplate;
    private final OrderPlacedEventMessageMapper messageMapper;

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.routing-key.order-placed}")
    private String orderPlacedRoutingKey;

    public RabbitOrderPlacedEventPublisher(RabbitTemplate rabbitTemplate,
                                           OrderPlacedEventMessageMapper messageMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.messageMapper = messageMapper;
    }

    @Override
    public void publish(OrderPlacedDomainEvent domainEvent) {
        OrderPlacedEventMessage message = messageMapper.toMessage(domainEvent);
        try {
            rabbitTemplate.convertAndSend(exchangeName, orderPlacedRoutingKey, message, amqpMessage -> {
                amqpMessage.getMessageProperties().setHeader("eventType", message.getEventType());
                amqpMessage.getMessageProperties().setHeader("eventVersion", message.getEventVersion());
                return amqpMessage;
            });

            log.info(
                    "Successfully published order.placed event: eventId={}, orderId={}, version={}",
                    message.getEventId(),
                    message.getPayload() != null ? message.getPayload().getOrderId() : message.getOrderId(),
                    message.getEventVersion()
            );
        } catch (Exception ex) {
            log.error(
                    "Failed to publish order.placed event: eventId={}, orderId={}, error={}",
                    message.getEventId(),
                    message.getPayload() != null ? message.getPayload().getOrderId() : message.getOrderId(),
                    ex.getMessage(),
                    ex
            );
            throw new EventPublicationException(
                    String.format("Unable to publish order.placed event for orderId=%s",
                            message.getPayload() != null ? message.getPayload().getOrderId() : message.getOrderId()),
                    ex
            );
        }
    }
}
