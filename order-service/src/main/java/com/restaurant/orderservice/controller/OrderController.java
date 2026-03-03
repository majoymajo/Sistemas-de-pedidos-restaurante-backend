package com.restaurant.orderservice.controller;

import com.restaurant.orderservice.dto.CreateOrderRequest;
import com.restaurant.orderservice.dto.DeleteAllOrdersResponse;
import com.restaurant.orderservice.dto.DeleteOrderResponse;
import com.restaurant.orderservice.dto.ErrorResponse;
import com.restaurant.orderservice.dto.OrderResponse;
import com.restaurant.orderservice.dto.UpdateStatusRequest;
import com.restaurant.orderservice.enums.OrderStatus;
import com.restaurant.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for order operations.
 * 
 * Provides endpoints for creating, retrieving, filtering, and updating orders.
 * Handles order management operations for restaurant staff through a REST API.
 * 
 * Validates Requirements: 2.1, 4.1, 5.1, 6.1
 */
@RestController
@RequestMapping("/orders")
@Tag(name = "Orders", description = "Order management endpoints for creating, retrieving, filtering, and updating orders")
public class OrderController {
    
    private final OrderService orderService;
    
    /**
     * Constructor for OrderController.
     * 
     * @param orderService Service for order operations
     */
    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    /**
     * POST /orders endpoint to create a new order.
     * 
     * Creates a new order with the specified table ID and items.
     * Validates that all products exist and are active before creating the order.
     * Publishes an order.placed event to RabbitMQ after successful creation.
     * 
     * @param request CreateOrderRequest containing tableId and list of items
     * @return ResponseEntity with 201 Created status and OrderResponse
     * 
     * Validates Requirements:
     * - 2.1: Order Service exposes POST /orders endpoint accepting tableId and items
     */
    @PostMapping
    @Operation(
            summary = "Create a new order",
            description = "Creates a new order for a specific table with the provided items. " +
                    "Validates that all products exist and are active. " +
                    "Publishes an order.placed event to RabbitMQ for asynchronous processing."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Order created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OrderResponse.class),
                            examples = @ExampleObject(
                                    name = "Created Order",
                                    value = """
                                            {
                                              "id": "550e8400-e29b-41d4-a716-446655440000",
                                              "tableId": 5,
                                              "status": "PENDING",
                                              "items": [
                                                {
                                                  "id": 1,
                                                  "productId": 1,
                                                  "quantity": 2,
                                                  "note": "Sin cebolla"
                                                },
                                                {
                                                  "id": 2,
                                                  "productId": 3,
                                                  "quantity": 1,
                                                  "note": null
                                                }
                                              ],
                                              "createdAt": "2024-01-15T10:30:00",
                                              "updatedAt": "2024-01-15T10:30:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid input data",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Invalid Table ID",
                                            value = """
                                                    {
                                                      "timestamp": "2024-01-15T10:30:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "Table ID must be positive"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Empty Items",
                                            value = """
                                                    {
                                                      "timestamp": "2024-01-15T10:30:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "Order must contain at least one item"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found - Product does not exist or is inactive",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Product Not Found",
                                    value = """
                                            {
                                              "timestamp": "2024-01-15T10:30:00",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "Product not found with id: 999"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Service Unavailable - Database o broker de mensajeria no accesible",
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
                                              "message": "Message broker is temporarily unavailable"
                                            }
                                            """
                            )
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Order creation request with table ID and items",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CreateOrderRequest.class),
                    examples = @ExampleObject(
                            name = "Order Request",
                            value = """
                                    {
                                      "tableId": 5,
                                      "items": [
                                        {
                                          "productId": 1,
                                          "quantity": 2,
                                          "note": "Sin cebolla"
                                        },
                                        {
                                          "productId": 3,
                                          "quantity": 1,
                                          "note": null
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse orderResponse = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Location", "/orders/" + orderResponse.getId())
                .body(orderResponse);
    }
    
    /**
     * GET /orders/{id} endpoint to retrieve an order by its ID.
     * 
     * Returns complete order details including all items, status, and timestamps.
     * 
     * @param id UUID of the order to retrieve
     * @return ResponseEntity with 200 OK status and OrderResponse
     * 
     * Validates Requirements:
     * - 4.1: Order Service exposes GET /orders/{id} endpoint
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Get order by ID",
            description = "Retrieves complete order details including all items, status, and timestamps."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Order retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OrderResponse.class),
                            examples = @ExampleObject(
                                    name = "Order Details",
                                    value = """
                                            {
                                              "id": "550e8400-e29b-41d4-a716-446655440000",
                                              "tableId": 5,
                                              "status": "IN_PREPARATION",
                                              "items": [
                                                {
                                                  "id": 1,
                                                  "productId": 1,
                                                  "quantity": 2,
                                                  "note": "Sin cebolla"
                                                }
                                              ],
                                              "createdAt": "2024-01-15T10:30:00",
                                              "updatedAt": "2024-01-15T10:35:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid UUID format",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Invalid UUID",
                                    value = """
                                            {
                                              "timestamp": "2024-01-15T10:30:00",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Invalid UUID format"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found - Order does not exist",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Order Not Found",
                                    value = """
                                            {
                                              "timestamp": "2024-01-15T10:30:00",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "Order not found with id: 550e8400-e29b-41d4-a716-446655440000"
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<OrderResponse> getOrderById(
            @Parameter(description = "UUID of the order to retrieve", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable("id") UUID id) {
        OrderResponse orderResponse = orderService.getOrderById(id);
        return ResponseEntity.ok(orderResponse);
    }
    
    /**
     * GET /orders endpoint to retrieve orders, optionally filtered by status.
     * 
     * If status parameter is provided, returns only orders with that status.
     * If status parameter is omitted, returns all orders.
     * 
     * @param status Optional OrderStatus to filter by (can be null)
     * @return ResponseEntity with 200 OK status and list of OrderResponse
     * 
     * Validates Requirements:
     * - 5.1: Order Service exposes GET /orders with optional status parameter
     */
    @GetMapping
    @Operation(
            summary = "Get all orders or filter by status",
            description = "Retrieves all orders or filters by status (PENDING, IN_PREPARATION, READY). " +
                    "If status parameter is omitted, returns all orders."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Orders retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = OrderResponse.class)),
                            examples = @ExampleObject(
                                    name = "Orders List",
                                    value = """
                                            [
                                              {
                                                "id": "550e8400-e29b-41d4-a716-446655440000",
                                                "tableId": 5,
                                                "status": "PENDING",
                                                "items": [
                                                  {
                                                    "id": 1,
                                                    "productId": 1,
                                                    "quantity": 2,
                                                    "note": "Sin cebolla"
                                                  }
                                                ],
                                                "createdAt": "2024-01-15T10:30:00",
                                                "updatedAt": "2024-01-15T10:30:00"
                                              },
                                              {
                                                "id": "660e8400-e29b-41d4-a716-446655440001",
                                                "tableId": 3,
                                                "status": "IN_PREPARATION",
                                                "items": [
                                                  {
                                                    "id": 2,
                                                    "productId": 2,
                                                    "quantity": 1,
                                                    "note": null
                                                  }
                                                ],
                                                "createdAt": "2024-01-15T10:25:00",
                                                "updatedAt": "2024-01-15T10:28:00"
                                              }
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid status value",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Invalid Status",
                                    value = """
                                            {
                                              "timestamp": "2024-01-15T10:30:00",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Invalid status value. Must be one of: PENDING, IN_PREPARATION, READY"
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<List<OrderResponse>> getOrders(
            @Parameter(description = "Optional status filter (comma-separated). Example: PENDING,IN_PREPARATION,READY",
                    required = false,
                    example = "PENDING,IN_PREPARATION,READY")
            @RequestParam(name = "status", required = false) List<OrderStatus> status) {
        List<OrderResponse> orders = orderService.getOrders(status);
        return ResponseEntity.ok(orders);
    }
    
    /**
     * PATCH /orders/{id}/status endpoint to update the status of an order.
     * 
     * Updates the order status and automatically updates the updatedAt timestamp.
     * 
     * @param id UUID of the order to update
     * @param request UpdateStatusRequest containing the new status
     * @return ResponseEntity with 200 OK status and OrderResponse
     * 
     * Validates Requirements:
     * - 6.1: Order Service exposes PATCH /orders/{id}/status endpoint
     */
    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Update order status",
            description = "Updates the status of an existing order. " +
                    "Valid status values are: PENDING, IN_PREPARATION, READY. " +
                    "The updatedAt timestamp is automatically updated."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Order status updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OrderResponse.class),
                            examples = @ExampleObject(
                                    name = "Updated Order",
                                    value = """
                                            {
                                              "id": "550e8400-e29b-41d4-a716-446655440000",
                                              "tableId": 5,
                                              "status": "READY",
                                              "items": [
                                                {
                                                  "id": 1,
                                                  "productId": 1,
                                                  "quantity": 2,
                                                  "note": "Sin cebolla"
                                                }
                                              ],
                                              "createdAt": "2024-01-15T10:30:00",
                                              "updatedAt": "2024-01-15T10:45:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Invalid status value",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Invalid Status",
                                    value = """
                                            {
                                              "timestamp": "2024-01-15T10:30:00",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "Invalid status value. Must be one of: PENDING, IN_PREPARATION, READY"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found - Order does not exist",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Order Not Found",
                                    value = """
                                            {
                                              "timestamp": "2024-01-15T10:30:00",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "Order not found with id: 550e8400-e29b-41d4-a716-446655440000"
                                            }
                                            """
                            )
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Status update request",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UpdateStatusRequest.class),
                    examples = @ExampleObject(
                            name = "Status Update",
                            value = """
                                    {
                                      "status": "READY"
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @Parameter(description = "UUID of the order to update", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateStatusRequest request) {
        OrderResponse orderResponse = orderService.updateOrderStatus(id, request.getStatus());
        return ResponseEntity.ok(orderResponse);
    }

    /**
     * DELETE /orders/{id} endpoint to delete one order.
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete one order",
            description = "Deletes the specified order. This endpoint is protected for kitchen operations."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Order deleted successfully"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found - Order does not exist",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public ResponseEntity<DeleteOrderResponse> deleteOrder(
            @Parameter(description = "UUID of the order to delete", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable("id") UUID id) {
        DeleteOrderResponse result = orderService.deleteOrder(id);
        return ResponseEntity.ok(result);
    }

    /**
     * DELETE /orders endpoint to delete all orders.
     */
    @DeleteMapping
    @Operation(
            summary = "Delete all orders",
            description = "Deletes all orders. Useful to reset the kitchen board and table availability."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "All orders deleted successfully")
    })
    public ResponseEntity<?> deleteAllOrders(
            @RequestHeader(value = "X-Confirm-Destructive", required = false) String confirmHeader) {
        if (confirmHeader == null || !"true".equalsIgnoreCase(confirmHeader)) {
            ErrorResponse error = ErrorResponse.builder()
                    .timestamp(java.time.LocalDateTime.now())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .error("Bad Request")
                    .message("Header X-Confirm-Destructive: true is required for bulk delete operations")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        DeleteAllOrdersResponse result = orderService.deleteAllOrders();
        return ResponseEntity.ok(result);
    }
}
