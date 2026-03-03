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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Additional branch coverage tests for OrderValidator.
 * Tests edge cases and all conditional branches.
 */
@ExtendWith(MockitoExtension.class)
class OrderValidatorBranchCoverageTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderValidator orderValidator;

    private Product activeProduct;

    @BeforeEach
    void setUp() {
        activeProduct = new Product();
        activeProduct.setId(1L);
        activeProduct.setName("Pizza");
        activeProduct.setIsActive(true);
    }

    @Test
    void validateCreateOrderRequest_withTableId1_acceptsLowerBoundary() {
        // Arrange
        OrderItemRequest itemRequest = new OrderItemRequest(1L, 1, null);
        CreateOrderRequest request = new CreateOrderRequest(1, List.of(itemRequest));
        
        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct));

        // Act & Assert
        assertThatCode(() -> orderValidator.validateCreateOrderRequest(request))
                .doesNotThrowAnyException();
    }

    @Test
    void validateCreateOrderRequest_withTableId12_acceptsUpperBoundary() {
        // Arrange
        OrderItemRequest itemRequest = new OrderItemRequest(1L, 1, null);
        CreateOrderRequest request = new CreateOrderRequest(12, List.of(itemRequest));
        
        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct));

        // Act & Assert
        assertThatCode(() -> orderValidator.validateCreateOrderRequest(request))
                .doesNotThrowAnyException();
    }

    @Test
    void validateCreateOrderRequest_withTableId13_rejectsAboveUpperBoundary() {
        // Arrange
        OrderItemRequest itemRequest = new OrderItemRequest(1L, 1, null);
        CreateOrderRequest request = new CreateOrderRequest(13, List.of(itemRequest));

        // Act & Assert
        assertThatThrownBy(() -> orderValidator.validateCreateOrderRequest(request))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("between 1 and 12");

        verify(productRepository, never()).findById(any());
    }

    @Test
    void validateCreateOrderRequest_withNegativeTableId_rejectsNegative() {
        // Arrange
        OrderItemRequest itemRequest = new OrderItemRequest(1L, 1, null);
        CreateOrderRequest request = new CreateOrderRequest(-5, List.of(itemRequest));

        // Act & Assert
        assertThatThrownBy(() -> orderValidator.validateCreateOrderRequest(request))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("positive");

        verify(productRepository, never()).findById(any());
    }

    @Test
    void validateCreateOrderRequest_withNullItems_rejectsNull() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(5, null);

        // Act & Assert
        assertThatThrownBy(() -> orderValidator.validateCreateOrderRequest(request))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("at least one item");

        verify(productRepository, never()).findById(any());
    }

    @Test
    void validateCreateOrderRequest_withEmptyItems_rejectsEmpty() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(5, Collections.emptyList());

        // Act & Assert
        assertThatThrownBy(() -> orderValidator.validateCreateOrderRequest(request))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("at least one item");

        verify(productRepository, never()).findById(any());
    }

    @Test
    void validateCreateOrderRequest_withInactiveProduct_throwsInactiveProductException() {
        // Arrange
        Product inactiveProduct = new Product();
        inactiveProduct.setId(1L);
        inactiveProduct.setName("Old Item");
        inactiveProduct.setIsActive(false);

        OrderItemRequest itemRequest = new OrderItemRequest(1L, 1, null);
        CreateOrderRequest request = new CreateOrderRequest(5, List.of(itemRequest));
        
        when(productRepository.findById(1L)).thenReturn(Optional.of(inactiveProduct));

        // Act & Assert
        assertThatThrownBy(() -> orderValidator.validateCreateOrderRequest(request))
                .isInstanceOf(InactiveProductException.class);

        verify(productRepository).findById(1L);
    }

    @Test
    void validateCreateOrderRequest_withMultipleItems_validateEachProduct() {
        // Arrange
        Product product1 = new Product();
        product1.setId(1L);
        product1.setIsActive(true);

        Product product2 = new Product();
        product2.setId(2L);
        product2.setIsActive(true);

        OrderItemRequest item1 = new OrderItemRequest(1L, 2, "Sin cebolla");
        OrderItemRequest item2 = new OrderItemRequest(2L, 1, null);
        CreateOrderRequest request = new CreateOrderRequest(5, List.of(item1, item2));
        
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product2));

        // Act & Assert
        assertThatCode(() -> orderValidator.validateCreateOrderRequest(request))
                .doesNotThrowAnyException();

        verify(productRepository).findById(1L);
        verify(productRepository).findById(2L);
    }

    @Test
    void validateCreateOrderRequest_withMissingProduct_throwsProductNotFoundException() {
        // Arrange
        OrderItemRequest itemRequest = new OrderItemRequest(999L, 1, null);
        CreateOrderRequest request = new CreateOrderRequest(5, List.of(itemRequest));
        
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderValidator.validateCreateOrderRequest(request))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productRepository).findById(999L);
    }

    @Test
    void validateCreateOrderRequest_withFirstProductInvalidStopsValidation() {
        // Arrange
        OrderItemRequest item1 = new OrderItemRequest(1L, 1, null);
        OrderItemRequest item2 = new OrderItemRequest(2L, 1, null);
        CreateOrderRequest request = new CreateOrderRequest(5, List.of(item1, item2));
        
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderValidator.validateCreateOrderRequest(request))
                .isInstanceOf(ProductNotFoundException.class);

        // Verify we only checked the first product (loop stops at first error)
        verify(productRepository).findById(1L);
        verify(productRepository, never()).findById(2L);
    }

    @Test
    void validateCreateOrderRequest_withMultipleItemsAndInactiveSecond_throwsError() {
        // Arrange
        Product activeProduct2 = new Product();
        activeProduct2.setId(1L);
        activeProduct2.setIsActive(true);

        Product inactiveProduct2 = new Product();
        inactiveProduct2.setId(2L);
        inactiveProduct2.setIsActive(false);

        OrderItemRequest item1 = new OrderItemRequest(1L, 2, null);
        OrderItemRequest item2 = new OrderItemRequest(2L, 1, "Extra");
        CreateOrderRequest request = new CreateOrderRequest(5, List.of(item1, item2));
        
        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct2));
        when(productRepository.findById(2L)).thenReturn(Optional.of(inactiveProduct2));

        // Act & Assert
        assertThatThrownBy(() -> orderValidator.validateCreateOrderRequest(request))
                .isInstanceOf(InactiveProductException.class);

        verify(productRepository).findById(1L);
        verify(productRepository).findById(2L);
    }

    @Test
    void validateCreateOrderRequest_withValidDataAllBranches_completesSuccessfully() {
        // This test exercises all successful paths through the validator
        // Arrange
        OrderItemRequest itemRequest = new OrderItemRequest(1L, 5, "Special instructions");
        CreateOrderRequest request = new CreateOrderRequest(6, List.of(itemRequest));
        
        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct));

        // Act & Assert
        assertThatCode(() -> orderValidator.validateCreateOrderRequest(request))
                .doesNotThrowAnyException();

        verify(productRepository).findById(1L);
    }
}
