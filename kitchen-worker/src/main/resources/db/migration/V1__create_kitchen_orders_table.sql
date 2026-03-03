CREATE TABLE IF NOT EXISTS kitchen_orders (
    id UUID PRIMARY KEY,
    table_id INTEGER NOT NULL CHECK (table_id > 0),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_kitchen_orders_status
    ON kitchen_orders (status);
