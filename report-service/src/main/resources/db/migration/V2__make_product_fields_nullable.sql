-- =============================================
-- Flyway Migration V2: Make product fields nullable
-- Adapts report_order_items to work with the current
-- order.placed event contract which only sends productId.
-- =============================================

ALTER TABLE report_order_items 
    ALTER COLUMN product_name DROP NOT NULL;

ALTER TABLE report_order_items 
    ALTER COLUMN price DROP NOT NULL;
