package com.restaurant.orderservice.service;

import com.restaurant.orderservice.dto.*;
import com.restaurant.orderservice.dto.DeleteOrderResponse;
import com.restaurant.orderservice.dto.DeleteAllOrdersResponse;
import com.restaurant.orderservice.application.port.out.OrderPlacedEventPublisherPort;
import com.restaurant.orderservice.domain.event.OrderPlacedDomainEvent;
import com.restaurant.orderservice.entity.Order;
import com.restaurant.orderservice.entity.OrderItem;
import com.restaurant.orderservice.enums.OrderStatus;
import com.restaurant.orderservice.exception.OrderNotFoundException;
import com.restaurant.orderservice.repository.OrderRepository;
import com.restaurant.orderservice.service.command.OrderCommandExecutor;
import com.restaurant.orderservice.service.command.PublishOrderPlacedEventCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing order operations.
 * 
 * Refactored to follow Single Responsibility Principle (SRP).
 * This service now focuses solely on orchestration, delegating to specialized components:
 * - OrderValidator: Business rule validation
 * - OrderMapper: Entity-DTO mapping (with N+1 optimization)
 * - OrderEventBuilder: Event construction
 * - OrderPlacedEventPublisherPort: Event publishing abstraction
 * 
 * Validates Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 3.1, 4.1, 4.2, 5.1, 5.2, 6.2
 */
@Service
@Slf4j
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final OrderValidator orderValidator;
    private final OrderMapper orderMapper;
    private final OrderPlacedEventPublisherPort orderPlacedEventPublisherPort;
    private final OrderCommandExecutor orderCommandExecutor;
    
    /**
     * Constructor for OrderService.
     * 
     * @param orderRepository Repository for accessing order data
     * @param productRepository Repository for accessing product data
     * @param orderPlacedEventPublisherPort Output port for publishing order events
     */
    @Autowired
    public OrderService(OrderRepository orderRepository, 
                       OrderValidator orderValidator,
                       OrderMapper orderMapper,
                       OrderPlacedEventPublisherPort orderPlacedEventPublisherPort,
                       OrderCommandExecutor orderCommandExecutor) {
        this.orderRepository = orderRepository;
        this.orderValidator = orderValidator;
        this.orderMapper = orderMapper;
        this.orderPlacedEventPublisherPort = orderPlacedEventPublisherPort;
        this.orderCommandExecutor = orderCommandExecutor;
    }
    
    /**
     * Creates a new order with the specified items for a table.
     * 
     * This method performs the following operations:
     * 1. Validates that all referenced products exist and are active
     * 2. Validates that tableId is valid and items list is not empty
     * 3. Creates an Order entity with status PENDING
     * 4. Creates associated OrderItem entities
     * 5. Persists the order to the database
     * 6. Publishes a versioned order.placed domain event to RabbitMQ
     * 7. Returns the created order as an OrderResponse
     * 
     * @param request CreateOrderRequest containing tableId and list of items
     * @return OrderResponse with the created order details
     * @throws ProductNotFoundException if any product does not exist or is inactive
     * @throws InvalidOrderException if tableId is invalid or items list is empty
     * 
     * Validates Requirements:
     * - 2.1: Order Service exposes POST /orders endpoint accepting tableId and items
     * - 2.2: Validates that all productIds exist and are active
     * - 2.3: Persists order with status PENDING in PostgreSQL
     * - 2.4: Generates unique UUID as order identifier
     * - 2.5: Automatically sets createdAt and updatedAt timestamps
     * - 2.6: Rejects order if productId doesn't exist or is inactive
     * - 2.7: Rejects order if tableId is missing or invalid
     * - 2.8: Rejects order if items list is empty
     * - 3.1: Publishes "order.placed" event to RabbitMQ after successful creation
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for table {}", request.getTableId());
        
        // Delegate validation to OrderValidator
        orderValidator.validateCreateOrderRequest(request);
        
        // Create Order entity
        Order order = new Order();
        order.setTableId(request.getTableId());
        order.setStatus(OrderStatus.PENDING);
        
        // Create OrderItem entities and associate with order
        List<OrderItem> orderItems = request.getItems().stream()
                .map(itemRequest -> {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(order);
                    orderItem.setProductId(itemRequest.getProductId());
                    orderItem.setQuantity(itemRequest.getQuantity());
                    orderItem.setNote(itemRequest.getNote());
                    return orderItem;
                })
                .collect(Collectors.toList());
        
        order.setItems(orderItems);
        
        // Save order to database (timestamps are set automatically by @PrePersist)
        Order savedOrder = orderRepository.save(order);
        
        log.info("Order created successfully: orderId={}, tableId={}, itemCount={}", 
                savedOrder.getId(), savedOrder.getTableId(), savedOrder.getItems().size());
        
        // Build and publish domain event through output port
        OrderPlacedDomainEvent event = buildOrderPlacedDomainEvent(savedOrder);
        orderCommandExecutor.execute(new PublishOrderPlacedEventCommand(orderPlacedEventPublisherPort, event));
        
        // Delegate mapping to OrderMapper
        return orderMapper.mapToOrderResponse(savedOrder);
    }
    
    /**
     * Retrieves an order by its unique identifier.
     * Only returns active (non-deleted) orders.
     * 
     * @param orderId UUID of the order to retrieve
     * @return OrderResponse with complete order details
     * @throws OrderNotFoundException if the order does not exist or is deleted
     * 
     * Validates Requirements:
     * - 4.1: Order Service exposes GET /orders/{id} endpoint
     * - 4.2: Returns complete order with all items, status, and timestamps
     * - SoftDelete: Excludes deleted orders (Copilot Instructions Section 4)
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId) {
        log.info("Retrieving order by id: {}", orderId);
        
        Order order = orderRepository.findByIdActive(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // Delegate mapping to OrderMapper
        return orderMapper.mapToOrderResponse(order);
    }
    
    /**
     * Retrieves orders, optionally filtered by status.
     * Only returns active (non-deleted) orders.
     * 
     * If status is null, returns all active orders.
     * If status is provided, returns only active orders with that status.
     * 
     * @param status Optional OrderStatus to filter by (can be null)
     * @return List of OrderResponse matching the filter criteria
     * 
     * Validates Requirements:
     * - 5.1: Order Service exposes GET /orders with optional status parameter
     * - 5.2: Returns only orders matching the specified status when provided
     * - Soft Delete: Excludes deleted orders (Copilot Instructions Section 4)
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(List<OrderStatus> status) {
        log.info("Retrieving orders with status filter: {}", status);
        
        List<Order> orders;
        if (status == null || status.isEmpty()) {
            // Return all active orders (exclude deleted)
            orders = orderRepository.findAllActive();
        } else {
            // Return active orders filtered by any of the provided statuses
            orders = orderRepository.findByStatusInActive(status);
        }
        
        // Delegate mapping to OrderMapper (optimized for batch)
        return orderMapper.mapToOrderResponseList(orders);
    }
    
    /**
     * Updates the status of an existing order.
     * Only updates active (non-deleted) orders.
     * 
     * The updatedAt timestamp is automatically updated by the @PreUpdate callback.
     * 
     * @param orderId UUID of the order to update
     * @param newStatus New status to set for the order
     * @return OrderResponse with updated order details
     * @throws OrderNotFoundException if the order does not exist or is deleted
     * @throws InvalidStatusTransitionException if the transition is not valid
     * 
     * Validates Requirements:
     * - 6.2: Updates order status and updatedAt timestamp
     * - Security: Validates status transition before applying (Copilot Instructions Section 4)
     * - Soft Delete: Only updates active orders (Copilot Instructions Section 4)
     */
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        log.info("Updating order status: orderId={}, newStatus={}", orderId, newStatus);
        
        Order order = orderRepository.findByIdActive(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // ⚠️ SECURITY: Validate status transition (Backend Enforcement)
        // Copilot Instructions Section 4: "Backend debe rechazar cambios de estado que no respeten el flujo definido"
        // Validation is now handled by Order.updateStatus()
        order.updateStatus(newStatus);
        
        // updatedAt is automatically updated by @PreUpdate
        Order updatedOrder = orderRepository.save(order);
        
        log.info("Order status updated successfully: orderId={}, status={}", 
                updatedOrder.getId(), updatedOrder.getStatus());
        
        return orderMapper.mapToOrderResponse(updatedOrder);
    }

    /**
     * Soft-deletes a single order by id.
     * 
     * The order is not physically removed from the database.
     * Instead, it is marked as deleted for audit purposes.
     * 
     * Cumple con Copilot Instructions:
     * - Sección 4: Security - Destructive Operations
     * - "Implementar soft delete (campo is_deleted, deleted_at, etc.)"
     * - "No permitir eliminación irreversible sin controles adicionales"
     *
     * @param orderId UUID of the order to delete
     * @throws OrderNotFoundException if the order does not exist or is already deleted
     */
    @Transactional
    public DeleteOrderResponse deleteOrder(UUID orderId) {
        log.info("Soft-deleting order: orderId={}", orderId);

        Order order = orderRepository.findByIdActive(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.markAsDeleted();
        orderRepository.save(order);
        
        log.info("Order soft-deleted successfully: orderId={}, deletedAt={}", 
                orderId, order.getDeletedAt());

        return DeleteOrderResponse.builder()
                .deletedId(orderId.toString())
                .deletedAt(order.getDeletedAt().toString())
                .deletedBy("KITCHEN")
                .build();
    }

    /**
     * Soft-deletes all active orders.
     * 
     * Orders are not physically removed from the database.
     * Instead, they are marked as deleted for audit purposes.
     * 
     * Cumple con Copilot Instructions:
     * - Sección 4: Security - Destructive Operations
     * - "Implementar soft delete (campo is_deleted, deleted_at, etc.)"
     * - "Añadir auditoría (who, when, what)"
     *
     * @return number of soft-deleted orders
     */
    @Transactional
    public DeleteAllOrdersResponse deleteAllOrders() {
        List<Order> activeOrders = orderRepository.findAllActive();
        int count = activeOrders.size();
        
        log.info("Soft-deleting all active orders: count={}", count);
        
        LocalDateTime deletedAt = LocalDateTime.now();
        
        // ⚠️ SECURITY: Soft delete instead of hard delete (Backend Enforcement)
        activeOrders.forEach(order -> {
            order.markAsDeleted();
            orderRepository.save(order);
        });
        
        log.info("All active orders soft-deleted successfully: count={}", count);
        return DeleteAllOrdersResponse.builder()
                .deletedCount(count)
                .deletedAt(deletedAt.toString())
                .deletedBy("KITCHEN")
                .build();
    }
    
    /**
     * Builds a domain event from an Order entity.
     * 
     * @param order The Order entity to convert to an event
     * @return domain event ready to be published through the output port
     */
    private OrderPlacedDomainEvent buildOrderPlacedDomainEvent(Order order) {
        List<OrderPlacedDomainEvent.OrderItemData> eventItems = order.getItems().stream()
                .map(item -> new OrderPlacedDomainEvent.OrderItemData(
                        item.getProductId(),
                        item.getQuantity()
                ))
                .collect(Collectors.toList());

        return OrderPlacedDomainEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(OrderPlacedDomainEvent.EVENT_TYPE)
                .eventVersion(OrderPlacedDomainEvent.CURRENT_VERSION)
                .occurredAt(LocalDateTime.now())
                .orderId(order.getId())
                .tableId(order.getTableId())
                .items(eventItems)
                .createdAt(order.getCreatedAt())
                .build();
    }
}
