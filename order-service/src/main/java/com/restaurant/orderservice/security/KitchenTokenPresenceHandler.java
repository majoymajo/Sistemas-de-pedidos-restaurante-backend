package com.restaurant.orderservice.security;

import com.restaurant.orderservice.exception.KitchenAccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Validates that kitchen token header is present in protected requests.
 */
public class KitchenTokenPresenceHandler extends AbstractKitchenSecurityHandler {

    private final String tokenHeaderName;

    public KitchenTokenPresenceHandler(String tokenHeaderName) {
        this.tokenHeaderName = tokenHeaderName;
    }

    @Override
    public void handle(HttpServletRequest request) {
        String token = request.getHeader(tokenHeaderName);
        if (token == null || token.isBlank()) {
            throw new KitchenAccessDeniedException("Kitchen authentication token is required");
        }
        handleNext(request);
    }
}
