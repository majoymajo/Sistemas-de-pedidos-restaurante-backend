-- =============================================
-- Flyway Migration V1: Report Service Tables
-- Creates the projection tables for the CQRS
-- read model used by report-service.
-- =============================================

CREATE TABLE report_orders (
    id          UUID            PRIMARY KEY,
    table_id    INTEGER         NOT NULL,
    status      VARCHAR(20)     NOT NULL,
    created_at  TIMESTAMP       NOT NULL,
    received_at TIMESTAMP       NOT NULL
);

CREATE TABLE report_order_items (
    id           BIGSERIAL       PRIMARY KEY,
    order_id     UUID            NOT NULL,
    product_id   BIGINT          NOT NULL,
    product_name VARCHAR(255)    NOT NULL,
    quantity     INTEGER         NOT NULL,
    price        NUMERIC(10, 2) NOT NULL,
    CONSTRAINT fk_report_order_items_order
        FOREIGN KEY (order_id) REFERENCES report_orders (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_report_orders_status ON report_orders (status);
CREATE INDEX idx_report_orders_status_created ON report_orders (status, created_at);
CREATE INDEX idx_report_order_items_order_id ON report_order_items (order_id);
