package com.restaurant.reportservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.restaurant.reportservice.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderReadyEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID eventId;
    private String eventType;
    private Integer eventVersion;
    private LocalDateTime occurredAt;
    private Payload payload;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Payload {
        private UUID orderId;
        private String status;
        private LocalDateTime updatedAt;
    }
}
