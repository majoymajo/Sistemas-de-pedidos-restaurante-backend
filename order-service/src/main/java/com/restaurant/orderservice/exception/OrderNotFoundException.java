package com.restaurant.orderservice.exception;

import java.util.UUID;

/**
 * Exception thrown when an order is not found in the database.
 * This exception is used to indicate that a referenced order ID does not exist.
 * 
 * Validates Requirements: 4.3, 6.4, 11.2
 */
public class OrderNotFoundException extends RuntimeException {
    
    /**
     * Constructs a new OrderNotFoundException with a descriptive message
     * containing the order ID that was not found.
     * 
     * @param orderId the UUID of the order that was not found
     */
    public OrderNotFoundException(UUID orderId) {
        super("Order not found with id: " + orderId);
    }
}
