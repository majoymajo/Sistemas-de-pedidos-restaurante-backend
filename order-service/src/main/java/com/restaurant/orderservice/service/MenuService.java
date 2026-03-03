package com.restaurant.orderservice.service;

import com.restaurant.orderservice.dto.ProductResponse;
import com.restaurant.orderservice.entity.Product;
import com.restaurant.orderservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing menu operations.
 * 
 * Provides business logic for retrieving active products from the menu.
 * Handles the mapping between Product entities and ProductResponse DTOs.
 * 
 * Validates Requirements: 1.1, 1.2, 1.3
 */
@Service
public class MenuService {
    
    private final ProductRepository productRepository;
    
    /**
     * Constructor for MenuService.
     * 
     * @param productRepository Repository for accessing product data
     */
    @Autowired
    public MenuService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    
    /**
     * Retrieves all active products from the menu.
     * 
     * This method fetches all products where isActive = true from the database
     * and maps them to ProductResponse DTOs for API consumption.
     * 
     * @return List of ProductResponse containing active products. 
     *         Returns empty list if no active products exist.
     * 
     * Validates Requirements:
     * - 1.1: Order Service exposes GET /menu endpoint that returns active products
     * - 1.2: When /menu is called, returns active products with menu metadata
     * - 1.3: Order Service includes only products where isActive is true
     */
    public List<ProductResponse> getActiveProducts() {
        List<Product> activeProducts = productRepository.findByIsActiveTrueOrderByIdAsc();
        
        return activeProducts.stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Maps a Product entity to a ProductResponse DTO.
     * 
     * @param product The Product entity to map
     * @return ProductResponse DTO with product information
     */
    private ProductResponse mapToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .isActive(product.getIsActive())
                .build();
    }
}
