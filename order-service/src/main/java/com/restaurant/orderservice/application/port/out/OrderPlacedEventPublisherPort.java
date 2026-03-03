package com.restaurant.orderservice.application.port.out;

import com.restaurant.orderservice.domain.event.OrderPlacedDomainEvent;

/**
 * Application output port for publishing order placed events.
 */
public interface OrderPlacedEventPublisherPort {
    void publish(OrderPlacedDomainEvent event);
}
