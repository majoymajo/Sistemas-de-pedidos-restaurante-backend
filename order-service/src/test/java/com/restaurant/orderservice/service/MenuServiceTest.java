package com.restaurant.orderservice.service;

import com.restaurant.orderservice.dto.ProductResponse;
import com.restaurant.orderservice.entity.Product;
import com.restaurant.orderservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private MenuService menuService;

    private Product activeProduct1;
    private Product activeProduct2;

    @BeforeEach
    void setUp() {
        activeProduct1 = new Product(1L, "Empanadas criollas", "Empanadas de carne con salsa casera.", true);
        activeProduct1.setPrice(new BigDecimal("450.00"));
        activeProduct1.setCategory("entradas");
        activeProduct1.setImageUrl("https://images.example/empanadas.jpg");

        activeProduct2 = new Product(2L, "Bife de chorizo", "Corte premium con papas rusticas.", true);
        activeProduct2.setPrice(new BigDecimal("1850.00"));
        activeProduct2.setCategory("principales");
        activeProduct2.setImageUrl("https://images.example/bife.jpg");
    }

    @Test
    void getActiveProducts_shouldReturnOnlyActiveProducts() {
        List<Product> activeProducts = Arrays.asList(activeProduct1, activeProduct2);
        when(productRepository.findByIsActiveTrueOrderByIdAsc()).thenReturn(activeProducts);

        List<ProductResponse> result = menuService.getActiveProducts();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ProductResponse::getId).containsExactly(1L, 2L);
        assertThat(result).extracting(ProductResponse::getName)
                .containsExactly("Empanadas criollas", "Bife de chorizo");
    }

    @Test
    void getActiveProducts_shouldReturnEmptyListWhenNoActiveProducts() {
        when(productRepository.findByIsActiveTrueOrderByIdAsc()).thenReturn(Collections.emptyList());

        List<ProductResponse> result = menuService.getActiveProducts();

        assertThat(result).isEmpty();
    }

    @Test
    void getActiveProducts_shouldMapProductToProductResponse() {
        List<Product> activeProducts = Collections.singletonList(activeProduct1);
        when(productRepository.findByIsActiveTrueOrderByIdAsc()).thenReturn(activeProducts);

        List<ProductResponse> result = menuService.getActiveProducts();

        assertThat(result).hasSize(1);
        ProductResponse response = result.get(0);
        assertThat(response.getId()).isEqualTo(activeProduct1.getId());
        assertThat(response.getName()).isEqualTo(activeProduct1.getName());
        assertThat(response.getDescription()).isEqualTo(activeProduct1.getDescription());
        assertThat(response.getPrice()).isEqualByComparingTo(activeProduct1.getPrice());
        assertThat(response.getCategory()).isEqualTo(activeProduct1.getCategory());
        assertThat(response.getImageUrl()).isEqualTo(activeProduct1.getImageUrl());
        assertThat(response.getIsActive()).isTrue();
    }

    @Test
    void getActiveProducts_shouldReturnAllProductFields() {
        List<Product> activeProducts = Arrays.asList(activeProduct1, activeProduct2);
        when(productRepository.findByIsActiveTrueOrderByIdAsc()).thenReturn(activeProducts);

        List<ProductResponse> result = menuService.getActiveProducts();

        assertThat(result).hasSize(2);
        result.forEach(productResponse -> {
            assertThat(productResponse.getId()).isNotNull();
            assertThat(productResponse.getName()).isNotBlank();
            assertThat(productResponse.getDescription()).isNotBlank();
            assertThat(productResponse.getPrice()).isNotNull();
            assertThat(productResponse.getCategory()).isNotBlank();
            assertThat(productResponse.getIsActive()).isNotNull();
        });
    }
}
