package com.restaurant.orderservice.infrastructure.messaging;

import com.restaurant.orderservice.domain.event.OrderPlacedDomainEvent;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Maps domain events to transport contract messages.
 */
@Component
public class OrderPlacedEventMessageMapper {

    public OrderPlacedEventMessage toMessage(OrderPlacedDomainEvent domainEvent) {
        List<OrderPlacedEventMessage.OrderItemPayload> items = Optional.ofNullable(domainEvent.getItems())
                .orElse(Collections.emptyList())
                .stream()
                .map(item -> OrderPlacedEventMessage.OrderItemPayload.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        OrderPlacedEventMessage.Payload payload = OrderPlacedEventMessage.Payload.builder()
                .orderId(domainEvent.getOrderId())
                .tableId(domainEvent.getTableId())
                .items(items)
                .createdAt(domainEvent.getCreatedAt())
                .build();

        return OrderPlacedEventMessage.builder()
                .eventId(domainEvent.getEventId())
                .eventType(domainEvent.getEventType())
                .eventVersion(domainEvent.getEventVersion())
                .occurredAt(domainEvent.getOccurredAt())
                .payload(payload)
                // Flat fields included to support mixed consumers during migration.
                .orderId(domainEvent.getOrderId())
                .tableId(domainEvent.getTableId())
                .items(items)
                .createdAt(domainEvent.getCreatedAt())
                .build();
    }
}
