package com.restaurant.orderservice.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.mockito.Mockito.*;

/**
 * Branch coverage tests for KitchenEndpointScopeHandler.
 * Tests all conditional branches in requiresKitchenAuth method.
 */
class KitchenEndpointScopeHandlerTest {

    private KitchenEndpointScopeHandler handler;
    private KitchenSecurityHandler mockNextHandler;

    @BeforeEach
    void setUp() {
        handler = new KitchenEndpointScopeHandler();
        mockNextHandler = mock(KitchenSecurityHandler.class);
        handler.setNext(mockNextHandler);
    }

    @Test
    void handle_withOptionsRequest_doesNotCallNext() {
        // Arrange - OPTIONS requests should not require kitchen auth
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/orders");

        // Act
        handler.handle(request);

        // Assert - next handler should not be called
        verify(mockNextHandler, never()).handle(any());
    }

    @Test
    void handle_withGetOrders_callsNext() {
        // Arrange - GET /orders requires kitchen auth
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/orders");

        // Act
        handler.handle(request);

        // Assert - next handler should be called
        verify(mockNextHandler).handle(request);
    }

    @Test
    void handle_withGetSingleOrder_doesNotCallNext() {
        // Arrange - GET /orders/{id} does not require kitchen auth
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/orders/123");

        // Act
        handler.handle(request);

        // Assert - next handler should not be called
        verify(mockNextHandler, never()).handle(any());
    }

    @Test
    void handle_withDeleteAllOrders_callsNext() {
        // Arrange - DELETE /orders requires kitchen auth
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/orders");

        // Act
        handler.handle(request);

        // Assert - next handler should be called
        verify(mockNextHandler).handle(request);
    }

    @Test
    void handle_withDeleteSingleOrder_callsNext() {
        // Arrange - DELETE /orders/{id} requires kitchen auth
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/orders/456");

        // Act
        handler.handle(request);

        // Assert - next handler should be called
        verify(mockNextHandler).handle(request);
    }

    @Test
    void handle_withDeleteOrderWithUuid_callsNext() {
        // Arrange - DELETE /orders/{uuid} requires kitchen auth
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/orders/550e8400-e29b-41d4-a716-446655440000");

        // Act
        handler.handle(request);

        // Assert - next handler should be called
        verify(mockNextHandler).handle(request);
    }

    @Test
    void handle_withPatchOrderStatus_callsNext() {
        // Arrange - PATCH /orders/{id}/status requires kitchen auth
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/orders/123/status");

        // Act
        handler.handle(request);

        // Assert - next handler should be called
        verify(mockNextHandler).handle(request);
    }

    @Test
    void handle_withPatchOrderStatusUuid_callsNext() {
        // Arrange - PATCH /orders/{uuid}/status requires kitchen auth
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/orders/550e8400-e29b-41d4-a716-446655440000/status");

        // Act
        handler.handle(request);

        // Assert - next handler should be called
        verify(mockNextHandler).handle(request);
    }

    @Test
    void handle_withPostOrders_doesNotCallNext() {
        // Arrange - POST /orders does not require kitchen auth
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/orders");

        // Act
        handler.handle(request);

        // Assert - next handler should not be called
        verify(mockNextHandler, never()).handle(any());
    }

    @Test
    void handle_withGetMenu_doesNotCallNext() {
        // Arrange - GET /menu does not require kitchen auth
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/menu");

        // Act
        handler.handle(request);

        // Assert - next handler should not be called
        verify(mockNextHandler, never()).handle(any());
    }

    @Test
    void handle_withPutRequest_doesNotCallNext() {
        // Arrange - PUT requests are not defined as requiring kitchen auth
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/orders/123");

        // Act
        handler.handle(request);

        // Assert - next handler should not be called
        verify(mockNextHandler, never()).handle(any());
    }

    @Test
    void handle_withDeleteOrdersSubresource_doesNotCallNext() {
        // Arrange - DELETE /orders/123/items does not match pattern
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/orders/123/items");

        // Act
        handler.handle(request);

        // Assert - next handler should not be called
        verify(mockNextHandler, never()).handle(any());
    }

    @Test
    void handle_withOptionsOrdersWithId_doesNotCallNext() {
        // Arrange - OPTIONS on any path should not require auth
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/orders/123/status");

        // Act
        handler.handle(request);

        // Assert - next handler should not be called
        verify(mockNextHandler, never()).handle(any());
    }

    @Test
    void handle_withGetOrdersCaseInsensitive_callsNext() {
        // Arrange - method matching should be case-insensitive
        MockHttpServletRequest request = new MockHttpServletRequest("get", "/orders");

        // Act
        handler.handle(request);

        // Assert - next handler should be called
        verify(mockNextHandler).handle(request);
    }

    @Test
    void handle_withPatchCaseInsensitive_callsNext() {
        // Arrange - PATCH method should work case-insensitively
        MockHttpServletRequest request = new MockHttpServletRequest("patch", "/orders/123/status");

        // Act
        handler.handle(request);

        // Assert - next handler should be called
        verify(mockNextHandler).handle(request);
    }
}
