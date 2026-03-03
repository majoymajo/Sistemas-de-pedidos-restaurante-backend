package com.restaurant.kitchenworker.enums;

/**
 * Enum representing the status of an order in the restaurant system.
 * 
 * Status flow:
 * PENDING -> IN_PREPARATION -> READY
 * 
 * - PENDING: Order has been created and is waiting to be processed
 * - IN_PREPARATION: Order is being prepared by the kitchen
 * - READY: Order is ready for delivery to the customer
 */
public enum OrderStatus {
    /**
     * Order has been created and is waiting to be processed by the kitchen
     */
    PENDING,
    
    /**
     * Order is currently being prepared by the kitchen
     */
    IN_PREPARATION,
    
    /**
     * Order is ready for delivery to the customer
     */
    READY
}
