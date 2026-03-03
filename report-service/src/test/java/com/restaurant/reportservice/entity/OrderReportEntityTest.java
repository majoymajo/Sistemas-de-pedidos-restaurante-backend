package com.restaurant.reportservice.entity;

import com.restaurant.reportservice.enums.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderReportEntityTest {

    // ── OrderReportEntity ───────────────────────────────────────────────

    @Test
    void shouldBeEqualWhenSameId() {
        UUID id = UUID.randomUUID();

        OrderReportEntity a = OrderReportEntity.builder()
                .id(id).tableId(1).status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now()).receivedAt(LocalDateTime.now())
                .build();

        OrderReportEntity b = OrderReportEntity.builder()
                .id(id).tableId(99).status(OrderStatus.READY)
                .createdAt(LocalDateTime.now().minusDays(1)).receivedAt(LocalDateTime.now().minusDays(1))
                .build();

        assertThat(a).isEqualTo(b);
    }

    @Test
    void shouldNotBeEqualWhenDifferentId() {
        OrderReportEntity a = OrderReportEntity.builder()
                .id(UUID.randomUUID()).tableId(1).status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now()).receivedAt(LocalDateTime.now())
                .build();

        OrderReportEntity b = OrderReportEntity.builder()
                .id(UUID.randomUUID()).tableId(1).status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now()).receivedAt(LocalDateTime.now())
                .build();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void shouldHaveSameHashCodeWhenSameId() {
        UUID id = UUID.randomUUID();

        OrderReportEntity a = OrderReportEntity.builder()
                .id(id).tableId(1).status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now()).receivedAt(LocalDateTime.now())
                .build();

        OrderReportEntity b = OrderReportEntity.builder()
                .id(id).tableId(99).status(OrderStatus.READY)
                .createdAt(LocalDateTime.now().minusDays(1)).receivedAt(LocalDateTime.now().minusDays(1))
                .build();

        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void shouldNotBeEqualToNull() {
        OrderReportEntity entity = OrderReportEntity.builder()
                .id(UUID.randomUUID()).tableId(1).status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now()).receivedAt(LocalDateTime.now())
                .build();

        assertThat(entity).isNotEqualTo(null);
    }

    @Test
    void shouldHandleNullId() {
        OrderReportEntity a = new OrderReportEntity();
        OrderReportEntity b = new OrderReportEntity();

        // Two entities with null id should NOT be equal (except identity)
        assertThat(a).isNotEqualTo(b);
        // hashCode should not throw
        assertThat(a.hashCode()).isEqualTo(0);
    }

    // ── OrderItemReportEntity ───────────────────────────────────────────

    @Test
    void shouldBeEqualWhenSameItemId() {
        OrderItemReportEntity a = OrderItemReportEntity.builder()
                .id(1L).productId(10L).productName("Pizza").quantity(2)
                .price(BigDecimal.valueOf(12.50))
                .build();

        OrderItemReportEntity b = OrderItemReportEntity.builder()
                .id(1L).productId(99L).productName("Burger").quantity(5)
                .price(BigDecimal.valueOf(99.99))
                .build();

        assertThat(a).isEqualTo(b);
    }

    @Test
    void shouldNotBeEqualWhenDifferentItemId() {
        OrderItemReportEntity a = OrderItemReportEntity.builder()
                .id(1L).productId(10L).productName("Pizza").quantity(2)
                .price(BigDecimal.valueOf(12.50))
                .build();

        OrderItemReportEntity b = OrderItemReportEntity.builder()
                .id(2L).productId(10L).productName("Pizza").quantity(2)
                .price(BigDecimal.valueOf(12.50))
                .build();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void shouldHaveSameHashCodeWhenSameItemId() {
        OrderItemReportEntity a = OrderItemReportEntity.builder()
                .id(42L).productId(10L).productName("Pizza").quantity(2)
                .price(BigDecimal.valueOf(12.50))
                .build();

        OrderItemReportEntity b = OrderItemReportEntity.builder()
                .id(42L).productId(99L).productName("Burger").quantity(5)
                .price(BigDecimal.valueOf(99.99))
                .build();

        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
