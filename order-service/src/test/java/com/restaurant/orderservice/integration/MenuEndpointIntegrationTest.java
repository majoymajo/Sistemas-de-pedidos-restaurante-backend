package com.restaurant.orderservice.integration;

import com.restaurant.orderservice.application.port.out.OrderPlacedEventPublisherPort;
import com.restaurant.orderservice.entity.Product;
import com.restaurant.orderservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for GET /menu endpoint.
 * Covers TEST_PLAN_V3 scenarios INT-MENU-01 through INT-MENU-03.
 *
 * @see docs/week-3-review/TEST_PLAN_V3.md §2.7
 */
@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestWebConfig.class)
class MenuEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @MockBean
    private OrderPlacedEventPublisherPort eventPublisherPort;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    private Product createProduct(String name, String category, BigDecimal price, boolean active) {
        Product product = new Product();
        product.setName(name);
        product.setDescription("Descripción de " + name);
        product.setPrice(price);
        product.setCategory(category);
        product.setImageUrl("https://example.com/" + name.toLowerCase().replace(" ", "-") + ".jpg");
        product.setIsActive(active);
        return productRepository.save(product);
    }

    // ── INT-MENU-01: Menú con productos activos ─────────────────────────

    @Test
    @DisplayName("INT-MENU-01: GET /menu retorna solo productos activos con isActive=true")
    void getMenu_returnsOnlyActiveProducts() throws Exception {
        // Arrange
        createProduct("Hamburguesa", "principales", BigDecimal.valueOf(15.50), true);
        createProduct("Pizza", "principales", BigDecimal.valueOf(20.00), true);
        createProduct("Old Soup", "otros", BigDecimal.valueOf(5.00), false);

        // Act & Assert
        mockMvc.perform(get("/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].isActive", everyItem(is(true))))
                .andExpect(header().string("Content-Type", containsString("application/json")));
    }

    // ── INT-MENU-02: Estructura completa del producto ───────────────────

    @Test
    @DisplayName("INT-MENU-02: GET /menu — cada producto contiene id, name, description, price, category, imageUrl, isActive")
    void getMenu_productContainsAllFields() throws Exception {
        // Arrange
        createProduct("Hamburguesa", "principales", BigDecimal.valueOf(15.50), true);

        // Act & Assert
        mockMvc.perform(get("/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].description").exists())
                .andExpect(jsonPath("$[0].price").exists())
                .andExpect(jsonPath("$[0].category").exists())
                .andExpect(jsonPath("$[0].imageUrl").exists())
                .andExpect(jsonPath("$[0].isActive").exists());
    }

    // ── INT-MENU-03: Productos inactivos excluidos ──────────────────────

    @Test
    @DisplayName("INT-MENU-03: GET /menu excluye productos con isActive=false")
    void getMenu_excludesInactiveProducts() throws Exception {
        // Arrange
        Product active = createProduct("Hamburguesa", "principales", BigDecimal.valueOf(15.50), true);
        createProduct("Deprecated Item", "otros", BigDecimal.valueOf(5.00), false);

        // Act & Assert
        mockMvc.perform(get("/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Hamburguesa")));
    }

    // ── Additional: Lista vacía retorna 200 con [] (HDU-02) ─────────────

    @Test
    @DisplayName("INT-MENU: GET /menu sin productos activos retorna 200 con arreglo vacío")
    void getMenu_noActiveProducts_returnsEmptyArray() throws Exception {
        // Arrange — only inactive products
        createProduct("Old Item", "otros", BigDecimal.valueOf(5.00), false);

        // Act & Assert
        mockMvc.perform(get("/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)))
                .andExpect(content().json("[]"));
    }
}
