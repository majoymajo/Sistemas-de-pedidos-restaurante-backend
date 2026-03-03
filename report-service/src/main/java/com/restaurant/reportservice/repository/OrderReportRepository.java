package com.restaurant.reportservice.repository;

import com.restaurant.reportservice.entity.OrderReportEntity;
import com.restaurant.reportservice.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderReportRepository extends JpaRepository<OrderReportEntity, UUID> {
    List<OrderReportEntity> findByStatus(OrderStatus status);
    List<OrderReportEntity> findByStatusAndCreatedAtBetween(OrderStatus status, LocalDateTime startDate, LocalDateTime endDate);
}
