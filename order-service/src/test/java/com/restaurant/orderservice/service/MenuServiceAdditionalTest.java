package com.restaurant.orderservice.service;

import com.restaurant.orderservice.dto.ProductResponse;
import com.restaurant.orderservice.entity.Product;
import com.restaurant.orderservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MenuService Additional Tests")
class MenuServiceAdditionalTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private MenuService menuService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product(1L, "Test Product", "Test Description", true);
        product.setPrice(BigDecimal.valueOf(100.00));
        product.setCategory("test-category");
        product.setImageUrl("https://example.com/image.jpg");
    }

    @Test
    @DisplayName("Should return empty list when no active products")
    void getActiveProducts_whenNoActiveProducts_shouldReturnEmptyList() {
        when(productRepository.findByIsActiveTrueOrderByIdAsc()).thenReturn(List.of());

        List<ProductResponse> result = menuService.getActiveProducts();

        assertThat(result).isEmpty();
        verify(productRepository).findByIsActiveTrueOrderByIdAsc();
    }

    @Test
    @DisplayName("Should map single active product correctly")
    void getActiveProducts_withSingleProduct_shouldMapCorrectly() {
        when(productRepository.findByIsActiveTrueOrderByIdAsc()).thenReturn(List.of(product));

        List<ProductResponse> result = menuService.getActiveProducts();

        assertThat(result).hasSize(1);
        ProductResponse response = result.get(0);
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Test Product");
        assertThat(response.getDescription()).isEqualTo("Test Description");
        assertThat(response.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        assertThat(response.getCategory()).isEqualTo("test-category");
        assertThat(response.getImageUrl()).isEqualTo("https://example.com/image.jpg");
    }

    @Test
    @DisplayName("Should map multiple products correctly preserving order")
    void getActiveProducts_withMultipleProducts_shouldPreserveOrder() {
        Product product1 = new Product(1L, "Product 1", "Description 1", true);
        product1.setPrice(BigDecimal.valueOf(10.00));
        product1.setCategory("cat1");
        product1.setImageUrl("https://example.com/1.jpg");

        Product product2 = new Product(2L, "Product 2", "Description 2", true);
        product2.setPrice(BigDecimal.valueOf(20.00));
        product2.setCategory("cat2");
        product2.setImageUrl("https://example.com/2.jpg");

        Product product3 = new Product(3L, "Product 3", "Description 3", true);
        product3.setPrice(BigDecimal.valueOf(30.00));
        product3.setCategory("cat3");
        product3.setImageUrl("https://example.com/3.jpg");

        when(productRepository.findByIsActiveTrueOrderByIdAsc()).thenReturn(List.of(product1, product2, product3));

        List<ProductResponse> result = menuService.getActiveProducts();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getName()).isEqualTo("Product 1");
        assertThat(result.get(1).getName()).isEqualTo("Product 2");
        assertThat(result.get(2).getName()).isEqualTo("Product 3");
    }

    @Test
    @DisplayName("Should map product with large price correctly")
    void getActiveProducts_withLargePrice_shouldMapCorrectly() {
        Product expensiveProduct = new Product(100L, "Expensive Item", "Premium Product", true);
        expensiveProduct.setPrice(BigDecimal.valueOf(9999.99));
        expensiveProduct.setCategory("premium");
        expensiveProduct.setImageUrl("https://example.com/expensive.jpg");

        when(productRepository.findByIsActiveTrueOrderByIdAsc()).thenReturn(List.of(expensiveProduct));

        List<ProductResponse> result = menuService.getActiveProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPrice()).isEqualByComparingTo(BigDecimal.valueOf(9999.99));
    }

    @Test
    @DisplayName("Should map product with null image URL")
    void getActiveProducts_withoutImageUrl_shouldMapWithNull() {
        Product productNoImage = new Product(50L, "No Image Product", "Product without image", true);
        productNoImage.setPrice(BigDecimal.valueOf(50.00));
        productNoImage.setCategory("basic");
        productNoImage.setImageUrl(null);

        when(productRepository.findByIsActiveTrueOrderByIdAsc()).thenReturn(List.of(productNoImage));

        List<ProductResponse> result = menuService.getActiveProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getImageUrl()).isNull();
    }

    @Test
    @DisplayName("Should handle products with very similar names")
    void getActiveProducts_withSimilarNames_shouldMapAll() {
        Product similar1 = new Product(1L, "Pizza", "Desc", true);
        similar1.setPrice(BigDecimal.valueOf(10.00));
        similar1.setCategory("pizza");

        Product similar2 = new Product(2L, "Pizza Grande", "Desc", true);
        similar2.setPrice(BigDecimal.valueOf(15.00));
        similar2.setCategory("pizza");

        Product similar3 = new Product(3L, "Pizza Especial", "Desc", true);
        similar3.setPrice(BigDecimal.valueOf(20.00));
        similar3.setCategory("pizza");

        when(productRepository.findByIsActiveTrueOrderByIdAsc()).thenReturn(List.of(similar1, similar2, similar3));

        List<ProductResponse> result = menuService.getActiveProducts();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(ProductResponse::getName)
                .containsExactly("Pizza", "Pizza Grande", "Pizza Especial");
    }
}
