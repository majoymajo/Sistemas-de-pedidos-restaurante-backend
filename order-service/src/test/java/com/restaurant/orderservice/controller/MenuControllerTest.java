package com.restaurant.orderservice.controller;

import com.restaurant.orderservice.dto.ProductResponse;
import com.restaurant.orderservice.service.MenuService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuControllerTest {

    @Mock
    private MenuService menuService;

    @InjectMocks
    private MenuController menuController;

    private List<ProductResponse> sampleProducts;

    @BeforeEach
    void setUp() {
        sampleProducts = Arrays.asList(
                ProductResponse.builder()
                        .id(1L)
                        .name("Empanadas criollas")
                        .description("Empanadas de carne con salsa casera.")
                        .price(new BigDecimal("450.00"))
                        .category("entradas")
                        .imageUrl("https://images.example/empanadas.jpg")
                        .isActive(true)
                        .build(),
                ProductResponse.builder()
                        .id(5L)
                        .name("Bife de chorizo")
                        .description("Corte premium con papas rusticas.")
                        .price(new BigDecimal("1850.00"))
                        .category("principales")
                        .imageUrl("https://images.example/bife.jpg")
                        .isActive(true)
                        .build()
        );
    }

    @Test
    void getMenu_returnsActiveProducts() {
        when(menuService.getActiveProducts()).thenReturn(sampleProducts);

        ResponseEntity<List<ProductResponse>> response = menuController.getMenu();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).isEqualTo(sampleProducts);
    }

    @Test
    void getMenu_returnsEmptyListWhenNoActiveProducts() {
        when(menuService.getActiveProducts()).thenReturn(Arrays.asList());

        ResponseEntity<List<ProductResponse>> response = menuController.getMenu();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();
    }
}
