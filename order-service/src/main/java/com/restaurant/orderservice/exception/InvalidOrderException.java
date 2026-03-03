package com.restaurant.orderservice.exception;

/**
 * Exception thrown when an order request contains invalid data.
 * This exception is used to indicate validation failures such as:
 * - Invalid or missing table ID
 * - Empty order items list
 * - Invalid order status values
 * - Other business rule violations
 * 
 * Validates Requirements: 2.6, 11.2
 */
public class InvalidOrderException extends RuntimeException {
    
    /**
     * Constructs a new InvalidOrderException with a descriptive message
     * explaining what validation failed.
     * 
     * @param message a descriptive message explaining the validation failure
     */
    public InvalidOrderException(String message) {
        super(message);
    }
}
