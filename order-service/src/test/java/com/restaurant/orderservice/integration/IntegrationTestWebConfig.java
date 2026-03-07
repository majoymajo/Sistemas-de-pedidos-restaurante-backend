package com.restaurant.orderservice.integration;

import com.restaurant.orderservice.security.KitchenSecurityInterceptor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Shared test configuration that replaces the production {@code WebConfig} bean.
 * <p>
 * The production WebConfig reads CORS origins from System.getenv(), which
 * throws IllegalStateException if neither CORS_ALLOWED_ORIGINS nor
 * CORS_ALLOWED_ORIGIN_PATTERNS is defined. In test contexts, this replacement
 * allows all origins and preserves the kitchen security interceptor chain.
 * <p>
 * Usage: add {@code @Import(IntegrationTestWebConfig.class)} to integration tests
 * alongside {@code spring.main.allow-bean-definition-overriding=true}.
 */
@TestConfiguration
public class IntegrationTestWebConfig {

    @Bean("webConfig")
    public WebMvcConfigurer webConfig(KitchenSecurityInterceptor interceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }

            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor).addPathPatterns("/**");
            }
        };
    }
}
