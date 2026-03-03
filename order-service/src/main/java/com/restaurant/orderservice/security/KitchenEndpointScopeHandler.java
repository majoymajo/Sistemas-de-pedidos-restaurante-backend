package com.restaurant.orderservice.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.AntPathMatcher;

/**
 * First handler in the chain. It decides whether the current request requires kitchen auth.
 */
public class KitchenEndpointScopeHandler extends AbstractKitchenSecurityHandler {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public void handle(HttpServletRequest request) {
        if (!requiresKitchenAuth(request)) {
            return;
        }
        handleNext(request);
    }

    private boolean requiresKitchenAuth(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return false;
        }
        if ("GET".equalsIgnoreCase(method) && "/orders".equals(uri)) {
            return true;
        }
        if ("DELETE".equalsIgnoreCase(method) && "/orders".equals(uri)) {
            return true;
        }
        if ("DELETE".equalsIgnoreCase(method) && PATH_MATCHER.match("/orders/*", uri)) {
            return true;
        }
        return "PATCH".equalsIgnoreCase(method) && PATH_MATCHER.match("/orders/*/status", uri);
    }
}
