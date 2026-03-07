package com.restaurant.orderservice.infrastructure.messaging;

import com.restaurant.orderservice.domain.event.OrderPlacedDomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.restaurant.orderservice.domain.event.OrderPlacedDomainEvent.OrderItemData;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderPlacedEventMessageMapperTest {

    private OrderPlacedEventMessageMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new OrderPlacedEventMessageMapper();
    }

    @Test
    void toMessage_shouldMapDomainEventToMessage() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime occurredAt = LocalDateTime.now();

        OrderPlacedDomainEvent domainEvent = OrderPlacedDomainEvent.builder()
                .eventId(eventId)
                .eventType("order.placed")
                .eventVersion(1)
                .orderId(orderId)
                .tableId(5)
                .items(List.of(
                        OrderItemData.builder()
                                .productId(1L)
                                .quantity(2)
                                .build(),
                        OrderItemData.builder()
                                .productId(2L)
                                .quantity(1)
                                .build()
                ))
                .createdAt(createdAt)
                .occurredAt(occurredAt)
                .build();

        OrderPlacedEventMessage message = mapper.toMessage(domainEvent);

        assertThat(message).isNotNull();
        assertThat(message.getEventId()).isEqualTo(eventId);
        assertThat(message.getEventType()).isEqualTo("order.placed");
        assertThat(message.getEventVersion()).isEqualTo(1);
        assertThat(message.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(message.getOrderId()).isEqualTo(orderId);
        assertThat(message.getTableId()).isEqualTo(5);
        assertThat(message.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void toMessage_shouldMapPayloadCorrectly() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        OrderPlacedDomainEvent domainEvent = OrderPlacedDomainEvent.builder()
                .eventId(eventId)
                .eventType("order.placed")
                .eventVersion(1)
                .orderId(orderId)
                .tableId(3)
                .items(List.of(
                        OrderItemData.builder()
                                .productId(10L)
                                .quantity(3)
                                .build()
                ))
                .createdAt(createdAt)
                .occurredAt(LocalDateTime.now())
                .build();

        OrderPlacedEventMessage message = mapper.toMessage(domainEvent);

        assertThat(message.getPayload()).isNotNull();
        assertThat(message.getPayload().getOrderId()).isEqualTo(orderId);
        assertThat(message.getPayload().getTableId()).isEqualTo(3);
        assertThat(message.getPayload().getCreatedAt()).isEqualTo(createdAt);
        assertThat(message.getPayload().getItems()).hasSize(1);
        assertThat(message.getPayload().getItems().get(0).getProductId()).isEqualTo(10L);
        assertThat(message.getPayload().getItems().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    void toMessage_shouldMapItemsCorrectly_whenMultipleItems() {
        OrderPlacedDomainEvent domainEvent = OrderPlacedDomainEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("order.placed")
                .eventVersion(1)
                .orderId(UUID.randomUUID())
                .tableId(1)
                .items(List.of(
                        OrderItemData.builder().productId(1L).quantity(1).build(),
                        OrderItemData.builder().productId(2L).quantity(2).build(),
                        OrderItemData.builder().productId(3L).quantity(5).build()
                ))
                .createdAt(LocalDateTime.now())
                .occurredAt(LocalDateTime.now())
                .build();

        OrderPlacedEventMessage message = mapper.toMessage(domainEvent);

        assertThat(message.getItems()).hasSize(3);
        assertThat(message.getPayload().getItems()).hasSize(3);
        
        assertThat(message.getItems().get(0).getProductId()).isEqualTo(1L);
        assertThat(message.getItems().get(0).getQuantity()).isEqualTo(1);
        
        assertThat(message.getItems().get(1).getProductId()).isEqualTo(2L);
        assertThat(message.getItems().get(1).getQuantity()).isEqualTo(2);
        
        assertThat(message.getItems().get(2).getProductId()).isEqualTo(3L);
        assertThat(message.getItems().get(2).getQuantity()).isEqualTo(5);
    }

    @Test
    void toMessage_shouldHandleNullItems() {
        OrderPlacedDomainEvent domainEvent = OrderPlacedDomainEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("order.placed")
                .eventVersion(1)
                .orderId(UUID.randomUUID())
                .tableId(1)
                .items(null)
                .createdAt(LocalDateTime.now())
                .occurredAt(LocalDateTime.now())
                .build();

        OrderPlacedEventMessage message = mapper.toMessage(domainEvent);

        assertThat(message.getItems()).isEmpty();
        assertThat(message.getPayload().getItems()).isEmpty();
    }

    @Test
    void toMessage_shouldHandleEmptyItems() {
        OrderPlacedDomainEvent domainEvent = OrderPlacedDomainEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("order.placed")
                .eventVersion(1)
                .orderId(UUID.randomUUID())
                .tableId(1)
                .items(List.of())
                .createdAt(LocalDateTime.now())
                .occurredAt(LocalDateTime.now())
                .build();

        OrderPlacedEventMessage message = mapper.toMessage(domainEvent);

        assertThat(message.getItems()).isEmpty();
        assertThat(message.getPayload().getItems()).isEmpty();
    }

    @Test
    void toMessage_shouldPreserveBackwardCompatibilityFields() {
        UUID orderId = UUID.randomUUID();
        Integer tableId = 7;
        LocalDateTime createdAt = LocalDateTime.now();

        OrderPlacedDomainEvent domainEvent = OrderPlacedDomainEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("order.placed")
                .eventVersion(1)
                .orderId(orderId)
                .tableId(tableId)
                .items(List.of(
                        OrderItemData.builder()
                                .productId(1L)
                                .quantity(1)
                                .build()
                ))
                .createdAt(createdAt)
                .occurredAt(LocalDateTime.now())
                .build();

        OrderPlacedEventMessage message = mapper.toMessage(domainEvent);

        // Check that legacy flat fields match the nested payload
        assertThat(message.getOrderId()).isEqualTo(message.getPayload().getOrderId());
        assertThat(message.getTableId()).isEqualTo(message.getPayload().getTableId());
        assertThat(message.getCreatedAt()).isEqualTo(message.getPayload().getCreatedAt());
        assertThat(message.getItems().size()).isEqualTo(message.getPayload().getItems().size());
    }
}
