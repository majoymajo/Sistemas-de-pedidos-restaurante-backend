package com.restaurant.orderservice.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that runs the kitchen security chain before controller execution.
 */
@Component
public class KitchenSecurityInterceptor implements HandlerInterceptor {

    private final KitchenSecurityHandler securityChain;

    public KitchenSecurityInterceptor(
            @Value("${security.kitchen.token-header}") String tokenHeaderName,
            @Value("${security.kitchen.token-value}") String expectedToken) {
        KitchenEndpointScopeHandler scopeHandler = new KitchenEndpointScopeHandler();
        KitchenTokenPresenceHandler presenceHandler = new KitchenTokenPresenceHandler(tokenHeaderName);
        KitchenTokenValueHandler valueHandler = new KitchenTokenValueHandler(tokenHeaderName, expectedToken);
        scopeHandler.setNext(presenceHandler).setNext(valueHandler);
        this.securityChain = scopeHandler;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        securityChain.handle(request);
        return true;
    }
}
