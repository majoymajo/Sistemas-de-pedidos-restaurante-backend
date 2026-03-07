package com.restaurant.qa.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para representar un item dentro de una orden.
 * Mapea al contrato JSON del endpoint POST /orders.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    
    private Integer productId;
    private Integer quantity;
}
