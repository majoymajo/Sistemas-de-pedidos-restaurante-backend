package com.restaurant.orderservice.exception;

/**
 * Exception thrown when kitchen token is present but invalid.
 * Results in HTTP 403 Forbidden.
 * Extends KitchenAccessDeniedException to maintain compatibility with existing tests.
 */
public class KitchenForbiddenException extends KitchenAccessDeniedException {

    public KitchenForbiddenException(String message) {
        super(message);
    }
}
