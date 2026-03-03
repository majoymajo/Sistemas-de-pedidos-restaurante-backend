package com.restaurant.reportservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummary {
    private Integer totalReadyOrders;
    private BigDecimal totalRevenue;
    @Builder.Default
    private List<ProductSummary> productBreakdown = new java.util.ArrayList<>();
}
