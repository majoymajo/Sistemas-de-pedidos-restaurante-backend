package com.restaurant.reportservice.application.command;

import com.restaurant.reportservice.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderReadyCommand {
    private UUID orderId;
    private OrderStatus status;
    private LocalDateTime updatedAt;
}
