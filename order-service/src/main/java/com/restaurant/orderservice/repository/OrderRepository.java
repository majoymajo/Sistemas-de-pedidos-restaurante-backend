package com.restaurant.orderservice.repository;

import com.restaurant.orderservice.entity.Order;
import com.restaurant.orderservice.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Order entity operations.
 * 
 * Provides database access methods for Order entities, including
 * standard CRUD operations and custom query methods for filtering orders.
 * 
 * Soft Delete Support:
 * Methods with "Active" suffix exclude soft-deleted orders.
 * All read operations should use these methods to respect soft delete.
 * 
 * Validates Requirements: 4.1, 5.1, 6.1
 * Cumple con Copilot Instructions: Sección 4 - Security
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    
    /**
     * Finds all orders with the specified status.
     * 
     * This method is used to filter orders by their current status,
     * allowing the restaurant staff to view orders at specific stages
     * of preparation (PENDING, IN_PREPARATION, or READY).
     * 
     * @param status The order status to filter by
     * @return List of orders matching the specified status. Returns empty list if no orders match.
     * 
     * Validates Requirements: 5.1, 5.2, 5.4
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Finds all orders with any of the specified statuses.
     *
     * This method allows filtering by multiple statuses in a single query,
     * which is useful for kitchen views that need PENDING, IN_PREPARATION,
     * and READY orders together.
     *
     * @param statuses List of order statuses to include
     * @return List of orders matching any of the specified statuses.
     */
    List<Order> findByStatusIn(List<OrderStatus> statuses);
    
    // ============================================
    // Soft Delete Aware Methods
    // ============================================
    
    /**
     * Finds all active (non-deleted) orders.
     * Excludes orders marked as soft-deleted.
     * 
     * @return List of active orders
     */
    @Query("SELECT o FROM Order o WHERE o.deleted = false")
    List<Order> findAllActive();
    
    /**
     * Finds an active order by ID.
     * Returns empty if order doesn't exist or is soft-deleted.
     * 
     * @param id Order UUID
     * @return Optional containing the order if found and not deleted
     */
    @Query("SELECT o FROM Order o WHERE o.id = :id AND o.deleted = false")
    Optional<Order> findByIdActive(UUID id);
    
    /**
     * Finds all active orders with the specified status.
     * Excludes soft-deleted orders.
     * 
     * @param status Order status to filter by
     * @return List of active orders with the specified status
     */
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.deleted = false")
    List<Order> findByStatusActive(OrderStatus status);
    
    /**
     * Finds all active orders with any of the specified statuses.
     * Excludes soft-deleted orders.
     * 
     * @param statuses List of order statuses to include
     * @return List of active orders matching any of the specified statuses
     */
    @Query("SELECT o FROM Order o WHERE o.status IN :statuses AND o.deleted = false")
    List<Order> findByStatusInActive(List<OrderStatus> statuses);
    
    /**
     * Counts all active (non-deleted) orders.
     * 
     * @return Number of active orders
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.deleted = false")
    long countActive();
}
