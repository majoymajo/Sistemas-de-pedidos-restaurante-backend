package com.restaurant.reportservice.integration;

import com.restaurant.reportservice.entity.OrderItemReportEntity;
import com.restaurant.reportservice.entity.OrderReportEntity;
import com.restaurant.reportservice.enums.OrderStatus;
import com.restaurant.reportservice.repository.OrderReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for GET /reports endpoint — TEST_PLAN_V3 compliance.
 * Covers INT-RPT-01 through INT-RPT-06 with stricter assertions
 * than the existing ReportControllerIntegrationTest.
 *
 * These tests validate ErrorResponse structure and correct HTTP codes
 * as specified in TEST_PLAN_V3.md §2.8.
 *
 * @see docs/week-3-review/TEST_PLAN_V3.md §2.8
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderReportRepository reportRepository;

    @BeforeEach
    void setUp() {
        reportRepository.deleteAll();
    }

    private OrderReportEntity createOrder(OrderStatus status, LocalDateTime createdAt,
                                          OrderItemReportEntity... items) {
        OrderReportEntity order = OrderReportEntity.builder()
                .id(UUID.randomUUID())
                .tableId(1)
                .status(status)
                .createdAt(createdAt)
                .receivedAt(LocalDateTime.now())
                .build();
        for (OrderItemReportEntity item : items) {
            order.addItem(item);
        }
        return reportRepository.save(order);
    }

    private OrderItemReportEntity item(Long productId, String name, int qty, BigDecimal price) {
        return OrderItemReportEntity.builder()
                .productId(productId)
                .productName(name)
                .quantity(qty)
                .price(price)
                .build();
    }

    // ── INT-RPT-01: Reporte con datos válidos ───────────────────────────

    @Test
    @DisplayName("INT-RPT-01: GET /reports con rango válido y datos retorna 200 con estructura completa")
    void getReport_validRangeWithData_returns200WithStructure() throws Exception {
        // Arrange
        createOrder(OrderStatus.READY,
                LocalDateTime.of(2026, 2, 15, 12, 0),
                item(1L, "Hamburguesa", 2, BigDecimal.valueOf(15.50)));

        // Act & Assert
        mockMvc.perform(get("/reports")
                        .param("startDate", "2026-02-01")
                        .param("endDate", "2026-02-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReadyOrders").exists())
                .andExpect(jsonPath("$.totalRevenue").exists())
                .andExpect(jsonPath("$.productBreakdown").isArray())
                .andExpect(jsonPath("$.totalReadyOrders", greaterThanOrEqualTo(1)))
                .andExpect(header().string("Content-Type", containsString("application/json")));
    }

    // ── INT-RPT-02: Formato de fecha inválido ───────────────────────────

    /**
     * Expected to FAIL initially.
     * Current ReportController returns ResponseEntity.badRequest().build() — empty body.
     * TEST_PLAN requires ErrorResponse body with descriptive message.
     */
    @Test
    @DisplayName("INT-RPT-02: GET /reports con fecha inválida retorna 400 con ErrorResponse (no body vacío)")
    void getReport_invalidDateFormat_returns400WithErrorResponse() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/reports")
                        .param("startDate", "invalid")
                        .param("endDate", "2026-02-25"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(header().string("Content-Type", containsString("application/json")));
    }

    // ── INT-RPT-03: Rango invertido (start > end) ───────────────────────

    /**
     * InvalidDateRangeException is treated as a Bad Request (400)
     * since the client provided an invalid date range parameter.
     */
    @Test
    @DisplayName("INT-RPT-03: GET /reports con rango invertido retorna 400 con ErrorResponse")
    void getReport_invertedDateRange_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/reports")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-02-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ── INT-RPT-04: Parámetros faltantes ────────────────────────────────

    /**
     * Expected to FAIL initially.
     * Spring returns 400 for missing required params, but body may be HTML
     * or Spring's default error structure rather than ErrorResponse.
     */
    @Test
    @DisplayName("INT-RPT-04: GET /reports sin query params retorna 400 con ErrorResponse")
    void getReport_missingParams_returns400WithErrorResponse() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/reports"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(header().string("Content-Type", containsString("application/json")));
    }

    // ── INT-RPT-05: Sin órdenes en rango ────────────────────────────────

    @Test
    @DisplayName("INT-RPT-05: GET /reports sin órdenes READY retorna 200 con métricas en cero")
    void getReport_noOrdersInRange_returnsZeroMetrics() throws Exception {
        // Arrange — no orders in DB

        // Act & Assert
        mockMvc.perform(get("/reports")
                        .param("startDate", "2026-02-01")
                        .param("endDate", "2026-02-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReadyOrders", is(0)))
                .andExpect(jsonPath("$.totalRevenue", is(0)))
                .andExpect(jsonPath("$.productBreakdown", hasSize(0)));
    }

    // ── INT-RPT-06: Solo órdenes READY incluidas ────────────────────────

    @Test
    @DisplayName("INT-RPT-06: GET /reports solo incluye órdenes READY en métricas")
    void getReport_onlyReadyOrdersContribute() throws Exception {
        // Arrange
        LocalDateTime date = LocalDateTime.of(2026, 2, 15, 12, 0);
        createOrder(OrderStatus.READY, date, item(1L, "Hamburguesa", 1, BigDecimal.valueOf(15.50)));
        createOrder(OrderStatus.PENDING, date, item(2L, "Pizza", 5, BigDecimal.valueOf(20.00)));
        createOrder(OrderStatus.IN_PREPARATION, date, item(3L, "Ensalada", 3, BigDecimal.valueOf(12.00)));

        // Act & Assert
        mockMvc.perform(get("/reports")
                        .param("startDate", "2026-02-01")
                        .param("endDate", "2026-02-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReadyOrders", is(1)))
                .andExpect(jsonPath("$.totalRevenue", is(15.50)))
                .andExpect(jsonPath("$.productBreakdown", hasSize(1)))
                .andExpect(jsonPath("$.productBreakdown[0].productId", is(1)));
    }
}
