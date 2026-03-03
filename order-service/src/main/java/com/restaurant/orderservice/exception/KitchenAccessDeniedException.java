package com.restaurant.orderservice.exception;

/**
 * Exception thrown when a kitchen-protected endpoint is accessed without valid credentials.
 */
public class KitchenAccessDeniedException extends RuntimeException {

    public KitchenAccessDeniedException(String message) {
        super(message);
    }
}
