package com.restaurant.kitchenworker.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Application command used by kitchen workflow after event contract validation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedCommand {
    private UUID orderId;
    private Integer tableId;
    private LocalDateTime createdAt;
}
