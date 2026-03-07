package com.restaurant.orderservice.exception;

/**
 * Exception thrown when a product exists but is inactive/discontinued.
 * Results in HTTP 422 Unprocessable Entity.
 */
public class InactiveProductException extends RuntimeException {

    public InactiveProductException(Long productId) {
        super("Product with id " + productId + " is inactive");
    }
}
