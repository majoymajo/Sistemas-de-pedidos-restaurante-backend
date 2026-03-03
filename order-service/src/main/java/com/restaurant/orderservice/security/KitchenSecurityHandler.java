package com.restaurant.orderservice.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Handler contract for kitchen endpoint security checks.
 */
public interface KitchenSecurityHandler {

    KitchenSecurityHandler setNext(KitchenSecurityHandler next);

    void handle(HttpServletRequest request);
}
