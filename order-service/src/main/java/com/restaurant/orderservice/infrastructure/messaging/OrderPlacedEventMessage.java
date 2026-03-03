package com.restaurant.orderservice.infrastructure.messaging;

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
 * Transport contract for order placed integration events (v1).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderPlacedEventMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID eventId;
    private String eventType;
    private Integer eventVersion;
    private LocalDateTime occurredAt;
    private Payload payload;

    // Legacy flat fields kept for backward compatibility.
    private UUID orderId;
    private Integer tableId;
    private List<OrderItemPayload> items;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Payload implements Serializable {
        private static final long serialVersionUID = 1L;
        private UUID orderId;
        private Integer tableId;
        private List<OrderItemPayload> items;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderItemPayload implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long productId;
        private Integer quantity;
    }
}
