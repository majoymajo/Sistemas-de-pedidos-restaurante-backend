package com.restaurant.qa.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para la respuesta de órdenes.
 * Mapea al contrato JSON retornado por GET /orders/{id}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderResponse {
    
    private String id;
    private Integer tableId;
    private String status;
    private List<OrderItemResponse> items;
    private String createdAt;
    private String updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderItemResponse {
        private Long id;
        private Integer productId;
        private String productName;
        private Integer quantity;
        private Double unitPrice;
        private Double subtotal;
    }
}
