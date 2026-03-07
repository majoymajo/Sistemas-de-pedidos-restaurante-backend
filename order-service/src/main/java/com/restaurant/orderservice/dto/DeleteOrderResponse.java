package com.restaurant.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for delete order operation.
 * Contains audit metadata for the deletion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteOrderResponse {
    private String deletedId;
    private String deletedAt;
    private String deletedBy;
}
