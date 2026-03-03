package com.restaurant.reportservice.service;

import com.restaurant.reportservice.domain.model.OrderItemReport;
import com.restaurant.reportservice.domain.model.OrderReport;
import com.restaurant.reportservice.domain.model.DateRange;
import com.restaurant.reportservice.domain.model.ReportSummary;
import com.restaurant.reportservice.domain.service.ReportAggregationService;
import com.restaurant.reportservice.domain.service.DateRangeFilter;
import com.restaurant.reportservice.dto.ReportResponseDTO;
import com.restaurant.reportservice.entity.OrderReportEntity;
import com.restaurant.reportservice.enums.OrderStatus;
import com.restaurant.reportservice.repository.OrderReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Application service that orchestrates report generation.
 * Coordinates between repository, date filtering, and aggregation.
 */
@Service
public class ReportService {

    private final OrderReportRepository orderReportRepository;
    private final ReportAggregationService aggregationService;
    private final DateRangeFilter dateRangeFilter;

    public ReportService(OrderReportRepository orderReportRepository,
                         ReportAggregationService aggregationService,
                         DateRangeFilter dateRangeFilter) {
        this.orderReportRepository = orderReportRepository;
        this.aggregationService = aggregationService;
        this.dateRangeFilter = dateRangeFilter;
    }

    @Transactional(readOnly = true)
    public ReportResponseDTO generateReport(LocalDate startDate, LocalDate endDate) {
        DateRange dateRange = dateRangeFilter.validateAndCreate(startDate, endDate);

        LocalDateTime startDateTime = dateRange.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = dateRange.getEndDate().atTime(23, 59, 59);

        List<OrderReportEntity> entities = orderReportRepository
                .findByStatusAndCreatedAtBetween(OrderStatus.READY, startDateTime, endDateTime);

        List<OrderReport> domainOrders = entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());

        ReportSummary summary = aggregationService.aggregate(domainOrders);

        return toDTO(summary);
    }

    private OrderReport toDomain(OrderReportEntity entity) {
        List<OrderItemReport> items = entity.getItems().stream()
                .map(item -> OrderItemReport.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderReport.builder()
                .id(entity.getId())
                .tableId(entity.getTableId())
                .status(entity.getStatus())
                .items(items)
                .createdAt(entity.getCreatedAt())
                .receivedAt(entity.getReceivedAt())
                .build();
    }

    private ReportResponseDTO toDTO(ReportSummary summary) {
        List<ReportResponseDTO.ProductBreakdownDTO> breakdown = summary.getProductBreakdown().stream()
                .map(ps -> ReportResponseDTO.ProductBreakdownDTO.builder()
                        .productId(ps.getProductId())
                        .productName(ps.getProductName())
                        .quantitySold(ps.getQuantitySold())
                        .totalAccumulated(ps.getTotalAccumulated())
                        .build())
                .collect(Collectors.toList());

        return ReportResponseDTO.builder()
                .totalReadyOrders(summary.getTotalReadyOrders())
                .totalRevenue(summary.getTotalRevenue())
                .productBreakdown(breakdown)
                .build();
    }
}
