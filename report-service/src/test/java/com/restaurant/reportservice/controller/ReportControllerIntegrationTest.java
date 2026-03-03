package com.restaurant.reportservice.controller;

import com.restaurant.reportservice.entity.OrderReportEntity;
import com.restaurant.reportservice.entity.OrderItemReportEntity;
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
 * Integration tests for ReportController.
 * Uses H2 database and full Spring context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderReportRepository orderReportRepository;

    @BeforeEach
    void setUp() {
        orderReportRepository.deleteAll();
    }

    @Test
    @DisplayName("Should return 200 with valid report for valid date range")
    void shouldReturnValidReportForValidDateRange() throws Exception {
        // Arrange
        LocalDateTime orderDate = LocalDateTime.of(2026, 2, 15, 12, 0);
        OrderReportEntity order = createOrderEntity(OrderStatus.READY, orderDate,
                createItem(1L, "Hamburguesa", 2, new BigDecimal("15.50"))
        );
        orderReportRepository.save(order);

        // Act & Assert
        mockMvc.perform(get("/reports")
                        .param("startDate", "2026-02-01")
                        .param("endDate", "2026-02-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReadyOrders", is(1)))
                .andExpect(jsonPath("$.totalRevenue", is(31.00)))
                .andExpect(jsonPath("$.productBreakdown", hasSize(1)))
                .andExpect(jsonPath("$.productBreakdown[0].productId", is(1)))
                .andExpect(jsonPath("$.productBreakdown[0].quantitySold", is(2)));
    }

    @Test
    @DisplayName("Should return 400 for invalid date format")
    void shouldReturn400ForInvalidDateFormat() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/reports")
                        .param("startDate", "invalid-date")
                        .param("endDate", "2026-02-28"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when startDate > endDate")
    void shouldReturn400WhenStartDateAfterEndDate() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/reports")
                        .param("startDate", "2026-02-28")
                        .param("endDate", "2026-02-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when required parameters are missing")
    void shouldReturn400WhenParametersMissing() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/reports"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return zero metrics when no data exists in date range")
    void shouldReturnZeroMetricsWhenNoData() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/reports")
                        .param("startDate", "2026-02-01")
                        .param("endDate", "2026-02-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReadyOrders", is(0)))
                .andExpect(jsonPath("$.totalRevenue", is(0)))
                .andExpect(jsonPath("$.productBreakdown", hasSize(0)));
    }

    @Test
    @DisplayName("Should only include READY orders in report")
    void shouldOnlyIncludeReadyOrders() throws Exception {
        // Arrange
        LocalDateTime orderDate = LocalDateTime.of(2026, 2, 15, 12, 0);
        
        OrderReportEntity readyOrder = createOrderEntity(OrderStatus.READY, orderDate,
                createItem(1L, "Hamburguesa", 1, new BigDecimal("15.50"))
        );
        OrderReportEntity pendingOrder = createOrderEntity(OrderStatus.PENDING, orderDate,
                createItem(2L, "Pizza", 5, new BigDecimal("20.00"))
        );

        orderReportRepository.save(readyOrder);
        orderReportRepository.save(pendingOrder);

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

    @Test
    @DisplayName("Should filter orders by date range correctly")
    void shouldFilterOrdersByDateRangeCorrectly() throws Exception {
        // Arrange
        OrderReportEntity inRange = createOrderEntity(OrderStatus.READY, 
                LocalDateTime.of(2026, 2, 15, 12, 0),
                createItem(1L, "Hamburguesa", 1, new BigDecimal("15.50"))
        );
        OrderReportEntity outOfRange = createOrderEntity(OrderStatus.READY,
                LocalDateTime.of(2026, 3, 15, 12, 0),
                createItem(2L, "Pizza", 1, new BigDecimal("20.00"))
        );

        orderReportRepository.save(inRange);
        orderReportRepository.save(outOfRange);

        // Act & Assert
        mockMvc.perform(get("/reports")
                        .param("startDate", "2026-02-01")
                        .param("endDate", "2026-02-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReadyOrders", is(1)))
                .andExpect(jsonPath("$.totalRevenue", is(15.50)));
    }

    @Test
    @DisplayName("Should aggregate multiple items from multiple orders")
    void shouldAggregateMultipleItemsFromMultipleOrders() throws Exception {
        // Arrange
        LocalDateTime orderDate = LocalDateTime.of(2026, 2, 15, 12, 0);
        
        OrderReportEntity order1 = createOrderEntity(OrderStatus.READY, orderDate,
                createItem(1L, "Hamburguesa", 2, new BigDecimal("15.50"))
        );
        OrderReportEntity order2 = createOrderEntity(OrderStatus.READY, orderDate,
                createItem(1L, "Hamburguesa", 1, new BigDecimal("15.50")),
                createItem(2L, "Pizza", 1, new BigDecimal("20.00"))
        );

        orderReportRepository.save(order1);
        orderReportRepository.save(order2);

        // Act & Assert
        mockMvc.perform(get("/reports")
                        .param("startDate", "2026-02-01")
                        .param("endDate", "2026-02-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReadyOrders", is(2)))
                .andExpect(jsonPath("$.totalRevenue", is(66.50)))
                .andExpect(jsonPath("$.productBreakdown", hasSize(2)));
    }

    @Test
    @DisplayName("Should include correct JSON structure in response")
    void shouldIncludeCorrectJSONStructure() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/reports")
                        .param("startDate", "2026-02-01")
                        .param("endDate", "2026-02-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReadyOrders").exists())
                .andExpect(jsonPath("$.totalRevenue").exists())
                .andExpect(jsonPath("$.productBreakdown").isArray());
    }

    @Test
    @DisplayName("Should handle same start and end date")
    void shouldHandleSameStartAndEndDate() throws Exception {
        // Arrange
        LocalDateTime orderDate = LocalDateTime.of(2026, 2, 15, 12, 0);
        OrderReportEntity order = createOrderEntity(OrderStatus.READY, orderDate,
                createItem(1L, "Hamburguesa", 1, new BigDecimal("15.50"))
        );
        orderReportRepository.save(order);

        // Act & Assert
        mockMvc.perform(get("/reports")
                        .param("startDate", "2026-02-15")
                        .param("endDate", "2026-02-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReadyOrders", is(1)));
    }

    // Helper methods
    private OrderReportEntity createOrderEntity(OrderStatus status, LocalDateTime createdAt,
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

        return order;
    }

    private OrderItemReportEntity createItem(Long productId, String productName,
                                             Integer quantity, BigDecimal price) {
        return OrderItemReportEntity.builder()
                .productId(productId)
                .productName(productName)
                .quantity(quantity)
                .price(price)
                .build();
    }
}
