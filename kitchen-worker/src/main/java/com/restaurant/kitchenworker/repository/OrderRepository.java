package com.restaurant.kitchenworker.repository;

import com.restaurant.kitchenworker.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository interface for Order entity in the kitchen worker service.
 * 
 * Provides CRUD operations for orders, allowing the kitchen worker
 * to retrieve and update order status during event processing.
 * 
 * Validates Requirements: 7.4, 9.4
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    // JpaRepository provides all necessary methods:
    // - findById(UUID id): to retrieve orders by ID
    // - save(Order order): to update order status
}
