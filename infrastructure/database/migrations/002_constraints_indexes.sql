ALTER TABLE orders
  ADD CONSTRAINT chk_orders_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED'));

ALTER TABLE order_item
  ADD CONSTRAINT chk_order_item_quantity_positive CHECK (quantity > 0);

ALTER TABLE order_item
  ADD CONSTRAINT fk_order_item_order
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_order_item_product
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE;

CREATE INDEX idx_order_item_order_id ON order_item(order_id);
CREATE INDEX idx_order_item_product_id ON order_item(product_id);
CREATE INDEX idx_orders_status ON orders(status);
