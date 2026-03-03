package com.restaurant.reportservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.restaurant.reportservice.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderReport {
    private UUID id;
    private Integer tableId;
    private OrderStatus status;
    @Builder.Default
    private List<OrderItemReport> items = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime receivedAt;
}
