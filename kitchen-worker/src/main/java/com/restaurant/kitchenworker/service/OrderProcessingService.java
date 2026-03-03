package com.restaurant.kitchenworker.service;

import com.restaurant.kitchenworker.application.command.OrderPlacedCommand;
import com.restaurant.kitchenworker.entity.Order;
import com.restaurant.kitchenworker.enums.OrderStatus;
import com.restaurant.kitchenworker.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service responsible for processing order events received from RabbitMQ.
 * 
 * This service handles the business logic for updating order status when
 * an order placed event is received. It updates the order status to IN_PREPARATION
 * and creates a local order projection when one does not exist yet.
 * 
 * Validates Requirements: 7.2, 7.3, 7.4, 7.5, 7.6
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderProcessingService {

    private final OrderRepository orderRepository;
    
    /**
     * Processes an order placed event by updating the order status to IN_PREPARATION.
     * 
     * This method:
     * 1. Retrieves the order from the database using the orderId from the event
     * 2. If the order doesn't exist locally, creates a new projection with event data
     * 3. Updates its status to IN_PREPARATION
     * 4. Saves the order (updatedAt is automatically updated by @PreUpdate)
     * 5. Logs successful processing information
     * 
     * Error handling:
     * - If the order is not found, creates a local record and continues processing
     * - If any exception occurs, logs the error and re-throws it to trigger
     *   the retry mechanism (Requirement 7.7)
     * 
     * @param command The validated command containing order details
     * @throws Exception if an error occurs during processing (to trigger retry)
     * 
     * Validates Requirements:
     * - 7.2: Deserialize event JSON payload
     * - 7.3: Log order details including orderId and tableId
     * - 7.4: Update order status to IN_PREPARATION
     * - 7.5: Update updatedAt timestamp
     * - 7.6: Handle non-existent orders gracefully without throwing exception
     */
    @Transactional
    public void processOrder(OrderPlacedCommand command) {
        try {
            log.info("Processing order event: orderId={}, tableId={}",
                    command.getOrderId(), command.getTableId());
            
            // Retrieve the order from the database
            Optional<Order> orderOpt = orderRepository.findById(command.getOrderId());
            
            Order order;
            if (orderOpt.isEmpty()) {
                // Order doesn't exist in kitchen-worker database, create it
                log.info("Order not found in kitchen-worker database, creating new record: orderId={}",
                        command.getOrderId());
                order = new Order();
                order.setId(command.getOrderId());
                order.setTableId(command.getTableId());
                order.setStatus(OrderStatus.PENDING);
                LocalDateTime referenceTime = command.getCreatedAt() != null
                        ? command.getCreatedAt()
                        : LocalDateTime.now();
                order.setCreatedAt(referenceTime);
                order.setUpdatedAt(referenceTime);
            } else {
                order = orderOpt.get();
            }
            
            // Update order status to IN_PREPARATION
            order.setStatus(OrderStatus.IN_PREPARATION);
            
            // Save the order (updatedAt is automatically updated by @PreUpdate)
            orderRepository.save(order);
            
            // Log successful processing
            log.info("Order processed successfully: orderId={}, tableId={}, newStatus={}",
                    command.getOrderId(), command.getTableId(), OrderStatus.IN_PREPARATION);
                
        } catch (Exception ex) {
            // Log the error with full context
            log.error("Error processing order event: orderId={}, tableId={}, error={}",
                    command.getOrderId(), command.getTableId(), ex.getMessage(), ex);
            
            // Re-throw the exception to trigger the retry mechanism
            // This allows RabbitMQ to retry the message processing
            throw ex;
        }
    }
}
