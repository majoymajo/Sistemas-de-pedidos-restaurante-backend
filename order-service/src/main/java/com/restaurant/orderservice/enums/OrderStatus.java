package com.restaurant.orderservice.enums;

import com.restaurant.orderservice.exception.InvalidStatusTransitionException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Enum representing the status of an order in the restaurant system.
 * 
 * Status flow (strictly enforced):
 * PENDING -> IN_PREPARATION -> READY
 * 
 * - PENDING: Order has been created and is waiting to be processed
 * - IN_PREPARATION: Order is being prepared by the kitchen
 * - READY: Order is ready for delivery to the customer
 * 
 * Cumple con Copilot Instructions:
 * - Sección 4: Security - Backend Enforcement
 * - "Backend debe rechazar cambios de estado que no respeten el flujo definido"
 */
public enum OrderStatus {
    /**
     * Order has been created and is waiting to be processed by the kitchen.
     * Can only transition to: IN_PREPARATION
     */
    PENDING,
    
    /**
     * Order is currently being prepared by the kitchen.
     * Can only transition to: READY
     */
    IN_PREPARATION,
    
    /**
     * Order is ready for delivery to the customer.
     * This is a final state - no further transitions allowed.
     */
    READY;
    
    /**
     * Map of valid transitions for each status.
     * Defines the allowed state machine flow.
     */
    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            PENDING, EnumSet.of(IN_PREPARATION),
            IN_PREPARATION, EnumSet.of(READY),
            READY, EnumSet.noneOf(OrderStatus.class)
    );
    
    /**
     * Checks if a transition from current status to new status is valid.
     * 
     * @param currentStatus The current status of the order
     * @param newStatus The desired new status
     * @return true if the transition is valid, false otherwise
     */
    public static boolean isValidTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }
        Set<OrderStatus> allowedTransitions = VALID_TRANSITIONS.get(currentStatus);
        return allowedTransitions != null && allowedTransitions.contains(newStatus);
    }
    
    /**
     * Validates a status transition and throws an exception if invalid.
     * 
     * @param currentStatus The current status of the order
     * @param newStatus The desired new status
     * @throws InvalidStatusTransitionException if the transition is not allowed
     */
    public static void validateTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (!isValidTransition(currentStatus, newStatus)) {
            throw new InvalidStatusTransitionException(currentStatus, newStatus);
        }
    }
}

