package com.restaurant.orderservice.repository;

import com.restaurant.orderservice.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Product entity operations.
 * 
 * Provides database access methods for Product entities, including
 * standard CRUD operations and custom query methods.
 * 
 * Validates Requirements: 1.1, 1.3
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    /**
     * Finds all products that are currently active (isActive = true).
     * 
     * This method is used to retrieve only active products for the menu endpoint,
     * ensuring that inactive products are not displayed to users.
     * 
     * @return List of active products. Returns empty list if no active products exist.
     * 
     * Validates Requirements: 1.1, 1.3
     */
    List<Product> findByIsActiveTrueOrderByIdAsc();
}
