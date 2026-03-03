package com.restaurant.reportservice.domain.service;

import com.restaurant.reportservice.domain.model.OrderItemReport;
import com.restaurant.reportservice.domain.model.OrderReport;
import com.restaurant.reportservice.domain.model.ProductSummary;
import com.restaurant.reportservice.domain.model.ReportSummary;
import com.restaurant.reportservice.enums.OrderStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Domain service for aggregating order data into report summaries.
 * Pure business logic: filters READY orders, sums revenue, and groups by product.
 */
@Component
public class ReportAggregationService {

    public ReportSummary aggregate(List<OrderReport> orders) {
        List<OrderReport> readyOrders = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.READY)
                .collect(Collectors.toList());

        int totalReadyOrders = readyOrders.size();

        BigDecimal totalRevenue = readyOrders.stream()
                .flatMap(order -> order.getItems().stream())
                .map(item -> {
                    BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
                    return price.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, ProductAccumulator> productMap = new LinkedHashMap<>();
        readyOrders.stream()
                .flatMap(order -> order.getItems().stream())
                .forEach(item -> productMap
                        .computeIfAbsent(item.getProductId(),
                                id -> new ProductAccumulator(id, item.getProductName()))
                        .accumulate(item));

        List<ProductSummary> productBreakdown = productMap.values().stream()
                .map(ProductAccumulator::toSummary)
                .collect(Collectors.toList());

        return ReportSummary.builder()
                .totalReadyOrders(totalReadyOrders)
                .totalRevenue(totalRevenue)
                .productBreakdown(productBreakdown)
                .build();
    }

    private static class ProductAccumulator {
        private final Long productId;
        private final String productName;
        private int quantitySold;
        private BigDecimal totalAccumulated;

        ProductAccumulator(Long productId, String productName) {
            this.productId = productId;
            this.productName = productName;
            this.quantitySold = 0;
            this.totalAccumulated = BigDecimal.ZERO;
        }

        void accumulate(OrderItemReport item) {
            this.quantitySold += item.getQuantity();
            BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
            this.totalAccumulated = this.totalAccumulated
                    .add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        ProductSummary toSummary() {
            return ProductSummary.builder()
                    .productId(productId)
                    .productName(productName)
                    .quantitySold(quantitySold)
                    .totalAccumulated(totalAccumulated)
                    .build();
        }
    }
}
