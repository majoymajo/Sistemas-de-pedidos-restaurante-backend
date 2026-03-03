package com.restaurant.orderservice.security;

import com.restaurant.orderservice.exception.KitchenAccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Branch coverage tests for KitchenTokenPresenceHandler.
 * Tests all conditional branches: null token, blank token, valid token.
 */
class KitchenTokenPresenceHandlerTest {

    private KitchenTokenPresenceHandler handler;
    private KitchenSecurityHandler mockNextHandler;
    private static final String HEADER_NAME = "X-Kitchen-Token";

    @BeforeEach
    void setUp() {
        handler = new KitchenTokenPresenceHandler(HEADER_NAME);
        mockNextHandler = mock(KitchenSecurityHandler.class);
        handler.setNext(mockNextHandler);
    }

    @Test
    void handle_withNullToken_throwsException() {
        // Arrange - no token header present
        MockHttpServletRequest request = new MockHttpServletRequest();
        // token is null

        // Act & Assert - should throw KitchenAccessDeniedException
        assertThatThrownBy(() -> handler.handle(request))
                .isInstanceOf(KitchenAccessDeniedException.class)
                .hasMessageContaining("Kitchen authentication token is required");

        verify(mockNextHandler, never()).handle(any());
    }

    @Test
    void handle_withEmptyToken_throwsException() {
        // Arrange - token header is empty string
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER_NAME, "");

        // Act & Assert - should throw KitchenAccessDeniedException
        assertThatThrownBy(() -> handler.handle(request))
                .isInstanceOf(KitchenAccessDeniedException.class)
                .hasMessageContaining("Kitchen authentication token is required");

        verify(mockNextHandler, never()).handle(any());
    }

    @Test
    void handle_withBlankToken_throwsException() {
        // Arrange - token header contains only whitespace
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER_NAME, "   ");

        // Act & Assert - should throw KitchenAccessDeniedException
        assertThatThrownBy(() -> handler.handle(request))
                .isInstanceOf(KitchenAccessDeniedException.class)
                .hasMessageContaining("Kitchen authentication token is required");

        verify(mockNextHandler, never()).handle(any());
    }

    @Test
    void handle_withValidToken_callsNext() {
        // Arrange - token header has valid value
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER_NAME, "valid-token");

        // Act - should not throw exception
        handler.handle(request);

        // Assert - next handler should be called
        verify(mockNextHandler).handle(request);
    }

    @Test
    void handle_withTokenContainingSpaces_callsNext() {
        // Arrange - token with spaces but not blank
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER_NAME, "token with spaces");

        // Act
        handler.handle(request);

        // Assert - should pass through as it's not blank
        verify(mockNextHandler).handle(request);
    }

    @Test
    void handle_withTabsInToken_throwsException() {
        // Arrange - token containing only tabs (blank)
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER_NAME, "\t\t");

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(request))
                .isInstanceOf(KitchenAccessDeniedException.class)
                .hasMessageContaining("Kitchen authentication token is required");
    }

    @Test
    void handle_withNewlinesInToken_throwsException() {
        // Arrange - token containing only newlines (blank)
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER_NAME, "\n\r");

        // Act & Assert
        assertThatThrownBy(() -> handler.handle(request))
                .isInstanceOf(KitchenAccessDeniedException.class)
                .hasMessageContaining("Kitchen authentication token is required");
    }
}
