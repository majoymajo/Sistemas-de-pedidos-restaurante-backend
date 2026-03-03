package com.restaurant.kitchenworker.event;

import com.restaurant.kitchenworker.exception.InvalidEventContractException;
import com.restaurant.kitchenworker.exception.UnsupportedEventVersionException;
import org.springframework.stereotype.Component;

/**
 * Validates incoming order.placed integration events before processing.
 */
@Component
public class OrderPlacedEventValidator {

    private static final int SUPPORTED_VERSION = 1;

    public void validate(OrderPlacedEvent event) {
        if (event == null) {
            throw new InvalidEventContractException("Event payload is null");
        }

        Integer version = event.resolveVersion();
        if (version == null) {
            throw new InvalidEventContractException("Event version is missing");
        }
        if (version != SUPPORTED_VERSION) {
            throw new UnsupportedEventVersionException(version);
        }

        if (event.getEventType() != null && !"order.placed".equals(event.getEventType())) {
            throw new InvalidEventContractException("Unexpected eventType: " + event.getEventType());
        }

        if (event.resolveOrderId() == null) {
            throw new InvalidEventContractException("orderId is required");
        }

        Integer tableId = event.resolveTableId();
        if (tableId == null || tableId <= 0) {
            throw new InvalidEventContractException("tableId must be a positive integer");
        }
    }
}
