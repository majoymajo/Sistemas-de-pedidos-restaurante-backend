package com.restaurant.reportservice.dto;

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
public class ReportResponseDTO {
    private Integer totalReadyOrders;
    private BigDecimal totalRevenue;
    @Builder.Default
    private List<ProductBreakdownDTO> productBreakdown = new java.util.ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductBreakdownDTO {
        private Long productId;
        private String productName;
        private Integer quantitySold;
        private BigDecimal totalAccumulated;
    }
}
