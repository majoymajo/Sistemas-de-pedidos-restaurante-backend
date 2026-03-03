package com.restaurant.orderservice.dto;

import com.restaurant.orderservice.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating the status of an existing order.
 * Contains the new status with validation constraints.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {
    
    @NotNull(message = "Status is required")
    private OrderStatus status;
}
