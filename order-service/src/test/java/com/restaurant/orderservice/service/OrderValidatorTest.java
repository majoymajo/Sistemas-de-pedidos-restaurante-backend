package com.restaurant.orderservice.service;

import com.restaurant.orderservice.dto.CreateOrderRequest;
import com.restaurant.orderservice.dto.OrderItemRequest;
import com.restaurant.orderservice.entity.Product;
import com.restaurant.orderservice.exception.InactiveProductException;
import com.restaurant.orderservice.exception.InvalidOrderException;
import com.restaurant.orderservice.exception.ProductNotFoundException;
import com.restaurant.orderservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderValidator.
 * 
 * Tests business rule validation for order creation.
 */
@ExtendWith(MockitoExtension.class)
class OrderValidatorTest {
    
    @Mock
    private ProductRepository productRepository;
    
    @InjectMocks
    private OrderValidator orderValidator;
    
    private Product activeProduct;
    private Product inactiveProduct;
    
    @BeforeEach
    void setUp() {
        activeProduct = new Product();
        activeProduct.setId(1L);
        activeProduct.setName("Pizza");
        activeProduct.setIsActive(true);
        
        inactiveProduct = new Product();
        inactiveProduct.setId(2L);
        inactiveProduct.setName("Old Burger");
        inactiveProduct.setIsActive(false);
    }
    
    @Test
    void validateCreateOrderRequest_withValidData_doesNotThrowException() {
        // Arrange
        OrderItemRequest itemRequest = new OrderItemRequest(1L, 2, "No onions");
        CreateOrderRequest request = new CreateOrderRequest(5, List.of(itemRequest));
        
        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct));
        
        // Act & Assert
        assertThatCode(() -> orderValidator.validateCreateOrderRequest(request))
                .doesNotThrowAnyException();
        
        verify(productRepository).findById(1L);
    }
    
    @Test
    void validateCreateOrderRequest_withNullTableId_throwsInvalidOrderException() {
        // Arrange
        OrderItemRequest itemRequest = new OrderItemRequest(1L, 1, null);
        CreateOrderRequest request = new CreateOrderRequest(null, List.of(itemRequest));
        
        // Act & Assert
        assertThatThrownBy(() -> orderValidator.validateCreateOrderRequest(request))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Table ID must be a positive integer");
        
        verify(productRepository, never()).findById(any());
    }
    
    @Test
    void validateCreateOrderRequest_withZeroTableId_throwsInvalidOrderException() {
        // Arrange
        OrderItemRequest itemRequest = new OrderItemRequest(1L, 1, null);
        CreateOrderRequest request = new CreateOrderRequest(0, List.of(itemRequest));
        
        // Act & Assert
        assertThatThrownBy(() -> orderValidator.validateCreateOrderRequest(request))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Table ID must be a positive integer");
        
        verify(productRepository, never()).findById(any());
    }
    
    @Test
    void validateCreateOrderRequest_withNegativeTableId_throwsInvalidOrderException() {
        // Arrange
        OrderItemRequest itemRequest = new OrderItemRequest(1L, 1, null);
        CreateOrderRequest request = new CreateOrderRequest(-1, List.of(itemRequest));
        
        // Act & Assert
        assertThatThrownBy(() -> orderValidator.validateCreateOrderRequest(request))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Table ID must be a positive integer");
        
        verify(productRepository, never()).findById(any());
    }
    
    @Test
    void validateCreateOrderRequest_withNullItems_throwsInvalidOrderException() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(5, null);
        
        // Act & Assert
        assertThatThrownBy(() -> orderValidator.validateCreateOrderRequest(request))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Order must contain at least one item");
        
        verify(productRepository, never()).findById(any());
    }
    
    @Test
    void validateCreateOrderRequest_withEmptyItems_throwsInvalidOrderException() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(5, Collections.emptyList());
        
        // Act & Assert
        assertThatThrownBy(() -> orderValidator.validateCreateOrderRequest(request))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Order must contain at least one item");
        
        verify(productRepository, never()).findById(any());
    }
    
    @Test
    void validateCreateOrderRequest_withNonExistentProduct_throwsProductNotFoundException() {
        // Arrange
        OrderItemRequest itemRequest = new OrderItemRequest(999L, 1, null);
        CreateOrderRequest request = new CreateOrderRequest(5, List.of(itemRequest));
        
        when(productRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> orderValidator.validateCreateOrderRequest(request))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("Product not found with id: 999");
        
        verify(productRepository).findById(999L);
    }
    
    @Test
    void validateCreateOrderRequest_withInactiveProduct_throwsInactiveProductException() {
        // Arrange
        OrderItemRequest itemRequest = new OrderItemRequest(2L, 1, null);
        CreateOrderRequest request = new CreateOrderRequest(5, List.of(itemRequest));
        
        when(productRepository.findById(2L)).thenReturn(Optional.of(inactiveProduct));
        
        // Act & Assert
        assertThatThrownBy(() -> orderValidator.validateCreateOrderRequest(request))
                .isInstanceOf(InactiveProductException.class)
                .hasMessageContaining("2");
        
        verify(productRepository).findById(2L);
    }
    
    @Test
    void validateCreateOrderRequest_withMultipleValidProducts_doesNotThrowException() {
        // Arrange
        Product product2 = new Product();
        product2.setId(3L);
        product2.setName("Burger");
        product2.setIsActive(true);
        
        OrderItemRequest item1 = new OrderItemRequest(1L, 2, "No onions");
        OrderItemRequest item2 = new OrderItemRequest(3L, 1, "Extra cheese");
        CreateOrderRequest request = new CreateOrderRequest(5, List.of(item1, item2));
        
        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct));
        when(productRepository.findById(3L)).thenReturn(Optional.of(product2));
        
        // Act & Assert
        assertThatCode(() -> orderValidator.validateCreateOrderRequest(request))
                .doesNotThrowAnyException();
        
        verify(productRepository).findById(1L);
        verify(productRepository).findById(3L);
    }
    
    @Test
    void validateCreateOrderRequest_withMixedActiveInactiveProducts_throwsInactiveProductException() {
        // Arrange
        OrderItemRequest item1 = new OrderItemRequest(1L, 2, null);
        OrderItemRequest item2 = new OrderItemRequest(2L, 1, null);
        CreateOrderRequest request = new CreateOrderRequest(5, List.of(item1, item2));
        
        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct));
        when(productRepository.findById(2L)).thenReturn(Optional.of(inactiveProduct));
        
        // Act & Assert
        assertThatThrownBy(() -> orderValidator.validateCreateOrderRequest(request))
                .isInstanceOf(InactiveProductException.class)
                .hasMessageContaining("2");
        
        verify(productRepository).findById(1L);
        verify(productRepository).findById(2L);
    }
}
