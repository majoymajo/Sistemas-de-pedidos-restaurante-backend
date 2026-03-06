package com.restaurant.qa.data;

import com.restaurant.qa.config.TestConstants;
import com.restaurant.qa.models.OrderItem;
import com.restaurant.qa.models.OrderRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder para crear datos de prueba de órdenes.
 * Facilita la creación de payloads válidos e inválidos para diferentes escenarios.
 * 
 * Uso:
 *   OrderRequest order = OrderDataBuilder.aValidOrder()
 *       .forTable(5)
 *       .withItem(1, 2)
 *       .build();
 */
public class OrderDataBuilder {

    private Integer tableId;
    private List<OrderItem> items = new ArrayList<>();

    // ========================================================================
    // Factory Methods
    // ========================================================================

    /**
     * Crea un builder con valores por defecto válidos.
     */
    public static OrderDataBuilder aValidOrder() {
        return new OrderDataBuilder()
                .forTable(5)
                .withItem(1, 2);
    }

    /**
     * Crea un builder para una orden con mesa inválida (fuera de rango).
     */
    public static OrderDataBuilder anInvalidTableOrder() {
        return new OrderDataBuilder()
                .forTable(0)
                .withItem(1, 1);
    }

    /**
     * Crea un builder para una orden vacía (sin items).
     */
    public static OrderDataBuilder anEmptyOrder() {
        return new OrderDataBuilder()
                .forTable(5);
    }

    /**
     * Crea un builder para una orden con producto inexistente.
     */
    public static OrderDataBuilder anOrderWithInvalidProduct() {
        return new OrderDataBuilder()
                .forTable(5)
                .withItem(99999, 1);
    }

    // ========================================================================
    // Builder Methods
    // ========================================================================

    /**
     * Establece el ID de mesa.
     */
    public OrderDataBuilder forTable(int tableId) {
        this.tableId = tableId;
        return this;
    }

    /**
     * Establece una mesa en el límite inferior (1).
     */
    public OrderDataBuilder forMinTable() {
        this.tableId = TestConstants.MIN_TABLE_ID;
        return this;
    }

    /**
     * Establece una mesa en el límite superior (12).
     */
    public OrderDataBuilder forMaxTable() {
        this.tableId = TestConstants.MAX_TABLE_ID;
        return this;
    }

    /**
     * Establece una mesa debajo del límite (0).
     */
    public OrderDataBuilder forTableBelowMin() {
        this.tableId = TestConstants.MIN_TABLE_ID - 1;
        return this;
    }

    /**
     * Establece una mesa arriba del límite (13).
     */
    public OrderDataBuilder forTableAboveMax() {
        this.tableId = TestConstants.MAX_TABLE_ID + 1;
        return this;
    }

    /**
     * Agrega un item a la orden.
     */
    public OrderDataBuilder withItem(int productId, int quantity) {
        this.items.add(OrderItem.builder()
                .productId(productId)
                .quantity(quantity)
                .build());
        return this;
    }

    /**
     * Agrega múltiples items a la orden.
     */
    public OrderDataBuilder withItems(List<OrderItem> items) {
        this.items.addAll(items);
        return this;
    }

    /**
     * Limpia todos los items de la orden.
     */
    public OrderDataBuilder withNoItems() {
        this.items.clear();
        return this;
    }

    /**
     * Agrega un item con cantidad inválida (0 o negativa).
     */
    public OrderDataBuilder withInvalidQuantityItem(int productId) {
        this.items.add(OrderItem.builder()
                .productId(productId)
                .quantity(0)
                .build());
        return this;
    }

    // ========================================================================
    // Build
    // ========================================================================

    /**
     * Construye el objeto OrderRequest.
     */
    public OrderRequest build() {
        return OrderRequest.builder()
                .tableId(tableId)
                .items(new ArrayList<>(items))
                .build();
    }

    // ========================================================================
    // Pre-built Scenarios
    // ========================================================================

    /**
     * Orden mínima válida: mesa 1, producto 1, cantidad 1.
     */
    public static OrderRequest minimalValidOrder() {
        return aValidOrder()
                .forMinTable()
                .withNoItems()
                .withItem(1, 1)
                .build();
    }

    /**
     * Orden con múltiples items para pruebas de cálculo de totales.
     */
    public static OrderRequest multiItemOrder() {
        return aValidOrder()
                .forTable(7)
                .withNoItems()
                .withItem(1, 2)
                .withItem(2, 3)
                .withItem(3, 1)
                .build();
    }

    /**
     * Orden para pruebas de límites de mesa (boundary testing).
     */
    public static OrderRequest boundaryTableOrder(int tableId) {
        return aValidOrder()
                .forTable(tableId)
                .build();
    }
}
