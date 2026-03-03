package com.restaurant.reportservice.service;

import com.restaurant.reportservice.domain.model.DateRange;
import com.restaurant.reportservice.domain.model.OrderReport;
import com.restaurant.reportservice.domain.model.OrderItemReport;
import com.restaurant.reportservice.domain.model.ReportSummary;
import com.restaurant.reportservice.domain.service.ReportAggregationService;
import com.restaurant.reportservice.domain.service.DateRangeFilter;
import com.restaurant.reportservice.dto.ReportResponseDTO;
import com.restaurant.reportservice.entity.OrderReportEntity;
import com.restaurant.reportservice.entity.OrderItemReportEntity;
import com.restaurant.reportservice.enums.OrderStatus;
import com.restaurant.reportservice.repository.OrderReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReportService application layer.
 * Verifies service orchestration and business rules.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private OrderReportRepository orderReportRepository;

    @Mock
    private ReportAggregationService aggregationService;

    @Mock
    private DateRangeFilter dateRangeFilter;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(orderReportRepository, aggregationService, dateRangeFilter);
    }

    @Test
    @DisplayName("Should generate report with only READY orders within date range")
    void shouldGenerateReportWithReadyOrdersOnly() {
        // Arrange
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 28);
        DateRange dateRange = DateRange.of(startDate, endDate);

        List<OrderReportEntity> readyOrders = Arrays.asList(
                createOrderEntity(OrderStatus.READY),
                createOrderEntity(OrderStatus.READY)
        );

        ReportSummary mockSummary = ReportSummary.builder()
                .totalReadyOrders(2)
                .totalRevenue(new BigDecimal("50.00"))
                .productBreakdown(Collections.emptyList())
                .build();

        when(dateRangeFilter.validateAndCreate(startDate, endDate)).thenReturn(dateRange);
        when(orderReportRepository.findByStatusAndCreatedAtBetween(
                eq(OrderStatus.READY), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(readyOrders);
        when(aggregationService.aggregate(anyList())).thenReturn(mockSummary);

        // Act
        ReportResponseDTO response = reportService.generateReport(startDate, endDate);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getTotalReadyOrders());
        assertEquals(new BigDecimal("50.00"), response.getTotalRevenue());
        verify(orderReportRepository).findByStatusAndCreatedAtBetween(
                eq(OrderStatus.READY), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(aggregationService).aggregate(anyList());
    }

    @Test
    @DisplayName("Should return zero metrics when no orders exist in date range")
    void shouldReturnZeroMetricsWhenNoOrders() {
        // Arrange
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 28);
        DateRange dateRange = DateRange.of(startDate, endDate);

        ReportSummary emptyMockSummary = ReportSummary.builder()
                .totalReadyOrders(0)
                .totalRevenue(BigDecimal.ZERO)
                .productBreakdown(Collections.emptyList())
                .build();

        when(dateRangeFilter.validateAndCreate(startDate, endDate)).thenReturn(dateRange);
        when(orderReportRepository.findByStatusAndCreatedAtBetween(
                eq(OrderStatus.READY), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(aggregationService.aggregate(Collections.emptyList())).thenReturn(emptyMockSummary);

        // Act
        ReportResponseDTO response = reportService.generateReport(startDate, endDate);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getTotalReadyOrders());
        assertEquals(BigDecimal.ZERO, response.getTotalRevenue());
        assertTrue(response.getProductBreakdown().isEmpty());
    }

    @Test
    @DisplayName("Should apply date filtering before aggregation")
    void shouldApplyDateFilteringBeforeAggregation() {
        // Arrange
        LocalDate startDate = LocalDate.of(2026, 2, 10);
        LocalDate endDate = LocalDate.of(2026, 2, 20);
        DateRange dateRange = DateRange.of(startDate, endDate);

        when(dateRangeFilter.validateAndCreate(startDate, endDate)).thenReturn(dateRange);
        when(orderReportRepository.findByStatusAndCreatedAtBetween(
                eq(OrderStatus.READY), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(aggregationService.aggregate(anyList())).thenReturn(ReportSummary.builder()
                .totalReadyOrders(0)
                .totalRevenue(BigDecimal.ZERO)
                .productBreakdown(Collections.emptyList())
                .build());

        // Act
        reportService.generateReport(startDate, endDate);

        // Assert
        verify(dateRangeFilter).validateAndCreate(startDate, endDate);
        verify(orderReportRepository).findByStatusAndCreatedAtBetween(
                eq(OrderStatus.READY), 
                eq(startDate.atStartOfDay()),
                eq(endDate.atTime(23, 59, 59)));
    }

    @Test
    @DisplayName("Should map domain summary to DTO correctly")
    void shouldMapDomainSummaryToDTOCorrectly() {
        // Arrange
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 28);
        DateRange dateRange = DateRange.of(startDate, endDate);

        ReportSummary mockSummary = ReportSummary.builder()
                .totalReadyOrders(5)
                .totalRevenue(new BigDecimal("120.50"))
                .productBreakdown(Collections.emptyList())
                .build();

        when(dateRangeFilter.validateAndCreate(startDate, endDate)).thenReturn(dateRange);
        when(orderReportRepository.findByStatusAndCreatedAtBetween(
                any(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(aggregationService.aggregate(anyList())).thenReturn(mockSummary);

        // Act
        ReportResponseDTO response = reportService.generateReport(startDate, endDate);

        // Assert
        assertEquals(mockSummary.getTotalReadyOrders(), response.getTotalReadyOrders());
        assertEquals(mockSummary.getTotalRevenue(), response.getTotalRevenue());
    }

    @Test
    @DisplayName("Should handle null date parameters gracefully")
    void shouldHandleNullDates() {
        // Arrange
        when(dateRangeFilter.validateAndCreate(null, null))
                .thenThrow(new IllegalArgumentException("Dates cannot be null"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            reportService.generateReport(null, null)
        );
    }

    @Test
    @DisplayName("Should have readOnly transaction on generateReport")
    void shouldHaveReadOnlyTransactionOnGenerateReport() throws NoSuchMethodException {
        java.lang.reflect.Method method = ReportService.class.getMethod("generateReport", LocalDate.class, LocalDate.class);
        org.springframework.transaction.annotation.Transactional transactional = method.getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        assertNotNull(transactional, "generateReport should be annotated with @Transactional");
        assertTrue(transactional.readOnly(), "generateReport transaction should be readOnly");
    }

    // Helper methods
    private OrderReportEntity createOrderEntity(OrderStatus status) {
        OrderReportEntity order = OrderReportEntity.builder()
                .id(UUID.randomUUID())
                .tableId(1)
                .status(status)
                .createdAt(LocalDateTime.now())
                .receivedAt(LocalDateTime.now())
                .build();
        
        order.addItem(OrderItemReportEntity.builder()
                .productId(1L)
                .productName("Test Product")
                .quantity(1)
                .price(new BigDecimal("25.00"))
                .build());
        
        return order;
    }
}

