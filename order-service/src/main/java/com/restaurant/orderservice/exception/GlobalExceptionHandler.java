package com.restaurant.orderservice.exception;

import com.restaurant.orderservice.dto.ErrorResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for the Order Service REST API.
 * 
 * This class provides centralized exception handling across all controllers,
 * converting exceptions into consistent ErrorResponse objects with appropriate
 * HTTP status codes.
 * 
 * Validates Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handles ProductNotFoundException.
     * Returns 404 Not Found when a referenced product does not exist or is not active.
     * 
     * @param ex the ProductNotFoundException that was thrown
     * @return ResponseEntity with ErrorResponse and 404 status
     * 
     * Validates Requirements: 11.2
     */
    /**
     * Handles InactiveProductException.
     * Returns 422 Unprocessable Entity when a referenced product is inactive.
     */
    @ExceptionHandler(InactiveProductException.class)
    public ResponseEntity<ErrorResponse> handleInactiveProduct(InactiveProductException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .error("Unprocessable Entity")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(ProductNotFoundException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    /**
     * Handles OrderNotFoundException.
     * Returns 404 Not Found when a referenced order does not exist.
     * 
     * @param ex the OrderNotFoundException that was thrown
     * @return ResponseEntity with ErrorResponse and 404 status
     * 
     * Validates Requirements: 11.2
     */
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    /**
     * Handles InvalidOrderException.
     * Returns 400 Bad Request when order validation fails.
     * 
     * @param ex the InvalidOrderException that was thrown
     * @return ResponseEntity with ErrorResponse and 400 status
     * 
     * Validates Requirements: 11.1
     */
    @ExceptionHandler(InvalidOrderException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrder(InvalidOrderException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    /**
     * Handles InvalidStatusTransitionException.
     * Returns 400 Bad Request when an invalid status transition is attempted.
     * 
     * Cumple con Copilot Instructions:
     * - Sección 4: Security - Backend Enforcement
     * - "Backend debe rechazar cambios de estado que no respeten el flujo definido"
     * 
     * @param ex the InvalidStatusTransitionException that was thrown
     * @return ResponseEntity with ErrorResponse and 400 status
     * 
     * Validates Requirements: 11.1, Security Requirement
     */
    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatusTransition(InvalidStatusTransitionException ex) {
        log.warn("Invalid status transition attempted: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    /**
     * Handles MethodArgumentNotValidException.
     * Returns 400 Bad Request when request validation fails (e.g., @Valid annotations).
     * Collects all field validation errors into a single message.
     * 
     * @param ex the MethodArgumentNotValidException that was thrown
     * @return ResponseEntity with ErrorResponse and 400 status
     * 
     * Validates Requirements: 11.1
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(String.join(", ", errors))
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    /**
     * Handles DataAccessException.
     * Returns 503 Service Unavailable when database operations fail.
     * 
     * @param ex the DataAccessException that was thrown
     * @return ResponseEntity with ErrorResponse and 503 status
     * 
     * Validates Requirements: 11.4
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseErrors(DataAccessException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable")
                .message("Database service is temporarily unavailable")
                .build();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }
    
    /**
     * Handles KitchenAccessDeniedException.
     * Returns 401 Unauthorized when kitchen token is missing or invalid.
     *
     * @param ex the KitchenAccessDeniedException that was thrown
     * @return ResponseEntity with ErrorResponse and 401 status
     */
    /**
     * Handles KitchenForbiddenException.
     * Returns 403 Forbidden when kitchen token is present but invalid.
     */
    @ExceptionHandler(KitchenForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleKitchenForbidden(KitchenForbiddenException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(KitchenAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleKitchenAccessDenied(KitchenAccessDeniedException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handles EventPublicationException.
     * Returns 503 Service Unavailable when message broker operations fail.
     *
     * @param ex the EventPublicationException that was thrown
     * @return ResponseEntity with ErrorResponse and 503 status
     */
    @ExceptionHandler(EventPublicationException.class)
    public ResponseEntity<ErrorResponse> handleEventPublicationError(EventPublicationException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable")
                .message("Message broker is temporarily unavailable")
                .build();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    /**
     * Handles all other uncaught exceptions.
     * Returns 500 Internal Server Error for unexpected errors.
     * 
     * @param ex the Exception that was thrown
     * @return ResponseEntity with ErrorResponse and 500 status
     * 
     * Validates Requirements: 11.3, 11.5, 11.6
     */
    /**
     * Handles HttpMessageNotReadableException.
     * Returns 400 Bad Request when the request body is malformed.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("Malformed request body")
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles MethodArgumentTypeMismatchException.
     * Returns 400 Bad Request when path variable or param type conversion fails.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("Invalid parameter: " + ex.getName())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(Exception ex) {
        log.error("Unhandled exception", ex);
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred")
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
