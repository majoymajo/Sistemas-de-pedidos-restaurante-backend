package com.restaurant.kitchenworker.event;

import com.restaurant.kitchenworker.exception.InvalidEventContractException;
import com.restaurant.kitchenworker.exception.UnsupportedEventVersionException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderPlacedEventValidatorTest {

    private final OrderPlacedEventValidator validator = new OrderPlacedEventValidator();

    @Test
    void validate_withSupportedVersionAndRequiredFields_passes() {
        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .eventType("order.placed")
                .eventVersion(1)
                .payload(OrderPlacedEvent.Payload.builder()
                        .orderId(UUID.randomUUID())
                        .tableId(9)
                        .createdAt(LocalDateTime.now())
                        .build())
                .build();

        validator.validate(event);
    }

    @Test
    void validate_withUnsupportedVersion_throwsException() {
        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .eventVersion(2)
                .payload(OrderPlacedEvent.Payload.builder()
                        .orderId(UUID.randomUUID())
                        .tableId(9)
                        .createdAt(LocalDateTime.now())
                        .build())
                .build();

        assertThatThrownBy(() -> validator.validate(event))
                .isInstanceOf(UnsupportedEventVersionException.class);
    }

    @Test
    void validate_withMissingOrderId_throwsException() {
        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .eventVersion(1)
                .payload(OrderPlacedEvent.Payload.builder()
                        .tableId(9)
                        .createdAt(LocalDateTime.now())
                        .build())
                .build();

        assertThatThrownBy(() -> validator.validate(event))
                .isInstanceOf(InvalidEventContractException.class)
                .hasMessageContaining("orderId is required");
    }
}
