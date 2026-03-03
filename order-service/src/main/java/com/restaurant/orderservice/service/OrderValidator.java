package com.restaurant.orderservice.service;

import com.restaurant.orderservice.dto.CreateOrderRequest;
import com.restaurant.orderservice.dto.OrderItemRequest;
import com.restaurant.orderservice.entity.Product;
import com.restaurant.orderservice.exception.InactiveProductException;
import com.restaurant.orderservice.exception.InvalidOrderException;
import com.restaurant.orderservice.exception.ProductNotFoundException;
import com.restaurant.orderservice.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validator for order business rules.
 * 
 * Single Responsibility: Validates order creation requests according to business rules.
 * Separated from OrderService to follow SRP and improve testability.
 */
@Component
@Slf4j
public class OrderValidator {
    
    private final ProductRepository productRepository;
    
    public OrderValidator(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    
    /**
     * Validates a create order request.
     * 
     * @param request The order request to validate
     * @throws InvalidOrderException if validation fails
     * @throws ProductNotFoundException if any product doesn't exist or is inactive
     */
    public void validateCreateOrderRequest(CreateOrderRequest request) {
        log.debug("Validating order request for table {}", request.getTableId());
        
        validateTableId(request.getTableId());
        validateItemsList(request.getItems());
        validateProducts(request.getItems());
    }
    
    private void validateTableId(Integer tableId) {
        if (tableId == null || tableId <= 0 || tableId > 12) {
            throw new InvalidOrderException("Table ID must be a positive integer between 1 and 12");
        }
    }
    
    private void validateItemsList(java.util.List<OrderItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new InvalidOrderException("Order must contain at least one item");
        }
    }
    
    
    private void validateProducts(java.util.List<OrderItemRequest> items) {
        for (OrderItemRequest itemRequest : items) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException(itemRequest.getProductId()));
            
            if (!product.getIsActive()) {
                throw new InactiveProductException(itemRequest.getProductId());
            }
        }
    }
}
