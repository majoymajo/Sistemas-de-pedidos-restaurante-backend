package com.restaurant.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for delete all orders operation.
 * Contains count and audit timestamp.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteAllOrdersResponse {
    private int deletedCount;
    private String deletedAt;
    private String deletedBy;
}
