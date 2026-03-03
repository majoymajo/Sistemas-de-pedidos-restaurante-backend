package com.restaurant.orderservice.exception;

import com.restaurant.orderservice.enums.OrderStatus;

/**
 * Exception thrown when attempting an invalid order status transition.
 * 
 * Cumple con Copilot Instructions:
 * - Sección 4: Security - Backend Enforcement
 * - "Backend debe rechazar cambios de estado que no respeten el flujo definido"
 */
public class InvalidStatusTransitionException extends RuntimeException {
    
    private final OrderStatus currentStatus;
    private final OrderStatus attemptedStatus;
    
    public InvalidStatusTransitionException(OrderStatus currentStatus, OrderStatus attemptedStatus) {
        super(String.format(
                "Invalid status transition from %s to %s. Valid transitions: PENDING → IN_PREPARATION → READY",
                currentStatus,
                attemptedStatus
        ));
        this.currentStatus = currentStatus;
        this.attemptedStatus = attemptedStatus;
    }
    
    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }
    
    public OrderStatus getAttemptedStatus() {
        return attemptedStatus;
    }
}
