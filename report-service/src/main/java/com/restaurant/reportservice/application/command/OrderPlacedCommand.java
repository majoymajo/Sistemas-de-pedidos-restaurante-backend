package com.restaurant.reportservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedCommand {
    private UUID orderId;
    private Integer tableId;
    private LocalDateTime createdAt;
    @Builder.Default
    private List<OrderItemCommand> items = new java.util.ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemCommand {
        private Long productId;
        private String productName;
        private Integer quantity;
        private java.math.BigDecimal price;
    }
}
