package com.restaurant.orderservice.security;

import com.restaurant.orderservice.exception.KitchenForbiddenException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Validates token value for kitchen-protected requests.
 */
public class KitchenTokenValueHandler extends AbstractKitchenSecurityHandler {

    private final String tokenHeaderName;
    private final String expectedToken;

    public KitchenTokenValueHandler(String tokenHeaderName, String expectedToken) {
        this.tokenHeaderName = tokenHeaderName;
        this.expectedToken = expectedToken;
    }

    @Override
    public void handle(HttpServletRequest request) {
        String token = request.getHeader(tokenHeaderName);
        if (!expectedToken.equals(token)) {
            throw new KitchenForbiddenException("Kitchen authentication token is invalid");
        }
        handleNext(request);
    }
}
