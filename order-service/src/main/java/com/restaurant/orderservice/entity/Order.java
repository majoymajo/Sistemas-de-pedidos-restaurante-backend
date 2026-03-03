package com.restaurant.orderservice.entity;

import com.restaurant.orderservice.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing an order in the restaurant system.
 * 
 * An order contains one or more order items for a specific table.
 * Each order has a status that tracks its lifecycle from creation to completion.
 * Timestamps are automatically managed through JPA lifecycle callbacks.
 * 
 * Soft Delete:
 * Orders are never physically deleted. Instead, they are marked as deleted
 * with a timestamp for audit purposes (Copilot Instructions Section 4).
 * 
 * Validates Requirements: 2.3, 2.4, 2.5, 9.1
 */
@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    /**
     * Unique identifier for the order.
     * Generated automatically as a UUID.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
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
     * List of items included in this order.
     * Configured with cascade ALL to propagate all operations to order items.
     * orphanRemoval = true ensures that removed items are deleted from the database.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
    
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
     * Soft delete flag. When true, the order is considered deleted.
     * Orders are never physically removed from the database for audit purposes.
     * 
     * Cumple con Copilot Instructions:
     * - Sección 4: Security - Destructive Operations
     * - "Implementar soft delete (campo is_deleted, deleted_at, etc.)"
     */
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;
    
    /**
     * Timestamp when the order was soft-deleted.
     * Null if the order has not been deleted.
     * Used for audit purposes.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
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
    
    /**
     * Updates the order status with validation.
     * Validates that the transition from current status to new status is allowed.
     * 
     * Allowed transitions:
     * - PENDING -> IN_PREPARATION
     * - IN_PREPARATION -> READY
     * - READY -> (no transitions allowed)
     * 
     * @param newStatus the new status to transition to
     * @throws com.restaurant.orderservice.exception.InvalidStatusTransitionException if transition is invalid
     */
    public void updateStatus(OrderStatus newStatus) {
        OrderStatus.validateTransition(this.status, newStatus);
        this.status = newStatus;
    }
    
    /**
     * Marks this order as soft-deleted.
     * Sets the deleted flag to true and records the deletion timestamp.
     */
    public void markAsDeleted() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
