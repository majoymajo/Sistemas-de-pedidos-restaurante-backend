package com.restaurant.orderservice.service.command;

import com.restaurant.orderservice.application.port.out.OrderPlacedEventPublisherPort;
import com.restaurant.orderservice.domain.event.OrderPlacedDomainEvent;

/**
 * Concrete command that publishes an order.placed event.
 */
public class PublishOrderPlacedEventCommand implements OrderCommand {

    private final OrderPlacedEventPublisherPort orderPlacedEventPublisherPort;
    private final OrderPlacedDomainEvent event;

    public PublishOrderPlacedEventCommand(OrderPlacedEventPublisherPort orderPlacedEventPublisherPort,
                                          OrderPlacedDomainEvent event) {
        this.orderPlacedEventPublisherPort = orderPlacedEventPublisherPort;
        this.event = event;
    }

    @Override
    public void execute() {
        orderPlacedEventPublisherPort.publish(event);
    }
}
