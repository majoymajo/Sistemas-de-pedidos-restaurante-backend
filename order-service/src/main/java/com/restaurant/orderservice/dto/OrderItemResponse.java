package com.restaurant.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for order item information.
 * 
 * Represents a single item within an order, including the product reference,
 * quantity, and any special notes.
 * 
 * Validates Requirements: 4.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    
    /**
     * Unique identifier of the order item.
     */
    private Long id;
    
    /**
     * ID of the product being ordered.
     */
    private Long productId;
    
    /**
     * Name of the product being ordered.
     */
    private String productName;
    
    /**
     * Quantity of the product ordered.
     */
    private Integer quantity;
    
    /**
     * Optional notes or special instructions for this item.
     */
    private String note;
}
