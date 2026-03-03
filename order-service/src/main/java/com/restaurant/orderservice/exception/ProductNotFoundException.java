package com.restaurant.orderservice.exception;

/**
 * Exception thrown when a product is not found in the database.
 * This exception is used to indicate that a referenced product ID does not exist
 * or the product is not active.
 * 
 * Validates Requirements: 2.6, 11.2
 */
public class ProductNotFoundException extends RuntimeException {
    
    /**
     * Constructs a new ProductNotFoundException with a descriptive message
     * containing the product ID that was not found.
     * 
     * @param productId the ID of the product that was not found
     */
    public ProductNotFoundException(Long productId) {
        super("Product not found with id: " + productId);
    }
}
