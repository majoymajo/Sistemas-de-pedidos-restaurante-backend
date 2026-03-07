package com.restaurant.orderservice.security;

import com.restaurant.orderservice.exception.KitchenForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Branch coverage tests for KitchenTokenValueHandler.
 * Tests all conditional branches: invalid token, valid token.
 */
class KitchenTokenValueHandlerTest {

    private KitchenTokenValueHandler handler;
    private KitchenSecurityHandler mockNextHandler;
    private static final String HEADER_NAME = "X-Kitchen-Token";
    private static final String EXPECTED_TOKEN = "secret-kitchen-token";

    @BeforeEach
    void setUp() {
        handler = new KitchenTokenValueHandler(HEADER_NAME, EXPECTED_TOKEN);
        mockNextHandler = mock(KitchenSecurityHandler.class);
        handler.setNext(mockNextHandler);
    }

    @Test
    void handle_withCorrectToken_callsNext() {
        // Arrange - request with correct token
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER_NAME, EXPECTED_TOKEN);

        // Act - should not throw exception
        handler.handle(request);

        // Assert - next handler should be called
        verify(mockNextHandler).handle(request);
    }

    @Test
    void handle_withIncorrectToken_throwsException() {
        // Arrange - request with wrong token
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER_NAME, "wrong-token");

        // Act & Assert - should throw KitchenForbiddenException
        assertThatThrownBy(() -> handler.handle(request))
                .isInstanceOf(KitchenForbiddenException.class)
                .hasMessageContaining("Kitchen authentication token is invalid");

        verify(mockNextHandler, never()).handle(any());
    }

    @Test
    void handle_withNullToken_throwsException() {
        // Arrange - request without token header
        MockHttpServletRequest request = new MockHttpServletRequest();
        // no header added

        // Act & Assert - should throw KitchenForbiddenException
        assertThatThrownBy(() -> handler.handle(request))
                .isInstanceOf(KitchenForbiddenException.class)
                .hasMessageContaining("Kitchen authentication token is invalid");

        verify(mockNextHandler, never()).handle(any());
    }

    @Test
    void handle_withEmptyToken_throwsException() {
        // Arrange - request with empty token
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER_NAME, "");

        // Act & Assert - should throw KitchenForbiddenException
        assertThatThrownBy(() -> handler.handle(request))
                .isInstanceOf(KitchenForbiddenException.class)
                .hasMessageContaining("Kitchen authentication token is invalid");

        verify(mockNextHandler, never()).handle(any());
    }

    @Test
    void handle_withTokenHavingExtraSpaces_throwsException() {
        // Arrange - token with extra spaces (exact match required)
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER_NAME, " " + EXPECTED_TOKEN + " ");

        // Act & Assert - should throw because exact match is required
        assertThatThrownBy(() -> handler.handle(request))
                .isInstanceOf(KitchenForbiddenException.class)
                .hasMessageContaining("Kitchen authentication token is invalid");

        verify(mockNextHandler, never()).handle(any());
    }

    @Test
    void handle_withCaseDifferentToken_throwsException() {
        // Arrange - token with different case
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER_NAME, EXPECTED_TOKEN.toUpperCase());

        // Act & Assert - should throw because comparison is case-sensitive
        assertThatThrownBy(() -> handler.handle(request))
                .isInstanceOf(KitchenForbiddenException.class)
                .hasMessageContaining("Kitchen authentication token is invalid");

        verify(mockNextHandler, never()).handle(any());
    }

    @Test
    void handle_withPartialMatch_throwsException() {
        // Arrange - token is a substring of expected token
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER_NAME, EXPECTED_TOKEN.substring(0, 5));

        // Act & Assert - should throw because partial match is not valid
        assertThatThrownBy(() -> handler.handle(request))
                .isInstanceOf(KitchenForbiddenException.class)
                .hasMessageContaining("Kitchen authentication token is invalid");

        verify(mockNextHandler, never()).handle(any());
    }
}
