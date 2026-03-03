package com.restaurant.orderservice.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Domain event emitted when an order is successfully created.
 * Keeps domain intent independent from transport-specific contracts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedDomainEvent {

    public static final int CURRENT_VERSION = 1;
    public static final String EVENT_TYPE = "order.placed";

    private UUID eventId;
    private String eventType;
    private Integer eventVersion;
    private LocalDateTime occurredAt;
    private UUID orderId;
    private Integer tableId;
    private List<OrderItemData> items;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemData {
        private Long productId;
        private Integer quantity;
    }
}
