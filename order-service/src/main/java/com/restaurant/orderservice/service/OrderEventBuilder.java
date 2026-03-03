package com.restaurant.orderservice.service;

import com.restaurant.orderservice.domain.event.OrderPlacedDomainEvent;
import com.restaurant.orderservice.entity.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builder for order-related domain events.
 * 
 * Single Responsibility: Constructs domain event objects from order entities.
 * Separated from OrderService to follow SRP and facilitate event schema evolution.
 */
@Component
@Slf4j
public class OrderEventBuilder {
    
    /**
     * Builds an OrderPlacedDomainEvent from an Order entity.
     * 
     * @param order The Order entity to convert to an event
     * @return OrderPlacedDomainEvent ready to be published through the output port
     */
    public OrderPlacedDomainEvent buildOrderPlacedEvent(Order order) {
        log.debug("Building OrderPlacedDomainEvent for order {}", order.getId());
        
        List<OrderPlacedDomainEvent.OrderItemData> eventItems = order.getItems().stream()
                .map(item -> new OrderPlacedDomainEvent.OrderItemData(
                        item.getProductId(),
                        item.getQuantity()
                ))
                .collect(Collectors.toList());
        
        return OrderPlacedDomainEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(OrderPlacedDomainEvent.EVENT_TYPE)
                .eventVersion(OrderPlacedDomainEvent.CURRENT_VERSION)
                .occurredAt(LocalDateTime.now())
                .orderId(order.getId())
                .tableId(order.getTableId())
                .items(eventItems)
                .createdAt(order.getCreatedAt())
                .build();
    }
}
