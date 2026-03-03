package com.restaurant.kitchenworker.entity;

import com.restaurant.kitchenworker.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an order in the kitchen worker service.
 * 
 * This is a simplified version of the Order entity that contains only
 * the fields necessary for the kitchen worker to update order status.
 * The kitchen worker does not need to access order items or other details.
 * 
 * Validates Requirements: 7.4, 9.4
 */
@Entity
@Table(name = "kitchen_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    /**
     * Unique identifier for the order.
     * Not generated - uses the UUID from the order-service.
     */
    @Id
    private UUID id;
    
    /**
     * The table number where the order was placed.
     * Must be a positive integer.
     * Cannot be null.
     */
    @Column(name = "table_id", nullable = false)
    private Integer tableId;
    
    /**
     * Current status of the order.
     * Stored as a string in the database.
     * Defaults to PENDING when a new order is created.
     * Cannot be null.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;
    
    /**
     * Timestamp when the order was created.
     * Set automatically on first persist.
     * Cannot be null and cannot be updated after creation.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when the order was last updated.
     * Set automatically on persist and updated on every modification.
     * Cannot be null.
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * JPA lifecycle callback executed before the entity is persisted for the first time.
     * Sets both createdAt and updatedAt to the current timestamp.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * JPA lifecycle callback executed before the entity is updated.
     * Updates the updatedAt timestamp to the current time.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
