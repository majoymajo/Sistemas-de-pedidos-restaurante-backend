package com.restaurant.orderservice.infrastructure.messaging;

import com.restaurant.orderservice.domain.event.OrderPlacedDomainEvent;
import com.restaurant.orderservice.exception.EventPublicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitOrderPlacedEventPublisherTest {

    private RabbitTemplate rabbitTemplate;
    private RabbitOrderPlacedEventPublisher publisher;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        publisher = new RabbitOrderPlacedEventPublisher(rabbitTemplate, new OrderPlacedEventMessageMapper());
        ReflectionTestUtils.setField(publisher, "exchangeName", "restaurant.exchange");
        ReflectionTestUtils.setField(publisher, "orderPlacedRoutingKey", "order.placed");
    }

    @Test
    void publish_whenRabbitTemplateFails_throwsEventPublicationException() {
        OrderPlacedDomainEvent event = sampleDomainEvent();
        doThrow(new RuntimeException("broker down"))
                .when(rabbitTemplate)
                .convertAndSend(eq("restaurant.exchange"), eq("order.placed"), any(), any(MessagePostProcessor.class));

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(EventPublicationException.class)
                .hasMessageContaining("Unable to publish order.placed event");
    }

    @Test
    void publish_whenRabbitTemplateSucceeds_sendsVersionedMessage() {
        OrderPlacedDomainEvent event = sampleDomainEvent();

        publisher.publish(event);

        ArgumentCaptor<OrderPlacedEventMessage> messageCaptor = ArgumentCaptor.forClass(OrderPlacedEventMessage.class);
        verify(rabbitTemplate).convertAndSend(
                eq("restaurant.exchange"),
                eq("order.placed"),
                messageCaptor.capture(),
                any(MessagePostProcessor.class)
        );

        OrderPlacedEventMessage sent = messageCaptor.getValue();
        assertThat(sent.getEventType()).isEqualTo(OrderPlacedDomainEvent.EVENT_TYPE);
        assertThat(sent.getEventVersion()).isEqualTo(OrderPlacedDomainEvent.CURRENT_VERSION);
        assertThat(sent.getPayload()).isNotNull();
        assertThat(sent.getPayload().getOrderId()).isEqualTo(event.getOrderId());
    }

    private OrderPlacedDomainEvent sampleDomainEvent() {
        return OrderPlacedDomainEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(OrderPlacedDomainEvent.EVENT_TYPE)
                .eventVersion(OrderPlacedDomainEvent.CURRENT_VERSION)
                .occurredAt(LocalDateTime.now())
                .orderId(UUID.randomUUID())
                .tableId(10)
                .items(Collections.emptyList())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
