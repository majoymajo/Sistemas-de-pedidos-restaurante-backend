package com.restaurant.orderservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a product (menu item) in the restaurant system.
 * 
 * Products are menu items that can be ordered by customers.
 * Each product has a name, description, and an active status flag.
 * 
 * Validates Requirements: 1.1, 1.3, 2.2, 9.1
 */
@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    
    /**
     * Unique identifier for the product.
     * Auto-generated using database identity strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Name of the product (e.g., "Pizza Margherita").
     * Cannot be null.
     */
    @Column(nullable = false)
    private String name;
    
    /**
     * Detailed description of the product.
     * Stored as TEXT to allow longer descriptions.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Product price in local currency.
     * Stored as decimal to avoid floating-point precision issues.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal price = java.math.BigDecimal.ZERO;

    /**
     * Product category used by frontend tabs (entradas, principales, postres, bebidas).
     */
    @Column(nullable = false, length = 50)
    private String category = "otros";

    /**
     * Optional image URL for richer menu rendering.
     */
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;
    
    /**
     * Flag indicating whether the product is currently active and available for ordering.
     * Only active products (isActive = true) should be displayed in the menu.
     * Defaults to true.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public Product(Long id, String name, String description, Boolean isActive) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.isActive = isActive;
    }
}
