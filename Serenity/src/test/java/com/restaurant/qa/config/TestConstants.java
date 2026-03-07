package com.restaurant.qa.config;

/**
 * Constantes utilizadas a lo largo de las pruebas.
 * Centraliza valores que podrían cambiar entre entornos.
 */
public final class TestConstants {

    private TestConstants() {
        // Utility class - no instantiation
    }

    // ========================================================================
    // Endpoints
    // ========================================================================
    public static final String ORDERS_ENDPOINT = "/orders";
    public static final String PRODUCTS_ENDPOINT = "/products";
    public static final String REPORTS_ENDPOINT = "/reports";

    // ========================================================================
    // Content Types
    // ========================================================================
    public static final String CONTENT_TYPE_JSON = "application/json";

    // ========================================================================
    // Business Rules - Table IDs
    // ========================================================================
    public static final int MIN_TABLE_ID = 1;
    public static final int MAX_TABLE_ID = 12;

    // ========================================================================
    // Order Statuses
    // ========================================================================
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_IN_PREPARATION = "IN_PREPARATION";
    public static final String STATUS_READY = "READY";

    // ========================================================================
    // HTTP Headers
    // ========================================================================
    public static final String HEADER_LOCATION = "Location";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    // ========================================================================
    // Timeouts (milliseconds)
    // ========================================================================
    public static final int DEFAULT_TIMEOUT = 5000;
    public static final int LONG_TIMEOUT = 30000;
}
