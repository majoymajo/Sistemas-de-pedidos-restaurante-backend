package com.restaurant.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for error information.
 * 
 * Provides a consistent error response structure across all API endpoints.
 * Includes timestamp, HTTP status code, error type, and descriptive message.
 * 
 * Validates Requirements: 11.1, 11.2, 11.3, 11.4, 11.5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    /**
     * Timestamp when the error occurred.
     */
    private LocalDateTime timestamp;
    
    /**
     * HTTP status code (e.g., 400, 404, 500, 503).
     */
    private Integer status;
    
    /**
     * Error type or category (e.g., "Bad Request", "Not Found", "Internal Server Error").
     */
    private String error;
    
    /**
     * Detailed error message describing what went wrong.
     */
    private String message;
}
