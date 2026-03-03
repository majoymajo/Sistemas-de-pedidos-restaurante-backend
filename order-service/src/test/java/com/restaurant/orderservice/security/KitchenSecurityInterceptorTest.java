package com.restaurant.orderservice.security;

import com.restaurant.orderservice.exception.KitchenAccessDeniedException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KitchenSecurityInterceptorTest {

    private final KitchenSecurityInterceptor interceptor =
            new KitchenSecurityInterceptor("X-Kitchen-Token", "1234");

    @Test
    void preHandle_allowsUnprotectedEndpoint() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/orders/abc");

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(allowed).isTrue();
    }

    @Test
    void preHandle_deniesProtectedEndpointWhenTokenMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/orders");

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(KitchenAccessDeniedException.class)
                .hasMessageContaining("required");
    }

    @Test
    void preHandle_deniesProtectedEndpointWhenTokenInvalid() {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/orders/123/status");
        request.addHeader("X-Kitchen-Token", "bad-token");

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(KitchenAccessDeniedException.class)
                .hasMessageContaining("invalid");
    }

    @Test
    void preHandle_allowsProtectedEndpointWhenTokenMatches() {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/orders/123/status");
        request.addHeader("X-Kitchen-Token", "1234");

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(allowed).isTrue();
    }

    @Test
    void preHandle_deniesDeleteAllWhenTokenMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/orders");

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(KitchenAccessDeniedException.class)
                .hasMessageContaining("required");
    }

    @Test
    void preHandle_allowsDeleteByIdWhenTokenMatches() {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/orders/123");
        request.addHeader("X-Kitchen-Token", "1234");

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(allowed).isTrue();
    }
}
