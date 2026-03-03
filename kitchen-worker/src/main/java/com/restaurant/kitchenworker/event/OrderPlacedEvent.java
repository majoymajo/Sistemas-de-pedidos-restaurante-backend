package com.restaurant.kitchenworker.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Event published when an order is successfully placed.
 * This event is sent to RabbitMQ for asynchronous processing by the Kitchen Worker.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderPlacedEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private UUID eventId;
    private String eventType;
    private Integer eventVersion;
    private LocalDateTime occurredAt;
    private Payload payload;

    // Legacy flat fields preserved to accept old producers during migration.
    private UUID orderId;
    private Integer tableId;
    private List<OrderItemEventData> items;
    private LocalDateTime createdAt;

    public UUID resolveOrderId() {
        if (payload != null && payload.getOrderId() != null) {
            return payload.getOrderId();
        }
        return orderId;
    }

    public Integer resolveTableId() {
        if (payload != null && payload.getTableId() != null) {
            return payload.getTableId();
        }
        return tableId;
    }

    public LocalDateTime resolveCreatedAt() {
        if (payload != null && payload.getCreatedAt() != null) {
            return payload.getCreatedAt();
        }
        return createdAt;
    }

    public Integer resolveVersion() {
        return eventVersion != null ? eventVersion : 1;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Payload implements Serializable {
        private static final long serialVersionUID = 1L;
        private UUID orderId;
        private Integer tableId;
        private List<OrderItemEventData> items;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderItemEventData implements Serializable {
        
        private static final long serialVersionUID = 1L;
        private Long productId;
        private Integer quantity;
    }
}
