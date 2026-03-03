package com.restaurant.orderservice.service;

import com.restaurant.orderservice.dto.OrderItemResponse;
import com.restaurant.orderservice.dto.OrderResponse;
import com.restaurant.orderservice.entity.Order;
import com.restaurant.orderservice.entity.OrderItem;
import com.restaurant.orderservice.entity.Product;
import com.restaurant.orderservice.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper for converting between Order entities and DTOs.
 * 
 * Single Responsibility: Handles all mapping logic between domain entities and DTOs.
 * Optimized to avoid N+1 query problem by batch loading products.
 */
@Component
@Slf4j
public class OrderMapper {
    
    private final ProductRepository productRepository;
    
    public OrderMapper(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    
    /**
     * Maps an Order entity to an OrderResponse DTO.
     * Optimized to avoid N+1 queries by batch loading all products.
     * 
     * @param order The Order entity to map
     * @return OrderResponse DTO with complete order information
     */
    public OrderResponse mapToOrderResponse(Order order) {
        // Batch load all products to avoid N+1 query problem
        List<Long> productIds = order.getItems().stream()
                .map(OrderItem::getProductId)
                .distinct()
                .collect(Collectors.toList());
        
        Map<Long, Product> productsMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
        
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> mapToOrderItemResponse(item, productsMap))
                .collect(Collectors.toList());
        
        return OrderResponse.builder()
                .id(order.getId())
                .tableId(order.getTableId())
                .status(order.getStatus())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
    
    
    /**
     * Maps an OrderItem entity to an OrderItemResponse DTO.
     * Uses pre-loaded products map to avoid N+1 queries.
     * 
     * @param orderItem The OrderItem entity to map
     * @param productsMap Map of product IDs to Product entities
     * @return OrderItemResponse DTO with order item information including product name
     */
    private OrderItemResponse mapToOrderItemResponse(OrderItem orderItem, Map<Long, Product> productsMap) {
        Product product = productsMap.get(orderItem.getProductId());
        String productName = product != null ? product.getName() : "Producto desconocido";
        
        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .productId(orderItem.getProductId())
                .productName(productName)
                .quantity(orderItem.getQuantity())
                .note(orderItem.getNote())
                .build();
    }
    
    /**
     * Maps a list of Order entities to OrderResponse DTOs.
     * Optimized for batch operations.
     * 
     * @param orders List of Order entities
     * @return List of OrderResponse DTOs
     */
    public List<OrderResponse> mapToOrderResponseList(List<Order> orders) {
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }
}
