package com.restaurant.kitchenworker.event;

import com.restaurant.kitchenworker.exception.InvalidEventContractException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Complementary contract tests for {@link OrderPlacedEventValidator}.
 * Covers UNIT-DOM-13 from TEST_PLAN_V3.
 */
class OrderPlacedEventValidatorContractTest {

    private final OrderPlacedEventValidator validator = new OrderPlacedEventValidator();

    @Test
    @DisplayName("UNIT-DOM-13: validate with incorrect eventType should throw InvalidEventContractException")
    void validate_withIncorrectEventType_shouldThrowInvalidEventContractException() {
        // Arrange
        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .eventType("order.updated")
                .eventVersion(1)
                .payload(OrderPlacedEvent.Payload.builder()
                        .orderId(UUID.randomUUID())
                        .tableId(5)
                        .createdAt(LocalDateTime.now())
                        .build())
                .build();

        // Act & Assert
        assertThatThrownBy(() -> validator.validate(event))
                .isInstanceOf(InvalidEventContractException.class)
                .hasMessageContaining("Unexpected eventType")
                .hasMessageContaining("order.updated");
    }
}
