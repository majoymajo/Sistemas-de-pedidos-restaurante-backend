package com.restaurant.qa.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para la solicitud de creación de órdenes.
 * Mapea al contrato JSON del endpoint POST /orders.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    
    private Integer tableId;
    private List<OrderItem> items;
}
