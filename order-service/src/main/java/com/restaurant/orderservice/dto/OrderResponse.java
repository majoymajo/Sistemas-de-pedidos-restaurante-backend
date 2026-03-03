package com.restaurant.orderservice.dto;

import com.restaurant.orderservice.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for order information.
 * 
 * Contains complete order details including all items, status, and timestamps.
 * Used to return order information in API responses.
 * 
 * Validates Requirements: 4.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    
    /**
     * Unique identifier of the order.
     */
    private UUID id;
    
    /**
     * The table number where the order was placed.
     */
    private Integer tableId;
    
    /**
     * Current status of the order (PENDING, IN_PREPARATION, READY).
     */
    private OrderStatus status;
    
    /**
     * List of items included in this order.
     */
    private List<OrderItemResponse> items;
    
    /**
     * Timestamp when the order was created.
     */
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when the order was last updated.
     */
    private LocalDateTime updatedAt;
}
