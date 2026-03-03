package com.restaurant.orderservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration class for OpenAPI/Swagger documentation.
 * 
 * Configures the OpenAPI specification for the Order Service REST API,
 * providing comprehensive API documentation accessible via Swagger UI.
 * 
 * Validates Requirements: 10.1, 10.2
 */
@Configuration
public class OpenAPIConfig {
    
    /**
     * Configures the OpenAPI bean with API metadata.
     * 
     * Defines the API title, version, description, and server information
     * that will be displayed in the Swagger UI documentation.
     * 
     * @return OpenAPI configuration object
     * 
     * Validates Requirements:
     * - 10.1: Order Service exposes Swagger/OpenAPI documentation at /swagger-ui.html
     * - 10.2: Order Service documents all REST endpoints with request/response schemas
     */
    @Bean
    public OpenAPI orderServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Restaurant Order Service API")
                        .version("1.0.0")
                        .description("REST API for managing restaurant orders. " +
                                "This service provides endpoints for menu retrieval, order creation, " +
                                "order retrieval, order filtering by status, and order status updates. " +
                                "Orders are processed asynchronously through RabbitMQ integration.")
                        .contact(new Contact()
                                .name("Restaurant Order System")
                                .email("support@restaurant-order-system.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development server")));
    }
}
