package com.restaurant.reportservice.service;

import com.restaurant.reportservice.application.command.OrderPlacedCommand;
import com.restaurant.reportservice.application.command.OrderReadyCommand;
import com.restaurant.reportservice.entity.OrderItemReportEntity;
import com.restaurant.reportservice.entity.OrderReportEntity;
import com.restaurant.reportservice.enums.OrderStatus;
import com.restaurant.reportservice.repository.OrderReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Application service that processes order events and persists them
 * into the report projection database. Implements idempotent upsert behavior.
 */
@Service
@Slf4j
public class OrderEventProcessingService {

    private final OrderReportRepository orderReportRepository;
    private final Clock clock;

    public OrderEventProcessingService(OrderReportRepository orderReportRepository, Clock clock) {
        this.orderReportRepository = orderReportRepository;
        this.clock = clock;
    }

    @Transactional
    public void processOrderPlaced(OrderPlacedCommand command) {
        Optional<OrderReportEntity> existing = orderReportRepository.findById(command.getOrderId());
        if (existing.isPresent()) {
            log.info("Order {} already exists, skipping (idempotent)", command.getOrderId());
            return;
        }

        OrderReportEntity order = OrderReportEntity.builder()
                .id(command.getOrderId())
                .tableId(command.getTableId())
                .status(OrderStatus.PENDING)
                .createdAt(command.getCreatedAt())
                .receivedAt(LocalDateTime.now(clock))
                .build();

        if (command.getItems() != null) {
            command.getItems().forEach(item -> {
                OrderItemReportEntity itemEntity = OrderItemReportEntity.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build();
                order.addItem(itemEntity);
            });
        }

        orderReportRepository.save(order);
        log.info("Projected order.placed event for order {}", command.getOrderId());
    }

    @Transactional
    public void processOrderReady(OrderReadyCommand command) {
        Optional<OrderReportEntity> existing = orderReportRepository.findById(command.getOrderId());
        if (existing.isPresent()) {
            OrderReportEntity order = existing.get();
            order.setStatus(OrderStatus.READY);
            orderReportRepository.save(order);
            log.info("Updated order {} to READY", command.getOrderId());
        } else {
            OrderReportEntity order = OrderReportEntity.builder()
                    .id(command.getOrderId())
                    .tableId(0)
                    .status(OrderStatus.READY)
                    .createdAt(command.getUpdatedAt())
                    .receivedAt(LocalDateTime.now(clock))
                    .build();
            orderReportRepository.save(order);
            log.info("Created order {} directly as READY (upsert)", command.getOrderId());
        }
    }
}
