package com.restaurant.orderservice.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Base handler that supports chaining to the next security check.
 */
public abstract class AbstractKitchenSecurityHandler implements KitchenSecurityHandler {

    private KitchenSecurityHandler next;

    @Override
    public KitchenSecurityHandler setNext(KitchenSecurityHandler next) {
        this.next = next;
        return next;
    }

    protected void handleNext(HttpServletRequest request) {
        if (next != null) {
            next.handle(request);
        }
    }
}
