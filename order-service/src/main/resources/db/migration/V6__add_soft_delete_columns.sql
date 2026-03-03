-- Migration: Add soft delete columns to orders table
-- 
-- Cumple con Copilot Instructions:
-- - Sección 4: Security - Destructive Operations
-- - "Implementar soft delete (campo is_deleted, deleted_at, etc.)"
--
-- This migration adds audit fields to support soft delete functionality:
-- - deleted: boolean flag indicating if the order is deleted
-- - deleted_at: timestamp when the order was deleted (for audit)
--
-- Orders will never be physically deleted from the database.

ALTER TABLE orders
ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE orders
ADD COLUMN deleted_at TIMESTAMP NULL;

-- Create index for better query performance on active orders
CREATE INDEX idx_orders_deleted ON orders(deleted);

-- Comment on columns for documentation
COMMENT ON COLUMN orders.deleted IS 'Soft delete flag. True if order is logically deleted.';
COMMENT ON COLUMN orders.deleted_at IS 'Timestamp when order was soft-deleted. Null if not deleted.';
