package com.restaurant.orderservice.controller;

import com.restaurant.orderservice.dto.ErrorResponse;
import com.restaurant.orderservice.dto.ProductResponse;
import com.restaurant.orderservice.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for menu operations.
 */
@RestController
@RequestMapping("/menu")
@Tag(name = "Menu", description = "Menu management endpoints for retrieving active products")
public class MenuController {

    private final MenuService menuService;

    @Autowired
    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    @Operation(
            summary = "Get active menu products",
            description = "Retrieves all active products available for ordering."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved active products",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ProductResponse.class)),
                            examples = @ExampleObject(
                                    name = "Active Products",
                                    value = """
                                            [
                                              {
                                                "id": 1,
                                                "name": "Empanadas criollas",
                                                "description": "Empanadas de carne con salsa casera.",
                                                "price": 450,
                                                "category": "entradas",
                                                "imageUrl": "https://images.unsplash.com/photo-1603360946369-dc9bb6258143?w=400&h=300&fit=crop",
                                                "isActive": true
                                              },
                                              {
                                                "id": 5,
                                                "name": "Bife de chorizo",
                                                "description": "Corte premium con papas rusticas.",
                                                "price": 1850,
                                                "category": "principales",
                                                "imageUrl": "https://images.unsplash.com/photo-1558030006-450675393462?w=400&h=300&fit=crop",
                                                "isActive": true
                                              }
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Service unavailable - Database is not accessible",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Service Unavailable",
                                    value = """
                                            {
                                              "timestamp": "2024-01-15T10:30:00",
                                              "status": 503,
                                              "error": "Service Unavailable",
                                              "message": "Database service is temporarily unavailable"
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<List<ProductResponse>> getMenu() {
        List<ProductResponse> activeProducts = menuService.getActiveProducts();
        return ResponseEntity.ok(activeProducts);
    }
}
