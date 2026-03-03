package com.restaurant.orderservice.exception;

import com.restaurant.orderservice.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GlobalExceptionHandler.
 * 
 * Validates Requirements: 11.1, 11.2, 11.3, 11.4
 */
class GlobalExceptionHandlerTest {
    
    private GlobalExceptionHandler exceptionHandler;
    
    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }
    
    /**
     * Test: ProductNotFoundException returns 404 Not Found
     * 
     * Validates Requirements: 11.2
     */
    @Test
    void handleProductNotFound_ReturnsNotFound() {
        // Arrange
        Long productId = 123L;
        ProductNotFoundException exception = new ProductNotFoundException(productId);
        
        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleProductNotFound(exception);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMessage()).contains("Product not found with id: 123");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }
    
    /**
     * Test: OrderNotFoundException returns 404 Not Found
     * 
     * Validates Requirements: 11.2
     */
    @Test
    void handleOrderNotFound_ReturnsNotFound() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        OrderNotFoundException exception = new OrderNotFoundException(orderId);
        
        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleOrderNotFound(exception);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMessage()).contains("Order not found with id:");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }
    
    /**
     * Test: InvalidOrderException returns 400 Bad Request
     * 
     * Validates Requirements: 11.1
     */
    @Test
    void handleInvalidOrder_ReturnsBadRequest() {
        // Arrange
        String errorMessage = "Table ID must be positive";
        InvalidOrderException exception = new InvalidOrderException(errorMessage);
        
        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidOrder(exception);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo(errorMessage);
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }
    
    /**
     * Test: MethodArgumentNotValidException returns 400 Bad Request with validation errors
     * 
     * Validates Requirements: 11.1
     */
    @Test
    void handleValidationErrors_ReturnsBadRequest() {
        // Arrange
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("createOrderRequest", "tableId", "Table ID is required");
        FieldError fieldError2 = new FieldError("createOrderRequest", "items", "Order must contain at least one item");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));
        
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);
        
        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationErrors(exception);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).contains("Table ID is required");
        assertThat(response.getBody().getMessage()).contains("Order must contain at least one item");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }
    
    /**
     * Test: DataAccessException returns 503 Service Unavailable
     * 
     * Validates Requirements: 11.4
     */
    @Test
    void handleDatabaseErrors_ReturnsServiceUnavailable() {
        // Arrange
        DataAccessException exception = new DataAccessException("Database connection failed") {};
        
        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDatabaseErrors(exception);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(503);
        assertThat(response.getBody().getError()).isEqualTo("Service Unavailable");
        assertThat(response.getBody().getMessage()).isEqualTo("Database service is temporarily unavailable");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    /**
     * Test: KitchenAccessDeniedException returns 401 Unauthorized
     */
    @Test
    void handleKitchenAccessDenied_ReturnsUnauthorized() {
        // Arrange
        KitchenAccessDeniedException exception = new KitchenAccessDeniedException("Kitchen authentication token is required");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleKitchenAccessDenied(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getError()).isEqualTo("Unauthorized");
        assertThat(response.getBody().getMessage()).contains("Kitchen authentication token is required");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    /**
     * Test: EventPublicationException returns 503 Service Unavailable
     */
    @Test
    void handleEventPublicationError_ReturnsServiceUnavailable() {
        // Arrange
        EventPublicationException exception =
                new EventPublicationException("Unable to publish order.placed event", new RuntimeException("broker down"));

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleEventPublicationError(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(503);
        assertThat(response.getBody().getError()).isEqualTo("Service Unavailable");
        assertThat(response.getBody().getMessage()).isEqualTo("Message broker is temporarily unavailable");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }
    
    /**
     * Test: Generic Exception returns 500 Internal Server Error
     * 
     * Validates Requirements: 11.3
     */
    @Test
    void handleGenericError_ReturnsInternalServerError() {
        // Arrange
        Exception exception = new RuntimeException("Unexpected error");
        
        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericError(exception);
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }
}
