package com.restaurant.orderservice.service;

import com.restaurant.orderservice.dto.ProductResponse;
import com.restaurant.orderservice.entity.Product;
import com.restaurant.orderservice.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Branch coverage tests for MenuService.
 * Tests edge cases and stream operations.
 */
@ExtendWith(MockitoExtension.class)
class MenuServiceBranchTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private MenuService menuService;

    @Test
    void getActiveProducts_withEmptyRepository_returnsEmptyList() {
        // Arrange
        when(productRepository.findByIsActiveTrueOrderByIdAsc()).thenReturn(new ArrayList<>());

        // Act
        List<ProductResponse> result = menuService.getActiveProducts();

        // Assert
        assertThat(result).isEmpty();
        verify(productRepository).findByIsActiveTrueOrderByIdAsc();
    }

    @Test
    void getActiveProducts_withSingleProduct_returnsSingleProductResponse() {
        // Arrange
        Product product = new Product();
        product.setId(1L);
        product.setName("Hamburguesa");
        product.setDescription("Hamburguesa clásica");
        product.setPrice(BigDecimal.valueOf(15.50));
        product.setCategory("principales");
        product.setImageUrl("http://example.com/hamburguesa.jpg");
        product.setIsActive(true);

        when(productRepository.findByIsActiveTrueOrderByIdAsc()).thenReturn(List.of(product));

        // Act
        List<ProductResponse> result = menuService.getActiveProducts();

        // Assert
        assertThat(result).hasSize(1);
        ProductResponse response = result.get(0);
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Hamburguesa");
        assertThat(response.getPrice()).isEqualTo(BigDecimal.valueOf(15.50));
        assertThat(response.getImageUrl()).isEqualTo("http://example.com/hamburguesa.jpg");
        assertThat(response.getIsActive()).isTrue();
    }

    @Test
    void getActiveProducts_withMultipleProducts_returnsAllAsResponses() {
        // Arrange
        Product product1 = new Product();
        product1.setId(1L);
        product1.setName("Hamburguesa");
        product1.setDescription("Hamburguesa clásica");
        product1.setPrice(BigDecimal.valueOf(15.50));
        product1.setCategory("principales");
        product1.setImageUrl("http://example.com/hamburguesa.jpg");
        product1.setIsActive(true);

        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Pizza Margherita");
        product2.setDescription("Pizza clásica");
        product2.setPrice(BigDecimal.valueOf(18.00));
        product2.setCategory("principales");
        product2.setImageUrl("http://example.com/pizza.jpg");
        product2.setIsActive(true);

        Product product3 = new Product();
        product3.setId(3L);
        product3.setName("Café");
        product3.setDescription("Café expreso");
        product3.setPrice(BigDecimal.valueOf(3.50));
        product3.setCategory("bebidas");
        product3.setImageUrl("http://example.com/cafe.jpg");
        product3.setIsActive(true);

        when(productRepository.findByIsActiveTrueOrderByIdAsc())
                .thenReturn(List.of(product1, product2, product3));

        // Act
        List<ProductResponse> result = menuService.getActiveProducts();

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getName()).isEqualTo("Hamburguesa");
        assertThat(result.get(1).getName()).isEqualTo("Pizza Margherita");
        assertThat(result.get(2).getName()).isEqualTo("Café");
    }

    @Test
    void getActiveProducts_mappingPreservesAllFields() {
        // Arrange
        Product product = new Product();
        product.setId(5L);
        product.setName("Ensalada César");
        product.setDescription("Ensalada fresca con pollo");
        product.setPrice(BigDecimal.valueOf(12.99));
        product.setCategory("ensaladas");
        product.setImageUrl("http://example.com/caesar.jpg");
        product.setIsActive(true);

        when(productRepository.findByIsActiveTrueOrderByIdAsc()).thenReturn(List.of(product));

        // Act
        List<ProductResponse> result = menuService.getActiveProducts();

        // Assert
        assertThat(result).hasSize(1);
        ProductResponse response = result.get(0);
        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getName()).isEqualTo("Ensalada César");
        assertThat(response.getDescription()).isEqualTo("Ensalada fresca con pollo");
        assertThat(response.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(12.99));
        assertThat(response.getCategory()).isEqualTo("ensaladas");
        assertThat(response.getIsActive()).isTrue();
    }
}
