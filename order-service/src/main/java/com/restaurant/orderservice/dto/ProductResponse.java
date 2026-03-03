package com.restaurant.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for product information.
 * 
 * Used to return product details in API responses, particularly for the menu endpoint.
 * Contains only the essential information needed by clients.
 * 
 * Validates Requirements: 1.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    
    /**
     * Unique identifier of the product.
     */
    private Long id;
    
    /**
     * Name of the product (e.g., "Pizza Margherita").
     */
    private String name;
    
    /**
     * Detailed description of the product.
     */
    private String description;

    /**
     * Product price.
     */
    private java.math.BigDecimal price;

    /**
     * Category label used by frontend tabs.
     */
    private String category;

    /**
     * Optional image URL.
     */
    private String imageUrl;

    /**
     * Active flag exposed for UI filtering.
     */
    private Boolean isActive;
}
