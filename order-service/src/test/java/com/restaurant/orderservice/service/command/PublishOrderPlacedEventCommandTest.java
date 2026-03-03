package com.restaurant.orderservice.service.command;

import com.restaurant.orderservice.application.port.out.OrderPlacedEventPublisherPort;
import com.restaurant.orderservice.domain.event.OrderPlacedDomainEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PublishOrderPlacedEventCommandTest {

    @Test
    void execute_delegatesToOrderPlacedEventPublisherPort() {
        OrderPlacedEventPublisherPort publisherPort = mock(OrderPlacedEventPublisherPort.class);
        OrderPlacedDomainEvent event = OrderPlacedDomainEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(OrderPlacedDomainEvent.EVENT_TYPE)
                .eventVersion(OrderPlacedDomainEvent.CURRENT_VERSION)
                .occurredAt(LocalDateTime.now())
                .orderId(UUID.randomUUID())
                .tableId(8)
                .items(Collections.emptyList())
                .createdAt(LocalDateTime.now())
                .build();
        PublishOrderPlacedEventCommand command = new PublishOrderPlacedEventCommand(publisherPort, event);

        command.execute();

        verify(publisherPort).publish(event);
    }
}
