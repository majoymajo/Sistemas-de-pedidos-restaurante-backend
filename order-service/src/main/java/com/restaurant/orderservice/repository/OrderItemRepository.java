package com.restaurant.orderservice.repository;

import com.restaurant.orderservice.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for OrderItem entity operations.
 * 
 * Provides database access methods for OrderItem entities, including
 * standard CRUD operations inherited from JpaRepository.
 * 
 * OrderItems represent individual line items within an order, containing
 * product references, quantities, and optional notes.
 * 
 * Validates Requirements: 2.1
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    // Standard CRUD operations are inherited from JpaRepository
    // No custom query methods are required for the MVP
}
